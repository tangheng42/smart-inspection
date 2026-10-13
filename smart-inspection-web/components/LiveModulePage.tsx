'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError, businessApi, BusinessRecord, PageResult } from '@/lib/api';

export type LiveModuleKey =
  | 'equipmentTypes' | 'equipment' | 'items' | 'plans'
  | 'tasks' | 'records' | 'faults' | 'orders';

type FieldType = 'text' | 'textarea' | 'number' | 'date' | 'time'
  | 'select' | 'boolean' | 'number-list';

type Field = {
  key: string;
  label: string;
  type?: FieldType;
  required?: boolean;
  options?: Array<{ value: string; label: string }>;
};

type Column = { key: string; label: string };
type Filter = { value: string; label: string };
type Config = {
  eyebrow: string;
  title: string;
  description: string;
  basePath: string;
  keywordParam: string;
  filterParam?: string;
  filters: Filter[];
  columns: Column[];
  fields?: Field[];
  canDelete?: boolean;
  statusToggle?: boolean;
  note: string;
};

const enabledFilters: Filter[] = [
  { value: '', label: '全部状态' },
  { value: 'true', label: '启用' },
  { value: 'false', label: '停用' },
];

const resultTypeOptions = [
  { value: 'BOOLEAN', label: '判断型' },
  { value: 'NUMBER', label: '数值型' },
  { value: 'TEXT', label: '文本型' },
];

const frequencyOptions = [
  { value: 'DAILY', label: '每日' },
  { value: 'WEEKLY', label: '每周' },
  { value: 'MONTHLY', label: '每月' },
];

