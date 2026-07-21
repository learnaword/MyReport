# API 设计：简化报告下载

**需求来源：** 一段话「简化报告支持下载」；依据 `docs/simple_report_download/技术方案.md`  
**功能短名：** `simple_report_download`  
**调用方文档（编码落地时）：** `docs/simple_report_download.md`（新建），并增量更新 `docs/ai_simple_report.md`  
**响应风格：** JSON 接口 `code`（0 / -1）+ `message`；下载成功为二进制流（见 §3）  
**路径前缀：** `/simple-report`  
**状态：** DRAFT — 仅契约，不编码  

**本项约定（对齐技术方案默认）：**

| 项 | 结论 |
|----|------|
| 按 run 下载 | 已有 `GET /runs/download`，本期**加固**（路径校验、友好文件名、`canDownload`） |
| 按配置下载 | **新增** `GET /download?id=` → 该配置最近一次可下载 SUCCESS 稿 |
| 可下载条件 | 仅 `runStatus=3`（SUCCESS）且文件存在且路径合法；**FAILED 不可下旧稿** |
| 失败 HTTP | 保持现网：**HTTP 200 + JSON** `{code:-1,message}`（与 managed 的 400 不同） |
| 软删配置 | `/download?id=` **拒绝**；`/runs/download?runId=` **仍允许**（历史稿） |
| Redis | **无新 key**；下载不读进度 |

---

## 涉及调用方

| 调用方 | 需改点 |
|--------|--------|
| 管理台 `/admin/index.html#simple-report` | 列表据 `canDownload` 显「下载」→ 调配置级 download；弹窗生成成功后「下载本次」→ runs/download |
| OpenClaw / Comate Skill | 可选补充「按配置 id 下最新稿」；原 `runs/download?runId=` 兼容 |
| 外部 HTTP 调用方 | 新增配置级下载；列表/详情新增出站字段（向后兼容，仅增字段） |
| `managed-report` / `createReport` | **无改动** |

---

## 1. 接口一览（本需求相关）

| 方法 | 路径 | 变更 | 说明 |
|------|------|------|------|
| GET | `/simple-report/download` | **新增** | 按配置 ID 下载最近可下载 `.docx` |
| GET | `/simple-report/runs/download` | **加固** | 按 `runId` 下载指定稿 |
| GET | `/simple-report/list` | **字段增量** | `canDownload`、`latestSuccessRunId` |
| GET | `/simple-report/runs/list` | **字段增量** | `canDownload` |
| GET | `/simple-report/runs/detail` | **字段增量** | `canDownload` |

其余 `/simple-report/*`（create、plan、confirm 等）契约不变。

---

## 2. 公共约定

### 2.1 `canDownload`（出站 boolean）

**单次 Run：**

```text
canDownload = true 当且仅当：
  runStatus == 3（SUCCESS）
  && (deliveryPath 非空 ? deliveryPath : filePath) 非空
  && 对应文件存在且为普通文件
  && 文件 canonical 路径落在服务物理根目录下
```

路径优先级：**`deliveryPath` > `filePath`**（与现网一致）。

**配置列表项：**

```text
canDownload = 该配置存在至少一条满足上式的 SUCCESS run
latestSuccessRunId = 上述「最近一条」的 runId；无可下则为 null
```

「最近」排序：`finishedTime` 降序，同则 `id` 降序；跳过文件已失效的 SUCCESS，取第一条仍可下的。

### 2.2 下载成功响应（二进制）

