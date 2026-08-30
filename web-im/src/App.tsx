import { useEffect, useRef, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Avatar, Badge, Button, Divider, Empty, Input, Tooltip } from 'antd';
import {
  BellOutlined, CommentOutlined, ContactsOutlined, EllipsisOutlined, LockOutlined,
  LogoutOutlined, MoreOutlined, PaperClipOutlined, SearchOutlined, SendOutlined,
  SettingOutlined, SmileOutlined, UserAddOutlined
} from '@ant-design/icons';
import { history, login, register, type Session } from './api/client';
import { useUiStore } from './store/ui';

type Delivery = 'sending' | 'sent' | 'syncing';
type ChatMessage = { id: string; text: string; from: 'me' | 'other'; at: string; status?: Delivery };
type Contact = { id: string; userId: string; name: string; initial: string; color: string; online: boolean; preview: string; time: string; unread?: number };

const contacts: Contact[] = [
  { id: 'ava', userId: 'u-ava', name: '安然', initial: '安', color: '#8D73EE', online: true, preview: '好的，我收到啦。', time: '10:42', unread: 2 },
  { id: 'noah', userId: 'u-noah', name: '陈诺', initial: '陈', color: '#F28A62', online: true, preview: '原型会议改到下午三点。', time: '09:18' },
  { id: 'mia', userId: 'u-mia', name: '米娅', initial: '米', color: '#29A89B', online: false, preview: '周末见！', time: '昨天' },
  { id: 'studio', userId: 'g-studio', name: 'Pulse 设计小组', initial: 'P', color: '#246BEB', online: true, preview: '林墨：我更新了交互稿。', time: '周一' }
];
const initialMessages: ChatMessage[] = [
  { id: 'm1', text: '早上好！我看到了 PulseIM 的新版本。', from: 'other', at: '10:38' },
  { id: 'm2', text: '嗨，欢迎来体验。会话状态会跟着多端同步更新。', from: 'me', at: '10:40', status: 'sent' },
  { id: 'm3', text: '好的，我收到啦。', from: 'other', at: '10:42' }
];

export function App() {
  const [session, setSession] = useState<Session | null>(() => {
    const stored = sessionStorage.getItem('pulseim.session');
    return stored ? JSON.parse(stored) as Session : null;
  });
  if (!session) return <Login onAuthenticated={setSession} />;
  return <Messenger session={session} onLogout={() => { sessionStorage.removeItem('pulseim.session'); setSession(null); }} />;
}

function Login({ onAuthenticated }: { onAuthenticated: (session: Session) => void }) {
  const [account, setAccount] = useState('pulse');
  const [password, setPassword] = useState('pulse123');
  const [isRegistering, setIsRegistering] = useState(false);
  const mutation = useMutation({
    mutationFn: () => isRegistering ? register(account, password) : login(account, password),
    onSuccess: (session) => { sessionStorage.setItem('pulseim.session', JSON.stringify(session)); onAuthenticated(session); }
  });
  return <main className="login-page">
    <section className="login-intro" aria-label="PulseIM 产品介绍">
      <div className="wordmark"><span className="pulse-mark"><i /><i /><i /></span> PulseIM</div>
      <p className="eyebrow">实时沟通，不打断思路</p>
      <h1>让每一次回应<br /><em>都有清晰的轨迹。</em></h1>
      <p className="intro-copy">为多人协作而设计的即时通讯空间。消息是否送达、设备是否在线，都以安静而明确的方式呈现。</p>
      <div className="signal-card"><span className="signal-orbit"><b /><b /><b /></span><div><strong>Pulse 轨迹</strong><p>发送、同步与在线状态一眼可见</p></div></div>
    </section>
    <section className="login-panel">
      <div className="login-card">
        <p className="eyebrow blue">{isRegistering ? '创建账号' : '欢迎回来'}</p><h2>{isRegistering ? '开始你的 PulseIM' : '登录到你的空间'}</h2><p className="muted">{isRegistering ? '创建账号后会自动进入会话空间。' : '使用账号继续查看会话和联系人。'}</p>
        <label>账号<Input value={account} onChange={(event) => setAccount(event.target.value)} autoComplete="username" /></label>
        <label>密码<Input.Password value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" /></label>
        {mutation.error && <p className="form-error">{mutation.error.message}</p>}
        <Button type="primary" size="large" block loading={mutation.isPending} onClick={() => mutation.mutate()}>{isRegistering ? '创建并进入' : '登录'}</Button>
        <p className="demo-hint">{isRegistering ? <button className="link-button" onClick={() => setIsRegistering(false)}>已有账号？去登录</button> : <><span>演示账号：<code>pulse</code> / <code>pulse123</code></span><button className="link-button" onClick={() => setIsRegistering(true)}>创建新账号</button></>}</p>
        <Divider plain>安全登录</Divider><p className="security-copy"><LockOutlined /> 登录后，连接会在客户端完成身份校验。</p>
      </div>
    </section>
  </main>;
}

