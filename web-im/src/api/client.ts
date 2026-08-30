const apiBase = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export type Session = { userId: string; accessToken: string; refreshToken: string };

export async function login(account: string, password: string): Promise<Session> {
  return authenticate('/api/auth/login', account, password);
}

export async function register(account: string, password: string): Promise<Session> {
  return authenticate('/api/auth/register', account, password);
}

async function authenticate(path: string, account: string, password: string): Promise<Session> {
  const response = await fetch(`${apiBase}${path}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json', 'X-Trace-Id': crypto.randomUUID() },
    body: JSON.stringify({ account, password, deviceId: 'web' })
  });
  if (!response.ok) throw new Error(path.endsWith('login') ? '账号或密码不正确，请使用演示账号 pulse / pulse123。' : '无法创建账号，请更换账号名称后重试。');
  return (await response.json()).data;
}

export type StoredMessage = { id: string; clientMessageId: string; conversationId: string; senderId: string; toUserId: string; content: string; sequence: number; createdAt: string };

export async function history(accessToken: string, conversationId: string): Promise<StoredMessage[]> {
  const response = await fetch(`${apiBase}/api/messages?conversationId=${encodeURIComponent(conversationId)}`, {
    headers: { Authorization: `Bearer ${accessToken}`, 'X-Trace-Id': crypto.randomUUID() }
  });
  if (!response.ok) throw new Error('无法加载历史消息。');
  return (await response.json()).data;
}