const configs: Record<LiveModuleKey, Config> = {
  equipmentTypes: {
    eyebrow: '基础资料', title: '设备类型',
    description: '维护设备分类，列表、详情和写操作均来自后端。',
    basePath: '/equipment-types', keywordParam: 'keyword',
    filterParam: 'enabled', filters: enabledFilters, statusToggle: true,
    columns: [
      { key: 'code', label: '类型编码' }, { key: 'name', label: '类型名称' },
      { key: 'equipmentCount', label: '设备数量' },
      { key: 'inspectionItemCount', label: '巡检项目' },
      { key: 'enabled', label: '状态' }, { key: 'createdAt', label: '创建时间' },
    ],
    fields: [
      { key: 'code', label: '类型编码', required: true },
      { key: 'name', label: '类型名称', required: true },
      { key: 'description', label: '类型说明', type: 'textarea' },
    ],
    note: '停用前由后端检查该类型下是否还有启用设备。',
  },
  equipment: {
    eyebrow: '设备资产', title: '设备台账',
    description: '查看和维护真实设备档案及运行状态。',
    basePath: '/equipment', keywordParam: 'keyword',
    filterParam: 'status', filters: [
      { value: '', label: '全部状态' }, { value: 'NORMAL', label: '正常' },
      { value: 'FAULT', label: '故障' }, { value: 'IN_REPAIR', label: '维修中' },
      { value: 'DISABLED', label: '已停用' },
    ], canDelete: true,
    columns: [
      { key: 'code', label: '设备编号' }, { key: 'name', label: '设备名称' },
      { key: 'equipmentTypeName', label: '设备类型' }, { key: 'areaName', label: '所属区域' },
      { key: 'status', label: '设备状态' }, { key: 'lastInspectionAt', label: '最近巡检' },
    ],
    fields: [
      { key: 'code', label: '设备编号', required: true },
      { key: 'name', label: '设备名称', required: true },
      { key: 'equipmentTypeId', label: '设备类型 ID', type: 'number', required: true },
      { key: 'areaId', label: '区域 ID', type: 'number', required: true },
      { key: 'model', label: '设备型号' }, { key: 'manufacturer', label: '生产厂家' },
      { key: 'serialNumber', label: '序列号' }, { key: 'installDate', label: '安装日期', type: 'date' },
      { key: 'remark', label: '备注', type: 'textarea' },
    ],
    note: '故障和维修中状态由巡检、工单业务维护，管理接口不能随意修改。',
  },
  items: {
    eyebrow: '巡检配置', title: '巡检项目',
    description: '维护真实巡检标准和结果类型。',
    basePath: '/inspection-items', keywordParam: 'keyword',
    filterParam: 'resultType', filters: [
      { value: '', label: '全部结果类型' }, ...resultTypeOptions,
    ], statusToggle: true,
    columns: [
      { key: 'code', label: '项目编码' }, { key: 'name', label: '项目名称' },
      { key: 'equipmentTypeName', label: '适用设备' }, { key: 'resultType', label: '结果类型' },
      { key: 'required', label: '是否必填' }, { key: 'enabled', label: '状态' },
    ],
    fields: [
      { key: 'code', label: '项目编码', required: true },
      { key: 'name', label: '项目名称', required: true },
      { key: 'equipmentTypeId', label: '设备类型 ID', type: 'number', required: true },
      { key: 'checkMethod', label: '检查方法', type: 'textarea' },
      { key: 'standardDescription', label: '合格标准', type: 'textarea', required: true },
      { key: 'resultType', label: '结果类型', type: 'select', required: true, options: resultTypeOptions },
      { key: 'unit', label: '单位' }, { key: 'minValue', label: '最小值', type: 'number' },
      { key: 'maxValue', label: '最大值', type: 'number' },
      { key: 'required', label: '结果必填', type: 'boolean' },
    ],
    note: '数值型项目的最小值、最大值和单位会由后端进行组合校验。',
  },
  plans: {
    eyebrow: '巡检中心', title: '巡检计划',
    description: '维护周期、人员、设备和巡检项目，并由定时任务生成任务。',
    basePath: '/inspection-plans', keywordParam: 'keyword',
    filterParam: 'enabled', filters: enabledFilters, statusToggle: true, canDelete: true,
    columns: [
      { key: 'name', label: '计划名称' }, { key: 'inspectorName', label: '巡检员' },
      { key: 'frequencyType', label: '执行周期' }, { key: 'executionTime', label: '执行时间' },
      { key: 'equipmentCount', label: '设备数' }, { key: 'enabled', label: '状态' },
    ],
    fields: [
      { key: 'name', label: '计划名称', required: true },
      { key: 'inspectorId', label: '巡检员 ID', type: 'number', required: true },
      { key: 'frequencyType', label: '执行周期', type: 'select', required: true, options: frequencyOptions },
      { key: 'executionTime', label: '执行时间', type: 'time', required: true },
      { key: 'weekDays', label: '每周执行日（1-7，逗号分隔）', type: 'number-list' },
      { key: 'monthDays', label: '每月执行日（1-28，逗号分隔）', type: 'number-list' },
      { key: 'startDate', label: '开始日期', type: 'date', required: true },
      { key: 'endDate', label: '结束日期', type: 'date' },
      { key: 'deadlineHours', label: '完成时限（小时）', type: 'number', required: true },
      { key: 'equipmentIds', label: '设备 ID（逗号分隔）', type: 'number-list', required: true },
      { key: 'inspectionItemIds', label: '巡检项目 ID（逗号分隔）', type: 'number-list', required: true },
      { key: 'remark', label: '备注', type: 'textarea' },
    ],
    note: '计划冲突、人员角色和设备与巡检项目类型匹配均由后端校验。',
  },
  tasks: {
    eyebrow: '巡检执行', title: '巡检任务',
    description: '查看定时生成的任务，并执行后端校验的状态操作。',
    basePath: '/inspection-tasks', keywordParam: 'taskNo', filterParam: 'status',
    filters: [
      { value: '', label: '全部状态' }, { value: 'PENDING', label: '待执行' },
      { value: 'IN_PROGRESS', label: '进行中' }, { value: 'COMPLETED', label: '已完成' },
      { value: 'OVERDUE', label: '已逾期' }, { value: 'CANCELLED', label: '已取消' },
    ],
    columns: [
      { key: 'taskNo', label: '任务编号' }, { key: 'planName', label: '计划名称' },
      { key: 'inspectorName', label: '巡检员' }, { key: 'taskDate', label: '任务日期' },
      { key: 'deadlineAt', label: '截止时间' }, { key: 'status', label: '状态' },
    ],
    note: '任务由 Spring Task 自动生成；提交巡检结果需要在任务详情中按设备和项目填写。',
  },
  records: {
    eyebrow: '巡检追溯', title: '巡检记录',
    description: '查询后端保存的设备巡检结果和异常关联。',
    basePath: '/inspection-records', keywordParam: 'taskNo', filterParam: 'normal',
    filters: [
      { value: '', label: '全部结果' }, { value: 'true', label: '正常' },
      { value: 'false', label: '异常' },
    ],
    columns: [
      { key: 'taskNo', label: '关联任务' }, { key: 'equipmentCode', label: '设备编号' },
      { key: 'equipmentName', label: '检查设备' }, { key: 'normal', label: '检查结果' },
      { key: 'inspectorName', label: '巡检员' }, { key: 'completedAt', label: '提交时间' },
    ],
    note: '巡检记录由提交巡检任务产生，页面只提供查询和追溯。',
  },
  faults: {
    eyebrow: '故障中心', title: '异常记录',
    description: '查看真实异常、严重等级及关联维修工单。',
    basePath: '/fault-records', keywordParam: 'faultNo', filterParam: 'faultLevel',
    filters: [
      { value: '', label: '全部等级' }, { value: 'LOW', label: '低' },
      { value: 'MEDIUM', label: '中' }, { value: 'HIGH', label: '高' },
      { value: 'URGENT', label: '紧急' },
    ],
    columns: [
      { key: 'faultNo', label: '异常编号' }, { key: 'equipmentName', label: '异常设备' },
      { key: 'faultType', label: '异常类型' }, { key: 'faultLevel', label: '异常等级' },
      { key: 'workOrderNo', label: '关联工单' }, { key: 'status', label: '处理状态' },
    ],
    note: '异常记录由提交异常巡检结果的事务自动创建，不提供独立新增按钮。',
  },
  orders: {
    eyebrow: '维修闭环', title: '维修工单',
    description: '查询并按角色执行派单、接单、维修提交、验收和取消。',
    basePath: '/work-orders', keywordParam: 'workOrderNo', filterParam: 'status',
    filters: [
      { value: '', label: '全部状态' },
      { value: 'PENDING_ASSIGNMENT', label: '待派单' },
      { value: 'PENDING_ACCEPTANCE', label: '待接单' },
      { value: 'PROCESSING', label: '处理中' },
      { value: 'PENDING_VERIFICATION', label: '待验收' },
      { value: 'COMPLETED', label: '已完成' },
      { value: 'REJECTED', label: '已驳回' },
      { value: 'CANCELLED', label: '已取消' },
    ],
    columns: [
      { key: 'workOrderNo', label: '工单编号' }, { key: 'equipmentName', label: '设备名称' },
      { key: 'priority', label: '优先级' }, { key: 'maintainerName', label: '维修人员' },
      { key: 'createdAt', label: '创建时间' }, { key: 'status', label: '工单状态' },
    ],
    note: '所有状态变化均调用后端接口，前端不直接修改工单状态。',
  },
};

