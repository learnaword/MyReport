# API 设计：报告模版配置

**需求来源：** `docs/report_template_config/PRD分析.md`、`docs/report_template_config/数据库设计.md`  
**功能短名：** `report_template_config`  
**调用方：** 管理端侧边栏「模版管理」页面；外部系统可只读拉取配置 JSON（本期不做报告生成闭环）  
**响应风格：** 与现有接口一致 — `code`（0 成功 / -1 失败）+ `message`；HTTP 状态码默认 200，业务成败看 `code`  
**路径风格：** 与 `/school`、`/grad-employment` 一致，扁平资源前缀 `/report-template`

---

## 0. 设计前提（已确认）

| 项 | 结论 |
|----|------|
| 持久化 | 需要；树落邻接表 |
| level | 服务端计算：根=1；子=`父.level+1`；客户端**勿传** `level`（传了也忽略） |
| 指标挂载 | 任意 `TITLE` 下可加子标题或指标；指标叶子不可再挂子 |
| 标题上限 | **无固定深度上限**（取消原「仅三级」）；任意 TITLE 下仍可继续「添加子标题」 |
| 排序 | 拖拽；同父 `sort_order` |
| 删除 | **软删除**（D2）；模版/节点带删除标记，查询默认过滤已删 |
| 复制 | **支持**（D4）；深拷贝节点树 **+ 封面/底图** |
| 外键 CASCADE | **不用**（D5）；删除由应用层软删级联 |
| 报告生成 | 本期不做 |
| 配置内容 | 只含结构与外观引用，不含统计数据 |
| 外观入口 | 列表「编辑结构」后「**编辑样式**」弹窗；配封面/底图 |
| 外观存储 | `cover_image` / `back_cover_image`（可空字符串引用） |
| 图片上传 | **支持**；jpg/png（含 jpeg），单文件 ≤ **2MB** |
| 上传写库 | **上传成功即写对应模版字段**（Q22 默认）；不必再点保存 |
| 访问形态 | 落盘后返回可 HTTP 访问的相对路径（如 `/files/report-template/{id}/...`）（Q23 默认） |
| 旧文件 | 替换/清除时**尽力删除**旧本地文件（Q21 默认；失败仅打日志） |

> 软删字段、外观两列见数据库设计；存量库需 `ALTER` 加列。

---

## 1. 接口一览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/report-template/list` | 模版分页列表 |
| POST | `/report-template/create` | 创建空模版（仅头信息） |
| GET | `/report-template/detail` | 模版详情（含整棵树） |
| GET | `/report-template/stat-fields` | 指标统计字段字典（中文 label + 库列名 value）；见 `docs/metric_field_select/API设计.md` |
| POST | `/report-template/update` | 更新模版头（名称/说明/状态） |
| POST | `/report-template/delete` | 软删模版（级联软删全部节点） |
| POST | `/report-template/copy` | 复制模版（新 ID + 深拷贝节点 + 外观） |
| POST | `/report-template/saveTree` | 整树保存（编辑结构弹窗） |
| POST | `/report-template/saveStyle` | 保存样式（手填 URL / 清除；不改树） |
| POST | `/report-template/uploadImage` | **上传封面或底图**（multipart；成功即写库） |
| GET | `/files/report-template/**` | 静态访问已上传图片（只读） |
| POST | `/report-template/node/create` | 新增节点（子标题或指标；根标题 `parentId` 为空） |
| POST | `/report-template/node/update` | 更新节点内容 |
| POST | `/report-template/node/delete` | 软删节点（级联软删子树） |
| POST | `/report-template/node/move` | 拖拽：改父节点和/或同父排序 |

说明：

- 详情接口返回的树形 JSON + 外观字段即「导出配置」；不另设 `/export`
- **结构**与**样式**分接口：`saveTree` / `saveStyle` / `uploadImage`，互不覆盖对方职责
- 上传与手填 URL **并存**：上传走 `uploadImage`；手填/清除走 `saveStyle`

---

## 2. 公共约定

### 2.1 成功 / 失败

```json
{ "code": 0, "message": "ok", "...": "业务字段" }
```

```json
{ "code": -1, "message": "模版不存在或已删除" }
```

### 2.2 枚举（请求/响应字符串）