| Header | 说明 |
|--------|------|
| Content-Type | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` |
| Content-Disposition | `attachment; filename="..."; filename*=UTF-8''...` |
| Content-Length | 文件字节数 |

- 文件名优先：`sanitize(报告名称).docx`（配置级用配置/快照中的报告名；run 级用 `reportName`）
- 空名回退：`simple-report-{runId}.docx`
- **禁止**客户端传磁盘路径；仅传 `id` / `runId`

### 2.3 下载失败响应（JSON）

HTTP **200**，`Content-Type: application/json`：

```json
{ "code": -1, "message": "可读失败原因" }
```

常见 `message`（文案可微调，语义固定）：

| message（示例） | 场景 |
|-----------------|------|
| 报告不存在或已删除 | 配置 id 无效 / 已软删（仅配置级） |
| 运行不存在 | runId 无效 |
| 报告尚未生成成功 | run 非 SUCCESS |
| 暂无可下载文件 | 配置无任何可下 SUCCESS |
| 文件不存在或已被清理 | 库有路径但磁盘无文件 |
| 文件路径非法 | 穿越物理根 |
| 下载失败 | 未预期异常（不暴露绝对路径） |

前端判定：`Content-Type` 含 `application/json`（或 `code` 可读）→ 失败；否则按 blob 保存。与 managed 管理台逻辑同型（managed 另有 `!r.ok`，本接口失败多为 200，以 JSON 类型为准）。

### 2.4 安全

- 参数仅 `id` / `runId`（Long）
- canonical 路径必须在 `prefixFilePhysicalPath` 下
- 对外 `message` 不返回本机绝对路径

---

## 3. API 明细

### 3.1 GET `/simple-report/download`（新增）

按**配置 ID**下载该配置最近一次可下载 Word。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | `simple_report.id`（未软删） |

**行为：**

1. 校验配置存在且未删  
2. 解析最近可下载 SUCCESS run  
3. 返回该 run 对应文件流（规则同 §2.1 / §2.2）

**成功：** docx 二进制（§2.2）  
**失败：** §2.3 JSON  

```bash
curl -OJ "http://localhost:9091/simple-report/download?id=1"
```

---

### 3.2 GET `/simple-report/runs/download`（加固）

按**运行 ID**下载指定稿。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| runId | Long | 是 | `simple_report_run.id` |

**行为（相对现网加固点）：**

| 项 | 现网 | 本契约 |
|----|------|--------|
| 状态 | 仅 SUCCESS | 保持 |
| 路径 | delivery 优先 | 保持 + **物理根校验** |
| 文件名 | 多用磁盘名 | **改为报告名 sanitize** |
| 关联配置已软删 | 未明确 | **仍允许下载**（有 run 即可） |

**成功 / 失败：** 同 §2.2 / §2.3  

```bash
curl -OJ "http://localhost:9091/simple-report/runs/download?runId=100"
```

---

### 3.3 GET `/simple-report/list`（字段增量）

原有分页参数不变。`content[]` **新增**：

| 字段 | 类型 | 说明 |
|------|------|------|
| canDownload | boolean | 是否存在可下成功稿 |
| latestSuccessRunId | Long \| null | 最近可下 runId；无可下为 `null` |

**成功响应片段：**

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
      "blockCount": 2,
      "canDownload": true,
      "latestSuccessRunId": 100,
      "createTime": "2026-07-21T10:00:00",
      "updateTime": "2026-07-21T12:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

`detail` **本期不强制**加 `canDownload`（列表足够驱动按钮）；若实现顺手可对称加上，非破坏性。

---

### 3.4 GET `/simple-report/runs/detail`（字段增量）

原有查询参数不变。成功体 **新增**：

| 字段 | 类型 | 说明 |
|------|------|------|
| canDownload | boolean | 本 run 是否可下载 |

其余字段（`runId`、`runStatus`、`deliveryPath`、`filePath`、`planMd` 等）不变。

```json
{
  "code": 0,
  "message": "ok",
  "runId": 100,
  "reportId": 1,
  "reportName": "周报简化",
  "runStatus": 3,
  "canDownload": true,
  "deliveryPath": "...",
  "filePath": "...",
  "failMessage": null
}
```

---

### 3.5 GET `/simple-report/runs/list`（字段增量）

分页参数不变。`content[]` 每项 **新增** `canDownload`（boolean）。列表仍可不带完整 `planMd`。

---

## 4. 调用方修改点

### 4.1 管理台 — 配置列表下载

```js
// list 项 canDownload === true 时展示按钮
fetch("/simple-report/download?id=" + reportId).then(function (r) {
  var ct = (r.headers.get("content-type") || "").toLowerCase();
  if (ct.indexOf("application/json") >= 0) {
    return r.json().then(function (body) {
      // body.code === -1 → 提示 body.message
    });
  }
  return r.blob().then(function (blob) {
    // 解析 Content-Disposition 文件名并保存 .docx
  });
});
```

### 4.2 管理台 — 本次 run 下载

生成轮询到 `runStatus === 3`（或 `canDownload === true`）后：

```js
fetch("/simple-report/runs/download?runId=" + runId)
  // 响应处理同 4.1
```

### 4.3 Skill / 外部系统

```text
# 指定某次运行
GET /simple-report/runs/download?runId={runId}

# 只要该配置最新成功稿
GET /simple-report/download?id={reportId}
```

成功后告知用户本地已保存，或给出上述 URL（内网可达时）。

### 4.4 进度 / 状态

无新 Redis。下载只看库表：

```text
runStatus：1待确认 2生成中 3成功 4失败 5取消
canDownload：仅成功且文件有效
配置级 download：等价于「该配置 latestSuccessRunId 对应文件」
```

---

## 5. 与既有文档关系

| 文档 | 关系 |
|------|------|
| `docs/ai_simple_report/API设计.md` §4.7 | 本文件细化/加固；编码后应回写 `canDownload` 与配置级 download |
| `docs/ai_simple_report.md` | 落地时增加配置级 download 与列表字段说明 |
| `docs/report_download.md` | 管理台报告下载范式；本需求**不修改**该文档，仅对齐交互模式 |

---

## 6. 注意事项

- 多次 SUCCESS 时，配置级下载是**最近一次可下稿**，不是某次指定 run；要精确版本请用 `runId`
- 生成成功后需刷新列表（或看弹窗「下载本次」）再下；`canDownload` 以服务端实时探文件为准
- 勿用 `deliveryPath` / `filePath` 拼静态 URL 下载
- 当前管理台无登录，与现有 admin 同级（内网可信）
- 本契约相对现网为**兼容增量**（新路径 + 新字段）；Skill 旧调用 `runs/download` 不受影响

---

## 7. 待确认（继承技术方案，已采纳默认）

| # | 项 | 本 API 采纳 |
|---|----|-------------|
| T1 | 配置级 `/download?id=` | **采纳** |
| T2 | FAILED 下旧稿 | **不采纳**（仅 SUCCESS） |
| T3 | 软删后 runs/download | **仍允许** |
| T4 | 弹窗下载范围 | 契约不约束 UI；建议成功后「下载本次」 |
| T5 | 失败用 400 | **不采纳**（保持 200+JSON） |

若评审推翻上表，先改正文再编码。
