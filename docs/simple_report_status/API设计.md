# API 设计：简化报告生成状态展示

**需求来源：** `docs/simple_report_status/PRD分析.md`、`技术方案.md`  
**功能短名：** `simple_report_status`  
**调用方文档（落地摘要）：** `docs/simple_report_status.md`  
**关联主契约：** `docs/ai_simple_report/API设计.md`、`docs/ai_simple_report.md`  
**响应风格：** 与现有一致 — `code`（0 成功 / -1 失败）+ `message`；HTTP 默认 200，业务成败看 `code`  
**路径前缀：** `/simple-report`（**无新路径**）  
**状态：** DRAFT — 仅契约，不编码  

**本项约定（对齐技术方案）：**

| 项 | 结论 |
|----|------|
| 变更形态 | **仅出站字段增量**；不新增/不改 URL、方法、入站参数 |
| 状态事实源 | `simple_report_run.run_status`（1/2/3/4/5）；`null` = 从未有 run |
| 列表聚合 | 取该配置下最近一次运行（`create_time DESC`）；无 run → `null` |
| 配置启用 | 字段名仍为 `status`（0/1），与生成态分离 |
| 下载字段 | `canDownload` / `latestSuccessRunId` 语义**不变**（仍仅 SUCCESS 可下载稿） |

---

## 涉及调用方

| 调用方 | 需改点 |
|--------|--------|
| 管理台 `/admin/index.html#simple-report` | 列表读 `latestRunStatus` 等字段；「配置状态」与「生成状态」分列；按态配色；页内有生成中时轮询 list |
| OpenClaw / Comate Skill | 若展示配置列表，改用新字段渲染生成态；文案表与 §2.2 对齐 |
| 仅调 `runs/detail` 轮询的客户端 | **无强制改动**（`runStatus` 不变）；可选复用同一文案/色映射 |
| `POST /report/createReport` / `managed-report` | **无改动** |

---

## 1. 接口一览（本期变更范围）

| 方法 | 路径 | 变更 |
|------|------|------|
| GET | `/simple-report/list` | **出站增量**：`latestRunStatus` / `latestRunId` / `latestFailMessage` |
| GET | `/simple-report/detail` | **出站增量**：同上三字段（技术方案默认同期） |
| 其它 `/simple-report/*` | — | **无契约变更** |

---

## 2. 公共约定

### 2.1 成功 / 失败

与现网一致，不变。

### 2.2 生成状态枚举（出站）

与既有 Run `runStatus` **完全同一套**；列表/详情用字段名 `latestRunStatus`：

| 值 | 含义 | 建议文案（调用方） |
|----|------|-------------------|
| `null` | 该配置下尚无任何 run | 未生成 |
| 1 | PENDING_CONFIRM | 待确认 |
| 2 | GENERATING | 生成中 |
| 3 | SUCCESS | 成功 |
| 4 | FAILED | 失败 |
| 5 | CANCELLED | 已取消 |

> 文案由调用方展示；服务端**不**强制返回 `latestRunStatusLabel`（避免双端文案漂移时改两次；若后续需要可再加 Should 字段）。

### 2.3 「列表展示用」聚合规则（服务端保证）

对单个配置 `id`（即 `reportId`）：

1. 取任意态 `create_time DESC, id DESC` 的第一条；若有 → 用其填充 `latestRun*`  
2. 否则：`latestRunStatus = null`，`latestRunId = null`，`latestFailMessage = null`

> 曾采用「优先待确认/生成中」，会导致历史未确认计划盖住更新的成功态；已改为始终取最近一次运行。

**与下载字段关系：**

| 字段 | 规则 |
|------|------|
| `latestRunStatus` | 按上表聚合（可为待确认/失败等） |
| `canDownload` / `latestSuccessRunId` | **仍**只看最近可下载的 SUCCESS 稿；可与 `latestRunStatus` 同时「待确认 + canDownload=true」 |

### 2.4 `latestFailMessage` 填充规则

| `latestRunStatus` | `latestFailMessage` |
|-------------------|---------------------|
| 4（失败） | 该展示 run 的 `failMessage`（可空字符串视为无详情） |
| 其它 / null | 固定 `null`（不回传历史失败文案，避免误导） |

---

## 3. GET `/simple-report/list`（增量）

### 3.1 请求参数

**不变。**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 从 0，默认 0 |
| size | int | 否 | 默认 20，最大 200 |
| status | int | 否 | 按**配置启用**状态筛（0/1），与生成态无关 |

### 3.2 成功响应（字段全量示例）

