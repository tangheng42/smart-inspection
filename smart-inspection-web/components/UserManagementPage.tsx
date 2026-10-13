'use client';

import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import {
  ApiError,
  PageResult,
  RoleCode,
  UserCreatePayload,
  UserDetail,
  UserSummary,
  userApi,
} from '@/lib/api';

const PAGE_SIZE = 10;
const emptyPage: PageResult<UserSummary> = {
  pageNum: 1,
  pageSize: PAGE_SIZE,
  total: 0,
  records: [],
};

const roleLabels: Record<RoleCode, string> = {
  ADMIN: '系统管理员',
  INSPECTOR: '巡检员',
  MAINTAINER: '维修人员',
};

type EditorState = {
  mode: 'create' | 'edit';
  id?: number;
  username: string;
  password: string;
  displayName: string;
  roleCode: RoleCode;
};

function formatDateTime(value?: string | null) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value.replace('T', ' ');
  return date.toLocaleString('zh-CN', { hour12: false });
}

function messageOf(error: unknown) {
  return error instanceof ApiError || error instanceof Error ? error.message : '操作失败，请稍后重试';
}

export default function UserManagementPage() {
  const [pageNum, setPageNum] = useState(1);
  const [page, setPage] = useState<PageResult<UserSummary>>(emptyPage);
  const [searchInput, setSearchInput] = useState('');
  const [username, setUsername] = useState('');
  const [roleCode, setRoleCode] = useState<'' | RoleCode>('');
  const [enabled, setEnabled] = useState<'' | 'true' | 'false'>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');
  const [editor, setEditor] = useState<EditorState | null>(null);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<UserDetail | null>(null);
  const [detailError, setDetailError] = useState('');
  const [detailLoading, setDetailLoading] = useState(false);
  const [passwordTarget, setPasswordTarget] = useState<UserSummary | null>(null);
  const [newPassword, setNewPassword] = useState('');

  const totalPages = Math.max(1, Math.ceil(page.total / PAGE_SIZE));
  const pageEnabled = useMemo(() => page.records.filter((item) => item.enabled).length, [page.records]);
  const pageDisabled = page.records.length - pageEnabled;

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await userApi.page({
        pageNum,
        pageSize: PAGE_SIZE,
        username: username || undefined,
        roleCode: roleCode || undefined,
        enabled: enabled === '' ? undefined : enabled === 'true',
      });
      setPage(data);
    } catch (requestError) {
      setError(messageOf(requestError));
      setPage({ ...emptyPage, pageNum });
    } finally {
      setLoading(false);
    }
  }, [enabled, pageNum, roleCode, username]);

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      void loadUsers();
    });
    return () => window.cancelAnimationFrame(frame);
  }, [loadUsers]);

  function notify(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(''), 2600);
  }

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPageNum(1);
    setUsername(searchInput.trim());
  }

  function resetFilters() {
    setSearchInput('');
    setUsername('');
    setRoleCode('');
    setEnabled('');
    setPageNum(1);
  }

  function openCreate() {
    setFormError('');
    setEditor({
      mode: 'create',
      username: '',
      password: '',
      displayName: '',
      roleCode: 'INSPECTOR',
    });
  }

  function openEdit(user: UserSummary) {
    setFormError('');
    setEditor({
      mode: 'edit',
      id: user.id,
      username: user.username,
      password: '',
      displayName: user.displayName,
      roleCode: user.roleCode,
    });
  }

  async function submitEditor(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editor) return;

    const usernameValue = editor.username.trim();
    const displayNameValue = editor.displayName.trim();
    if (!displayNameValue) {
      setFormError('请输入显示名称');
      return;
    }
    if (editor.mode === 'create' && (!usernameValue || !editor.password)) {
      setFormError('请输入账号和初始密码');
      return;
    }
    if (editor.mode === 'create' && editor.password.length < 6) {
      setFormError('初始密码至少需要 6 个字符');
      return;
    }

    setSaving(true);
    setFormError('');
    try {
      if (editor.mode === 'create') {
        const payload: UserCreatePayload = {
          username: usernameValue,
          password: editor.password,
          displayName: displayNameValue,
          roleCode: editor.roleCode,
        };
        await userApi.create(payload);
        notify('用户创建成功，数据已写入数据库');
      } else if (editor.id) {
        await userApi.update(editor.id, {
          displayName: displayNameValue,
          roleCode: editor.roleCode,
        });
        notify('用户信息已更新');
      }
      setEditor(null);
      await loadUsers();
    } catch (requestError) {
      setFormError(messageOf(requestError));
    } finally {
      setSaving(false);
    }
  }

  async function showDetail(id: number) {
    setDetailOpen(true);
    setDetail(null);
    setDetailError('');
    setDetailLoading(true);
    try {
      setDetail(await userApi.detail(id));
    } catch (requestError) {
      setDetailError(messageOf(requestError));
    } finally {
      setDetailLoading(false);
    }
  }

  async function changeStatus(user: UserSummary) {
    const action = user.enabled ? '停用' : '启用';
    if (!window.confirm(`确认${action}用户“${user.displayName}”吗？`)) return;
    try {
      await userApi.updateStatus(user.id, !user.enabled);
      notify(`用户已${action}`);
      await loadUsers();
    } catch (requestError) {
      notify(messageOf(requestError));
    }
  }

  async function submitPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!passwordTarget) return;
    if (newPassword.length < 6 || newPassword.length > 64) {
      setFormError('新密码长度必须在 6 到 64 个字符之间');
      return;
    }
    setSaving(true);
    setFormError('');
    try {
      await userApi.resetPassword(passwordTarget.id, newPassword);
      setPasswordTarget(null);
      setNewPassword('');
      notify('密码已重置');
    } catch (requestError) {
      setFormError(messageOf(requestError));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="page-stack module-page">
      <section className="module-header">
        <div>
          <p>组织与权限</p>
          <h1>用户管理</h1>
          <span>维护管理员、巡检员和维修人员账号；本页数据来自 MySQL。</span>
        </div>
        <button className="button primary" type="button" onClick={openCreate}>＋ 新增用户</button>
      </section>

      <section className="summary-grid">
        <article className="summary-card"><span>匹配用户总数</span><strong>{page.total}</strong></article>
        <article className="summary-card teal"><span>本页启用</span><strong>{pageEnabled}</strong></article>
        <article className="summary-card gray"><span>本页禁用</span><strong>{pageDisabled}</strong></article>
        <article className="summary-card blue"><span>当前页</span><strong>{page.pageNum}/{totalPages}</strong></article>
      </section>

      <section className="data-panel">
        <div className="data-toolbar">
          <form className="toolbar-search-form" onSubmit={submitSearch}>
            <div className="search-box"><span>⌕</span><input value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="按登录账号搜索" maxLength={32} /></div>
            <button className="toolbar-button" type="submit">查询</button>
          </form>
          <select aria-label="角色筛选" value={roleCode} onChange={(event) => { setRoleCode(event.target.value as '' | RoleCode); setPageNum(1); }}>
            <option value="">全部角色</option>
            <option value="ADMIN">系统管理员</option>
            <option value="INSPECTOR">巡检员</option>
            <option value="MAINTAINER">维修人员</option>
          </select>
          <select aria-label="状态筛选" value={enabled} onChange={(event) => { setEnabled(event.target.value as '' | 'true' | 'false'); setPageNum(1); }}>
            <option value="">全部状态</option>
            <option value="true">启用</option>
            <option value="false">禁用</option>
          </select>
          <button className="toolbar-button" type="button" onClick={resetFilters}>重置</button>
          <button className="toolbar-button" type="button" onClick={() => void loadUsers()}>刷新</button>
          <span className="result-count">真实接口 · 共 {page.total} 条</span>
        </div>

        {error && <div className="management-alert" role="alert"><strong>数据加载失败</strong><span>{error}</span><button type="button" onClick={() => void loadUsers()}>重新加载</button></div>}

        <div className="data-table-wrap">
          <table className="data-table management-table">
            <thead><tr><th>账号</th><th>显示名称</th><th>角色</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>
              {!loading && page.records.map((user) => (
                <tr key={user.id}>
                  <td>{user.username}</td>
                  <td>{user.displayName}</td>
                  <td>{roleLabels[user.roleCode]}</td>
                  <td><span className={`tag ${user.enabled ? 'success' : 'neutral'}`}>{user.enabled ? '启用' : '禁用'}</span></td>
                  <td>{formatDateTime(user.createdAt)}</td>
                  <td className="row-actions">
                    <button type="button" onClick={() => void showDetail(user.id)}>详情</button>
                    <button type="button" onClick={() => openEdit(user)}>编辑</button>
                    <button type="button" onClick={() => { setFormError(''); setNewPassword(''); setPasswordTarget(user); }}>重置密码</button>
                    <button className={user.enabled ? 'danger-action' : ''} type="button" onClick={() => void changeStatus(user)}>{user.enabled ? '停用' : '启用'}</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {loading && <div className="empty-state"><strong>正在读取用户数据…</strong><span>请求 GET /api/users</span></div>}
          {!loading && !error && page.records.length === 0 && <div className="empty-state"><strong>没有找到用户</strong><span>请调整账号、角色或状态筛选条件</span></div>}
        </div>

        <div className="table-footer">
          <span>第 {page.pageNum} 页，共 {totalPages} 页 · 每页 {PAGE_SIZE} 条</span>
          <div>
            <button type="button" disabled={loading || pageNum <= 1} onClick={() => setPageNum((value) => value - 1)}>‹</button>
            <button type="button" className="current">{pageNum}</button>
            <button type="button" disabled={loading || pageNum >= totalPages} onClick={() => setPageNum((value) => value + 1)}>›</button>
          </div>
        </div>
      </section>

      <p className="module-note"><i>i</i>新增、编辑、启停和重置密码都会调用真实后端接口；密码不会在列表或详情中返回。</p>

      {detailOpen && (
        <div className="drawer-layer" onMouseDown={() => setDetailOpen(false)}>
          <aside className="detail-drawer" onMouseDown={(event) => event.stopPropagation()}>
            <div className="drawer-head"><div><span>用户详情接口</span><h2>{detail?.displayName || '正在加载'}</h2></div><button type="button" onClick={() => setDetailOpen(false)}>×</button></div>
            {detailLoading && <div className="drawer-state">正在请求 GET /api/users/{'{userId}'}…</div>}
            {detailError && <div className="management-alert compact" role="alert"><strong>详情加载失败</strong><span>{detailError}</span></div>}
            {detail && <div className="detail-list">
              <div><span>用户 ID</span><strong>{detail.id}</strong></div>
              <div><span>登录账号</span><strong>{detail.username}</strong></div>
              <div><span>显示名称</span><strong>{detail.displayName}</strong></div>
              <div><span>角色</span><strong>{roleLabels[detail.roleCode]}</strong></div>
              <div><span>账号状态</span><strong>{detail.enabled ? '启用' : '禁用'}</strong></div>
              <div><span>创建时间</span><strong>{formatDateTime(detail.createdAt)}</strong></div>
              <div><span>更新时间</span><strong>{formatDateTime(detail.updatedAt)}</strong></div>
            </div>}
            <div className="drawer-tip">详情数据由后端按用户 ID 查询，前端不会读取或展示密码哈希。</div>
            <button className="button primary drawer-action" type="button" onClick={() => setDetailOpen(false)}>关闭详情</button>
          </aside>
        </div>
      )}

      {editor && (
        <div className="modal-layer" onMouseDown={() => !saving && setEditor(null)}>
          <form className="form-modal" onSubmit={submitEditor} onMouseDown={(event) => event.stopPropagation()}>
            <div className="modal-head"><div><span>真实数据提交</span><h2>{editor.mode === 'create' ? '新增用户' : '编辑用户'}</h2></div><button type="button" disabled={saving} onClick={() => setEditor(null)}>×</button></div>
            <div className="form-grid">
              <label><span>登录账号</span><input value={editor.username} onChange={(event) => setEditor({ ...editor, username: event.target.value })} placeholder="例如 inspector03" maxLength={32} disabled={editor.mode === 'edit' || saving} /></label>
              {editor.mode === 'create' && <label><span>初始密码</span><input type="password" autoComplete="new-password" value={editor.password} onChange={(event) => setEditor({ ...editor, password: event.target.value })} placeholder="至少 6 个字符" maxLength={64} disabled={saving} /></label>}
              <label><span>显示名称</span><input value={editor.displayName} onChange={(event) => setEditor({ ...editor, displayName: event.target.value })} placeholder="请输入姓名或岗位名称" maxLength={50} disabled={saving} /></label>
              <label><span>用户角色</span><select value={editor.roleCode} onChange={(event) => setEditor({ ...editor, roleCode: event.target.value as RoleCode })} disabled={saving}><option value="ADMIN">系统管理员</option><option value="INSPECTOR">巡检员</option><option value="MAINTAINER">维修人员</option></select></label>
            </div>
            {formError && <p className="modal-error" role="alert">{formError}</p>}
            <p className="modal-tip">{editor.mode === 'create' ? '提交后将调用 POST /api/users，并由后端使用 BCrypt 加密密码。' : '账号不能修改；这里只提交显示名称和角色。'}</p>
            <div className="modal-actions"><button className="button secondary" type="button" disabled={saving} onClick={() => setEditor(null)}>取消</button><button className="button primary" type="submit" disabled={saving}>{saving ? '正在保存…' : '保存'}</button></div>
          </form>
        </div>
      )}

      {passwordTarget && (
        <div className="modal-layer" onMouseDown={() => !saving && setPasswordTarget(null)}>
          <form className="form-modal form-modal-small" onSubmit={submitPassword} onMouseDown={(event) => event.stopPropagation()}>
            <div className="modal-head"><div><span>账号安全</span><h2>重置 {passwordTarget.displayName} 的密码</h2></div><button type="button" disabled={saving} onClick={() => setPasswordTarget(null)}>×</button></div>
            <div className="form-grid single-column"><label><span>新密码</span><input type="password" autoComplete="new-password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} placeholder="6 到 64 个字符" maxLength={64} disabled={saving} /></label></div>
            {formError && <p className="modal-error" role="alert">{formError}</p>}
            <p className="modal-tip">提交后后端会重新 BCrypt 加密，不会保存明文密码。</p>
            <div className="modal-actions"><button className="button secondary" type="button" disabled={saving} onClick={() => setPasswordTarget(null)}>取消</button><button className="button primary" type="submit" disabled={saving}>{saving ? '正在重置…' : '确认重置'}</button></div>
          </form>
        </div>
      )}

      {toast && <div className="app-toast" role="status">✓ {toast}</div>}
    </div>
  );
}
