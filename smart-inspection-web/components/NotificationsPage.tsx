'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { notificationApi, NotificationItem } from '@/lib/api';
import { NOTIFICATION_PUSH_EVENT, notifyUnreadCountChanged } from '@/lib/notification-socket';

const labels: Record<NotificationItem['type'], string> = {
  WORK_ORDER_ASSIGNED: '工单通知',
  URGENT_FAULT: '紧急故障',
  VERIFICATION_PASSED: '验收通过',
  VERIFICATION_REJECTED: '验收驳回',
};

const tones: Record<NotificationItem['type'], string> = {
  WORK_ORDER_ASSIGNED: 'info',
  URGENT_FAULT: 'danger',
  VERIFICATION_PASSED: 'success',
  VERIFICATION_REJECTED: 'warning',
};

export default function NotificationsPage() {
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      const page = await notificationApi.page();
      setItems(page.records);
      setError('');
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '消息加载失败');
    }
  }, []);

  useEffect(() => {
    void load();
    const refresh = () => void load();
    window.addEventListener(NOTIFICATION_PUSH_EVENT, refresh);
    return () => window.removeEventListener(NOTIFICATION_PUSH_EVENT, refresh);
  }, [load]);

  const unread = useMemo(() => items.filter((item) => !item.read).length, [items]);
  const urgent = useMemo(
    () => items.filter((item) => item.type === 'URGENT_FAULT').length,
    [items],
  );

  async function markAllRead() {
    await notificationApi.markAllRead();
    setItems((current) => current.map((item) => ({ ...item, read: true })));
    notifyUnreadCountChanged();
  }

  async function markRead(id: number) {
    await notificationApi.markRead(id);
    setItems((current) => current.map((item) =>
      item.id === id ? { ...item, read: true } : item));
    notifyUnreadCountChanged();
  }

  return <div className="page-stack module-page">
    <section className="module-header"><div><p>消息中心</p><h1>消息通知</h1><span>实时查看工单派发和验收相关提醒。</span></div><button className="button secondary" onClick={() => void markAllRead()}>全部标为已读</button></section>
    <section className="summary-grid"><article className="summary-card"><span>全部消息</span><strong>{items.length}</strong></article><article className="summary-card orange"><span>未读消息</span><strong>{unread}</strong></article><article className="summary-card red"><span>紧急通知</span><strong>{urgent}</strong></article><article className="summary-card teal"><span>已处理</span><strong>{items.length - unread}</strong></article></section>
    {error && <p className="module-note"><i>!</i>{error}</p>}
    <section className="notification-layout"><nav><button className="is-active">全部消息 <em>{items.length}</em></button></nav><div className="notification-list">{items.map((item) => <article className={!item.read ? 'is-unread' : ''} key={item.id}><i className={tones[item.type]}>{labels[item.type].slice(0, 1)}</i><div><p><span>{labels[item.type]}</span><time>{new Date(item.createdAt).toLocaleString('zh-CN')}</time></p><strong>{item.title}</strong><small>{item.content}</small></div>{!item.read && <button onClick={() => void markRead(item.id)}>标为已读</button>}</article>)}</div></section>
    <p className="module-note"><i>i</i>页面数据来自后端通知表；在线时通过 WebSocket 自动刷新。</p>
  </div>;
}
