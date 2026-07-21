# 报告模版配置

## 涉及调用方

- 管理台：`/admin/index.html#template`（侧边栏「模版管理」）
- 外部系统：只读拉取配置树 JSON（本期不对接报告生成）

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /report-template/list | 模版分页列表 |
| POST | /report-template/create | 创建空模版 |
| GET | /report-template/detail | 模版详情（含树 = 导出 JSON） |
| POST | /report-template/update | 更新模版头 |
| POST | /report-template/delete | 软删模版（级联软删节点） |
| POST | /report-template/copy | 复制模版 |
| POST | /report-template/saveTree | 整树保存（编辑结构弹窗） |
| POST | /report-template/saveStyle | 手填 URL / 清除外观 |
| POST | /report-template/uploadImage | 上传封面或底图（multipart，成功即写库） |
| GET | /files/report-template/** | 访问已上传图片 |
| POST | /report-template/node/create | 新增标题/指标 |
| POST | /report-template/node/update | 更新节点 |
| POST | /report-template/node/delete | 软删节点（级联子树） |
| POST | /report-template/node/move | 拖拽/同级重排 |

详细字段与校验见 `docs/report_template_config/API设计.md`。

### GET /report-template/list

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 从 0，默认 0 |
| size | int | 否 | 默认 20，最大 200 |
| status | int | 否 | 1 启用 / 0 停用 |
| keyword | string | 否 | 名称模糊 |

**响应：** `{ "code": 0, "message": "ok", "content": [...], "page", "size", "totalElements", "totalPages" }`

### GET /report-template/detail

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 模版 ID |

**成功响应（节选）：**

```json
{
  "code": 0,
  "message": "ok",
  "templateId": 1,
  "name": "就业质量报告模版",
  "description": "",
  "status": 1,
  "coverImage": "https://example.com/cover.png",
  "backCoverImage": null,
  "nodes": [
    {
      "id": 10,
      "nodeType": "TITLE",
      "level": 1,
      "name": "一、就业概况",
      "intro": "本章介绍整体就业情况",
      "children": [
        {
          "id": 11,
          "nodeType": "METRIC",
          "level": 2,
          "name": "就业率",
          "statField": "employment_rate",
          "valueFormat": "PERCENT",
          "displayType": "CHART",
          "chartStyle": "BAR",
          "children": []
        }
      ]
    }
  ]
}
```

`level` 规则：根标题为 1；任意子节点 = 父 level + 1（由服务端计算）。标题深度不限（无三级封顶）。

### POST /report-template/saveTree

编辑结构弹窗一次提交整棵树：服务端软删原节点后按 `nodes` 重建。

```json
{
  "id": 1,
  "nodes": [
    {
      "nodeType": "TITLE",
      "name": "一、就业概况",
      "intro": "本章介绍整体就业情况",
      "children": [
        {
          "nodeType": "METRIC",
          "name": "就业率",
          "statField": "employment_rate",
          "valueFormat": "PERCENT",
          "displayType": "CHART",
          "chartStyle": "BAR",
          "children": []
        }
      ]
    }
  ]
}
```

**成功：** `{ "code": 0, "message": "保存成功" }`

### POST /report-template/saveStyle

编辑样式弹窗：只更新封面/底图，不改节点树。

```json
{
  "id": 1,
  "coverImage": "https://example.com/cover.png",
  "backCoverImage": "https://example.com/back.png"
}
```

传 `null` 或 `""` 清除对应字段；至少提交一个样式字段。

**成功：** `{ "code": 0, "message": "保存成功" }`

### POST /report-template/uploadImage

`multipart/form-data`：`id`、`slot`（`cover`|`backCover`）、`file`。仅 jpg/png，≤2MB；成功即写库。

```bash
curl -F "id=1" -F "slot=cover" -F "file=@./cover.png" \
  "http://localhost:9091/report-template/uploadImage"
```

**成功：** `{ "code": 0, "message": "上传成功", "slot": "cover", "url": "/files/report-template/1/cover.png", "coverImage": "...", "backCoverImage": "..." }`

上传目录：`REPORT_TEMPLATE_UPLOAD_DIR`（默认 `~/myreport/uploads`），访问前缀 `/files/report-template/`。

### POST /report-template/node/create

```json
{
  "templateId": 1,
  "parentId": 10,
  "nodeType": "METRIC",
  "name": "就业率",
  "statField": "employment_rate",
  "valueFormat": "PERCENT",
  "displayType": "CHART",
  "chartStyle": "BAR"
}
```

根一级标题：`parentId` 省略，`nodeType`=`TITLE`。

### POST /report-template/node/move

```json
{
  "id": 11,
  "parentId": 12,
  "orderedSiblingIds": [15, 11, 16]
}
```

`orderedSiblingIds` 须恰好覆盖**新父**下全部未删子节点（含被移动节点）。

## 调用方修改点

### 1. 管理台

- 打开：`http://localhost:9091/admin/index.html#template`
- 列表新建/改名/复制/删除；「编辑结构」→ `saveTree`；「编辑样式」可**上传**图片（`uploadImage`）或手填 URL（`saveStyle`）
- 弹窗内取消会丢弃未保存的手填修改；上传成功已即时落库
- `detail` 含 `coverImage` / `backCoverImage`（可 null）与 `nodes`

### 2. 外部只读

```bash
curl "http://localhost:9091/report-template/detail?id=1"
```

`code === 0` 时使用 `nodes` 树；配置不含统计数据。

### 3. 进度 / 异步

无；均为同步接口。

## 注意事项

- 响应风格：`code` 0 成功 / -1 失败；看业务码而非仅 HTTP 状态
- 删除为软删除；列表与详情默认不含已删数据
- 本期不做 `createReport` 对接；JSON 字段不兼容旧版 `reportJsonArr`
- 设计文档：`docs/report_template_config/`（PRD / 数据库 / API）
