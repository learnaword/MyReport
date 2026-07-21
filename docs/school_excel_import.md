# 学校 Excel 导入

## 涉及调用方

- 浏览器管理台：`/admin/index.html#school`
- 静态页：`/school/import.html`（`/school/import` 会跳转管理台）
- 其它系统如需对接：直接调用下方上传与进度接口

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /school/import | 跳转到管理台学校分区 |
| POST | /school/importExcel | 异步提交 Excel 导入任务 |
| GET | /school/importProgress | 查询导入进度 |

### POST /school/importExcel

**请求：** `multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | `.xlsx` / `.xls`，≤ 5MB |

**Excel 约定：**

- 使用第一个工作表
- 首行为表头，须含列名：`名称` / `学校名称` / `name`（任选其一）
- 第二行起为数据；空名称行跳过
- 名称长度 ≤ 128

**成功响应（任务已提交，非导入完成）：**

```json
{
  "code": 0,
  "message": "导入任务已提交",
  "taskId": "a1b2c3d4e5f6..."
}
```

**失败响应：**

```json
{
  "code": -1,
  "message": "未找到名称列，请使用表头「名称」或「name」"
}
```

**异步约定：** Redis key `import:progress:{taskId}`，TTL 24h。前端用 `sessionStorage` 保存 `taskId`，刷新后可恢复进度条。

### GET /school/importProgress

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | String | 是 | 提交接口返回的任务 ID |

**响应字段：**

| 字段 | 说明 |
|------|------|
| nState | `1` 处理中 / `2` 成功 / `4` 失败 |
| nProgress / nTotal | 进度分子/分母 |
| strMessage | 当前说明 |
| result | 成功时含 `successCount`、`totalParsed` |

```json
{
  "code": 0,
  "message": "ok",
  "taskId": "a1b2c3d4e5f6...",
  "type": "school",
  "nState": 2,
  "nProgress": 100,
  "nTotal": 100,
  "strMessage": "导入成功",
  "result": { "successCount": 3, "totalParsed": 3 }
}
```

## 调用方修改点

### 1. 页面打开

`http://localhost:9091/admin/index.html#school`

### 2. 请求示例

```bash
# 提交
curl -X POST "http://localhost:9091/school/importExcel" -F "file=@schools.xlsx"
# 轮询
curl "http://localhost:9091/school/importProgress?taskId=YOUR_TASK_ID"
```

### 3. 响应处理

```js
// POST code === 0 仅表示任务已提交，需用 taskId 轮询 importProgress
// nState === 2 时从 result.successCount 取新增条数
```

## 注意事项

- 每次导入均为新增，不做按名称去重
- 仅返回通用失败信息，不暴露服务端路径与堆栈
- 刷新管理台后，若 `sessionStorage` 仍有 `taskId`，会自动恢复进度条展示
