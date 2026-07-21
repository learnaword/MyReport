# 管理总页与列表接口

## 涉及调用方

- 浏览器管理台：`/admin/index.html`（根路径 `/`、`/admin` 会跳转至此）

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | / | 跳转管理总页 |
| GET | /admin | 跳转管理总页 |
| GET | /school/list | 学校分页列表 |
| GET | /grad-employment/list | 就业去向分页列表 |
| POST | /school/importExcel | 学校 Excel 异步导入（返回 taskId） |
| POST | /grad-employment/importExcel | 就业去向 Excel 异步导入（返回 taskId） |
| GET | /school/importProgress | 学校导入进度 |
| GET | /grad-employment/importProgress | 就业去向导入进度 |

### GET /school/list

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 从 0 开始，默认 0 |
| size | int | 否 | 默认 20，最大 200 |

**成功响应：**

```json
{
  "code": 0,
  "message": "ok",
  "content": [{ "id": 1, "name": "示例大学", "createTime": "2026-07-20T19:00:00", "updateTime": "..." }],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### GET /grad-employment/list

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 从 0 开始，默认 0 |
| size | int | 否 | 默认 20，最大 200 |
| schoolId | Long | 否 | 按学校过滤 |

`content` 含 `schoolId` 等宽表字段。

### POST /grad-employment/importExcel

可附带 `schoolId`（可选默认学校）。Excel 可含「学校ID」或「学校名称」（按名称精确匹配 `school` 表）；皆无时随机选一所学校。唯一键：`schoolId + studentNo + graduationYear`。

## 调用方修改点

### 1. 打开总页

`http://localhost:9091/` 或 `http://localhost:9091/admin/index.html`

侧边栏：

- `#school` 学校管理
- `#data` 数据管理
- `#template` 模版管理（报告模版配置，见 `docs/report_template_config.md`）
- `#report` 报告管理（创建/生成，见 `docs/report_manage.md`）

### 2. 列表请求示例

```bash
curl "http://localhost:9091/school/list?page=0&size=20"
curl "http://localhost:9091/grad-employment/list?page=0&size=20"
```

## 注意事项

- 管理台内可直接导入 Excel 并刷新列表
- 导入为异步：上传后返回 `taskId`，前端轮询进度；`sessionStorage` 保存任务 ID，刷新页面后进度条会自动恢复
- 旧导入页 `/school/import`、`/grad-employment/import` 已重定向到总页对应分区
