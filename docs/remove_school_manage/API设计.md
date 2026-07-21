# API 设计：去掉学校管理（默认武汉大学）

**需求来源：** `docs/remove_school_manage/PRD分析.md`、`技术方案.md`  
**功能短名：** `remove_school_manage`  
**调用方文档（编码同次落地）：** 更新 `docs/admin_console.md`、`docs/school_excel_import.md`、`docs/report_manage.md`、`docs/grad_employment_excel_import.md`；同步 `AGENTS.md` 摘要  
**响应风格：** 与现有一致 — `code`（0 成功 / -1 失败）+ `message`；HTTP 默认 200，业务成败看 `code`（下载 / 浏览器重定向除外）  
**状态：** DRAFT — 仅契约，不编码  

**本项约定（对齐技术方案）：**

| 项 | 结论 |
|----|------|
| 默认校 | 名称写死「武汉大学」；服务端按名查找/自动创建；**不**对外暴露「选校」API |
| 入参 schoolId | 就业导入/列表、报告 create/update：**可传但忽略**，一律强制默认校 |
| Excel 学校列 | 「学校名称 / 学校ID」可读入但**覆盖为默认校**，不再因校名不存在失败 |
| 学校产品 API | `/school/list`、`importExcel`、`importProgress` **下线**（返回失败） |
| 旧入口 | `GET /school/import` → 重定向管理台 `#data` |
| 报告唯一性 | 未删记录下 **同一 templateId 仅一份**（不再按学校+模版） |
| 报告列表 schoolName | 出站固定 `"武汉大学"` |
| 新增路径 | **无**（不新增 `/default-school` 之类接口） |

---

## 涉及调用方

| 调用方 | 需改点 |
|--------|--------|
| 管理台 `/admin/index.html` | 去掉学校管理与选校；create 不传 `schoolId`；不再调 `/school/list` |
| 外部脚本若调 `/school/*` | 停止调用；改走就业导入 / 报告管理 |
| 外部若调 `managed-report/create` 且必传 `schoolId` | 改为仅 `name` + `templateId`；仍传 `schoolId` 亦可（被忽略） |
| 外部若依赖就业 `schoolId` 过滤多校 | 失效：列表仅返回武汉大学数据 |
| `POST /report/createReport` | **契约不变**；管理台生成路径由服务端填 `lSchoolId`；外部若自传可继续，本需求不强改该接口 |

---

## 1. 接口变更一览

| 方法 | 路径 | 变更类型 | 说明 |
|------|------|----------|------|
| GET | `/school/import` | 行为变更 | 重定向目标：`#school` → `#data` |
| GET | `/school/list` | **下线** | 恒失败 |
| POST | `/school/importExcel` | **下线** | 恒失败 |
| GET | `/school/importProgress` | **下线** | 恒失败 |
| GET | `/grad-employment/list` | 语义变更 | `schoolId` 忽略；仅返回默认校数据 |
| POST | `/grad-employment/importExcel` | 语义变更 | `schoolId` 忽略；行一律归属默认校 |
| GET | `/grad-employment/importProgress` | 无变更 | 进度结构不变 |
| GET | `/grad-employment/import` | 无变更 | 仍跳转 `#data` |
| GET | `/managed-report/list` | 出站语义 | `schoolName` 固定「武汉大学」 |
| GET | `/managed-report/detail` | 出站语义 | 同上 |
| POST | `/managed-report/create` | 入参变更 | `schoolId` 不再必填；忽略若传；唯一性改模版维度 |
| POST | `/managed-report/update` | 入参变更 | `schoolId` 忽略；可校正库内为默认校 |
| POST | `/managed-report/generate` | 行为微调 | 按默认校（或已校正的实例 school_id）聚合；路径/响应不变 |
| POST | `/managed-report/delete` | 无变更 | — |
| GET | `/managed-report/download` | 无变更 | — |
| POST | `/report/createReport` | 无强制变更 | 见 §6 |

---

## 2. 公共约定

### 2.1 成功 / 失败

```json
{ "code": 0, "message": "ok", "...": "业务字段" }
```

```json
{ "code": -1, "message": "可读失败原因" }
```

### 2.2 默认学校（产品常量，非 API 字段配置）

| 项 | 值 |
|----|-----|
| 默认学校名称 | `武汉大学` |
| 对外是否可改 | 否（写死） |
| 出站 `schoolName` | 恒为上述名称（报告 list/detail） |
| 出站 `schoolId` | 可为库内真实 id（调试用）；调用方**勿依赖固定数字**（id 随库自增） |

### 2.3 兼容策略：忽略而非报错

对已废弃的选校入参，采用 **忽略强制默认**（技术方案 Q5），避免旧客户端因「仍传 schoolId」整单失败。

---

## 3. 学校相关 API（下线 / 重定向）

### 3.1 GET `/school/import`