```json
{
  "code": 0,
  "message": "ok",
  "content": [
    {
      "id": 1,
      "name": "周报简化",
      "status": 1,
      "deliveryDir": "simple/weekly",
      "notifyEmail": "a@qq.com",
      "scheduleEnabled": 0,
      "cronExpr": null,
      "blockCount": 2,
      "canDownload": true,
      "latestSuccessRunId": 8,
      "latestRunStatus": 2,
      "latestRunId": 12,
      "latestFailMessage": null,
      "createTime": "2026-07-21T10:00:00",
      "updateTime": "2026-07-21T22:00:00"
    },
    {
      "id": 2,
      "name": "新配置",
      "status": 1,
      "deliveryDir": "simple/demo",
      "notifyEmail": null,
      "scheduleEnabled": 0,
      "cronExpr": null,
      "blockCount": 1,
      "canDownload": false,
      "latestSuccessRunId": null,
      "latestRunStatus": null,
      "latestRunId": null,
      "latestFailMessage": null,
      "createTime": "2026-07-21T11:00:00",
      "updateTime": "2026-07-21T11:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

### 3.3 出站字段表（相对旧契约）

| 字段 | 类型 | 变更 | 说明 |
|------|------|------|------|
| status | Integer | 保留 | 配置启用 0/1，**不是**生成状态 |
| canDownload | Boolean | 保留 | |
| latestSuccessRunId | Long \| null | 保留 | |
| **latestRunStatus** | Integer \| null | **新增** | 展示用生成态，见 §2.2 |
| **latestRunId** | Long \| null | **新增** | 展示态对应的 run id；无 run 为 null |
| **latestFailMessage** | String \| null | **新增** | 仅失败态有值 |

### 3.4 失败响应

不变：`{ "code": -1, "message": "查询失败" }`

---

## 4. GET `/simple-report/detail`（增量）

### 4.1 请求参数

**不变：** `id`（Long，必填）

### 4.2 成功响应增量

在现有 detail 字段（含 `blocks`）上增加：

| 字段 | 类型 | 说明 |
|------|------|------|
| latestRunStatus | Integer \| null | 同 list 聚合规则 |
| latestRunId | Long \| null | |
| latestFailMessage | String \| null | |

**示例片段：**

```json
{
  "code": 0,
  "message": "ok",
  "id": 1,
  "name": "周报简化",
  "status": 1,
  "deliveryDir": "simple/weekly",
  "notifyEmail": "a@qq.com",
  "blocks": [],
  "canDownload": false,
  "latestSuccessRunId": null,
  "latestRunStatus": 4,
  "latestRunId": 15,
  "latestFailMessage": "上游接口超时",
  "createTime": "2026-07-21T10:00:00",
  "updateTime": "2026-07-21T22:10:00"
}
```

> 注：若当前 detail 尚未返回 `canDownload` / `latestSuccessRunId`，本期实现时**建议一并与 list 对齐**（Should，与下载能力一致）；本设计强制增量仅为三枚 `latestRun*`。

### 4.3 失败响应

不变：配置不存在等 → `code: -1` + 可读 `message`。

---

## 5. 明确不改的接口

下列接口路径、入站、出站**本项不改**（生成态仍用既有 `runStatus`）：

| 方法 | 路径 |
|------|------|
| POST | `/simple-report/plan` |
| POST | `/simple-report/runs/confirm` |
| POST | `/simple-report/runs/cancel` |
| GET | `/simple-report/runs/detail` |
| GET | `/simple-report/runs/list` |
| GET | `/simple-report/runs/download` |
| GET | `/simple-report/download` |
| 配置 CRUD 等 | `/simple-report/create` … |

**异步 / Redis：** 仍以 `engineReportId` + `report:list:{engineReportId}` 为准；列表侧以 `latestRunStatus` 离散态为主，不在 list 响应中塞 Redis 进度百分比。

---

## 6. 调用方对接要点

### 6.1 管理台列表

```js
// code === 0
row.status              // 配置启用：1 启用 / 0 停用
row.latestRunStatus     // 生成态：null | 1..5
row.latestRunId         // 可跳转 runs/detail 或继续 confirm
row.latestFailMessage   // 失败徽章 title
row.canDownload         // 是否显示下载（与生成态独立）

// 页内存在 latestRunStatus === 2 时，约 3s 轮询本 list
```

### 6.2 文案与颜色（调用方职责）

服务端只给数字/`null`；颜色与中文在管理台实现（见技术方案 §7）。Skill 若展示列表，须使用 §2.2 文案表，**勿**把 `status`（启用）当成生成态。

### 6.3 兼容性

- 旧客户端忽略未知字段 → 行为与改前一致（仍只看见启用态）。  
- 新客户端依赖新字段 → 须容忍 `latestRunStatus === null`。  
- **无**破坏性改名；不删除既有字段。

---

## 7. 验收清单（契约）

| # | 项 | 通过标准 |
|---|-----|----------|
| 1 | list 含三字段 | 任意配置行均出现 `latestRunStatus` / `latestRunId` / `latestFailMessage` 键 |
| 2 | 无 run | 三者均为 `null` |
| 3 | 仅 plan | `latestRunStatus === 1`，`latestRunId` 为该 run |
| 4 | confirm 后 | 变为 `2`，直至成功 `3` 或失败 `4` |
| 5 | 失败 | `latestRunStatus === 4` 且 `latestFailMessage` 与 runs/detail 一致（同 run） |
| 6 | 最近一次优先 | 存在更早待确认、更新成功时，`latestRunStatus` 为 `3` |
| 7 | 下载独立 | 上条场景下 `canDownload` 仍可为 `true` |
| 8 | detail | 同 id 的三字段与 list 聚合结果一致 |

---

## 8. 待确认（契约层残留）

- [ ] **A1** detail 是否强制带 `canDownload` / `latestSuccessRunId`（方案建议 Should 对齐；默认实现时**一起带上**）  
- [ ] **A2** 是否增加只读出站 `latestRunStatusLabel`（默认 **不做**，文案归调用方）

---

*Status: DRAFT — API contract only. 编码时同步落地 `docs/simple_report_status.md` 与主文档交叉链接；点名「开始编码」后再改代码。*
