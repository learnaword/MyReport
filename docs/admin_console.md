# 管理总页与列表接口

## 涉及调用方

- 浏览器管理台：`/admin/index.html`（根路径 `/`、`/admin` 会跳转至此）

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | / | 跳转管理总页 |
| GET | /admin | 跳转管理总页 |
| GET | /grad-employment/list | 就业去向分页列表（仅默认校武汉大学） |
| POST | /grad-employment/importExcel | 就业去向 Excel 异步导入（返回 taskId） |
| GET | /grad-employment/importProgress | 就业去向导入进度 |
| GET | /school/list | **已下线**（返回 `code=-1`） |
| POST | /school/importExcel | **已下线** |
| GET | /school/importProgress | **已下线** |

> 学校管理产品能力已移除；数据默认归属 **武汉大学**。详见 `docs/remove_school_manage/API设计.md`。

### GET /grad-employment/list

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 从 0 开始，默认 0 |
| size | int | 否 | 默认 20，最大 200 |
| schoolId | Long | 否 | **忽略**；服务端只返回武汉大学数据 |

`content` 含 `schoolId` 等宽表字段。

### POST /grad-employment/importExcel

`schoolId` 可传但**忽略**。Excel 中「学校名称/学校ID」不参与归属，一律写入默认校。唯一键：`schoolId + studentNo + graduationYear`（均为默认校下）。

## 调用方修改点

### 1. 打开总页

`http://localhost:9091/` 或 `http://localhost:9091/admin/index.html`

侧边栏：

- `#data` 数据管理
- `#template` 模版管理（报告模版配置，见 `docs/report_template_config.md`）
- `#report` 报告管理（创建/生成，见 `docs/report_manage.md`）
- `#simple-report` 简化报告（见 `docs/ai_simple_report.md`）
- `#school` 已下线，访问时落到 `#data`

### 2. 列表请求示例

```bash
curl "http://localhost:9091/grad-employment/list?page=0&size=20"
```

## 注意事项

- 管理台内可直接导入就业 Excel 并刷新列表（无需先导入学校）
- 导入为异步：上传后返回 `taskId`，前端轮询进度；`sessionStorage` 保存任务 ID，刷新页面后进度条会自动恢复
- 旧导入页 `/school/import`、`/grad-employment/import` 重定向到总页对应分区（学校入口 → 数据管理）