const valueLabels: Record<string, string> = {
  true: '启用', false: '停用', NORMAL: '正常', FAULT: '故障', IN_REPAIR: '维修中',
  DISABLED: '已停用', BOOLEAN: '判断型', NUMBER: '数值型', TEXT: '文本型',
  DAILY: '每日', WEEKLY: '每周', MONTHLY: '每月', PENDING: '待执行',
  IN_PROGRESS: '进行中', COMPLETED: '已完成', OVERDUE: '已逾期', CANCELLED: '已取消',
  LOW: '低', MEDIUM: '中', HIGH: '高', URGENT: '紧急', REPORTED: '已上报',
  ORDER_CREATED: '已生成工单', PROCESSING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭',
  PENDING_ASSIGNMENT: '待派单', PENDING_ACCEPTANCE: '待接单',
  PENDING_VERIFICATION: '待验收', REJECTED: '已驳回',
};

function displayValue(value: unknown, key?: string): string {
  if (value === null || value === undefined || value === '') return '—';
  if (typeof value === 'boolean') {
    if (key === 'normal') return value ? '正常' : '异常';
    if (key === 'required') return value ? '必填' : '选填';
    return value ? '启用' : '停用';
  }
  if (Array.isArray(value)) {
    return value.map((item) => typeof item === 'object' && item !== null
      ? displayValue((item as BusinessRecord).name ?? (item as BusinessRecord).code ?? (item as BusinessRecord).id)
      : displayValue(item)).join('、') || '—';
  }
  if (typeof value === 'object') return JSON.stringify(value);
  const text = String(value);
  return valueLabels[text] ?? text.replace('T', ' ');
}

function statusTone(value: unknown) {
  const text = displayValue(value);
  if (/故障|异常|紧急|逾期|取消|停用|驳回/.test(text)) return 'danger';
  if (/正常|启用|完成|关闭|解决/.test(text)) return 'success';
  if (/处理|维修|进行中/.test(text)) return 'info';
  if (/待|重要|已上报/.test(text)) return 'warning';
  return 'neutral';
}

