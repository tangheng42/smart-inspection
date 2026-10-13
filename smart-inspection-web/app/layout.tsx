import type { Metadata } from 'next';
import './globals.css';

const siteOrigin = process.env.NEXT_PUBLIC_SITE_URL ?? 'http://localhost:3000';

export const metadata: Metadata = {
  metadataBase: new URL(siteOrigin),
  title: '智巡云｜智能设备巡检与故障工单平台',
  description: '智巡云智能设备巡检与故障工单平台',
  openGraph: {
    title: '智巡云',
    description: '智能设备巡检与故障工单平台',
    type: 'website',
    images: [
      {
        url: '/og.png',
        width: 1200,
        height: 630,
        alt: '智巡云——智能设备巡检与故障工单平台',
      },
    ],
  },
  twitter: {
    card: 'summary_large_image',
    title: '智巡云',
    description: '智能设备巡检与故障工单平台',
    images: ['/og.png'],
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body>{children}</body>
    </html>
  );
}
