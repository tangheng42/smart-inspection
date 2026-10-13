'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { ReactNode, useEffect, useMemo, useState } from 'react';
import { navigation } from '@/lib/navigation';
import { notificationApi } from '@/lib/api';
import {
  connectNotificationSocket,
  NOTIFICATION_PUSH_EVENT,
  NOTIFICATION_READ_EVENT,
} from '@/lib/notification-socket';

const groups = ['工作台', '基础资料', '巡检中心', '故障工单'] as const;

export default function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [userName, setUserName] = useState('系统管理员');
  const [ready, setReady] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      const token = sessionStorage.getItem('smart-inspection-token');
      const demo = sessionStorage.getItem('smart-inspection-demo');
      if (!token && !demo) {
        router.replace('/');
        return;
      }
      const savedUser = sessionStorage.getItem('smart-inspection-user');
      if (savedUser) {
        try {
          const parsed = JSON.parse(savedUser) as { displayName?: string; username?: string };
          setUserName(parsed.displayName || parsed.username || '系统管理员');
        } catch {
          setUserName('系统管理员');
        }
      }
      setReady(true);
    });
    return () => window.cancelAnimationFrame(frame);
  }, [router]);

  useEffect(() => {
    if (!ready || !sessionStorage.getItem('smart-inspection-token')) return;
    const refreshUnreadCount = () => {
      notificationApi.unreadCount()
        .then(({ count }) => setUnreadCount(count))
        .catch(() => undefined);
    };
    refreshUnreadCount();
    const onNotification = () => setUnreadCount((count) => count + 1);
    window.addEventListener(NOTIFICATION_PUSH_EVENT, onNotification);
    window.addEventListener(NOTIFICATION_READ_EVENT, refreshUnreadCount);
    const disconnect = connectNotificationSocket();
    return () => {
      disconnect();
      window.removeEventListener(NOTIFICATION_PUSH_EVENT, onNotification);
      window.removeEventListener(NOTIFICATION_READ_EVENT, refreshUnreadCount);
    };
  }, [ready]);

  const current = useMemo(
    () => navigation.find((item) => pathname === item.href),
    [pathname],
  );

  function logout() {
    sessionStorage.removeItem('smart-inspection-token');
    sessionStorage.removeItem('smart-inspection-demo');
    sessionStorage.removeItem('smart-inspection-user');
    router.replace('/');
  }

  if (!ready) return <main className="app-loading">正在进入智巡云工作台…</main>;

  return (
    <div className={`app-shell ${collapsed ? 'is-collapsed' : ''}`}>
      <aside className={`app-sidebar ${mobileOpen ? 'is-open' : ''}`}>
        <div className="sidebar-brand">
          <div className="brand-symbol" aria-hidden="true"><i /><i /><i /></div>
          <div className="sidebar-brand-copy"><strong>智巡云</strong><span>SMART INSPECTION</span></div>
        </div>
        <nav className="sidebar-nav" aria-label="主导航">
          {groups.map((group) => (
            <div className="nav-group" key={group}>
              <p>{group}</p>
              {navigation.filter((item) => item.group === group).map((item) => (
                <Link
                  className={pathname === item.href ? 'nav-item is-active' : 'nav-item'}
                  href={item.href}
                  key={item.href}
                  onClick={() => setMobileOpen(false)}
                >
                  <span className="nav-icon">{item.short}</span>
                  <span className="nav-label">{item.label}</span>
                  {item.href === '/work-orders' && <em>5</em>}
                </Link>
              ))}
            </div>
          ))}
        </nav>
        <div className="sidebar-status"><i /><span>服务运行正常</span></div>
      </aside>

      {mobileOpen && <button className="sidebar-mask" aria-label="关闭菜单" onClick={() => setMobileOpen(false)} />}

      <section className="app-main">
        <header className="topbar">
          <div className="topbar-left">
            <button className="menu-button desktop-menu" onClick={() => setCollapsed((value) => !value)} aria-label="收起或展开导航">☰</button>
            <button className="menu-button mobile-menu" onClick={() => setMobileOpen(true)} aria-label="打开导航">☰</button>
            <span className="topbar-divider" />
            <div><strong>{current?.label || '智巡云工作台'}</strong><span>设备巡检与故障工单平台</span></div>
          </div>
          <div className="topbar-actions">
            <Link className="notice-button" href="/notifications" aria-label={`消息通知，${unreadCount}条未读`}><span>信</span>{unreadCount > 0 && <i />}</Link>
            <div className="user-menu">
              <span className="user-avatar">管</span>
              <div><strong>{userName}</strong><span>管理员</span></div>
              <button onClick={logout}>退出</button>
            </div>
          </div>
        </header>
        <main className="workspace-content">{children}</main>
      </section>
    </div>
  );
}
