# AI 简化报告

## 涉及调用方

- 管理台：`/admin/index.html#simple-report`（侧边栏「简化报告」，编码时落地）
- OpenClaw / Comate Skill：调本 API 做 plan → 确认 → 出 Word（不出本地 docx）
- 上游数据接口：按约定返回 `{ labels, values }`

完整契约见：`docs/ai_simple_report/API设计.md`。

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /simple-report/list | 配置分页列表 |
| GET | /simple-report/detail | 配置详情（含 blocks） |
| POST | /simple-report/create | 创建配置（可带多接口区块） |
| POST | /simple-report/update | 更新配置元信息 |
| POST | /simple-report/save-blocks | 整表替换区块 |
| POST | /simple-report/delete | 软删配置 |
| POST | /simple-report/plan | 生成 MD 计划（待确认） |
| PUT | /simple-report/runs/plan | 可选：覆盖 planMd 文案 |
| POST | /simple-report/runs/confirm | 确认后异步生成 Word |
| POST | /simple-report/runs/cancel | 取消待确认 |
| GET | /simple-report/runs/detail | 运行详情 |
| GET | /simple-report/runs/list | 运行列表 |
| GET | /simple-report/runs/download | 按 runId 下载 `.docx` |
| GET | /simple-report/download | 按配置 id 下载最近成功稿 |

列表含 `canDownload` / `latestSuccessRunId`，以及展示用生成态 `latestRunStatus` / `latestRunId` / `latestFailMessage`（与配置启用字段 `status` 分离）。生成状态对接详见 [`docs/simple_report_status.md`](simple_report_status.md)；下载对接详见 [`docs/simple_report_download.md`](simple_report_download.md)。

响应风格：`code`（0 成功 / -1 失败）+ `message`。

## 调用方修改点

### 1. 请求示例 — 创建（多接口）

```json
{
  "name": "周报简化",
  "deliveryDir": "simple/weekly",
  "notifyEmail": "yourname@qq.com",
  "blocks": [
    {
      "title": "学院就业人数",
      "httpMethod": "GET",
      "url": "https://data.example.com/api/emp/by-college",
      "renderStyle": "BAR"
    },
    {
      "title": "去向分布",
      "httpMethod": "GET",
      "url": "https://data.example.com/api/emp/destination",
      "renderStyle": "TABLE"
    }
  ]
}
```

### 2. 编排：plan → confirm

```js
// plan
const plan = await fetch("/simple-report/plan", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ id: 1, userPrompt: "本周概况" })
}).then((r) => r.json());
// code === 0 → 展示 plan.planMd，等人确认

// confirm（异步）
const conf = await fetch("/simple-report/runs/confirm", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ runId: plan.runId })
}).then((r) => r.json());
// code === 0 → 任务已提交；engineReportId 用于 Redis 进度
```

### 3. 进度 / 状态

```text
配置列表 / 详情：latestRunStatus（null未生成 / 1待确认 / 2生成中 / 3成功 / 4失败 / 5取消）
  聚合：该配置下最近一次运行（按创建时间）
  详见 docs/simple_report_status.md

单次运行：GET /simple-report/runs/detail?runId=
  runStatus: 1待确认 / 2生成中 / 3成功 / 4失败 / 5取消

Redis：report:list:{engineReportId}
  engineReportId = 1000000000 + runId

交付：成功后文件在配置的 deliveryDir（detail.deliveryPath）
下载：GET /simple-report/runs/download?runId= 或 GET /simple-report/download?id=
  详见 docs/simple_report_download.md
```

### 4. 上游数据响应

```json
{
  "labels": ["计算机", "机械"],
  "values": [120, 80]
}
```

## 注意事项

- 须人工确认后才出 Word；定时默认也只生成待确认计划
- 勿用 Word MCP / 龙虾本地 python-docx 作为主出稿路径
- 外部自管 `createReport` 的 `reportId` 不要使用 ≥ 1000000000
- URL host 须在服务端白名单内（防 SSRF）；本地联调 demo 需 `allow-loopback=true`
- **邮件**：配置 QQ SMTP 后，报告配置填 `notifyEmail`；生成成功会把 `.docx` 作为附件发出

## QQ 邮箱发信

1. QQ 邮箱 → 设置 → 账户 → 开启 **POP3/SMTP** → 生成**授权码**（不是 QQ 密码）
2. `.env` 示例：

```text
MAIL_ENABLED=true
MAIL_HOST=smtp.qq.com
MAIL_PORT=465
MAIL_USERNAME=你的QQ号@qq.com
MAIL_PASSWORD=授权码
MAIL_FROM_NAME=MyReport
```

3. 简化报告配置里填写「通知邮箱」（收件人，可与发件人相同）
4. 确认生成成功后自动发附件；SMTP 未配齐时跳过发信，Word 仍算成功

## 联调假数据接口

| 方法 | 路径 | 说明 | 建议样式 |
|------|------|------|----------|
| GET | /demo/stat/by-college | 按学院人数；可选 `?year=2024` | BAR |
| GET | /demo/stat/by-destination | 按去向分布 | TABLE / PIE |

响应示例：`{ "labels": ["计算机学院", ...], "values": [128, ...] }`

管理台区块示例（本机 9091）：

```text
学院人数|GET|http://127.0.0.1:9091/demo/stat/by-college|BAR
去向分布|GET|http://127.0.0.1:9091/demo/stat/by-destination|TABLE
```
