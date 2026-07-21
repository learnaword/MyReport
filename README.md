# MyReport

面向就业质量 / 统计场景的 **Word 报告生成服务**：根据 JSON 节点树与全局配置，异步生成 `.docx`，并通过 Redis 回写进度与锁。

默认端口 **9091**。当前按 **单校默认（武汉大学）** 运行：学校管理产品 API 已下线，就业数据与报告实例默认归属武汉大学。

---

## 功能概览

| 能力 | 说明 |
|------|------|
| 核心报告引擎 | `POST /report/createReport` 提交任务，后台线程用 Spire.Doc 渲染 Word |
| 报告模版配置 | 树形章节 / 指标节点；管理台可维护模版 |
| 报告管理 | 选模版一键聚合就业指标并生成；支持下载 `.docx` |
| 就业去向数据 | Excel 异步导入、分页列表；按默认校写入 |
| AI 简化报告 | 多数据接口配置 → plan（Markdown）→ 确认 → 异步出稿；可选邮件附件 |
| 管理台 | 静态页 `/admin/index.html`：数据 / 模版 / 报告 / 简化报告 |

响应约定：`code` 为 `0` 成功、`-1` 失败，附带 `message`。

---

## 技术栈

| 层级 | 选型 |
|------|------|
| 语言 | Java 8 |
| 框架 | Spring Boot 2.7.18（可执行 JAR） |
| API | Spring Web MVC REST |
| 持久化 | Spring Data JPA + MySQL 8（`ddl-auto=update`） |
| 缓存 / 进度 | Jedis 连接池（自研 `framework.redis`） |
| Word | Spire.Doc 11.1.1（`lib/` system 依赖）+ Apache POI 4.1.2 |
| JSON | Fastjson |
| 邮件（可选） | Spring Mail + QQ SMTP |
| 图表文案（可选） | 通义千问（`DASHSCOPE_*`） |
| 构建 | Maven + Makefile + Docker Compose |

---

## 架构

```
HTTP 请求
  → Controller
  → Service（就业 / 模版 / 报告管理 / 简化报告）或直接进入引擎
  → SpireReportUtil / SimpleReportUtil（后台线程）
  → TemplateUtil / WordUtil / TableUtil / Chart…
  → Redis（进度、锁）+ 落盘 .docx
```

核心生成链路：

```
POST /report/createReport
  → ReportController
  → SpireReportUtil.createReport（校验 + 异步）
  → createSingleReport / doCreateReport
  → RedisFileStateUtil + 输出 .docx
```

报告管理会先按模版树聚合默认校就业指标，组装 `reportJsonArr`，再走同一套引擎；`reportId` 使用管理报告实例 id。

简化报告的引擎 `reportId`（`engineReportId`）= `1000000000 + runId`，避免与 `managed_report.id` 冲突。

---

## 目录结构

```text
MyReport/
├── src/main/java/com/myreport/
│   ├── ReportApplication.java      # 启动类
│   ├── controller/                 # REST 入口
│   ├── service/                    # 业务服务
│   ├── entity/ / repository/       # JPA
│   ├── vo/                         # 请求体
│   ├── config/                     # 邮件、上传、简化报告等配置
│   ├── util/word/                  # Spire 报告主流程
│   ├── util/word/common/           # 模板、文本、表格、坐标等
│   ├── util/excel/                 # Excel 导入解析
│   └── framework/redis/            # Jedis、进度、锁
├── src/main/resources/
│   ├── application.yml
│   ├── static/admin/               # 管理台
│   ├── template/ / word/template/  # Word 模板资源
│   └── ...
├── lib/spire.doc-11.1.1.jar        # Spire 本地包（须随仓库保留）
├── docs/                           # 功能与对接文档
├── docker-compose.yml              # MySQL + Redis
├── Dockerfile
├── Makefile
├── .env.example                    # 环境变量模板
└── AGENTS.md                       # Agent / 开发者上下文
```

---

## 快速开始

### 前置条件

- JDK 8+
- Maven 3.x
- Docker（用于一键起 MySQL / Redis；也可自备实例）

### 1. 配置环境变量

```bash
cp .env.example .env
# 按需修改 MYSQL_*、REDIS_*、SERVER_PORT、MAIL_*、DASHSCOPE_* 等
```

**不要提交含真实密码的 `.env`。** 应用通过 spring-dotenv 读取；含 `&` 的 `MYSQL_URL` 勿用 shell `source`。

### 2. 启动基础设施

```bash
make infra-up          # MySQL(3306) + Redis(6379)
make infra-status      # 查看状态
# make infra-down      # 停止
```

默认库名 `report`，root 密码见 compose / `.env.example`。

### 3. 运行应用

```bash
make run               # mvn spring-boot:run
# 或
make build && make package
java -jar target/MyReport-*.jar
```

浏览器打开：

- 管理台：http://localhost:9091/ 或 http://localhost:9091/admin/index.html

### 4. Docker 运行应用（可选）

```bash
make docker-build
make docker-run        # 映射 9091，读取 .env；需宿主机已有 MySQL/Redis 或自改网络
```

---

## 常用 Make 命令

| 命令 | 说明 |
|------|------|
| `make infra-up` | 启动 MySQL + Redis |
| `make infra-down` | 停止基础设施 |
| `make run` | 本地启动应用 |
| `make build` | `mvn compile` |
| `make test` | `mvn test` |
| `make package` | 打包（跳过测试） |
| `make docker-build` | 构建镜像 `myreport` |
| `make clean` | 清理编译产物 |

---

## 环境变量（摘要）

