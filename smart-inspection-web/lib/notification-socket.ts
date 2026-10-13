import type { NotificationPushEvent } from './api';

export const NOTIFICATION_PUSH_EVENT = 'zhixunyun:notification';
export const NOTIFICATION_READ_EVENT = 'zhixunyun:notification-read';

export function notifyUnreadCountChanged(): void {
  window.dispatchEvent(new Event(NOTIFICATION_READ_EVENT));
}

export function connectNotificationSocket(): () => void {
  const token = sessionStorage.getItem('smart-inspection-token');
  if (!token) return () => undefined;

  let socket: WebSocket | null = null;
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  let stopped = false;

  const connect = () => {
    const configured = process.env.NEXT_PUBLIC_WS_URL;
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const base = configured || `${protocol}//${window.location.host}/ws/notifications`;
    socket = new WebSocket(`${base}?token=${encodeURIComponent(token)}`);
    socket.onmessage = (message) => {
      try {
        const event = JSON.parse(message.data) as NotificationPushEvent | { type: string };
        if (event.type !== 'CONNECTED') {
          window.dispatchEvent(new CustomEvent(NOTIFICATION_PUSH_EVENT, { detail: event }));
        }
      } catch {
        // 单条消息格式异常不影响后续消息。
      }
    };
    socket.onclose = () => {
      if (!stopped) reconnectTimer = setTimeout(connect, 3000);
    };
  };

  connect();
  return () => {
    stopped = true;
    if (reconnectTimer) clearTimeout(reconnectTimer);
    socket?.close();
  };
}
