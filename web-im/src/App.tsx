import { useEffect, useMemo, useState } from 'react';
import {
  Avatar,
  Button,
  Empty,
  Input,
  Modal,
  Segmented,
  Space,
  Tag,
  Tooltip,
  message as toast,
} from 'antd';
import {
  BellOutlined,
  CheckCircleOutlined,
  CrownOutlined,
  DashboardOutlined,
  DeleteOutlined,
  LogoutOutlined,
  MessageOutlined,
  PlusOutlined,
  PushpinOutlined,
  ReloadOutlined,
  RetweetOutlined,
  SendOutlined,
  TeamOutlined,
  UserAddOutlined,
} from '@ant-design/icons';
import {
  accept,
  addGroupMember,
  createDirect,
  createGroup,
  createGroupConversation,
  dashboard,
  delivery,
  friends,
  groupMembers,
  groups,
  history,
  login,
  markRead,
  outbox,
  recallMessage,
  register,
  removeGroupMember,
  requestFriend,
  requests,
  sendMessage,
  setGroupRole,
  type Dashboard,
  type DeliveryEvent,
  type Friend,
  type FriendRequest,
  type Group,
  type GroupMember,
  type OutboxEvent,
  type Session,
  type StoredMessage,
} from './api/client';
import './styles.css';

type ActiveTarget = {
  kind: 'direct' | 'group';
  id: string;
  title: string;
  subtitle: string;
  conversationId: string;
  peerUserId?: string;
  groupId?: string;
};

type TraceState = {
  message: StoredMessage;
  delivery: DeliveryEvent[];
  outbox: OutboxEvent[];
};

const wsUrl = import.meta.env.VITE_WS_URL ?? 'ws://localhost:8090/im/ws';

export function App() {
  const [session, setSession] = useState<Session | null>(() => {
    const saved = sessionStorage.getItem('pulseim-session');
    return saved ? JSON.parse(saved) : null;
  });

  if (!session) {
    return <LoginPanel onDone={(next) => {
      sessionStorage.setItem('pulseim-session', JSON.stringify(next));
      setSession(next);
    }} />;
  }

  return <ChatWorkspace session={session} onLogout={() => {
    sessionStorage.removeItem('pulseim-session');
    setSession(null);
  }} />;
}

function LoginPanel({ onDone }: { onDone: (session: Session) => void }) {
  const [account, setAccount] = useState('pulse');
  const [password, setPassword] = useState('pulse123');
  const [registering, setRegistering] = useState(false);
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    setLoading(true);
    try {
      onDone(await (registering ? register(account, password) : login(account, password)));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : '登录失败');
    } finally {
      setLoading(false);
    }
  };

  return <main className="login-page">
    <section className="login-intro">
      <div className="wordmark"><PulseMark /> PulseIM</div>
      <p className="eyebrow">distributed messaging lab</p>
      <h1>把每条消息的路径都看清楚。</h1>
      <p className="intro-copy">好友、群聊、Outbox、RabbitMQ、Netty 推送和 ACK 轨迹收在一个界面里，演示时不靠口头描述撑场面。</p>
      <div className="signal-card">
        <span className="signal-orbit"><b /><b /><b /></span>
        <div><strong>Pulse trace</strong><p>实时连接、发送中、已持久化和失败状态都用同一套视觉语言表达。</p></div>
      </div>
    </section>
    <section className="login-panel">
      <div className="login-card">
        <PulseMark />
        <h2>{registering ? '创建账号' : '登录 PulseIM'}</h2>
        <p className="muted">用两个账号分别登录不同浏览器，就能演示好友、群聊和链路大屏。</p>
        <Input value={account} onChange={(event) => setAccount(event.target.value)} placeholder="账号" onPressEnter={submit} />
        <Input.Password value={password} onChange={(event) => setPassword(event.target.value)} placeholder="密码" onPressEnter={submit} />
        <Button type="primary" block loading={loading} onClick={submit}>{registering ? '创建并进入' : '登录'}</Button>
        <Button type="link" block onClick={() => setRegistering(!registering)}>{registering ? '返回登录' : '没有账号，创建一个'}</Button>
      </div>
    </section>
  </main>;
}

