# API 设计：AI 简化报告

**需求来源：** `docs/ai_simple_report/PRD分析.md`、`技术方案.md`、`数据库设计.md`  
**功能短名：** `ai_simple_report`  
**调用方文档（落地摘要）：** 同步见 `docs/ai_simple_report.md`  
**响应风格：** 与现有一致 — `code`（0 成功 / -1 失败）+ `message`；HTTP 默认 200，业务成败看 `code`（下载接口除外）  
**路径前缀：** `/simple-report`  
**状态：** DRAFT — 仅契约，不编码  

**本项约定（对齐技术方案 / 库表）：**

| 项 | 结论 |
|----|------|
| 出 Word | 本服务 Spire；Agent **不**本地 python-docx / Word MCP |
| 流程 | `plan`（出 MD）→ **人工** `confirm` → 异步生成 → 目录交付 |
| 多接口 | 配置 `blocks[]`；一份 Word 多区块 |
| 数据契约 | 上游 HTTP 响应须为 `{labels,values}` 或 `{code,data:{labels,values}}` |
| Redis | `engineReportId = 1000000000 + runId`；进度 key `report:list:{engineReportId}` |

---

## 涉及调用方

| 调用方 | 需改点 |
|--------|--------|
| 管理台 `/admin/index.html#simple-report`（新建 panel） | 配置 CRUD、展示 planMd、确认、轮询 run、下载 |
| OpenClaw / Comate Skill（薄客户端） | 调 plan → 展示 MD → 用户确认后 confirm → 轮询 |
| 上游数据接口提供方 | 按 §7 契约返回 JSON |
| `POST /report/createReport` / `managed-report` | **无改动**（id 段约定：外部勿占用 ≥1e9 的 reportId） |

---

## 1. 接口一览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/simple-report/list` | 配置分页列表 |
| GET | `/simple-report/detail` | 配置详情（含 blocks） |
| POST | `/simple-report/create` | 创建配置（可带 blocks） |
| POST | `/simple-report/update` | 更新配置元信息 |
| POST | `/simple-report/save-blocks` | **整表替换**区块列表 |
| POST | `/simple-report/delete` | 软删配置 |
| POST | `/simple-report/plan` | 生成计划 MD，创建 Run（待确认） |
| PUT | `/simple-report/runs/plan` | 可选：仅覆盖 planMd 文案（不改执行快照） |
| POST | `/simple-report/runs/confirm` | 确认执行（异步出 Word） |
| POST | `/simple-report/runs/cancel` | 可选：取消待确认 Run |
| GET | `/simple-report/runs/detail` | Run 详情（含 planMd、状态、路径） |
| GET | `/simple-report/runs/list` | 某配置或全局 Run 列表 |
| GET | `/simple-report/runs/download` | 下载已生成 `.docx` |

---

## 2. 公共约定

### 2.1 成功 / 失败

```json
{ "code": 0, "message": "ok", "...": "业务字段" }
```

```json
{ "code": -1, "message": "可读失败原因" }
```

### 2.2 枚举（出站）

**配置 `status`：** `0` 停用 / `1` 启用  

**区块 `renderStyle`：** `TABLE` \| `BAR` \| `PIE` \| `LINE`  

**区块 `httpMethod`：** `GET` \| `POST`  

**Run `runStatus`：**

| 值 | 含义 |
|----|------|
| 1 | PENDING_CONFIRM 待确认 |
| 2 | GENERATING 生成中 |
| 3 | SUCCESS 成功 |
| 4 | FAILED 失败 |
| 5 | CANCELLED 已取消 |

**Run `triggerType`：** `0` 人工 / `1` 定时  

### 2.3 Block 对象（入站/出站共用）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 否 | 出站有；`save-blocks` 入站可忽略（整表替换按数组顺序重建） |
| sortOrder | Integer | 否 | 不传则按数组下标 |
| title | String | 是 | ≤128 |
| httpMethod | String | 是 | GET/POST |
| url | String | 是 | http(s)；须过 SSRF 白名单 |
| queryJson | String/Object | 否 | 建议传 JSON 对象；服务端可存字符串 |
| bodyJson | String/Object | 否 | POST 可用 |
| renderStyle | String | 是 | TABLE/BAR/PIE/LINE |
| promptHint | String | 否 | ≤512 |

### 2.4 异步与 Redis