| 字段 | 取值 |
|------|------|
| `nodeType` | `TITLE` / `METRIC` |
| `valueFormat` | `PERCENT` / `COUNT` |
| `displayType` | `TABLE` / `CHART` |
| `chartStyle` | `BAR` / `PIE` / `LINE` |
| `status` | `1` 启用 / `0` 停用 |

### 2.3 节点树节点形状（详情/创建更新返回）

**TITLE：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | |
| nodeType | String | `TITLE` |
| level | Integer | 服务端计算：根=1；子=父+1；**标题无最大层数限制** |
| name | String | 标题名称 |
| intro | String | 介绍/内容，可空 |
| children | Array | 子节点（标题或指标） |

**METRIC：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | |
| nodeType | String | `METRIC` |
| level | Integer | = 父.level + 1 |
| name | String | 指标独立名称 |
| statField | String | 就业宽表白名单列名（规范 snake_case）；可选值见 `GET /report-template/stat-fields` |
| valueFormat | String | PERCENT / COUNT |
| displayType | String | TABLE / CHART |
| chartStyle | String\|null | CHART 时必有；TABLE 时为 null |
| children | Array | 恒为 `[]` |

---

## 3. 模版接口

### 3.1 GET `/report-template/list`

模版分页列表（不含树）。

**Query：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 从 0，默认 0 |
| size | int | 否 | 默认 20，最大 200 |
| status | int | 否 | 按启用状态过滤；不传则全部未删除 |
| keyword | string | 否 | 按 `name` 模糊匹配 |

**成功响应：**

```json
{
  "code": 0,
  "message": "ok",
  "content": [
    {
      "id": 1,
      "name": "就业质量报告模版",
      "description": "",
      "status": 1,
      "createTime": "2026-07-20T23:00:00",
      "updateTime": "2026-07-20T23:10:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 3.2 POST `/report-template/create`

创建空模版（无节点）。

**请求 Body：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 最长 128 |
| description | String | 否 | 最长 512 |
| status | Integer | 否 | 默认 1 |

```json
{
  "name": "就业质量报告模版",
  "description": "",
  "status": 1
}
```

**成功响应：**

```json
{
  "code": 0,
  "message": "创建成功",
  "id": 1
}
```

---

### 3.3 GET `/report-template/detail`

模版头 + 完整树（即配置 JSON）。

**Query：**

| 参数 | 类型 | 必填 |
|------|------|------|
| id | Long | 是 |

**成功响应：**

```json
{
  "code": 0,
  "message": "ok",
  "templateId": 1,
  "name": "就业质量报告模版",
  "description": "",
  "status": 1,
  "coverImage": "https://example.com/cover.png",
  "backCoverImage": "https://example.com/back.png",
  "createTime": "2026-07-20T23:00:00",
  "updateTime": "2026-07-20T23:10:00",
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
          "statField": "college_name",
          "valueFormat": "PERCENT",
          "displayType": "CHART",
          "chartStyle": "BAR",
          "children": []
        },
        {
          "id": 12,
          "nodeType": "TITLE",
          "level": 2,
          "name": "分学院情况",
          "intro": "",
          "children": []
        }
      ]
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| coverImage | String\|null | 封面图引用；未配置为 `null` |
| backCoverImage | String\|null | 底图引用；未配置为 `null` |

**失败：** 不存在或已软删 → `code=-1`。

> 「编辑样式」弹窗打开时用本接口回显外观；也可只读 `coverImage`/`backCoverImage`，不必渲染整树。

---

### 3.4 POST `/report-template/update`

只改模版头（名称/说明/状态），**不改树、不改封面/底图**（外观走 `saveStyle`）。

**请求 Body：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | |
| name | String | 否 | 传则更新 |
| description | String | 否 | 传则更新（可传 `""` 清空） |
| status | Integer | 否 | 0 / 1 |

```json
{
  "id": 1,
  "name": "就业质量报告模版-修订",
  "status": 1
}
```

**成功：** `{ "code": 0, "message": "更新成功" }`

---

### 3.5 POST `/report-template/delete`

软删模版，并软删其下全部节点。

**请求 Body：**

```json
{ "id": 1 }
```

**成功：** `{ "code": 0, "message": "删除成功" }`  
**幂等：** 已删再删仍返回成功（或 `code=0` +「已删除」）。

---

### 3.6 POST `/report-template/copy`