function roleCode(): string {
  try {
    const user = JSON.parse(sessionStorage.getItem('smart-inspection-user') || '{}') as { roleCode?: string };
    return user.roleCode || '';
  } catch {
    return '';
  }
}

function formFromDetail(detail: BusinessRecord, fields: Field[]) {
  const result: Record<string, string | boolean> = {};
  fields.forEach((field) => {
    let value = detail[field.key];
    if (field.key === 'equipmentIds' && Array.isArray(detail.equipment)) {
      value = detail.equipment.map((item) => (item as BusinessRecord).id);
    }
    if (field.key === 'inspectionItemIds' && Array.isArray(detail.inspectionItems)) {
      value = detail.inspectionItems.map((item) => (item as BusinessRecord).id);
    }
    result[field.key] = typeof value === 'boolean'
      ? value
      : Array.isArray(value) ? value.join(',') : value == null ? '' : String(value);
  });
  return result;
}

function buildPayload(form: Record<string, string | boolean>, fields: Field[]) {
  const payload: Record<string, unknown> = {};
  fields.forEach((field) => {
    const value = form[field.key];
    if (field.type === 'boolean') {
      payload[field.key] = Boolean(value);
    } else if (field.type === 'number') {
      if (value !== '') payload[field.key] = Number(value);
    } else if (field.type === 'number-list') {
      if (typeof value === 'string' && value.trim()) {
        payload[field.key] = value.split(',').map((item) => Number(item.trim())).filter(Number.isFinite);
      }
    } else if (typeof value === 'string' && value.trim()) {
      payload[field.key] = value.trim();
    }
  });
  if (payload.frequencyType === 'DAILY') {
    delete payload.weekDays;
    delete payload.monthDays;
  } else if (payload.frequencyType === 'WEEKLY') {
    delete payload.monthDays;
  } else if (payload.frequencyType === 'MONTHLY') {
    delete payload.weekDays;
  }
  return payload;
}