| 项 | 约定 |
|----|------|
| confirm 成功 | 仅表示**任务已提交**；Word 异步生成 |
| 进度 | Redis key：`report:list:{engineReportId}`（与现网一致） |
| 锁 | `report:lock:{engineReportId}` |
| 终态 | 以 `GET .../runs/detail` 的 `runStatus` + `deliveryPath` 为准 |
| engineReportId | `1000000000 + runId`，confirm 后才有值 |

---

## 3. 配置类 API

### 3.1 GET `/simple-report/list`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 从 0，默认 0 |
| size | int | 否 | 默认 20，最大 200 |
| status | int | 否 | 按启用状态筛 |

**成功响应：**

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
      "scheduleEnabled": 0,
      "cronExpr": null,
      "blockCount": 2,
      "createTime": "2026-07-21T10:00:00",
      "updateTime": "2026-07-21T10:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### 3.2 GET `/simple-report/detail`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 配置 id |

**成功：**

```json
{
  "code": 0,
  "message": "ok",
  "id": 1,
  "name": "周报简化",
  "status": 1,
  "deliveryDir": "simple/weekly",
  "scheduleEnabled": 0,
  "cronExpr": null,
  "blocks": [
    {
      "id": 10,
      "sortOrder": 0,
      "title": "学院就业人数",
      "httpMethod": "GET",
      "url": "https://data.example.com/api/emp/by-college",
      "queryJson": "{\"year\":2024}",
      "bodyJson": null,
      "renderStyle": "BAR",
      "promptHint": "按学院统计人数"
    },
    {
      "id": 11,
      "sortOrder": 1,
      "title": "去向分布",
      "httpMethod": "GET",
      "url": "https://data.example.com/api/emp/destination",
      "queryJson": null,
      "bodyJson": null,
      "renderStyle": "TABLE",
      "promptHint": null
    }
  ],
  "createTime": "2026-07-21T10:00:00",
  "updateTime": "2026-07-21T10:00:00"
}
```

### 3.3 POST `/simple-report/create`

**请求：**

