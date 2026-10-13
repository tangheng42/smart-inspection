export type NavigationItem = {
  href: string;
  label: string;
  short: string;
  group: '工作台' | '基础资料' | '巡检中心' | '故障工单';
};

export const navigation: NavigationItem[] = [
  { href: '/dashboard', label: '运营总览', short: '总', group: '工作台' },
  { href: '/users', label: '用户管理', short: '人', group: '基础资料' },
  { href: '/areas', label: '区域管理', short: '区', group: '基础资料' },
  { href: '/equipment-types', label: '设备类型', short: '类', group: '基础资料' },
  { href: '/equipment', label: '设备台账', short: '设', group: '基础资料' },
  { href: '/inspection-items', label: '巡检项目', short: '项', group: '巡检中心' },
  { href: '/inspection-plans', label: '巡检计划', short: '计', group: '巡检中心' },
  { href: '/inspection-tasks', label: '巡检任务', short: '任', group: '巡检中心' },
  { href: '/inspection-records', label: '巡检记录', short: '录', group: '巡检中心' },
  { href: '/fault-records', label: '异常记录', short: '异', group: '故障工单' },
  { href: '/work-orders', label: '维修工单', short: '单', group: '故障工单' },
  { href: '/notifications', label: '消息通知', short: '信', group: '故障工单' },
  { href: '/statistics', label: '数据统计', short: '统', group: '工作台' },
];