深拷贝：新模版头 + 新节点 ID + **封面/底图**；树结构与字段内容一致；`name` 默认原名 + `（副本）`（可被请求覆盖）。

**请求 Body：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 源模版 |
| name | String | 否 | 新模版名称 |

```json
{ "id": 1, "name": "就业质量报告模版（副本）" }
```

**成功响应：**

```json
{
  "code": 0,
  "message": "复制成功",
  "id": 2
}
```

调用方可再 `GET /report-template/detail?id=2` 拉取新树与外观。

---

### 3.7 POST `/report-template/saveTree`

整树保存（编辑结构弹窗）。软删原节点后按 `nodes` 重建；**不修改** `coverImage` / `backCoverImage`。

**请求 Body：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 模版 ID |
| nodes | Array | 是 | 根节点数组；可 `[]` 表示清空树 |

（节点形状同详情，可无 `id`/`level`；服务端重算 level。）

**成功：** `{ "code": 0, "message": "保存成功" }`

---

### 3.8 POST `/report-template/saveStyle`

保存样式（手填 URL / 清除）。只更新封面/底图字符串，**不修改**节点树。  
**上传请用 `uploadImage`**，不要把 multipart 塞进本接口。

**请求 Body：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 模版 ID |
| coverImage | String\|null | 否 | 封面图引用；传 `null` 或 `""` 表示清除；**不传该 key 则保持原值** |
| backCoverImage | String\|null | 否 | 底图引用；语义同封面 |

```json
{
  "id": 1,
  "coverImage": "https://example.com/cover.png",
  "backCoverImage": "/files/report-template/1/back.png"
}
```

清除示例：

```json
{
  "id": 1,
  "coverImage": null,
  "backCoverImage": ""
}
```

**校验：**

| 规则 | 表现 |
|------|------|
| 模版存在且未删 | 否则 `code=-1` |
| 单字段长度 | > 1024 → 失败 |
| 至少传一个外观字段 | 两 key 均未出现 → `code=-1`，「请至少提交一个样式字段」 |
| 清除本地上传图 | 若原值为本服务 `/files/report-template/...`，清除时**尽力删除**磁盘文件 |

**成功：** `{ "code": 0, "message": "保存成功" }`

---

### 3.9 POST `/report-template/uploadImage`

上传封面或底图（**编辑样式**弹窗主路径）。`multipart/form-data`，**成功即更新模版对应字段并返回可访问 URL**。

**表单字段：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 模版 ID |
| slot | String | 是 | `cover` = 封面；`backCover` = 底图 |
| file | File | 是 | 图片文件 |

**校验：**

| 规则 | 表现 |
|------|------|
| 模版存在且未删 | 否则失败 |
| slot | 仅 `cover` / `backCover` |
| 扩展名 | 仅 `.jpg` / `.jpeg` / `.png`（大小写不敏感） |
| 大小 | ≤ **2MB**（2097152 字节）；超限 →「图片不能超过 2MB」 |
| 空文件 | 「请选择图片文件」 |

**成功响应：**

```json
{
  "code": 0,
  "message": "上传成功",
  "slot": "cover",
  "url": "/files/report-template/1/cover.png",
  "coverImage": "/files/report-template/1/cover.png",
  "backCoverImage": "/files/report-template/1/back.png"
}
```

说明：

- `url`：本次上传槽位的新引用  
- `coverImage` / `backCoverImage`：写库后的**当前**外观（便于前端刷新表单，未改动的槽位原样返回）  
- 落盘目录约定：`{uploadRoot}/report-template/{templateId}/cover.{ext}` 或 `back.{ext}`（同槽位再上传则覆盖同名文件或换扩展名后删旧）  
- 对外 URL 前缀：`/files/report-template/{templateId}/...`（由静态映射到 `uploadRoot`）

**失败示例 message：**

- 图片格式仅支持 jpg/png  
- 图片不能超过 2MB  
- 模版不存在或已删除  
- 上传失败，请重试  

**Content-Type：** `multipart/form-data`（不要用 JSON Body）

---

### 3.10 GET `/files/report-template/**`

只读访问已上传文件（Spring 静态资源或专用 Controller 转发）。

| 项 | 约定 |
|----|------|
| 示例 | `GET /files/report-template/1/cover.png` |
| 鉴权 | 与现管理台一致（本期无登录则同样裸访问） |
| 404 | 文件不存在 |

