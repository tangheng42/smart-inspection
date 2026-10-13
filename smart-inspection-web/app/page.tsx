'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';

const API_BASE_URL =
  (process.env.NEXT_PUBLIC_API_BASE_URL ?? '/api').replace(/\/$/, '');

type Feedback = {
  type: 'error' | 'success';
  message: string;
} | null;

type LoginData = {
  userId: number;
  username: string;
  displayName: string;
  roleCode: 'ADMIN' | 'INSPECTOR' | 'MAINTAINER';
  token: string;
  tokenType: string;
  expiresIn: number;
};

type LoginResult = {
  code: number;
  message: string;
  data: LoginData | null;
};

export default function Home() {
  const router = useRouter();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<Feedback>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFeedback(null);

    const normalizedUsername = username.trim();
    if (!normalizedUsername || !password) {
      setFeedback({ type: 'error', message: '请输入账号和密码' });
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: normalizedUsername, password }),
      });

      const payload = await response.json().catch(() => null) as LoginResult | null;
      if (!response.ok || payload?.code !== 0) {
        throw new Error(payload?.message || '登录失败，请检查账号和密码');
      }

      if (!payload?.data?.token) {
        throw new Error('登录响应中缺少 Token');
      }

      sessionStorage.setItem('smart-inspection-token', payload.data.token);
      sessionStorage.setItem('smart-inspection-user', JSON.stringify(payload.data));
      setFeedback({
        type: 'success',
        message: `欢迎回来，${payload.data.displayName || normalizedUsername}`,
      });
      router.push('/dashboard');
    } catch (error) {
      const message =
        error instanceof TypeError
          ? '暂时无法连接后端服务，请确认服务已启动'
          : error instanceof Error
            ? error.message
            : '登录失败，请稍后重试';
      setFeedback({ type: 'error', message });
    } finally {
      setIsSubmitting(false);
    }
  }

  function enterDemo() {
    sessionStorage.setItem('smart-inspection-demo', 'true');
    sessionStorage.setItem('smart-inspection-user', JSON.stringify({ displayName: '演示管理员' }));
    router.push('/dashboard');
  }

  return (
    <main className="login-shell">
      <section className="brand-panel" aria-labelledby="product-title">
        <div className="brand-topline">
          <div className="brand-mark" aria-hidden="true">
            <span />
            <span />
            <span />
          </div>
          <div>
            <p className="brand-name">智巡云</p>
            <p className="brand-en">SMART INSPECTION CLOUD</p>
          </div>
        </div>

        <div className="brand-copy">
          <p className="eyebrow">设备巡检 · 故障闭环 · 工单协同</p>
          <h1 id="product-title">
            让每一次巡检
            <br />
            都有迹可循
          </h1>
          <p className="brand-description">
            从巡检计划到维修验收，用清晰的状态流转连接设备、人员与现场，让隐患更早被发现，让处理进度始终透明。
          </p>
        </div>

        <div className="workflow-card" aria-label="核心业务流程">
          <div className="workflow-heading">
            <div>
              <p>今日业务链路</p>
              <span>实时状态概览</span>
            </div>
            <span className="live-pill"><i />系统在线</span>
          </div>

          <div className="workflow-line">
            {['巡检计划', '巡检任务', '异常上报', '维修工单', '验收关闭'].map(
              (step, index) => (
                <div className="workflow-step" key={step}>
                  <span className={index < 3 ? 'step-dot is-active' : 'step-dot'}>
                    {index + 1}
                  </span>
                  <span>{step}</span>
                </div>
              ),
            )}
          </div>

          <div className="metric-row">
            <div><strong>1,248</strong><span>在管设备</span></div>
            <div><strong>86</strong><span>今日巡检</span></div>
            <div><strong>12</strong><span>待处理工单</span></div>
          </div>
        </div>

        <p className="brand-footer">ZHI XUN YUN · EQUIPMENT OPERATIONS</p>
      </section>

      <section className="login-panel" aria-label="用户登录">
        <div className="mobile-brand">
          <div className="brand-mark" aria-hidden="true">
            <span />
            <span />
            <span />
          </div>
          <div>
            <p className="brand-name">智巡云</p>
            <p className="brand-en">SMART INSPECTION CLOUD</p>
          </div>
        </div>

        <div className="login-card">
          <div className="login-heading">
            <p className="eyebrow">WELCOME BACK</p>
            <h2>登录工作台</h2>
            <p>使用管理员、巡检员或维修人员账号登录</p>
          </div>

          <form onSubmit={handleSubmit} noValidate>
            <label htmlFor="username">登录账号</label>
            <div className="field-wrap">
              <span className="field-prefix" aria-hidden="true">ID</span>
              <input
                id="username"
                name="username"
                type="text"
                autoComplete="username"
                maxLength={32}
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                placeholder="请输入登录账号"
                disabled={isSubmitting}
              />
            </div>

            <div className="label-row">
              <label htmlFor="password">登录密码</label>
              <span>区分大小写</span>
            </div>
            <div className="field-wrap">
              <span className="field-prefix" aria-hidden="true">••</span>
              <input
                id="password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                maxLength={64}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="请输入登录密码"
                disabled={isSubmitting}
              />
              <button
                className="password-toggle"
                type="button"
                onClick={() => setShowPassword((visible) => !visible)}
                aria-label={showPassword ? '隐藏密码' : '显示密码'}
              >
                {showPassword ? '隐藏' : '显示'}
              </button>
            </div>

            {feedback && (
              <p className={`form-feedback is-${feedback.type}`} role="status">
                {feedback.message}
              </p>
            )}

            <button className="submit-button" type="submit" disabled={isSubmitting}>
              <span>{isSubmitting ? '正在验证身份…' : '进入工作台'}</span>
              <span aria-hidden="true">→</span>
            </button>
            <button className="demo-button" type="button" onClick={enterDemo} disabled={isSubmitting}>
              后端未启动？进入演示工作台
            </button>
          </form>

          <div className="security-note">
            <span aria-hidden="true">✓</span>
            <p><strong>安全访问</strong>登录凭证仅用于身份验证，请勿向他人透露账号信息。</p>
          </div>
        </div>

        <p className="login-footer">仅限授权人员访问 · 智巡云设备运维平台</p>
      </section>
    </main>
  );
}