function ChatWorkspace({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const [tab, setTab] = useState<'chat' | 'group'>('chat');
  const [friendList, setFriendList] = useState<Friend[]>([]);
  const [friendRequests, setFriendRequests] = useState<FriendRequest[]>([]);
  const [groupList, setGroupList] = useState<Group[]>([]);
  const [members, setMembers] = useState<GroupMember[]>([]);
  const [active, setActive] = useState<ActiveTarget | null>(null);
  const [messages, setMessages] = useState<StoredMessage[]>([]);
  const [text, setText] = useState('');
  const [replyTo, setReplyTo] = useState<StoredMessage | null>(null);
  const [dashboardOpen, setDashboardOpen] = useState(false);
  const [dashboardData, setDashboardData] = useState<Dashboard | null>(null);
  const [trace, setTrace] = useState<TraceState | null>(null);
  const [friendModal, setFriendModal] = useState(false);
  const [friendId, setFriendId] = useState('');
  const [groupModal, setGroupModal] = useState(false);
  const [groupName, setGroupName] = useState('');
  const [inviteOpen, setInviteOpen] = useState(false);
  const [inviteId, setInviteId] = useState('');
  const [connection, setConnection] = useState<'connecting' | 'online' | 'offline'>('connecting');

  const currentUserRole = useMemo(() => members.find((member) => member.userId === session.userId)?.role, [members, session.userId]);
  const canManageGroup = currentUserRole === 'OWNER' || currentUserRole === 'ADMIN';

  const loadShell = async () => {
    const [nextFriends, nextRequests, nextGroups] = await Promise.all([
      friends(session.accessToken),
      requests(session.accessToken),
      groups(session.accessToken),
    ]);
    setFriendList(nextFriends);
    setFriendRequests(nextRequests);
    setGroupList(nextGroups);
  };

  const loadMessages = async (target = active) => {
    if (!target) return;
    const next = await history(session.accessToken, target.conversationId);
    setMessages(next);
    if (next.length > 0) {
      const latest = next[next.length - 1].sequence;
      try { await markRead(session.accessToken, target.conversationId, latest); } catch { /* read state is best effort in the demo UI */ }
    }
  };

  const loadMembers = async (target = active) => {
    if (!target?.groupId) {
      setMembers([]);
      return;
    }
    setMembers(await groupMembers(session.accessToken, target.groupId));
  };

  useEffect(() => {
    loadShell().catch((error) => toast.error(error.message));
  }, [session.accessToken]);

  useEffect(() => {
    loadMessages().catch((error) => toast.error(error.message));
    loadMembers().catch((error) => toast.error(error.message));
  }, [active?.conversationId]);

  useEffect(() => {
    let closed = false;
    let socket: WebSocket | null = null;
    let pingTimer = 0;
    let reconnectTimer = 0;

    const connect = () => {
      setConnection('connecting');
      socket = new WebSocket(wsUrl);
      socket.onopen = () => {
        socket?.send(JSON.stringify({ version: '1', requestId: crypto.randomUUID(), command: 'AUTH', data: { token: session.accessToken } }));
        pingTimer = window.setInterval(() => socket?.readyState === WebSocket.OPEN && socket.send(JSON.stringify({ version: '1', requestId: crypto.randomUUID(), command: 'PING', data: {} })), 20000);
      };
      socket.onmessage = (event) => {
        const frame = JSON.parse(event.data);
        if (frame.type === 'AUTHENTICATED' || frame.type === 'PONG') setConnection('online');
        if (frame.type === 'MESSAGE_EVENT') {
          loadShell().catch(() => undefined);
          if (!active || frame.data?.conversationId === active.conversationId) loadMessages().catch(() => undefined);
        }
      };
      socket.onclose = () => {
        window.clearInterval(pingTimer);
        setConnection('offline');
        if (!closed) reconnectTimer = window.setTimeout(connect, 1600);
      };
      socket.onerror = () => setConnection('offline');
    };

    connect();
    return () => {
      closed = true;
      window.clearInterval(pingTimer);
      window.clearTimeout(reconnectTimer);
      socket?.close();
    };
  }, [session.accessToken, active?.conversationId]);

  const openDirect = async (friend: Friend) => {
    const conversation = await createDirect(session.accessToken, friend.friendUserId);
    setActive({ kind: 'direct', id: friend.friendUserId, peerUserId: friend.friendUserId, title: friend.friendUserId, subtitle: '单聊会话', conversationId: conversation.id });
    setTab('chat');
  };

  const openGroup = async (group: Group) => {
    try {
      const conversation = await createGroupConversation(session.accessToken, group.id);
      setActive({ kind: 'group', id: group.id, groupId: group.id, title: group.name, subtitle: `${group.ownerId === session.userId ? '我创建的群聊' : '群聊'}`, conversationId: conversation.id });
      setTab('group');
    } catch (error) {
      toast.warning('群会话正在通过 RabbitMQ 同步，稍后点刷新再进入');
    }
  };

  const submitMessage = async () => {
    if (!active || !text.trim()) return;
    const mentions = active.kind === 'group'
      ? members.filter((member) => text.includes(`@${member.userId}`)).map((member) => member.userId)
      : [];
    const targetUser = active.kind === 'group' ? active.conversationId : active.peerUserId!;
    try {
      const saved = await sendMessage(session.accessToken, active.conversationId, targetUser, text.trim(), {
        replyToMessageId: replyTo?.id,
        replyPreview: replyTo?.content.slice(0, 80),
        mentions: mentions.length ? JSON.stringify(mentions) : undefined,
      });
      setMessages((current) => [...current, saved]);
      setText('');
      setReplyTo(null);
      loadShell().catch(() => undefined);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : '发送失败');
    }
  };

  const openDashboard = async () => {
    setDashboardData(await dashboard(session.accessToken));
    setDashboardOpen(true);
  };

  const openTrace = async (item: StoredMessage) => {
    const [deliveryEvents, outboxEvents] = await Promise.all([
      delivery(session.accessToken, item.id),
      outbox(session.accessToken, item.id),
    ]);
    setTrace({ message: item, delivery: deliveryEvents, outbox: outboxEvents });
  };

  const inviteMember = async () => {
    if (!active?.groupId || !inviteId.trim()) return;
    await addGroupMember(session.accessToken, active.groupId, inviteId.trim());
    toast.success('成员已邀请，群会话会通过 RabbitMQ 同步');
    setInviteId('');
    setInviteOpen(false);
    await Promise.all([loadMembers(), loadShell()]);
  };

  const createNewGroup = async () => {
    if (!groupName.trim()) return;
    const group = await createGroup(session.accessToken, groupName.trim());
    setGroupName('');
    setGroupModal(false);
    await loadShell();
    await openGroup(group);
  };

  return <main className="pulse-workspace">
    <aside className="pulse-sidebar">
      <div className="sidebar-top">
        <div className="brand-lockup"><PulseMark /><span>PulseIM</span></div>
        <Tooltip title="退出登录"><Button shape="circle" icon={<LogoutOutlined />} onClick={onLogout} /></Tooltip>
      </div>
      <Segmented block value={tab} onChange={(value) => setTab(value as 'chat' | 'group')} options={[{ label: '联系人', value: 'chat', icon: <MessageOutlined /> }, { label: '群聊', value: 'group', icon: <TeamOutlined /> }]} />
      <div className="sidebar-actions">
        <Button icon={<UserAddOutlined />} onClick={() => setFriendModal(true)}>添加好友</Button>
        <Button icon={<PlusOutlined />} type="primary" onClick={() => setGroupModal(true)}>创建群聊</Button>
      </div>
      <div className="request-stack">
        {friendRequests.map((request) => <div className="request-card" key={request.id}>
          <span>{request.fromUserId}</span>
          <Button size="small" type="link" onClick={() => accept(session.accessToken, request.id).then(loadShell)}>同意</Button>
        </div>)}
      </div>
      <div className="conversation-list">
        {tab === 'chat' && friendList.map((friend) => <button className={`conversation ${active?.id === friend.friendUserId ? 'selected' : ''}`} key={friend.friendUserId} onClick={() => openDirect(friend)}>
          <Avatar>{friend.friendUserId.slice(0, 2).toUpperCase()}</Avatar>
          <span><strong>{friend.friendUserId}</strong><small>点开建立单聊会话</small></span>
        </button>)}
        {tab === 'group' && groupList.map((group) => <button className={`conversation ${active?.id === group.id ? 'selected' : ''}`} key={group.id} onClick={() => openGroup(group)}>
          <Avatar icon={<TeamOutlined />} />
          <span><strong>{group.name}</strong><small>{group.ownerId === session.userId ? '群主' : group.ownerId}</small></span>
        </button>)}
      </div>
    </aside>

    <section className="chat-surface">
      {active ? <>
        <header className="chat-header">
          <div className="contact-title">
            <Avatar icon={active.kind === 'group' ? <TeamOutlined /> : undefined}>{active.kind === 'direct' ? active.title.slice(0, 2).toUpperCase() : undefined}</Avatar>
            <div><h2>{active.title}</h2><p><span className={`tiny-dot ${connection}`} />{connectionLabel(connection)} · {active.subtitle}</p></div>
          </div>
          <Space>
            {active.kind === 'group' && <Button icon={<UserAddOutlined />} onClick={() => setInviteOpen(true)}>邀请</Button>}
            <Button icon={<ReloadOutlined />} onClick={() => Promise.all([loadShell(), loadMessages(), loadMembers()])}>刷新</Button>
            <Button type="primary" icon={<DashboardOutlined />} onClick={openDashboard}>链路大屏</Button>
          </Space>
        </header>
        <div className="message-scroller">
          {messages.length === 0 && <Empty description="还没有消息" />}
          {messages.map((item) => <MessageBubble key={item.id} item={item} mine={item.senderId === session.userId} onReply={setReplyTo} onTrace={openTrace} onRecall={async () => {
            const recalled = await recallMessage(session.accessToken, item.id);
            setMessages((current) => current.map((message) => message.id === item.id ? recalled : message));
          }} />)}
        </div>
        <footer className="composer">
          {replyTo && <div className="reply-preview"><RetweetOutlined /><span>{replyTo.content}</span><Button type="text" size="small" onClick={() => setReplyTo(null)}>取消</Button></div>}
          {active.kind === 'group' && members.length > 0 && <div className="mention-strip">
            {members.slice(0, 6).map((member) => <Button size="small" key={member.userId} onClick={() => setText((value) => `${value}@${member.userId} `)}>@{member.userId.slice(0, 8)}</Button>)}
          </div>}
          <Input.TextArea value={text} autoSize={{ minRows: 1, maxRows: 4 }} onChange={(event) => setText(event.target.value)} onPressEnter={(event) => { if (!event.shiftKey) { event.preventDefault(); submitMessage(); } }} placeholder="输入消息，Enter 发送，Shift+Enter 换行" />
          <Button type="primary" icon={<SendOutlined />} onClick={submitMessage}>发送</Button>
        </footer>
      </> : <div className="empty-view"><Empty description="选择一个好友或群聊开始" /></div>}
    </section>

    <aside className="detail-pane">
      <GroupDetail active={active} members={members} canManage={canManageGroup} currentUserId={session.userId} token={session.accessToken} onChanged={loadMembers} />
    </aside>

    <FriendModal open={friendModal} value={friendId} onChange={setFriendId} onCancel={() => setFriendModal(false)} onOk={async () => { await requestFriend(session.accessToken, friendId.trim()); toast.success('好友申请已发送'); setFriendId(''); setFriendModal(false); }} />
    <Modal open={groupModal} title="创建群聊" onCancel={() => setGroupModal(false)} onOk={createNewGroup} okText="创建" cancelText="取消">
      <Input value={groupName} onChange={(event) => setGroupName(event.target.value)} placeholder="群名称" />
    </Modal>
    <Modal open={inviteOpen} title="邀请群成员" onCancel={() => setInviteOpen(false)} onOk={inviteMember} okText="邀请" cancelText="取消">
      <Input value={inviteId} onChange={(event) => setInviteId(event.target.value)} placeholder="成员 userId" />
    </Modal>
    <DashboardModal open={dashboardOpen} data={dashboardData} onCancel={() => setDashboardOpen(false)} />
    <TraceModal trace={trace} onCancel={() => setTrace(null)} />
  </main>;
}

