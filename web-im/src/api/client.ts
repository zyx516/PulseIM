const apiBase = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export type Session = { userId: string; accessToken: string; refreshToken: string };
export type Friend = { friendUserId: string };
export type FriendRequest = { id: string; fromUserId: string; toUserId: string; message?: string; status?: string };
export type Group = { id: string; name: string; ownerId: string; createdAt: string };
export type GroupMember = { id: string; groupId: string; userId: string; role: 'OWNER' | 'ADMIN' | 'MEMBER'; joinedAt: string };
export type StoredMessage = {
  id: string;
  clientMessageId: string;
  conversationId: string;
  senderId: string;
  toUserId: string;
  content: string;
  sequence: number;
  status: string;
  createdAt: string;
  recalledAt?: string | null;
  replyToMessageId?: string | null;
  replyPreview?: string | null;
  mentions?: string | null;
};
export type Conversation = {
  id: string;
  type: 'DIRECT' | 'GROUP';
  memberA?: string | null;
  memberB?: string | null;
  groupId?: string | null;
  latestSequence: number;
  readSequence: number;
  unreadCount: number;
  pinned: boolean;
  muted: boolean;
  lastMessagePreview?: string | null;
  updatedAt: string;
  createdAt: string;
};
export type DeliveryEvent = { id?: string; messageId?: string; stage: string; detail: string; occurredAt: string };
export type OutboxEvent = { eventId: string; aggregateId: string; status: string; attempts: number; lastError?: string; createdAt: string; publishedAt?: string };
export type Dashboard = { relatedMessages: number; stages: Record<string, number>; recentFailures: DeliveryEvent[] };

type SendOptions = { replyToMessageId?: string; replyPreview?: string; mentions?: string };

async function call<T>(path: string, token: string, method = 'GET', body?: unknown): Promise<T> {
  const response = await fetch(apiBase + path, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      'X-Trace-Id': crypto.randomUUID(),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) throw new Error((await response.text()) || '请求失败');
  if (response.status === 204) return undefined as T;
  return (await response.json()).data as T;
}

async function auth(path: string, account: string, password: string) {
  const response = await fetch(apiBase + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ account, password, deviceId: 'web-' + crypto.randomUUID() }),
  });
  if (!response.ok) throw new Error('账号或密码错误');
  return (await response.json()).data as Session;
}

export const login = (account: string, password: string) => auth('/api/auth/login', account, password);
export const register = (account: string, password: string) => auth('/api/auth/register', account, password);
export const friends = (token: string) => call<Friend[]>('/api/friends', token);
export const requestFriend = (token: string, toUserId: string) => call('/api/friends/requests', token, 'POST', { toUserId, message: '想和你成为好友' });
export const requests = (token: string) => call<FriendRequest[]>('/api/friends/requests', token);
export const accept = (token: string, id: string) => call(`/api/friends/requests/${id}/accept`, token, 'POST');
export const conversations = (token: string) => call<Conversation[]>('/api/conversations', token);
export const createDirect = (token: string, peerUserId: string) => call<Conversation>('/api/conversations/direct', token, 'POST', { peerUserId });
export const createGroupConversation = (token: string, groupId: string) => call<Conversation>('/api/conversations/groups', token, 'POST', { groupId });
export const updateConversationSettings = (token: string, conversationId: string, pinned: boolean, muted: boolean) => call<Conversation>(`/api/conversations/${encodeURIComponent(conversationId)}/settings`, token, 'POST', { pinned, muted });
export const markRead = (token: string, conversationId: string, readSequence: number) => call<Conversation>(`/api/conversations/${encodeURIComponent(conversationId)}/read`, token, 'POST', { readSequence });
export const history = (token: string, conversationId: string, afterSequence = 0) => call<StoredMessage[]>(`/api/messages?conversationId=${encodeURIComponent(conversationId)}&afterSequence=${afterSequence}`, token);
export const sendMessage = (token: string, conversationId: string, toUserId: string, content: string, options: SendOptions = {}) => call<StoredMessage>('/api/messages', token, 'POST', { clientMessageId: crypto.randomUUID(), conversationId, toUserId, content, ...options });
export const recallMessage = (token: string, messageId: string) => call<StoredMessage>(`/api/messages/${messageId}/recall`, token, 'POST');
export const delivery = (token: string, messageId: string) => call<DeliveryEvent[]>(`/api/messages/${messageId}/delivery`, token);
export const outbox = (token: string, messageId: string) => call<OutboxEvent[]>(`/api/messages/${messageId}/outbox`, token);
export const retryOutbox = (token: string, eventId: string) => call<void>(`/api/messages/outbox/${eventId}/retry`, token, 'POST');
export const dashboard = (token: string) => call<Dashboard>('/api/messages/dashboard', token);
export const groups = (token: string) => call<Group[]>('/api/groups', token);
export const createGroup = (token: string, name: string) => call<Group>('/api/groups', token, 'POST', { name });
export const addGroupMember = (token: string, groupId: string, userId: string) => call<GroupMember>(`/api/groups/${groupId}/members`, token, 'POST', { userId, role: 'MEMBER' });
export const groupMembers = (token: string, groupId: string) => call<GroupMember[]>(`/api/groups/${groupId}/members`, token);
export const removeGroupMember = (token: string, groupId: string, userId: string) => call<void>(`/api/groups/${groupId}/members/${userId}`, token, 'DELETE');
export const setGroupRole = (token: string, groupId: string, userId: string, role: 'ADMIN' | 'MEMBER') => call<GroupMember>(`/api/groups/${groupId}/members/${userId}/role`, token, 'POST', { role });