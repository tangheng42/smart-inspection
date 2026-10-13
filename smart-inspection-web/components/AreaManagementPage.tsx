'use client';

import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import {
  ApiError,
  AreaDetail,
  AreaPayload,
  AreaSummary,
  PageResult,
  areaApi,
} from '@/lib/api';

const PAGE_SIZE = 10;
const emptyPage: PageResult<AreaSummary> = {
  pageNum: 1,
  pageSize: PAGE_SIZE,
  total: 0,
  records: [],
};

type EditorState = {
  mode: 'create' | 'edit';
  id?: number;
  code: string;
  name: string;
  description: string;
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

export default function AreaManagementPage() {
  const [pageNum, setPageNum] = useState(1);
  const [page, setPage] = useState<PageResult<AreaSummary>>(emptyPage);
  const [searchInput, setSearchInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [enabled, setEnabled] = useState<'' | 'true' | 'false'>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');
  const [editor, setEditor] = useState<EditorState | null>(null);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<AreaDetail | null>(null);
  const [detailError, setDetailError] = useState('');
  const [detailLoading, setDetailLoading] = useState(false);

  const totalPages = Math.max(1, Math.ceil(page.total / PAGE_SIZE));
  const linkedEquipment = useMemo(
    () => page.records.reduce((total, area) => total + (area.equipmentCount || 0), 0),
    [page.records],
  );
  const pageDisabled = useMemo(() => page.records.filter((item) => !item.enabled).length, [page.records]);

  const loadAreas = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await areaApi.page({
        pageNum,
        pageSize: PAGE_SIZE,
        keyword: keyword || undefined,
        enabled: enabled === '' ? undefined : enabled === 'true',
      });
      setPage(data);
    } catch (requestError) {
      setError(messageOf(requestError));
      setPage({ ...emptyPage, pageNum });
    } finally {
      setLoading(false);
    }
  }, [enabled, keyword, pageNum]);

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      void loadAreas();
    });
    return () => window.cancelAnimationFrame(frame);
  }, [loadAreas]);

  function notify(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(''), 2600);
  }

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPageNum(1);
    setKeyword(searchInput.trim());
  }

  function resetFilters() {
    setSearchInput('');
    setKeyword('');
    setEnabled('');
    setPageNum(1);
  }

  function openCreate() {
    setFormError('');
    setEditor({ mode: 'create', code: '', name: '', description: '' });
  }

  async function openEdit(area: AreaSummary) {
    setFormError('');
    setSaving(true);
    try {
      const data = await areaApi.detail(area.id);
      setEditor({
        mode: 'edit',
        id: data.id,
        code: data.code,
        name: data.name,
        description: data.description || '',
      });
    } catch (requestError) {
      notify(messageOf(requestError));
    } finally {
      setSaving(false);
    }
  }

  async function submitEditor(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editor) return;

    const payload: AreaPayload = {
      code: editor.code.trim(),
      name: editor.name.trim(),
      description: editor.description.trim() || undefined,
    };
    if (!payload.code || !payload.name) {
      setFormError('请输入区域编码和区域名称');
      return;
    }

    setSaving(true);
    setFormError('');
    try {
      if (editor.mode === 'create') {
        await areaApi.create(payload);
        notify('区域创建成功，数据已写入数据库');
      } else if (editor.id) {
        await areaApi.update(editor.id, payload);
        notify('区域信息已更新');
      }
      setEditor(null);
      await loadAreas();
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
      setDetail(await areaApi.detail(id));
    } catch (requestError) {
      setDetailError(messageOf(requestError));
    } finally {
      setDetailLoading(false);
    }
  }

  async function changeStatus(area: AreaSummary) {
    const action = area.enabled ? '停用' : '启用';
    const warning = area.enabled && area.equipmentCount > 0
      ? `该区域关联了 ${area.equipmentCount} 台设备，后端会拒绝停用。仍要提交校验吗？`
      : `确认${action}区域“${area.name}”吗？`;
    if (!window.confirm(warning)) return;
    try {
      await areaApi.updateStatus(area.id, !area.enabled);
      notify(`区域已${action}`);
      await loadAreas();
    } catch (requestError) {
      notify(messageOf(requestError));
    }
  }

  return (
    <div className="page-stack module-page">
      <section className="module-header">
        <div>
          <p>基础资料</p>
          <h1>区域管理</h1>
          <span>维护设备所在区域的编码、名称和启停状态；本页数据来自 MySQL。</span>
        </div>
        <button className="button primary" type="button" onClick={openCreate}>＋ 新增区域</button>
      </section>

      <section className="summary-grid">
        <article className="summary-card"><span>匹配区域总数</span><strong>{page.total}</strong></article>
        <article className="summary-card teal"><span>本页启用</span><strong>{page.records.length - pageDisabled}</strong></article>
        <article className="summary-card blue"><span>本页关联设备</span><strong>{linkedEquipment}</strong></article>
        <article className="summary-card gray"><span>本页停用</span><strong>{pageDisabled}</strong></article>
      </section>

      <section className="data-panel">
        <div className="data-toolbar">
          <form className="toolbar-search-form" onSubmit={submitSearch}>
            <div className="search-box"><span>⌕</span><input value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="搜索区域编码或名称" maxLength={50} /></div>
            <button className="toolbar-button" type="submit">查询</button>
          </form>
          <select aria-label="状态筛选" value={enabled} onChange={(event) => { setEnabled(event.target.value as '' | 'true' | 'false'); setPageNum(1); }}>
            <option value="">全部状态</option>
            <option value="true">启用</option>
            <option value="false">停用</option>
          </select>
          <button className="toolbar-button" type="button" onClick={resetFilters}>重置</button>
          <button className="toolbar-button" type="button" onClick={() => void loadAreas()}>刷新</button>
          <span className="result-count">真实接口 · 共 {page.total} 条</span>
        </div>

        {error && <div className="management-alert" role="alert"><strong>数据加载失败</strong><span>{error}</span><button type="button" onClick={() => void loadAreas()}>重新加载</button></div>}

        <div className="data-table-wrap">
          <table className="data-table management-table">
            <thead><tr><th>区域编码</th><th>区域名称</th><th>设备数量</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>
              {!loading && page.records.map((area) => (
                <tr key={area.id}>
                  <td>{area.code}</td>
                  <td>{area.name}</td>
                  <td>{area.equipmentCount || 0} 台</td>
                  <td><span className={`tag ${area.enabled ? 'success' : 'neutral'}`}>{area.enabled ? '启用' : '停用'}</span></td>
                  <td>{formatDateTime(area.createdAt)}</td>
                  <td className="row-actions">
                    <button type="button" onClick={() => void showDetail(area.id)}>详情</button>
                    <button type="button" disabled={saving} onClick={() => void openEdit(area)}>编辑</button>
                    <button className={area.enabled ? 'danger-action' : ''} type="button" onClick={() => void changeStatus(area)}>{area.enabled ? '停用' : '启用'}</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {loading && <div className="empty-state"><strong>正在读取区域数据…</strong><span>请求 GET /api/areas</span></div>}
          {!loading && !error && page.records.length === 0 && <div className="empty-state"><strong>没有找到区域</strong><span>请调整关键词或状态筛选条件</span></div>}
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

      <p className="module-note"><i>i</i>停用区域时，后端会再次检查是否关联设备；前端提示不能替代后端业务校验。</p>

      {detailOpen && (
        <div className="drawer-layer" onMouseDown={() => setDetailOpen(false)}>
          <aside className="detail-drawer" onMouseDown={(event) => event.stopPropagation()}>
            <div className="drawer-head"><div><span>区域详情接口</span><h2>{detail?.name || '正在加载'}</h2></div><button type="button" onClick={() => setDetailOpen(false)}>×</button></div>
            {detailLoading && <div className="drawer-state">正在请求 GET /api/areas/{'{areaId}'}…</div>}
            {detailError && <div className="management-alert compact" role="alert"><strong>详情加载失败</strong><span>{detailError}</span></div>}
            {detail && <div className="detail-list">
              <div><span>区域 ID</span><strong>{detail.id}</strong></div>
              <div><span>区域编码</span><strong>{detail.code}</strong></div>
              <div><span>区域名称</span><strong>{detail.name}</strong></div>
              <div><span>区域状态</span><strong>{detail.enabled ? '启用' : '停用'}</strong></div>
              <div><span>关联设备</span><strong>{detail.equipmentCount || 0} 台</strong></div>
              <div><span>区域说明</span><strong>{detail.description || '—'}</strong></div>
              <div><span>创建时间</span><strong>{formatDateTime(detail.createdAt)}</strong></div>
              <div><span>更新时间</span><strong>{formatDateTime(detail.updatedAt)}</strong></div>
            </div>}
            <div className="drawer-tip">详情接口同时返回关联设备数量，为停用操作提供业务判断依据。</div>
            <button className="button primary drawer-action" type="button" onClick={() => setDetailOpen(false)}>关闭详情</button>
          </aside>
        </div>
      )}

      {editor && (
        <div className="modal-layer" onMouseDown={() => !saving && setEditor(null)}>
          <form className="form-modal" onSubmit={submitEditor} onMouseDown={(event) => event.stopPropagation()}>
            <div className="modal-head"><div><span>真实数据提交</span><h2>{editor.mode === 'create' ? '新增区域' : '编辑区域'}</h2></div><button type="button" disabled={saving} onClick={() => setEditor(null)}>×</button></div>
            <div className="form-grid">
              <label><span>区域编码</span><input value={editor.code} onChange={(event) => setEditor({ ...editor, code: event.target.value })} placeholder="例如 AREA-A" maxLength={30} disabled={saving} /></label>
              <label><span>区域名称</span><input value={editor.name} onChange={(event) => setEditor({ ...editor, name: event.target.value })} placeholder="例如 A 区配电房" maxLength={50} disabled={saving} /></label>
              <label className="form-span-2"><span>区域说明</span><textarea value={editor.description} onChange={(event) => setEditor({ ...editor, description: event.target.value })} placeholder="可选，填写区域位置或用途" maxLength={255} disabled={saving} /></label>
            </div>
            {formError && <p className="modal-error" role="alert">{formError}</p>}
            <p className="modal-tip">{editor.mode === 'create' ? '提交后将调用 POST /api/areas，区域编码由后端和数据库共同保证唯一。' : '提交后将调用 PUT /api/areas/{areaId} 更新现有记录。'}</p>
            <div className="modal-actions"><button className="button secondary" type="button" disabled={saving} onClick={() => setEditor(null)}>取消</button><button className="button primary" type="submit" disabled={saving}>{saving ? '正在保存…' : '保存'}</button></div>
          </form>
        </div>
      )}

      {toast && <div className="app-toast" role="status">✓ {toast}</div>}
    </div>
  );
}