**行为：** HTTP 302/跳转至 `/admin/index.html#data`（原为 `#school`）。

**调用方：** 书签、旧链接；管理台不再依赖。

---

### 3.2 GET `/school/list`（下线）

**请求参数：** 原 `page` / `size` 可忽略。

**响应（恒失败）：**

```json
{
  "code": -1,
  "message": "学校管理已下线"
}
```

**调用方修改点：** 删除所有对该接口的调用（含填学校下拉）。

---

### 3.3 POST `/school/importExcel`（下线）

**响应（恒失败）：**

```json
{
  "code": -1,
  "message": "学校管理已下线"
}
```

**说明：** 不再创建导入任务；不产生 `taskId`。请改用 `POST /grad-employment/importExcel`。

---

### 3.4 GET `/school/importProgress`（下线）

**响应（恒失败）：**

```json
{
  "code": -1,
  "message": "学校管理已下线"
}
```

或与「任务不存在」等价失败文案；**不**再返回进行中的学校导入进度。

---

## 4. 就业去向 API（语义变更）

### 4.1 GET `/grad-employment/list`

| 参数 | 类型 | 必填 | 变更后说明 |
|------|------|------|------------|
| page | int | 否 | 不变，默认 0 |
| size | int | 否 | 不变，默认 20，最大 200 |
| schoolId | Long | 否 | **忽略**；服务端只返回默认校（武汉大学）下的记录 |

**成功响应形态不变**（`content` / 分页字段）。`content[].schoolId` 为默认校真实 id。

**失败：** 与现网一致（查询失败等）。

**调用方修改点：**

- 管理台去掉学校筛选下拉，请求可不带 `schoolId`
- 勿再期望通过 `schoolId` 查看其它学校历史行（MVP 不可见）

---

### 4.2 POST `/grad-employment/importExcel`

**请求：** `multipart/form-data`

| 参数 | 类型 | 必填 | 变更后说明 |
|------|------|------|------------|
| file | File | 是 | 不变：`.xlsx` / `.xls`，大小上限沿用现网 |
| schoolId | Long | 否 | **忽略**；不再作为默认校来源 |

**Excel 约定变更：**

| 项 | 变更前 | 变更后 |
|----|--------|--------|
| 「学校名称」 | 精确匹配 `school.name`，找不到则失败 | **忽略归属**；强制默认校 |
| 「学校ID」 | 可指定学校 | **忽略归属**；强制默认校 |
| 无学校信息 | 随机选校 / 参数 schoolId | **强制默认校**（自动 ensure 存在） |
| 唯一键 | `school_id + 学号 + 毕业年份` | **不变**（全部落在同一默认校下） |
| 其它 56 列表头 | 不变 | 不变 |

**成功响应（任务已提交，不变）：**

```json
{
  "code": 0,
  "message": "导入任务已提交",
  "taskId": "a1b2c3d4e5f6..."
}
```

**失败响应示例（不再因校名）：**

```json
{
  "code": -1,
  "message": "请上传 Excel 文件"
}
```

**异步约定（不变）：** Redis key `import:progress:{taskId}`，TTL 24h；`nState`：`1` 处理中 / `2` 成功 / `4` 失败。

**调用方修改点：**

```js
// 不再传 schoolId；Excel 可仍含「学校名称」列，服务端不据此失败
const fd = new FormData();
fd.append("file", file);
await fetch("/grad-employment/importExcel", { method: "POST", body: fd });
```

---

### 4.3 GET `/grad-employment/importProgress`

无契约变更。成功时 `result` 仍含 `batchId`、`inserted`、`updated`、`totalParsed` 等。

---

## 5. 报告管理 API（入参 / 出站变更）

### 5.1 GET `/managed-report/list`

**请求：** `page` / `size` 不变。

**出站字段变更：**

| 字段 | 变更后 |
|------|--------|
| schoolId | 实例上的学校 id（应为默认校；历史行在 update/generate 时可被校正） |
| schoolName | **恒为** `"武汉大学"`（不查他校真实名展示） |
| 其它 | 不变（含 `canDownload`、`generateStatus` 等） |

**响应示例：**

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

---

### 5.2 GET `/managed-report/detail`

与 list 单项字段语义一致：`schoolName` 固定 `"武汉大学"`。请求仍为 `id` 必填。

---

### 5.3 POST `/managed-report/create`

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 报告名称，≤128 |
| templateId | Long | 是 | 模版 ID（须存在、未删；创建时宜启用） |
| schoolId | Long | **否** | **忽略**；服务端写入默认校 |

**推荐请求：**

```json
{
  "name": "2024就业质量报告",
  "templateId": 2
}
```

**仍兼容（schoolId 被忽略）：**

```json
{
  "name": "2024就业质量报告",
  "schoolId": 999,
  "templateId": 2
}
```

