import Link from 'next/link';

const metrics = [
  { label: '今日待巡检', value: '18', trend: '较昨日 +3', tone: 'teal', mark: '巡' },
  { label: '今日已完成', value: '68', trend: '完成率 79.1%', tone: 'blue', mark: '完' },
  { label: '异常设备', value: '7', trend: '2 个紧急异常', tone: 'orange', mark: '异' },
  { label: '待处理工单', value: '12', trend: '5 单待接单', tone: 'red', mark: '单' },
];

const tasks = [
  ['JX-20260828-014', 'A区配电房日常巡检', '张巡', '09:30', '进行中'],
  ['JX-20260828-015', '空压机组安全巡检', '李明', '10:20', '待执行'],
  ['JX-20260828-016', '教学楼消防设施巡检', '王静', '13:30', '待执行'],
  ['JX-20260828-017', '泵房设备专项巡检', '赵磊', '15:00', '待执行'],
];

const orders = [
  { title: '3号空压机温度异常', meta: 'WO-20260828-008 · 一级紧急', time: '8分钟前', level: 'high' },
  { title: '配电柜指示灯不亮', meta: 'WO-20260828-007 · 二级重要', time: '32分钟前', level: 'middle' },
  { title: '消防水泵异响', meta: 'WO-20260828-006 · 二级重要', time: '1小时前', level: 'middle' },
];

export default function DashboardPage() {
  return (
    <div className="page-stack dashboard-page">
      <section className="page-intro">
        <div><p>2026年8月28日 · 星期五</p><h1>早上好，今天也要守护好每一台设备。</h1><span>当前园区整体运行平稳，仍有 2 项紧急异常需要优先处理。</span></div>
        <div className="intro-actions"><Link className="button secondary" href="/inspection-tasks">查看今日任务</Link><Link className="button primary" href="/work-orders">处理维修工单</Link></div>
      </section>

      <section className="metric-grid">
        {metrics.map((item) => <article className={`metric-card tone-${item.tone}`} key={item.label}><div><span>{item.label}</span><strong>{item.value}</strong><small>{item.trend}</small></div><i>{item.mark}</i></article>)}
      </section>

      <section className="dashboard-grid">
        <article className="panel task-panel">
          <div className="panel-heading"><div><h2>今日巡检任务</h2><p>按计划时间优先展示待处理任务</p></div><Link href="/inspection-tasks">全部任务 →</Link></div>
          <div className="compact-table">
            <div className="table-row table-head"><span>任务编号</span><span>任务名称</span><span>巡检员</span><span>计划时间</span><span>状态</span></div>
            {tasks.map((task) => <div className="table-row" key={task[0]}>{task.slice(0, 4).map((cell) => <span key={cell}>{cell}</span>)}<span><b className={`status-dot ${task[4] === '进行中' ? 'processing' : 'pending'}`}>{task[4]}</b></span></div>)}
          </div>
        </article>

        <article className="panel order-panel">
          <div className="panel-heading"><div><h2>紧急工单</h2><p>需要优先响应的设备故障</p></div><Link href="/work-orders">查看工单 →</Link></div>
          <div className="order-list">{orders.map((order) => <div className="order-item" key={order.title}><span className={`risk-mark ${order.level}`}>!</span><div><strong>{order.title}</strong><span>{order.meta}</span></div><time>{order.time}</time></div>)}</div>
          <div className="order-summary"><span>平均响应时长</span><strong>18<small>分钟</small></strong><div><i style={{ width: '74%' }} /></div><p>优于本月平均水平 12%</p></div>
        </article>
      </section>

      <section className="dashboard-grid lower-grid">
        <article className="panel health-panel">
          <div className="panel-heading"><div><h2>设备运行概况</h2><p>当前 1,248 台设备状态分布</p></div><Link href="/equipment">设备台账 →</Link></div>
          <div className="health-content"><div className="donut"><div><strong>96.4%</strong><span>正常率</span></div></div><div className="legend"><p><i className="normal" /><span>正常运行</span><strong>1,203</strong></p><p><i className="warning" /><span>故障</span><strong>19</strong></p><p><i className="repair" /><span>维修中</span><strong>16</strong></p><p><i className="disabled" /><span>已停用</span><strong>10</strong></p></div></div>
        </article>
        <article className="panel activity-panel">
          <div className="panel-heading"><div><h2>最新动态</h2><p>关键业务操作实时记录</p></div></div>
          <div className="timeline"><p><i className="done" /><span><strong>巡检任务已完成</strong>张巡完成 A区配电房巡检<time>09:18</time></span></p><p><i className="fault" /><span><strong>发现设备异常</strong>3号空压机温度达到 86℃<time>09:06</time></span></p><p><i className="order" /><span><strong>维修人员已接单</strong>陈工接收 WO-20260828-005<time>08:52</time></span></p></div>
        </article>
      </section>
      <p className="demo-hint">当前工作台数据为界面演示数据；对应后端接口开发完成后会自动切换为实时数据。</p>
    </div>
  );
}