```json
{
  "name": "周报简化",
  "deliveryDir": "simple/weekly",
  "status": 1,
  "scheduleEnabled": 0,
  "cronExpr": null,
  "blocks": [
    {
      "title": "学院就业人数",
      "httpMethod": "GET",
      "url": "https://data.example.com/api/emp/by-college",
      "queryJson": { "year": 2024 },
      "renderStyle": "BAR",
      "promptHint": "按学院统计人数"
    }
  ]
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| name | 是 | 未删唯一 |
| deliveryDir | 是 | 无 `../`；落在 delivery-root 下 |
| status | 否 | 默认 1 |
| scheduleEnabled | 否 | 默认 0；为 1 时 cronExpr 必填 |
| blocks | 否 | 可空，之后 `save-blocks`；plan 前至少 1 块 |

**成功：** `{ "code": 0, "message": "创建成功", "id": 1 }`  

**失败示例：** 名称冲突、目录非法、URL host 不在白名单、renderStyle 非法。

### 3.4 POST `/simple-report/update`

**请求：**

```json
{
  "id": 1,
  "name": "周报简化-v2",
  "deliveryDir": "simple/weekly",
  "status": 1,
  "scheduleEnabled": 1,
  "cronExpr": "0 0 9 * * ?"
}
```

- **不**在此接口改 blocks（避免与整表替换语义混用）  
- **成功：** `{ "code": 0, "message": "更新成功" }`

### 3.5 POST `/simple-report/save-blocks`

整表替换该报告下所有区块（按数组顺序写入 `sortOrder=0..n-1`）。

```json
{
  "id": 1,
  "blocks": [
    {
      "title": "学院就业人数",
      "httpMethod": "GET",
      "url": "https://data.example.com/api/emp/by-college",
      "queryJson": { "year": 2024 },
      "renderStyle": "BAR",
      "promptHint": "按学院"
    },
    {
      "title": "去向分布",
      "httpMethod": "GET",
      "url": "https://data.example.com/api/emp/destination",
      "renderStyle": "PIE"
    }
  ]
}
```

**成功：** `{ "code": 0, "message": "保存成功", "blockCount": 2 }`  

**约束：** `blocks` 可为 `[]`（清空）；但随后 `plan` 将因无区块失败。单报告区块建议上限 50。

### 3.6 POST `/simple-report/delete`

```json
{ "id": 1 }
```

**成功：** `{ "code": 0, "message": "删除成功" }`  

软删配置；历史 Run 保留；块物理删除（对齐库表默认 D2）。

---

## 4. 编排与运行 API

### 4.1 POST `/simple-report/plan`

根据**当前配置**生成 MD 计划，并落库快照；状态直接 `PENDING_CONFIRM`。

```json
{
  "id": 1,
  "userPrompt": "出一份本周就业概况，突出学院差异"
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| id | 是 | 配置 id |
| userPrompt | 否 | ≤2000；写入 MD「诉求」节 |

**成功：**

```json
{
  "code": 0,
  "message": "计划已生成，待确认",
  "runId": 100,
  "runStatus": 1,
  "planMd": "# 简化报告计划：周报简化\n\n## 诉求\n出一份本周就业概况…\n\n## 1. 学院就业人数\n- 查询目的：按学院统计人数\n- 接口：GET https://data.example.com/api/emp/by-college\n- 渲染：BAR\n- 预期数据：labels/values\n\n## 2. 去向分布\n...\n\n## 交付\n- 目录：simple/weekly\n"
}
```

**失败：** 配置停用/已删、无区块、校验失败。

**说明：** MVP 服务端**模板生成** MD；不在此接口调上游拉数、不出 Word。

### 4.2 PUT `/simple-report/runs/plan`（可选）

仅覆盖给人看的 `planMd`，**不修改** `config_snapshot_json`（防篡改执行接口）。

```json
{
  "runId": 100,
  "planMd": "# 修订后的计划\n..."
}
```

**成功：** `{ "code": 0, "message": "计划文案已更新" }`  

**约束：** 仅 `runStatus=1`（PENDING_CONFIRM）可改。

### 4.3 POST `/simple-report/runs/confirm`

```json
{ "runId": 100 }
```

**成功：**

```json
{
  "code": 0,
  "message": "报告生成任务已提交",
  "runId": 100,
  "runStatus": 2,
  "engineReportId": 1000000100
}
```

**行为：**

1. 校验 `PENDING_CONFIRM`  
2. 分配 `engineReportId`，置 `GENERATING`  
3. 异步：按快照拉数 → 组装 → `SpireReportUtil.createReport` → 复制到 deliveryDir  
4. 调用方轮询 `runs/detail` 或 Redis  

**失败：** 状态不对、已有生成中冲突等 → `code=-1`。

### 4.4 POST `/simple-report/runs/cancel`（Should）

```json
{ "runId": 100 }
```

仅 `PENDING_CONFIRM` → `CANCELLED`。  
**成功：** `{ "code": 0, "message": "已取消" }`

### 4.5 GET `/simple-report/runs/detail`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| runId | Long | 是 | |

**成功：**

```json
{
  "code": 0,
  "message": "ok",
  "runId": 100,
  "reportId": 1,
  "reportName": "周报简化",
  "userPrompt": "出一份本周就业概况…",
  "planMd": "# 简化报告计划：…",
  "runStatus": 3,
  "engineReportId": 1000000100,
  "warnCount": 0,
  "filePath": "files/.../report/周报简化_1000000100.docx",
  "deliveryPath": "files/.../simple/weekly/周报简化_100_20260721120000.docx",
  "failMessage": null,
  "triggerType": 0,
  "confirmedTime": "2026-07-21T12:00:01",
  "finishedTime": "2026-07-21T12:01:10",
  "createTime": "2026-07-21T11:59:00",
  "updateTime": "2026-07-21T12:01:10"
}
```

- 不默认回传完整 `configSnapshotJson`（体积大）；调试可加 `?includeSnapshot=1`（Could）  
- `runStatus=3` 且 `deliveryPath` 非空 → 可下载  

### 4.6 GET `/simple-report/runs/list`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reportId | Long | 否 | 按配置过滤 |
| runStatus | int | 否 | |
| page | int | 否 | 默认 0 |
| size | int | 否 | 默认 20 |

**成功：** 分页结构同 list；`content[]` 为 Run 摘要（可无完整 `planMd`，仅 `planMdPreview` 前 200 字，或列表不带 MD）。

建议列表字段：`runId, reportId, reportName, runStatus, engineReportId, warnCount, deliveryPath, failMessage, triggerType, createTime, finishedTime`。

### 4.7 GET `/simple-report/runs/download`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| runId | Long | 是 | |

**行为：** 优先读 `deliveryPath`，否则 `filePath`；以附件流返回 `.docx`。  

**失败：** 非 SUCCESS 或文件不存在 → JSON `{code:-1,...}` 或 HTTP 404（实现与 `managed-report/download` 对齐）。

```text
curl -OJ "http://localhost:9091/simple-report/runs/download?runId=100"
```

---

## 5. 调用方修改点

### 5.1 管理台 / Skill — 主流程

```js
// 1) 配置（可一次 create 带 blocks，或 create 后再 save-blocks）
await fetch("/simple-report/create", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    name: "周报简化",
    deliveryDir: "simple/weekly",
    blocks: [
      {
        title: "学院就业人数",
        httpMethod: "GET",
        url: "https://data.example.com/api/emp/by-college",
        renderStyle: "BAR"
      }
    ]
  })
});

// 2) 出计划
const planRes = await fetch("/simple-report/plan", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ id: 1, userPrompt: "本周概况" })
}).then((r) => r.json());
// 展示 planRes.planMd，等待人工确认

// 3) 确认出 Word
const conf = await fetch("/simple-report/runs/confirm", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ runId: planRes.runId })
}).then((r) => r.json());
// conf.engineReportId → 可选读 Redis report:list:{engineReportId}

// 4) 轮询终态
async function waitDone(runId) {
  for (;;) {
    const d = await fetch("/simple-report/runs/detail?runId=" + runId).then((r) => r.json());
    if (d.runStatus === 3) return d;          // SUCCESS
    if (d.runStatus === 4) throw new Error(d.failMessage || "failed");
    await new Promise((r) => setTimeout(r, 2000));
  }
}
```

### 5.2 OpenClaw Skill 约束

1. 只调上述 HTTP，**禁止**引导 python-docx / Word MCP 作为主出稿。  
2. `plan` 后必须等人明确确认再 `confirm`（定时 Job 另案，默认也只 plan）。  
3. 成功后告知 `deliveryPath` 或提供 download URL。

### 5.3 进度 / 状态

```text
Redis: report:list:{engineReportId}
库表: simple_report_run.run_status（1→2→3/4）
交付: delivery_path（MVP 目录）
```

---

## 6. 与既有 API 关系

| API | 关系 |
|-----|------|
| `POST /report/createReport` | 内部由 confirm 流水线调用；外部调用方无感 |
| `/managed-report/*` | 并存；勿混用 id；reportId≥1e9 段留给简化报告 |
| `/report-template/*` | 无依赖 |

---

## 7. 上游数据接口契约（调用方：数据提供方）

简化报告服务端拉取时，**仅接受**：

```json
{
  "labels": ["计算机", "机械"],
  "values": [120, 80]
}
```

或：

```json
{
  "code": 0,
  "data": {
    "labels": ["计算机", "机械"],
    "values": [120, 80]
  }
}
```

| 规则 | 说明 |
|------|------|
| labels/values 等长 | ≥1 |
| values | 数字或数字字符串 |
| code≠0 | 该区块失败（占位继续，warnCount+1） |
| 鉴权 | MVP 无；勿依赖 Header Token |

单块失败不导致整单失败（除非全部失败）——对齐技术方案默认。

---

## 8. 注意事项

- `deliveryDir`、`url` 做路径/SSRF 校验；失败返回明确 `message`，不暴露内网探测细节。  
- `failMessage` 不写堆栈、密码、完整内网绝对路径。  
- confirm 幂等：非 `PENDING_CONFIRM` 直接 `code=-1`，勿重复提交 Spire。  
- 改配置不影响已创建 Run 的快照执行结果。  
- 外部系统若自管 `createReport` 的 `reportId`，**不要使用 ≥ 1000000000**。

---

## 9. 待确认（API 层）

- [ ] **A1** `queryJson`/`bodyJson` 入站统一为 JSON 对象还是允许字符串？（设计默认：对象优先，字符串兼容）  
- [ ] **A2** `runs/list` 是否默认不返回 `planMd`？（设计默认：**不返回全文**）  
- [ ] **A3** download 与 managed 一致用 query `runId`，是否接受？  
- [ ] **A4** cancel 是否进 MVP？（库表预留状态 5；API 标 Should）  

---

*Status: DRAFT — API 契约 only. 实现须用户点名「开始编码」或「拆 Task」后进行。*
