# 智巡云 · 设备巡检与维修工单平台

基于 Java 17、Spring Boot 3、MyBatis、MySQL 的设备巡检项目，配套 React / TypeScript 管理前端。

## 功能

设备与区域管理、巡检项目与计划、巡检任务与记录、异常上报、维修工单、统计看板、用户权限、文件上传和站内通知。

## 目录

- `smart-inspection/`：Maven 多模块后端，包含 common、pojo、server。
- `smart-inspection-web/`：React 前端，使用 Vinext / Vite。
- [数据库初始化说明](smart-inspection/sql/README.md)
- [接口文档](smart-inspection/docs/智巡云接口文档.md)

## 本地运行

准备 JDK 17、Maven、MySQL 8、Redis、MinIO，以及 Node.js 22.13+ 和 pnpm。

1. 依次执行 `smart-inspection/sql/002_create_core_business_tables.sql` 和 `003_seed_demo_data.sql`，创建数据库与演示数据。
2. 在 IDE 的运行配置或启动终端中设置以下环境变量（后端不会自动加载 `.env` 文件）：

| 变量 | 说明 |
| --- | --- |
| `DB_USERNAME` | MySQL 用户，默认 root |
| `DB_PASSWORD` | 本地 MySQL 密码 |
| `JWT_SECRET_KEY` | 自行生成至少 32 字节的随机密钥 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接参数，默认 localhost:6379 |
| `MINIO_ENDPOINT` | MinIO 地址，默认 http://localhost:9000 |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | 自己的 MinIO 凭据 |
| `MINIO_BUCKET` | 存储桶，默认 smart-inspection |

3. 在 IDE 中导入 `smart-inspection/pom.xml`，运行 `InspectionServerApplication`，默认端口为 8080。
4. 将前端 `.env.example` 复制为 `.env.local`，按后端实际地址设置 API 和 WebSocket 地址。在 `smart-inspection-web` 中执行：

```sh
pnpm install
pnpm dev
```

前端地址为 `http://localhost:3000`。默认示例 API 指向 `http://localhost:8080/api`，WebSocket 指向 `ws://localhost:8080/ws/notifications`。

演示账号见数据库初始化说明，仅供本地学习使用。实际部署需修改演示账号密码，并通过环境变量提供数据库、JWT 和 MinIO 凭据。

RabbitMQ 通知功能默认关闭，可通过 `ZHIXUNYUN_RABBITMQ_ENABLED` 和对应连接参数启用。

## 版本说明

此仓库整理自本地项目当前源码，前后端一并保存。未包含依赖包、构建输出、IDE 设置、压缩备份和原有 Git 历史。