> 外部系统若部署在不同域名，可将 `detail` 中的相对路径拼成绝对 URL，或后续再加文件服务域名配置。

---

## 4. 节点接口

### 4.1 POST `/report-template/node/create`

在指定父标题下新增子标题或指标；`parentId` 为空时新增**一级标题**。

**请求 Body（公共）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| templateId | Long | 是 | |
| parentId | Long | 否 | 空=根一级标题；非空则父必须是未删的 TITLE，且属同一模版 |
| nodeType | String | 是 | TITLE / METRIC |
| name | String | 是 | |
| sortOrder | Integer | 否 | 不传则追加到同父末尾 |

**TITLE 追加字段：**

| 参数 | 类型 | 必填 |
|------|------|------|
| intro | String | 否 |

**METRIC 追加字段：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| statField | String | 是 | 须为 `GET /report-template/stat-fields` 的 `value`（或可归一别名）；非法拒绝 |
| valueFormat | String | 是 | PERCENT / COUNT |
| displayType | String | 是 | TABLE / CHART |
| chartStyle | String | 条件 | displayType=CHART 必填；TABLE 勿传或 null |

**创建一级标题示例：**

```json
{
  "templateId": 1,
  "nodeType": "TITLE",
  "name": "一、就业概况",
  "intro": "本章介绍整体就业情况"
}
```

**创建指标示例：**

```json
{
  "templateId": 1,
  "parentId": 10,
  "nodeType": "METRIC",
  "name": "分学院人数",
  "statField": "college_name",
  "valueFormat": "COUNT",
  "displayType": "TABLE"
}
```

**成功响应：**

```json
{
  "code": 0,
  "message": "创建成功",
  "id": 11,
  "level": 2
}
```

**校验失败示例 message（`code=-1`）：**

- 父节点不存在 / 已删除 / 非 TITLE  
- 父为 METRIC（指标下不可再挂子节点）  
- METRIC 缺字段或 CHART 无 chartStyle  
- templateId 与父节点不属同一模版  

`level` 一律服务端按父计算（`parent.level + 1`），请求体带 `level` 忽略。  
任意深度的 TITLE 均可再创建子 TITLE 或 METRIC。

---

### 4.2 POST `/report-template/node/update`

更新节点展示字段；**不可**改 `nodeType`、`templateId`（改层级用 move）。

**TITLE Body：**

```json
{
  "id": 10,
  "name": "一、就业概况（修订）",
  "intro": "更新后的介绍"
}
```

**METRIC Body：**

```json
{
  "id": 11,
  "name": "分学院人数",
  "statField": "college_name",
  "valueFormat": "COUNT",
  "displayType": "TABLE",
  "chartStyle": null
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | |
| name | String | 否 | |
| intro | String | 否 | 仅 TITLE；METRIC 传了忽略 |
| statField / valueFormat / displayType / chartStyle | — | 否 | 仅 METRIC；规则同创建 |

**成功：** `{ "code": 0, "message": "更新成功" }`

---

### 4.3 POST `/report-template/node/delete`

软删节点，并软删其整棵子树。

```json
{ "id": 10 }
```

**成功：** `{ "code": 0, "message": "删除成功" }`

---

### 4.4 POST `/report-template/node/move`

拖拽：变更父节点和/或同级顺序。

**推荐契约（整段同父重排，前端拖拽最稳）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 被拖节点 |
| parentId | Long | 否 | 新父；`null` 表示变为根（仅 TITLE 且算出 level=1 合法） |
| orderedSiblingIds | Long[] | 是 | **新父下**全部未删子节点 ID，按目标顺序排列（须包含 `id`） |

```json
{
  "id": 11,
  "parentId": 12,
  "orderedSiblingIds": [15, 11, 16]
}
```

**服务端行为：**

1. 校验 `id` 未删、属某模版  
2. 校验新父（若有）：同模版、TITLE、未删；METRIC 新父必为 TITLE；**不再校验「标题 level≤3」**  
3. 禁止把节点移到自己的子孙下（环）  
4. 禁止 METRIC 作为任何人的父  
5. 重算被拖节点及其子树所有节点的 `level`（子树内保持相对 +1 关系；标题可超过原三级）  
6. 按 `orderedSiblingIds` 写同父 `sort_order`（0..n-1）  
7. 若节点换父，旧父下剩余兄弟由服务端自动压缩 `sort_order`

**成功：** `{ "code": 0, "message": "移动成功", "level": 3 }`（返回节点新 level）

---

## 5. 调用方修改点

### 5.1 管理端侧边栏

`/admin/index.html#template`「模版管理」：

