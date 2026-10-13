export type RoleCode = 'ADMIN' | 'INSPECTOR' | 'MAINTAINER';

export type PageResult<T> = {
  pageNum: number;
  pageSize: number;
  total: number;
  records: T[];
};

type ApiResult<T> = {
  code: number;
  message: string;
  data: T;
};

export type UserSummary = {
  id: number;
  username: string;
  displayName: string;
  roleCode: RoleCode;
  enabled: boolean;
  createdAt: string;
};

export type UserDetail = UserSummary & {
  updatedAt: string;
};

export type UserQuery = {
  pageNum: number;
  pageSize: number;
  username?: string;
  roleCode?: RoleCode;
  enabled?: boolean;
};

export type UserCreatePayload = {
  username: string;
  password: string;
  displayName: string;
  roleCode: RoleCode;
};

export type UserUpdatePayload = Pick<UserCreatePayload, 'displayName' | 'roleCode'>;

export type AreaSummary = {
  id: number;
  code: string;
  name: string;
  enabled: boolean;
  equipmentCount: number;
  createdAt: string;
};

export type AreaDetail = AreaSummary & {
  description?: string | null;
  updatedAt: string;
};

export type AreaQuery = {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  enabled?: boolean;
};

export type AreaPayload = {
  code: string;
  name: string;
  description?: string;
};

export type NotificationItem = {
  id: number;
  type: 'WORK_ORDER_ASSIGNED' | 'URGENT_FAULT' | 'VERIFICATION_PASSED' | 'VERIFICATION_REJECTED';
  title: string;
  content: string;
  relatedBizType?: string | null;
  relatedBizId?: number | null;
  read: boolean;
  readAt?: string | null;
  createdAt: string;
};

export type NotificationPushEvent = {
  eventId: string;
  recipientId: number;
  type: NotificationItem['type'];
  title: string;
  content: string;
  relatedBizType?: string | null;
  relatedBizId?: number | null;
  occurredAt: string;
};

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly code?: number,
    public readonly status?: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

const API_BASE_URL =
  (process.env.NEXT_PUBLIC_API_BASE_URL ?? '/api').replace(/\/$/, '');

export function buildQuery(params: Record<string, string | number | boolean | undefined>) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') search.set(key, String(value));
  });
  const query = search.toString();
  return query ? `?${query}` : '';
}

function getToken() {
  if (typeof window === 'undefined') return null;
  return sessionStorage.getItem('smart-inspection-token');
}

function clearExpiredSession() {
  if (typeof window === 'undefined') return;
  sessionStorage.removeItem('smart-inspection-token');
  sessionStorage.removeItem('smart-inspection-user');
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getToken();
  if (!token) {
    throw new ApiError('当前没有有效登录凭证，请退出演示模式后使用真实账号登录', 11004, 401);
  }

  const headers = new Headers(init.headers);
  headers.set('Authorization', `Bearer ${token}`);
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers,
      cache: 'no-store',
    });
  } catch {
    throw new ApiError('无法连接后端服务，请确认 8080 端口的服务已启动');
  }

  const payload = (await response.json().catch(() => null)) as ApiResult<T> | null;
  if (!response.ok || !payload || payload.code !== 0) {
    if (response.status === 401 || payload?.code === 11004) clearExpiredSession();
    throw new ApiError(
      payload?.message || `请求失败（HTTP ${response.status}）`,
      payload?.code,
      response.status,
    );
  }

  return payload.data;
}

export const userApi = {
  page(query: UserQuery) {
    return apiRequest<PageResult<UserSummary>>(`/users${buildQuery(query)}`);
  },
  detail(id: number) {
    return apiRequest<UserDetail>(`/users/${id}`);
  },
  create(payload: UserCreatePayload) {
    return apiRequest<{ id: number }>('/users', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  update(id: number, payload: UserUpdatePayload) {
    return apiRequest<null>(`/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  },
  updateStatus(id: number, enabled: boolean) {
    return apiRequest<null>(`/users/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    });
  },
  resetPassword(id: number, newPassword: string) {
    return apiRequest<null>(`/users/${id}/password/reset`, {
      method: 'POST',
      body: JSON.stringify({ newPassword }),
    });
  },
};

export const areaApi = {
  page(query: AreaQuery) {
    return apiRequest<PageResult<AreaSummary>>(`/areas${buildQuery(query)}`);
  },
  detail(id: number) {
    return apiRequest<AreaDetail>(`/areas/${id}`);
  },
  create(payload: AreaPayload) {
    return apiRequest<{ id: number }>('/areas', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  update(id: number, payload: AreaPayload) {
    return apiRequest<null>(`/areas/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  },
  updateStatus(id: number, enabled: boolean) {
    return apiRequest<null>(`/areas/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    });
  },
  options(keyword?: string) {
    return apiRequest<Array<{ id: number; code: string; name: string }>>(
      `/areas/options${buildQuery({ keyword })}`,
    );
  },
};

export type BusinessRecord = Record<string, unknown> & { id: number };

export const businessApi = {
  page(path: string, query: Record<string, string | number | boolean | undefined>) {
    return apiRequest<PageResult<BusinessRecord>>(`${path}${buildQuery(query)}`);
  },
  detail(path: string, id: number) {
    return apiRequest<BusinessRecord>(`${path}/${id}`);
  },
  options(path: string, query: Record<string, string | number | boolean | undefined> = {}) {
    return apiRequest<Array<BusinessRecord>>(`${path}/options${buildQuery(query)}`);
  },
  create(path: string, payload: Record<string, unknown>) {
    return apiRequest<{ id: number }>(path, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  update(path: string, id: number, payload: Record<string, unknown>) {
    return apiRequest<null>(`${path}/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  },
  updateStatus(path: string, id: number, enabled: boolean) {
    return apiRequest<null>(`${path}/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    });
  },
  patch(path: string, id: number, suffix: string, payload: Record<string, unknown>) {
    return apiRequest<null>(`${path}/${id}/${suffix}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  },
  remove(path: string, id: number) {
    return apiRequest<null>(`${path}/${id}`, { method: 'DELETE' });
  },
  action(path: string, id: number, action: string, payload?: Record<string, unknown>) {
    return apiRequest<BusinessRecord | null>(`${path}/${id}/${action}`, {
      method: 'POST',
      body: payload === undefined ? undefined : JSON.stringify(payload),
    });
  },
};

export const notificationApi = {
  page(pageNum = 1, pageSize = 50) {
    return apiRequest<PageResult<NotificationItem>>(
      `/notifications${buildQuery({ pageNum, pageSize })}`,
    );
  },
  unreadCount() {
    return apiRequest<{ count: number }>('/notifications/unread-count');
  },
  markRead(id: number) {
    return apiRequest<null>(`/notifications/${id}/read`, { method: 'PATCH' });
  },
  markAllRead() {
    return apiRequest<null>('/notifications/read-all', { method: 'PATCH' });
  },
};