function MessageBubble({ item, mine, onReply, onTrace, onRecall }: { item: StoredMessage; mine: boolean; onReply: (message: StoredMessage) => void; onTrace: (message: StoredMessage) => void; onRecall: () => void }) {
  const recalled = item.status === 'RECALLED' || Boolean(item.recalledAt);
  return <div className={`message-row ${mine ? 'me' : ''}`}>
    <div className="bubble">
      {item.replyPreview && <small className="reply-card">↳ {item.replyPreview}</small>}
      <p>{recalled ? '这条消息已撤回' : renderMentionText(item.content)}</p>
      <footer>
        <span>#{item.sequence}</span>
        <span>{new Date(item.createdAt).toLocaleTimeString()}</span>
        <Button type="link" size="small" onClick={() => onReply(item)}>引用</Button>
        <Button type="link" size="small" onClick={() => onTrace(item)}>轨迹</Button>
        {mine && !recalled && <Button type="link" size="small" danger onClick={onRecall}>撤回</Button>}
      </footer>
    </div>
  </div>;
}

function GroupDetail({ active, members, canManage, currentUserId, token, onChanged }: { active: ActiveTarget | null; members: GroupMember[]; canManage: boolean; currentUserId: string; token: string; onChanged: () => Promise<void> }) {
  if (!active?.groupId) {
    return <div className="detail-empty"><MessageOutlined /><h3>会话信息</h3><p>打开群聊后这里会显示成员和管理入口。</p></div>;
  }
  return <div className="group-detail">
    <h3>{active.title}</h3>
    <p>{members.length} 位成员 · {canManage ? '可管理' : '成员视图'}</p>
    <div className="detail-section">
      {members.map((member) => <div className="member-row" key={member.userId}>
        <Avatar size="small">{member.userId.slice(0, 2).toUpperCase()}</Avatar>
        <span>{member.userId}{member.userId === currentUserId ? '（我）' : ''}</span>
        <Tag icon={member.role === 'OWNER' ? <CrownOutlined /> : undefined}>{member.role}</Tag>
        {canManage && member.role !== 'OWNER' && <Space size={2}>
          <Tooltip title="切换管理员"><Button size="small" icon={<PushpinOutlined />} onClick={async () => { await setGroupRole(token, active.groupId!, member.userId, member.role === 'ADMIN' ? 'MEMBER' : 'ADMIN'); await onChanged(); }} /></Tooltip>
          <Tooltip title="移出群聊"><Button size="small" danger icon={<DeleteOutlined />} onClick={async () => { await removeGroupMember(token, active.groupId!, member.userId); await onChanged(); }} /></Tooltip>
        </Space>}
      </div>)}
    </div>
  </div>;
}

