# MyReport — Agent Context

Word 报告生成服务：根据 JSON 节点与全局配置，异步生成 `.docx`，并通过 Redis 回写进度。

## Tech Stack

| Layer | Choice |
|-------|--------|
| Language | Java 8 (`maven.compiler.source/target=1.8`) |
| Framework | Spring Boot 2.7.18（JAR，非 WAR） |
| API | Spring Web MVC REST |
| Persistence | Spring Data JPA + MySQL 8（`entity`/`repository` 包尚空，ddl-auto=update） |
| Cache / 进度 | Jedis 连接池（自研 `framework.redis`，非 Spring Data Redis 客户端） |
| Word 引擎 | Spire.Doc 11.1.1（`lib/spire.doc-11.1.1.jar`，system scope）+ Apache POI 4.1.2 |
| JSON | Fastjson 1.2.83 |
| Test | JUnit 5 |
| Port | `9091`（`SERVER_PORT`） |

## Architecture

单体：`Controller` 接请求 → `SpireReportUtil` 开线程生成 Word → Redis 写进度/锁。无独立 Service 层；核心逻辑在 `util.word`。

```
POST /report/createReport
  → ReportController
  → SpireReportUtil.createReport（校验 + 异步）
  → createSingleReport / doCreateReport
  → TemplateUtil / WordUtil / TableUtil / Chart…
  → RedisFileStateUtil（进度）+ 落盘 .docx
```

## Package Map（`com.myreport`）

| Package | Purpose |
|---------|---------|
| `controller` | REST 入口（报告生成、学校/就业导入、模版、**报告管理**） |
| `vo` | 请求体（如 `CreateReportVO`） |
| `util.word` | Spire 报告主流程（`SpireReportUtil`） |
| `util.word.common` | 模板、文本、坐标、高度、异常收集等 |
| `util.word.common.table` | 表格（Spire / POI） |
| `util` | `Constant`、`FileUtil` |
| `framework.redis` | Jedis 池、`RedisTemplate`、文件进度 |
| `framework.jpa` | `@EntityScan` / `@EnableJpaRepositories` |
| `entity` / `repository` | JPA：学校、就业、模版、**报告实例**等 |
| `service` | 学校/就业/模版/**报告管理**业务 |

## Key Entry Points

| 场景 | 路径 |
|------|------|
| 启动类 | `src/main/java/com/myreport/ReportApplication.java` |
| 创建报告 API | `controller/ReportController.java` → `POST /report/createReport` |
| 报告生成核心 | `util/word/SpireReportUtil.java` |
| 模板常量 | `util/word/WordContant.java` |
| Redis Key | `util/Constant.java` → `RedisKey` |
| 配置 | `src/main/resources/application.yml` + `.env.example` |
| Word 模板资源 | `src/main/resources/template/`（大量 `.docx`） |
| 本地 Spire JAR | `lib/spire.doc-11.1.1.jar` |

## API Snapshot

**`POST /report/createReport`**

- Body：`CreateReportVO`（`reportId` 必填；`reportName`、`lSchoolId`、`reportJsonArr`、`overallSetting`、动态 `attributes`）
- 成功：`{ "code": 0, "message": "报告生成任务已提交", "reportId": ... }`
- 失败：`{ "code": -1, "message": "..." }`

生成在后台线程执行；客户端应通过 Redis 进度 key（见 `Constant.RedisKey`）轮询状态，而非同步等待文件。

**报告管理（管理台一键生成）** — 详见 `docs/report_manage.md`

- `POST /managed-report/create`：名称 + 学校 + 模版
- `POST /managed-report/generate`：读模版树、聚合就业指标、组装 `reportJsonArr` 后触发生成；`reportId` = 实例 id
- 管理台：`/admin/index.html#report`

## Build & Run

```bash
make infra-up          # MySQL + Redis（docker compose）
make run               # mvn spring-boot:run
make build             # mvn compile
make test              # mvn test
make package           # mvn package -DskipTests
make docker-build      # 镜像 myreport
```

环境变量见 `.env.example`：`SERVER_PORT`、`MYSQL_*`、`REDIS_*`。勿提交含真实密码的 `.env`。

## Conventions for Agents

- **Java 8**：避免使用 9+ 语法（`var`、`record`、text blocks 等）。
- **最小改动**：报告逻辑集中在 `util.word`；改 API 契约时同步 `docs/`（见 `.cursor/rules/api-change-frontend-docs.mdc`）。
- **Spire JAR**：改依赖需保留 `lib/` + pom `systemPath`，且 `spring-boot-maven-plugin` 已 `includeSystemScope`。
- **异步与 Redis**：生成失败路径需清理 lock / 更新进度（参考 `SpireReportUtil` 现有 `catch`）。
- **分层演进**：新增持久化放 `entity` + `repository`；可抽 Service，但勿平行再造一套 Redis 客户端。
- **提交**：约定式提交（`feat`/`fix`/`docs`/…）；中文或英文均可，与仓库历史一致即可。

## Where to Look

| I want to… | Look at… |
|------------|----------|
| 改创建报告接口 | `ReportController` + `CreateReportVO` |
| 改 Word 生成流程 | `SpireReportUtil` |
| 改章节/模板渲染 | `TemplateUtil`、`WordUtil` |
| 改表格 | `common/table/*` |
| 改进度/锁 | `RedisFileStateUtil`、`Constant.RedisKey` |
| 加数据表 | `entity` + `repository` + `application.yml` datasource |
| 改端口/DB/Redis | `application.yml`、`.env.example`、`docker-compose.yml` |

## Docs & Rules

- 项目记忆：`.cursor/rules/myreport-context.mdc`
- 开发约定：`.cursor/rules/project-conventions.mdc`
- 需求点名制交付：`.cursor/rules/requirements-delivery-pipeline.mdc`
- 接口变更调用方文档：`.cursor/rules/api-change-frontend-docs.mdc`
- 安全护栏：`.cursor/rules/security-guardrails.mdc`