**成功：** `{ "code": 0, "message": "创建成功", "id": 1 }`

**失败示例：**

```json
{ "code": -1, "message": "该模版已存在报告" }
```

```json
{ "code": -1, "message": "模版不存在或已删除" }
```

**约束变更：**

| 项 | 变更前 | 变更后 |
|----|--------|--------|
| 必填 | name + schoolId + templateId | name + templateId |
| 唯一 | 同校 + 同模版一份未删 | **同模版**一份未删 |

---

### 5.4 POST `/managed-report/update`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 报告实例 ID |
| name | String | 否 | 改名称 |
| templateId | Long | 否 | 改模版（须满足模版唯一；生成中拒绝改模版，沿用现规） |
| schoolId | Long | 否 | **忽略**；服务端可将实例 `school_id` 校正为默认校 |

**成功：** `{ "code": 0, "message": "更新成功" }`

---

### 5.5 POST `/managed-report/generate`

**请求 / 成功响应不变：**

```json
{ "id": 1 }
```

```json
{ "code": 0, "message": "报告生成任务已提交", "reportId": 1 }
```

**异步约定（不变）：**

```text
Redis：report:list:{reportId} / report:lock:{reportId}
库表：generateStatus / filePath / failMessage
```

**行为说明（调用方可感知）：** 聚合数据源固定为默认校（武汉大学）就业行；不再按用户所选其它学校取数。

---

### 5.6 POST `/managed-report/delete` / GET `/managed-report/download`

契约不变。详见既有 `docs/report_manage.md`、`docs/report_download.md`。

---

## 6. `POST /report/createReport`（说明）

| 项 | 约定 |
|----|------|
| 路径 / 必填 | **不改**：`reportId` 仍必填；`reportJsonArr` 等由调用方自备 |
| `lSchoolId` | 字段保留；本需求**不**强制外部调用方必传或必为武大 |
| 管理台一键生成 | 内部组装时写入默认校 id 到 `lSchoolId`（实现细节，外部无感） |

若未来要求外部 `createReport` 也强制武大，另开需求；本期不改该接口文档主契约。

---

## 7. 调用方修改点（汇总）

### 7.1 管理台

```text
1. 删除学校管理 panel 及对 /school/list、/school/importExcel 的调用
2. 数据管理：去掉学校筛选/指定学校；导入只传 file
3. 报告管理：创建/编辑只传 name + templateId；列表学校列展示「武汉大学」或不展示选校
4. 引导文案：数据 → 模版 → 报告（无「先导入学校」）
5. hash #school → 建议落到 #data
```

### 7.2 创建报告示例（变更后）

```js
await fetch("/managed-report/create", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ name: "报告名", templateId: 2 })
});
```

### 7.3 就业导入示例（变更后）

```js
const fd = new FormData();
fd.append("file", file);
// 不要再 append("schoolId", ...)
await fetch("/grad-employment/importExcel", { method: "POST", body: fd });
```

### 7.4 已下线接口探测

```bash
curl -s "http://localhost:9091/school/list?page=0&size=20"
# 期望：{"code":-1,"message":"学校管理已下线"}
```

---

## 8. 错误文案约定（建议实现统一）

| 场景 | message（建议） |
|------|-----------------|
| 学校 list/import/progress | `学校管理已下线` |
| 报告同模版重复 | `该模版已存在报告` |
| 报告缺名称 / 模版 | 沿用现有必填校验文案 |
| 就业缺文件 / 格式 | 沿用现有文案 |
| 就业因校名失败 | **不应再出现** |

对外 `message` 不暴露堆栈、本机绝对路径。

---

## 9. 明确不做（API 层）

| 项 | 原因 |
|----|------|
| 新增 `GET /school/default` | 无需；名称写死，id 不稳定对外承诺 |
| 版本前缀 `/v2` | 与现网扁平风格一致，原地改语义 + 文档 |
| 删除 `schoolId` 出站字段 | 保留便于排障；产品 UI 可不展示 |
| 改 simple-report / 模版 / download 路径 | 与本需求无关 |

---

## 10. 编码时文档落地清单

按 `api-change-frontend-docs` 规则，实现同次须更新：

| 文件 | 动作 |
|------|------|
| `docs/admin_console.md` | 去掉学校分区与 `/school/list` 成功示例；标明下线 |
| `docs/school_excel_import.md` | 文首「已废弃」+ 指向就业导入 |
| `docs/grad_employment_excel_import.md` | 强制武大、忽略 schoolId/学校列 |
| `docs/report_manage.md` | create 无必填 schoolId；唯一性；schoolName 固定 |
| `AGENTS.md` | 学校导入产品描述收敛；默认武大一句 |

可选：根目录短页 `docs/remove_school_manage.md` 链到本文；非必须。

---

*本文仅 API 设计。数据库设计 / Task / 编码须用户另行点名后产出。*
