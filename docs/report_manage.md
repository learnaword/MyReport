# 报告管理

## 涉及调用方

- 管理台：`/admin/index.html#report`（侧边栏「报告管理」）
- 外部系统：可直接调本 API 创建报告并触发生成（无需自备 `reportJsonArr`）

> 学校固定为 **武汉大学**；创建时不必传 `schoolId`。详见 `docs/remove_school_manage/API设计.md`。

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /managed-report/list | 报告分页列表 |
| GET | /managed-report/detail | 报告详情 |
| POST | /managed-report/create | 创建报告（名称 + 模版） |
| POST | /managed-report/update | 更新报告 |
| POST | /managed-report/delete | 软删报告（并尽力删除已生成文件） |
| POST | /managed-report/generate | 按模版聚合指标并异步生成 Word |
| GET | /managed-report/download | 按报告 ID 下载 `.docx` |

响应风格：`code`（0 成功 / -1 失败）+ `message`。

### GET /managed-report/list

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 从 0，默认 0 |
| size | int | 否 | 默认 20，最大 200 |

**响应：**

```json
{
  "code": 0,
  "message": "ok",
  "content": [
    {
      "id": 1,
      "name": "2024就业质量报告",
      "schoolId": 3,
      "schoolName": "武汉大学",
      "templateId": 2,
      "templateName": "就业质量模版",
      "generateStatus": 0,
      "filePath": null,
      "failMessage": null,
      "lastGenerateTime": null,
      "createTime": "2026-07-21T10:00:00",
      "updateTime": "2026-07-21T10:00:00",
      "canDownload": false
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

`generateStatus`：`0` 草稿 / `1` 生成中 / `2` 成功 / `3` 失败。  
`schoolName` 恒为 `"武汉大学"`。

### POST /managed-report/create

```json
{
  "name": "2024就业质量报告",
  "templateId": 2
}
```

`schoolId` 可选，**传了也忽略**。

**成功：** `{ "code": 0, "message": "创建成功", "id": 1 }`

约束：同一模版仅允许一份未删报告；模版须启用。

### POST /managed-report/generate

```json
{ "id": 1 }
```

**成功：** `{ "code": 0, "message": "报告生成任务已提交", "reportId": 1 }`

**异步约定：**

- 提交成功后后台生成 Word；`reportId` 即实例 `id`
- Redis 进度：`report:list:{reportId}`（与既有 `createReport` 一致）
- Redis 锁：`report:lock:{reportId}`
- 终态回写库表：`generateStatus`、`filePath`（成功）或 `failMessage`（失败）

流程：读模版树 → 按**武汉大学**就业数据对 METRIC 做 COUNT/PERCENT → 组装静态指标 `reportJsonArr` → `SpireReportUtil.createReport`。

### GET /managed-report/download

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 报告实例 ID |

**成功：** `.docx` 文件流（`Content-Disposition` 附件；文件名优先为报告名称）。  
**失败：** HTTP 400 + `{ "code": -1, "message": "..." }`。

规则与示例见 `docs/report_download.md`。列表项含 `canDownload`。

## 调用方修改点

### 1. 创建并生成

```js
await fetch("/managed-report/create", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ name: "报告名", templateId: 2 })
});
await fetch("/managed-report/generate", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ id: 1 })
});
```

### 2. 响应处理

```js
// code === 0 表示任务已提交；轮询 detail.generateStatus 或 Redis 进度
// generateStatus === 2 时 filePath 为落盘路径
```

### 3. 进度 / 状态

```text
库表 generate_status：0草稿 1生成中 2成功 3失败
Redis key：report:list:{id} / report:lock:{id}
```

## 注意事项

- 模版停用后不可生成；生成中不可改模版或删除
- 指标 `statField` 须为就业宽表白名单字段（如 `college_name`）
- PERCENT：分母为该校该字段非空人数，展示两位小数并带 `%`
- 单指标无数据时跳过该节点；全部跳过则提交失败
- 既有 `POST /report/createReport` 仍可用（调用方自备 JSON）