列表操作顺序：

`编辑结构` → **`编辑样式`** → 改名 / 复制 / 删除

| 操作 | 行为 |
|------|------|
| 编辑结构 | 弹窗；任意 TITLE 均可「子标题」「指标」（**不再隐藏深层「子标题」**）；保存 → `saveTree` |
| 编辑样式 | 弹窗回显 `detail`；**上传** → `POST /report-template/uploadImage`（`slot=cover\|backCover`）；手填 URL / 清空 → `saveStyle`；可用 `<img src="{url}">` 预览 |
| 列表/详情 | `list` / `detail`（detail 含外观 + nodes） |

### 5.2 外部只读调用方

```bash
curl "http://localhost:9091/report-template/detail?id=1"
```

`coverImage` / `backCoverImage` 可能是：

- 外链：`https://...`
- 本服务上传：`/files/report-template/{id}/cover.png`（需拼 host 访问）

### 5.3 上传示例（管理端）

```bash
curl -F "id=1" -F "slot=cover" -F "file=@./cover.png" \
  "http://localhost:9091/report-template/uploadImage"
```

### 5.4 进度 / 异步

无。上传为同步接口（文件小，≤2MB）。

---

## 6. 校验规则摘要（与库表一致）

| 规则 | API 表现 |
|------|----------|
| 根只能是 TITLE level=1 | `parentId` 空且 `nodeType=TITLE` |
| 子 level = 父+1 | 服务端写库（标题、指标均适用） |
| 标题深度 | **无 API 层固定上限**；可无限嵌套子标题 |
| METRIC 无子 | 失败 |
| CHART 必有 chartStyle | 缺则失败 |
| 软删不可见 | list/detail 过滤 |
| saveStyle 长度 | >1024 → 失败 |
| uploadImage 格式 | 非 jpg/png → 失败 |
| uploadImage 大小 | >2MB → 失败 |
| uploadImage 写库 | 成功即更新对应列 |
| saveTree 不影响外观 | 不写 cover/back 列 |

**库表备注（非 API 契约变更）：** 现列 `level` 为 `TINYINT`，物理上限约 127；一般业务足够。若需更深再另改库类型（编码时同步库表设计即可）。

---

## 7. 明确不做（本期）

| 项 | 说明 |
|----|------|
| 对接 `createReport` | PRD Q12 |
| OSS / 云存储 | 本机（或配置的 uploadRoot）落盘 |
| 图片裁剪、水印 | 未要求 |
| 封面文字 | Q16 |
| 章节背景图上传 | Q17 |
| 鉴权 | 与现管理台同级 |

---

## 8. 待确认

- [已确认默认] **Q21** 替换/清除时尽力删旧文件  
- [已确认默认] **Q22** 上传即写库  
- [已确认默认] **Q23** `/files/report-template/**` 相对路径  
- [ ] **A8** `uploadRoot` 环境变量名（建议 `REPORT_TEMPLATE_UPLOAD_DIR`）  
- [已默认：托管图物理复制] **A9** 复制模版时图片策略  
- [已确认] **A10** 标题无限层级：取消 `TITLE.level <= 3` 校验与前端「仅 L1–L2 可加子标题」限制  

---

## 9. 与编码落点（备注，非实现）

| 层 | 预期 |
|----|------|
| Service | 删除 `MAX_TITLE_LEVEL` /「超过三级」校验（create / move / saveTree） |
| 管理端 | `renderNodesHtml`：凡 TITLE 均显示「子标题」（去掉 `level < 3`） |
| 库表 | 可选：文档注明 TINYINT；暂可不改列类型 |

---

## 10. 变更摘要（标题无限层级）

| 项 | 旧 | 新 |
|----|----|----|
| 创建/移动 TITLE | level>3 失败 | 允许任意深度 |
| saveTree | 同左 | 同左放开 |
| 管理端 | L3 不显示「子标题」 | 任意 TITLE 可加子标题 |
| 路径/字段 | 不变 | 不变 |

*本文仅 API 设计。编码须用户另行点名后产出。*  
*编码落地时同步更新 `docs/report_template_config.md`。*
