# 简化报告生成状态（调用方）

## 涉及调用方

- 管理台：`/admin/index.html#simple-report`（列表「生成状态」列 + 配色）
- OpenClaw / Comate Skill：若渲染配置列表，改读生成态字段
- 仅轮询 `runs/detail` 的客户端：无强制改动

完整增量契约见：`docs/simple_report_status/API设计.md`。  
主契约仍见：`docs/ai_simple_report.md`、`docs/ai_simple_report/API设计.md`。

## API

本期**无新路径**；仅下列接口**出站字段增量**：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /simple-report/list | 配置分页列表，增加展示用生成态 |
| GET | /simple-report/detail | 配置详情，增加同上字段 |

**新增出站字段：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| latestRunStatus | Integer \| null | 出站 | 展示用生成态；无 run 为 `null` |
| latestRunId | Long \| null | 出站 | 对应 run id |
| latestFailMessage | String \| null | 出站 | 仅 `latestRunStatus=4` 时有值 |

**`latestRunStatus` 取值：**

| 值 | 含义 | 建议文案 |
|----|------|----------|
| null | 从未生成 | 未生成 |
| 1 | 待确认 | 待确认 |
| 2 | 生成中 | 生成中 |
| 3 | 成功 | 成功 |
| 4 | 失败 | 失败 |
| 5 | 已取消 | 已取消 |

**聚合规则：** 取该配置下最近一次运行（按创建时间）；无 run 则为 `null`。

**不变：** `status` = 配置启用/停用；`canDownload` / `latestSuccessRunId` 仍只表示可下载成功稿。

**响应：** `{ "code": 0, "message": "ok", ... }`

**异步约定：** 出稿进度仍可用 Redis `report:list:{engineReportId}`；列表以 `latestRunStatus` 离散态为主。

## 调用方修改点

### 1. 请求示例

```text
GET /simple-report/list?page=0&size=20
GET /simple-report/detail?id=1
```

（入站参数无变化。）

### 2. 响应处理

```js
// list：code === 0
// row.status              → 配置启用（0/1），勿当生成态
// row.latestRunStatus     → 生成态（null | 1..5），按上表映射文案 + 配色
// row.latestFailMessage   → 失败时作悬停提示
// row.canDownload         → 是否显示下载（可与「待确认」并存）

if (body.content.some((r) => r.latestRunStatus === 2)) {
  // 约 3s 再拉一次 list，直到本页无「生成中」
}
```

### 3. 进度 / 状态

```text
配置列表展示态：latestRunStatus（本增量）
单次运行终态：GET /simple-report/runs/detail?runId= → runStatus
Redis：report:list:{engineReportId}，engineReportId = 1000000000 + runId
```

## 注意事项

- `status` 与 `latestRunStatus` 语义不同，勿混用
- 色盲友好：颜色仅为增强，必须同时展示中文文案
- 旧客户端忽略新字段即可兼容；新 UI 须处理 `null`（未生成）