function DashboardModal({ open, data, onCancel }: { open: boolean; data: Dashboard | null; onCancel: () => void }) {
  return <Modal width={760} open={open} footer={null} onCancel={onCancel} title="Pulse 链路大屏">
    <div className="dashboard">
      <section className="metric-hero"><span>{data?.relatedMessages ?? 0}</span><p>当前账号相关消息</p></section>
      <div className="stage-grid">
        {Object.entries(data?.stages ?? {}).map(([stage, count]) => <article key={stage}><CheckCircleOutlined /><strong>{stage}</strong><span>{count}</span></article>)}
        {Object.keys(data?.stages ?? {}).length === 0 && <Empty description="暂无链路事件" />}
      </div>
      <h3>最近失败</h3>
      <div className="failure-list">
        {(data?.recentFailures ?? []).map((failure) => <p key={`${failure.stage}-${failure.occurredAt}`}><BellOutlined /> {failure.stage} · {failure.detail}</p>)}
        {(data?.recentFailures ?? []).length === 0 && <p>暂无失败事件</p>}
      </div>
    </div>
  </Modal>;
}

function TraceModal({ trace, onCancel }: { trace: TraceState | null; onCancel: () => void }) {
  return <Modal width={720} open={Boolean(trace)} footer={null} onCancel={onCancel} title="消息投递轨迹">
    {trace && <div className="trace-panel">
      <p className="trace-copy">{trace.message.content}</p>
      <h3>投递事件</h3>
      {trace.delivery.map((event) => <p key={`${event.stage}-${event.occurredAt}`}><strong>{event.stage}</strong><span>{event.detail}</span><time>{new Date(event.occurredAt).toLocaleString()}</time></p>)}
      <h3>Outbox</h3>
      {trace.outbox.map((event) => <p key={event.eventId}><strong>{event.status}</strong><span>{event.eventId}</span><time>{event.attempts} 次</time></p>)}
    </div>}
  </Modal>;
}

function FriendModal({ open, value, onChange, onCancel, onOk }: { open: boolean; value: string; onChange: (value: string) => void; onCancel: () => void; onOk: () => Promise<void> }) {
  return <Modal open={open} title="添加好友" onCancel={onCancel} onOk={onOk} okText="发送申请" cancelText="取消">
    <Input value={value} onChange={(event) => onChange(event.target.value)} placeholder="对方 userId" />
  </Modal>;
}

function PulseMark() {
  return <span className="pulse-mark"><i /><i /><i /></span>;
}

function connectionLabel(connection: 'connecting' | 'online' | 'offline') {
  if (connection === 'online') return '实时在线';
  if (connection === 'connecting') return '连接中';
  return '离线，正在重连';
}

function renderMentionText(content: string) {
  const parts = content.split(/(@[^\s@]+)/g);
  return parts.map((part, index) => part.startsWith('@') ? <Tag color="blue" key={`${part}-${index}`}>{part}</Tag> : <span key={`${part}-${index}`}>{part}</span>);
}