| 变量 | 说明 |
|------|------|
| `SERVER_PORT` | HTTP 端口，默认 `9091` |
| `MYSQL_URL` / `MYSQL_USERNAME` / `MYSQL_PASSWORD` | 数据源 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` / `REDIS_DB` | Redis |
| `REPORT_TEMPLATE_UPLOAD_DIR` | 模版封面/底图上传根目录（空则 `~/myreport/uploads`） |
| `SIMPLE_REPORT_DELIVERY_ROOT` | 简化报告交付根目录 |
| `MAIL_*` | QQ SMTP 发信（简化报告附件通知） |
| `DASHSCOPE_API_KEY` / `DASHSCOPE_MODEL` | 通义千问图表分析（可选） |
| `CHART_ANALYST_ENABLED` | 是否启用图表 AI 文案 |

完整模板见 [`.env.example`](.env.example)。

---

## 主要 API

### 核心引擎

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/report/createReport` | 提交报告生成任务（异步） |

- Body：`CreateReportVO`（`reportId` 必填；`reportName`、`reportJsonArr`、`overallSetting` 等）
- 成功：`{ "code": 0, "message": "报告生成任务已提交", "reportId": ... }`
- 客户端应通过 Redis 进度 key（见 `Constant.RedisKey`）轮询，勿同步等待文件

### 报告管理

详见 [`docs/report_manage.md`](docs/report_manage.md)、[`docs/report_download.md`](docs/report_download.md)。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/managed-report/create` | 创建报告（名称 + 模版；学校固定武汉大学） |
| POST | `/managed-report/generate` | 聚合指标并异步生成 |
| GET | `/managed-report/download?id=` | 下载 `.docx` |
| GET | `/managed-report/list` | 分页列表 |

同模版仅允许一份未删报告。

### 就业去向

详见 [`docs/grad_employment_excel_import.md`](docs/grad_employment_excel_import.md)、[`docs/admin_console.md`](docs/admin_console.md)。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/grad-employment/list` | 分页列表（仅默认校） |
| POST | `/grad-employment/importExcel` | Excel 异步导入 |
| GET | `/grad-employment/importProgress` | 导入进度 |

### 报告模版

详见 [`docs/report_template_config.md`](docs/report_template_config.md)、[`docs/metric_field_select/API设计.md`](docs/metric_field_select/API设计.md)。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/report-template/stat-fields` | 指标统计字段字典（`value`/`label`） |

METRIC 节点的 `statField` 须取自该字典。

### AI 简化报告

详见 [`docs/ai_simple_report.md`](docs/ai_simple_report.md)。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/simple-report/create` | 创建配置（可含多 blocks） |
| POST | `/simple-report/plan` | 生成待确认 Markdown 计划 |
| POST | `/simple-report/runs/confirm` | 确认后异步出 Word |
| GET | `/simple-report/runs/download` | 下载交付稿 |

区块渲染风格：`TABLE` / `BAR` / `PIE` / `LINE`。Agent / Skill 应调本 API，不以 Word MCP / python-docx 为主路径。

### 已下线

`/school/list`、`/school/importExcel` 等学校管理产品接口已下线。说明见 [`docs/remove_school_manage.md`](docs/remove_school_manage.md)。

---

## 管理台分区

| Hash | 功能 |
|------|------|
| `#data` | 就业去向导入与列表 |
| `#template` | 报告模版配置 |
| `#report` | 报告创建 / 生成 / 下载 |
| `#simple-report` | 简化报告配置与运行 |
| `#school` | 已下线，访问时落到 `#data` |

---

## 文档索引

| 文档 | 内容 |
|------|------|
| [AGENTS.md](AGENTS.md) | 给 Agent / 开发者的项目上下文 |
| [docs/admin_console.md](docs/admin_console.md) | 管理台与列表接口 |
| [docs/report_manage.md](docs/report_manage.md) | 报告管理 |
| [docs/report_download.md](docs/report_download.md) | 报告下载 |
| [docs/report_template_config.md](docs/report_template_config.md) | 模版配置 |
| [docs/grad_employment_excel_import.md](docs/grad_employment_excel_import.md) | 就业 Excel 导入 |
| [docs/ai_simple_report.md](docs/ai_simple_report.md) | AI 简化报告 |
| [docs/remove_school_manage.md](docs/remove_school_manage.md) | 去掉学校管理 / 单校默认 |
| `docs/<feature>/` | 各功能的 PRD、技术方案、库表、API 设计 |

接口变更时须同步更新 `docs/` 对接说明（见仓库规则 `api-change-frontend-docs`）。

---

## 开发约定（摘要）

- **Java 8**：勿用 `var`、`record`、text blocks 等 9+ 语法
- **最小改动**：报告渲染优先落在 `util.word`；勿平行再造 Redis 客户端
- **Spire**：`lib/` + pom `systemPath` 必须一致；打包需 `includeSystemScope`
- **异步失败**：须清理 lock、更新 Redis 进度（对齐现有 `catch`）
- **安全**：参数化查询；勿硬编码密钥；勿在对外 `message` 中暴露堆栈/本机路径
- **提交**：约定式提交（`feat` / `fix` / `docs` / …）

更完整约定见 [AGENTS.md](AGENTS.md) 与 `.cursor/rules/`。

---

## 注意事项

1. 首次运行依赖 MySQL、Redis 可用；表结构由 JPA `ddl-auto=update` 维护。
2. 生成任务异步执行：接口只表示「已提交」，最终状态看 Redis 进度或业务表状态字段。
3. Spire 为商业组件的本地 JAR，缺失会导致编译/打包失败。
4. 邮件、通义千问为可选能力，未配置时相关功能会跳过或降级。
5. 单校模式下，导入就业数据时 Excel 内学校字段不参与归属，一律写入「武汉大学」。