export default function LiveModulePage({ module }: { module: LiveModuleKey }) {
  const config = configs[module];
  const [page, setPage] = useState<PageResult<BusinessRecord>>({ pageNum: 1, pageSize: 10, total: 0, records: [] });
  const [pageNum, setPageNum] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [filter, setFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState<BusinessRecord | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<Record<string, string | boolean>>({});
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const query: Record<string, string | number | boolean | undefined> = {
        pageNum, pageSize: 10,
        [config.keywordParam]: keyword.trim() || undefined,
      };
      if (config.filterParam && filter) {
        query[config.filterParam] = filter === 'true' ? true : filter === 'false' ? false : filter;
      }
      setPage(await businessApi.page(config.basePath, query));
      setError('');
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '数据加载失败');
    } finally {
      setLoading(false);
    }
  }, [config, filter, keyword, pageNum]);

  useEffect(() => { void load(); }, [load]);

  const totalPages = Math.max(1, Math.ceil(page.total / page.pageSize));
  const summary = useMemo(() => {
    const abnormal = page.records.filter((row) => /FAULT|URGENT|OVERDUE|REJECTED|false/.test(String(row.status ?? row.faultLevel ?? row.normal))).length;
    const active = page.records.filter((row) => /true|NORMAL|IN_PROGRESS|PROCESSING|COMPLETED/.test(String(row.enabled ?? row.status))).length;
    return [
      { label: '符合条件', value: String(page.total) },
      { label: '当前页', value: String(page.records.length), tone: 'blue' },
      { label: '正常 / 进行中', value: String(active), tone: 'teal' },
      { label: '需关注', value: String(abnormal), tone: 'orange' },
    ];
  }, [page]);

  function notify(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(''), 2600);
  }

  async function openDetail(id: number) {
    try {
      setSelected(await businessApi.detail(config.basePath, id));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '详情加载失败');
    }
  }

  function openCreate() {
    const initial: Record<string, string | boolean> = {};
    config.fields?.forEach((field) => {
      initial[field.key] = field.type === 'boolean' ? true
        : field.key === 'deadlineHours' ? '24'
          : field.options?.[0]?.value ?? '';
    });
    setEditingId(null);
    setForm(initial);
    setShowForm(true);
  }

  async function openEdit(id: number) {
    if (!config.fields) return;
    try {
      const detail = await businessApi.detail(config.basePath, id);
      setEditingId(id);
      setForm(formFromDetail(detail, config.fields));
      setShowForm(true);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '详情加载失败');
    }
  }

  async function save() {
    if (!config.fields) return;
    setSaving(true);
    try {
      const payload = buildPayload(form, config.fields);
      if (editingId == null) await businessApi.create(config.basePath, payload);
      else await businessApi.update(config.basePath, editingId, payload);
      setShowForm(false);
      notify(editingId == null ? '新增成功' : '修改成功');
      await load();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '保存失败');
    } finally {
      setSaving(false);
    }
  }

  async function toggleStatus(row: BusinessRecord) {
    const next = !Boolean(row.enabled);
    if (!window.confirm(`确认${next ? '启用' : '停用'}这条数据吗？`)) return;
    try {
      await businessApi.updateStatus(config.basePath, row.id, next);
      notify(next ? '已启用' : '已停用');
      await load();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '状态修改失败');
    }
  }

  async function remove(row: BusinessRecord) {
    if (!window.confirm('确认删除这条数据吗？删除后无法恢复。')) return;
    try {
      await businessApi.remove(config.basePath, row.id);
      notify('删除成功');
      await load();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '删除失败');
    }
  }

  async function action(id: number, name: string, payload?: Record<string, unknown>) {
    try {
      await businessApi.action(config.basePath, id, name, payload);
      notify('操作成功');
      setSelected(null);
      await load();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : '操作失败');
    }
  }

  async function changeEquipmentStatus(row: BusinessRecord) {
    const current = String(row.status);
    const next = current === 'DISABLED' ? 'NORMAL' : 'DISABLED';
    const reason = window.prompt(`请输入${next === 'DISABLED' ? '停用' : '恢复'}原因`);
    if (!reason) return;
    try {
      await businessApi.patch(config.basePath, row.id, 'status', { status: next, reason });
      notify(next === 'DISABLED' ? '设备已停用' : '设备已恢复正常');
      await load();
    } catch (reasonValue) {
      setError(reasonValue instanceof Error ? reasonValue.message : '设备状态修改失败');
    }
  }

  async function submitInspectionTask(row: BusinessRecord) {
    try {
      const detail = await businessApi.detail(config.basePath, row.id);
      const equipment = Array.isArray(detail.equipment) ? detail.equipment as BusinessRecord[] : [];
      const equipmentResults: Record<string, unknown>[] = [];
      for (const device of equipment) {
        const inspectionItems = Array.isArray(device.inspectionItems)
          ? device.inspectionItems as BusinessRecord[] : [];
        const itemResults: Record<string, unknown>[] = [];
        for (const item of inspectionItems) {
          const resultValue = window.prompt(
            `${displayValue(device.equipmentName)} / ${displayValue(item.itemName)}\n` +
            `标准：${displayValue(item.standardDescription)}\n请输入检查结果`,
          );
          if (resultValue === null) return;
          const normalText = window.prompt('结果是否正常？输入 Y 或 N', 'Y');
          if (normalText === null) return;
          itemResults.push({
            inspectionItemId: Number(item.inspectionItemId),
            resultValue,
            normal: normalText.trim().toUpperCase() !== 'N',
          });
        }
        const normal = itemResults.every((item) => item.normal === true);
        const result: Record<string, unknown> = {
          equipmentId: Number(device.equipmentId), normal, itemResults,
        };
        const remark = window.prompt(`${displayValue(device.equipmentName)} 的巡检备注，可留空`);
        if (remark) result.remark = remark;
        if (!normal) {
          const faultType = window.prompt('请输入异常类型');
          const faultLevel = window.prompt('请输入异常等级：LOW / MEDIUM / HIGH / URGENT', 'MEDIUM');
          const description = window.prompt('请输入异常描述');
          if (!faultType || !faultLevel || !description) {
            setError('异常设备必须填写异常类型、等级和描述');
            return;
          }
          result.fault = { faultType, faultLevel: faultLevel.toUpperCase(), description, imageUrls: [] };
        }
        equipmentResults.push(result);
      }
      if (equipmentResults.length === 0) {
        setError('任务详情中没有可提交的设备');
        return;
      }
      await action(row.id, 'submit', { equipmentResults });
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '巡检结果提交失败');
    }
  }

  function taskActions(row: BusinessRecord) {
    const role = roleCode();
    const status = String(row.status);
    return <>
      {role === 'INSPECTOR' && (status === 'PENDING' || status === 'OVERDUE') &&
        <button onClick={() => void action(row.id, 'start')}>开始巡检</button>}
      {role === 'INSPECTOR' && status === 'IN_PROGRESS' &&
        <button onClick={() => void submitInspectionTask(row)}>提交结果</button>}
      {role === 'ADMIN' && !['COMPLETED', 'CANCELLED'].includes(status) &&
        <button onClick={() => { const reason = window.prompt('请输入取消原因'); if (reason) void action(row.id, 'cancel', { reason }); }}>取消</button>}
    </>;
  }

  function orderActions(row: BusinessRecord) {
    const role = roleCode();
    const status = String(row.status);
    return <>
      {role === 'ADMIN' && status === 'PENDING_ASSIGNMENT' && <button onClick={() => {
        const maintainerId = window.prompt('请输入维修人员 ID');
        const dueAt = window.prompt('请输入截止时间，例如 2026-09-16T18:00:00');
        if (maintainerId && dueAt) void action(row.id, 'assign', { maintainerId: Number(maintainerId), dueAt });
      }}>派单</button>}
      {role === 'MAINTAINER' && status === 'PENDING_ACCEPTANCE' && <button onClick={() => void action(row.id, 'accept')}>接单</button>}
      {role === 'MAINTAINER' && status === 'PROCESSING' && <button onClick={() => {
        const solution = window.prompt('请输入维修方案');
        const resultDescription = window.prompt('请输入维修结果');
        const images = window.prompt('请输入已上传的图片 URL，多个用逗号分隔');
        if (solution && resultDescription && images) void action(row.id, 'submit-result', {
          solution, resultDescription, imageUrls: images.split(',').map((value) => value.trim()).filter(Boolean),
        });
      }}>提交维修结果</button>}
      {role === 'ADMIN' && status === 'PENDING_VERIFICATION' && <button onClick={() => {
        const decision = window.prompt('输入 PASS 表示通过，输入 REJECT 表示驳回');
        const remark = window.prompt('请输入验收说明');
        if (remark && decision && ['PASS', 'REJECT'].includes(decision.toUpperCase())) {
          void action(row.id, 'verify', { passed: decision.toUpperCase() === 'PASS', remark });
        }
      }}>验收</button>}
      {role === 'MAINTAINER' && status === 'REJECTED' && <button onClick={() => {
        const remark = window.prompt('请输入重新处理说明');
        if (remark) void action(row.id, 'rework', { remark });
      }}>重新处理</button>}
      {role === 'ADMIN' && !['COMPLETED', 'CANCELLED'].includes(status) && <button onClick={() => {
        const reason = window.prompt('请输入取消原因');
        if (reason) void action(row.id, 'cancel', { reason });
      }}>取消</button>}
    </>;
  }

  return <div className="page-stack module-page">
    <section className="module-header">
      <div><p>{config.eyebrow}</p><h1>{config.title}</h1><span>{config.description}</span></div>
      {config.fields && <button className="button primary" onClick={openCreate}>＋ 新增{config.title}</button>}
    </section>

    {module === 'orders' && <div className="workflow-strip">{['待派单', '待接单', '处理中', '待验收', '已完成'].map((step, index) => <div key={step}><i>{index + 1}</i><span>{step}</span>{index < 4 && <b>→</b>}</div>)}</div>}

    <section className="summary-grid">{summary.map((item) => <article key={item.label} className={item.tone ? `summary-card ${item.tone}` : 'summary-card'}><span>{item.label}</span><strong>{item.value}</strong></article>)}</section>
    {error && <p className="module-note"><i>!</i>{error}</p>}

    <section className="data-panel">
      <div className="data-toolbar">
        <div className="search-box"><span>⌕</span><input value={keyword} onChange={(event) => { setKeyword(event.target.value); setPageNum(1); }} placeholder={`搜索${config.title}…`} /></div>
        <select value={filter} onChange={(event) => { setFilter(event.target.value); setPageNum(1); }}>{config.filters.map((item) => <option value={item.value} key={item.value}>{item.label}</option>)}</select>
        <button className="toolbar-button" onClick={() => { setKeyword(''); setFilter(''); setPageNum(1); }}>重置</button>
        <span className="result-count">{loading ? '正在加载…' : `共 ${page.total} 条真实记录`}</span>
      </div>
      <div className="data-table-wrap">
        <table className="data-table"><thead><tr>{config.columns.map((column) => <th key={column.key}>{column.label}</th>)}<th>操作</th></tr></thead><tbody>
          {page.records.map((row) => <tr key={row.id}>{config.columns.map((column) => {
            const value = row[column.key];
            const tagged = ['status', 'faultLevel', 'priority', 'enabled', 'normal', 'required'].includes(column.key);
            return <td key={column.key}>{tagged ? <span className={`tag ${statusTone(value)}`}>{displayValue(value, column.key)}</span> : displayValue(value, column.key)}</td>;
          })}<td><button onClick={() => void openDetail(row.id)}>详情</button>{config.fields && <button onClick={() => void openEdit(row.id)}>编辑</button>}{config.statusToggle && <button onClick={() => void toggleStatus(row)}>{row.enabled ? '停用' : '启用'}</button>}{module === 'equipment' && ['NORMAL', 'DISABLED'].includes(String(row.status)) && <button onClick={() => void changeEquipmentStatus(row)}>{row.status === 'DISABLED' ? '恢复' : '停用'}</button>}{config.canDelete && <button onClick={() => void remove(row)}>删除</button>}{module === 'tasks' && taskActions(row)}{module === 'orders' && orderActions(row)}</td></tr>)}
        </tbody></table>
        {!loading && page.records.length === 0 && <div className="empty-state"><strong>没有找到真实数据</strong><span>请调整搜索条件，或先通过相应业务创建数据</span></div>}
      </div>
      <div className="table-footer"><span>第 {pageNum} 页，共 {totalPages} 页</span><div><button disabled={pageNum <= 1} onClick={() => setPageNum((value) => value - 1)}>‹</button><button className="current">{pageNum}</button><button disabled={pageNum >= totalPages} onClick={() => setPageNum((value) => value + 1)}>›</button></div></div>
    </section>
    <p className="module-note"><i>i</i>{config.note}</p>

    {selected && <div className="drawer-layer" onMouseDown={() => setSelected(null)}><aside className="detail-drawer" onMouseDown={(event) => event.stopPropagation()}><div className="drawer-head"><div><span>{config.eyebrow}</span><h2>{displayValue(selected.name ?? selected.taskNo ?? selected.faultNo ?? selected.workOrderNo ?? selected.code)}</h2></div><button onClick={() => setSelected(null)}>×</button></div><div className="detail-list">{Object.entries(selected).map(([key, value]) => <div key={key}><span>{config.columns.find((column) => column.key === key)?.label ?? key}</span><strong>{displayValue(value, key)}</strong></div>)}</div><button className="button primary drawer-action" onClick={() => setSelected(null)}>关闭详情</button></aside></div>}

    {showForm && config.fields && <div className="modal-layer" onMouseDown={() => !saving && setShowForm(false)}><div className="form-modal" onMouseDown={(event) => event.stopPropagation()}><div className="modal-head"><div><span>{config.eyebrow}</span><h2>{editingId == null ? `新增${config.title}` : `编辑${config.title}`}</h2></div><button disabled={saving} onClick={() => setShowForm(false)}>×</button></div><div className="form-grid">{config.fields.map((field) => <label key={field.key}><span>{field.label}{field.required ? ' *' : ''}</span>{field.type === 'textarea' ? <textarea value={String(form[field.key] ?? '')} onChange={(event) => setForm({ ...form, [field.key]: event.target.value })} /> : field.type === 'select' ? <select value={String(form[field.key] ?? '')} onChange={(event) => setForm({ ...form, [field.key]: event.target.value })}>{field.options?.map((option) => <option value={option.value} key={option.value}>{option.label}</option>)}</select> : field.type === 'boolean' ? <input type="checkbox" checked={Boolean(form[field.key])} onChange={(event) => setForm({ ...form, [field.key]: event.target.checked })} /> : <input type={field.type === 'number-list' ? 'text' : field.type ?? 'text'} value={String(form[field.key] ?? '')} onChange={(event) => setForm({ ...form, [field.key]: event.target.value })} />}</label>)}</div><p className="modal-tip">确认后将提交到后端并写入数据库，业务规则由后端校验。</p><div className="modal-actions"><button className="button secondary" disabled={saving} onClick={() => setShowForm(false)}>取消</button><button className="button primary" disabled={saving} onClick={() => void save()}>{saving ? '正在提交…' : '确认提交'}</button></div></div></div>}
    {toast && <div className="app-toast">✓ {toast}</div>}
  </div>;
}