function Messenger({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const { view, setView } = useUiStore();
  return <main className="app-shell">
    <nav className="rail" aria-label="主导航">
      <div className="rail-logo"><span className="pulse-mark"><i /><i /><i /></span></div>
      <div className="rail-actions">
        <RailButton icon={<CommentOutlined />} active={view === 'messages'} label="会话" onClick={() => setView('messages')} />
        <RailButton icon={<ContactsOutlined />} active={view === 'contacts'} label="联系人" onClick={() => setView('contacts')} />
        <RailButton icon={<BellOutlined />} active={false} label="通知" badge onClick={() => {}} />
      </div>
      <div className="rail-bottom"><RailButton icon={<SettingOutlined />} active={view === 'settings'} label="设置" onClick={() => setView('settings')} /><Tooltip title="退出登录"><button className="avatar-button" onClick={onLogout} aria-label="退出登录"><Avatar size={34}>P</Avatar></button></Tooltip></div>
    </nav>
    {view === 'messages' && <MessageLayout session={session} />}
    {view === 'contacts' && <Contacts />}
    {view === 'settings' && <Settings session={session} onLogout={onLogout} />}
  </main>;
}

function RailButton({ icon, active, label, badge, onClick }: { icon: React.ReactNode; active: boolean; label: string; badge?: boolean; onClick: () => void }) {
  return <Tooltip title={label} placement="right"><button className={`rail-button ${active ? 'active' : ''}`} onClick={onClick} aria-label={label}>{badge ? <Badge dot offset={[-1, 2]}>{icon}</Badge> : icon}</button></Tooltip>;
}

function MessageLayout({ session }: { session: Session }) {
  const { activeId, setActiveId } = useUiStore();
  const [messages, setMessages] = useState(initialMessages);
  const [connection, setConnection] = useState<'connecting' | 'online' | 'offline'>('connecting');
  const socketRef = useRef<WebSocket | null>(null);
  const visibleContacts = session.userId === 'u-ava'
    ? [{ id: 'pulse', userId: 'u-pulse', name: 'Pulse', initial: 'P', color: '#246BEB', online: true, preview: '欢迎来体验 PulseIM。', time: '刚刚' }]
    : contacts;
  const active = visibleContacts.find((contact) => contact.id === activeId) ?? visibleContacts[0];
  const conversationId = directConversationId(session.userId, active.userId);

  useEffect(() => {
    let reconnectTimer: number | undefined;
    let disposed = false;
    function connect() {
      setConnection('connecting');
      const socket = new WebSocket(import.meta.env.VITE_IM_WS_URL ?? 'ws://localhost:8090/im/ws');
      socketRef.current = socket;
      socket.onopen = () => socket.send(JSON.stringify({ version: '1', requestId: crypto.randomUUID(), command: 'AUTH', data: { token: session.accessToken } }));
    socket.onmessage = (event) => {
      const envelope = JSON.parse(event.data) as { type: string; data: { clientMessageId?: string; content?: string; fromUserId?: string; conversationId?: string } };
      if (envelope.type === 'AUTHENTICATED') setConnection('online');
      if (envelope.type === 'MESSAGE_ACCEPTED' && envelope.data.clientMessageId) setMessages((current) => current.map((item) => item.id === envelope.data.clientMessageId ? { ...item, status: 'sent' } : item));
      if (envelope.type === 'MESSAGE_EVENT' && envelope.data.content && envelope.data.conversationId === conversationId) setMessages((current) => [...current, { id: crypto.randomUUID(), text: envelope.data.content!, from: 'other', at: now() }]);
    };
      socket.onerror = () => socket.close();
      socket.onclose = () => { if (!disposed) { setConnection('offline'); reconnectTimer = window.setTimeout(connect, 2000); } };
    }
    connect();
    return () => { disposed = true; if (reconnectTimer) window.clearTimeout(reconnectTimer); socketRef.current?.close(); };
  }, [session.accessToken, conversationId]);

  useEffect(() => {
    history(session.accessToken, conversationId).then((stored) => {
      if (stored.length) setMessages(stored.map((message) => ({ id: message.id, text: message.content, from: message.senderId === session.userId ? 'me' : 'other', at: formatTime(message.createdAt), status: message.senderId === session.userId ? 'sent' : undefined })));
      else setMessages([]);
    }).catch(() => setMessages([]));
  }, [conversationId, session.accessToken, session.userId]);

  function send(text: string) {
    const clientMessageId = crypto.randomUUID();
    setMessages((current) => [...current, { id: clientMessageId, text, from: 'me', at: now(), status: connection === 'online' ? 'sending' : 'syncing' }]);
    if (socketRef.current?.readyState === WebSocket.OPEN) socketRef.current.send(JSON.stringify({ version: '1', requestId: crypto.randomUUID(), command: 'SEND_MESSAGE', data: { clientMessageId, conversationId, toUserId: active.userId, content: text } }));
  }
  return <>
    <aside className="conversation-pane"><div className="pane-header"><div><p className="eyebrow blue">消息</p><h2>会话</h2></div><button className="icon-button" aria-label="新建会话"><UserAddOutlined /></button></div>
      <Input className="search" prefix={<SearchOutlined />} placeholder="搜索会话" aria-label="搜索会话" />
      <div className="conversation-list">{visibleContacts.map((contact) => <button className={`conversation ${contact.id === active.id ? 'selected' : ''}`} key={contact.id} onClick={() => setActiveId(contact.id)}>
        <PulseAvatar contact={contact} /><span className="conversation-copy"><span><strong>{contact.name}</strong><time>{contact.time}</time></span><small>{contact.preview}</small></span>{contact.unread && <b className="unread">{contact.unread}</b>}</button>)}</div>
    </aside>
    <section className="chat-pane"><header className="chat-header"><div className="contact-title"><PulseAvatar contact={active} /><div><h2>{active.name}</h2><p><span className={`tiny-dot ${connection}`} />{connection === 'online' ? '已连接' : connection === 'connecting' ? '正在连接' : '离线，消息将等待同步'}</p></div></div><div><Tooltip title="会话详情"><button className="icon-button"><EllipsisOutlined /></button></Tooltip></div></header>
      <div className="message-scroller"><div className="day-label">今天</div>{messages.map((message) => <MessageBubble key={message.id} message={message} />)}</div>
      <Composer onSend={send} disabled={false} />
    </section>
    <aside className="detail-pane"><div className="detail-heading"><PulseAvatar contact={active} size={68} /><h3>{active.name}</h3><p>{active.online ? '在线 · 可接收消息' : '暂时离线'}</p></div><Divider /><section className="detail-section"><span>会话偏好</span><button>消息免打扰 <i /></button><button>置顶会话 <i /></button></section><section className="detail-section"><span>成员</span><div className="member-row"><PulseAvatar contact={active} size={30} /><span>{active.name}</span></div></section></aside>
  </>;
}

function PulseAvatar({ contact, size = 42 }: { contact: Contact; size?: number }) { return <span className={`pulse-avatar ${contact.online ? 'online' : ''}`}><Avatar size={size} style={{ background: contact.color }}>{contact.initial}</Avatar></span>; }
function MessageBubble({ message }: { message: ChatMessage }) { return <article className={`message-row ${message.from}`}><div className="bubble">{message.text}<footer><time>{message.at}</time>{message.from === 'me' && <span className={`delivery ${message.status ?? ''}`}>{message.status === 'sending' ? '发送中' : message.status === 'syncing' ? '等待同步' : '已送达'}</span>}</footer></div></article>; }
function Composer({ onSend, disabled }: { onSend: (text: string) => void; disabled: boolean }) { const [value, setValue] = useState(''); const submit = () => { if (!value.trim()) return; onSend(value.trim()); setValue(''); }; return <div className="composer"><div className="compose-tools"><button aria-label="添加表情"><SmileOutlined /></button><button aria-label="添加附件"><PaperClipOutlined /></button></div><Input.TextArea value={value} onChange={(event) => setValue(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); submit(); } }} placeholder="写点什么…" autoSize={{ minRows: 1, maxRows: 4 }} disabled={disabled} /><Button type="primary" shape="circle" icon={<SendOutlined />} onClick={submit} aria-label="发送消息" /></div>; }
function Contacts() { return <section className="empty-view"><div><ContactsOutlined /><h2>联系人</h2><p>好友申请与联系人管理会显示在这里。</p><Button type="primary">添加联系人</Button></div></section>; }
function Settings({ session, onLogout }: { session: Session; onLogout: () => void }) { return <section className="settings-view"><p className="eyebrow blue">安全</p><h1>登录设备</h1><div className="device-card"><span className="device-icon"><LockOutlined /></span><div><strong>当前浏览器</strong><p>{session.userId} · 正在使用</p></div><span className="current">当前设备</span></div><Button danger icon={<LogoutOutlined />} onClick={onLogout}>退出当前设备</Button></section>; }
function directConversationId(left: string, right: string) { return `direct-${[left, right].sort().join('-')}`; }
function formatTime(iso: string) { return new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(iso)); }
function now() { return formatTime(new Date().toISOString()); }
