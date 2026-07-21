# API 设计：指标统计字段下拉选择

**需求来源：** `docs/metric_field_select/PRD分析.md`、`docs/metric_field_select/技术方案.md`  
**功能短名：** `metric_field_select`  
**调用方：** 本仓库管理台「模版管理 → 编辑结构」；外部若自行调用模版节点写入接口，须遵守收紧后的 `statField` 白名单  
**响应风格：** 与现有一致 — `code`（0 成功 / -1 失败）+ `message`；HTTP 默认 200，业务成败看 `code`  
**路径风格：** 挂在既有前缀 `/report-template`（技术方案 T3）

**本项约定（采用技术方案推荐）：**

| 项 | 结论 |
|----|------|
| 字典路径 | `GET /report-template/stat-fields` |
| 落库 | 保存时归一为规范 snake_case `value`（T1） |
| 未知字段 | 前端保存前拦截未改选的未知项；后端仍拒绝非白名单（T2） |
| `detail` | 不增加 `statFieldLabel`，仅返回列名 |

---

## 涉及调用方

| 调用方 | 需改点 |
|--------|--------|
| 管理台 `/admin/index.html#template`（编辑结构） | 拉字典；统计字段改下拉；未知字段 UI；提交仍传 `statField` |
| 外部直接调 `node/create` / `node/update` / `saveTree` | `statField` 必须为字典 `value`；非法将被拒绝 |
| `POST /report/createReport` / 报告一键生成 | **无改动**（不读本字典） |

---

## 1. 接口一览（本功能）

| 方法 | 路径 | 说明 | 变更类型 |
|------|------|------|----------|
| GET | `/report-template/stat-fields` | 可选统计字段字典（中文 label + 库列名 value） | **新增** |
| POST | `/report-template/node/create` | METRIC 的 `statField` 须 ∈ 字典 | **语义收紧** |
| POST | `/report-template/node/update` | 同上 | **语义收紧** |
| POST | `/report-template/saveTree` | 树中 METRIC 的 `statField` 同上 | **语义收紧** |
| GET | `/report-template/detail` | 仍只返回 `statField` 列名 | 无结构变更 |

完整模版 API 见 `docs/report_template_config/API设计.md`（已同步本变更摘要）。

---

## 2. 公共约定

### 2.1 成功 / 失败

```json
{ "code": 0, "message": "ok", "...": "业务字段" }
```

```json
{ "code": -1, "message": "统计字段不在可选列表中" }
```

### 2.2 `statField` 语义（变更）

| 项 | 说明 |
|----|------|
| 含义 | `grad_employment_record` 可聚合业务列的**规范库列名** |
| 合法来源 | 仅 `GET /report-template/stat-fields` 返回的 `value` |
| 非法 | 空、未知列名、任意自造字符串 → `code=-1` |
| 别名入站 | 若传入可识别别名（如历史 camelCase），服务端**归一**为规范 `value` 再落库 |
| 出站 | `detail` / 节点响应中的 `statField` 为规范列名（无中文） |

### 2.3 字典项形状

| 字段 | 类型 | 说明 |
|------|------|------|
| value | String | 库列名，如 `college_name`；写入节点时用此值 |
| label | String | 中文展示名，如 `学院名称`；仅配置 UI 用 |

---

## 3. 新增：GET `/report-template/stat-fields`

返回当前服务支持的全部统计字段选项（有序、扁平、不分页）。

**Query：** 无。

**成功响应：**

```json
{
  "code": 0,
  "message": "ok",
  "fields": [
    { "value": "student_no", "label": "学号" },
    { "value": "student_name", "label": "姓名" },
    { "value": "graduation_year", "label": "毕业年份" },
    { "value": "gender_name", "label": "性别" },
    { "value": "education_name", "label": "学历" },
    { "value": "college_name", "label": "学院名称" },
    { "value": "major_name", "label": "专业名称" },
    { "value": "destination_category", "label": "毕业去向类别" },
    { "value": "employer_name", "label": "单位名称" },
    { "value": "unit_name_after_occupation", "label": "单位名称" },
    { "value": "further_study_school_name", "label": "单位名称" }
  ]
}
```

> 完整列表以实现侧 Catalog 为准；文案对齐 `docs/graduate_employment/数据库设计.md` §2。上表为形态示例。附录 A 为与当前聚合白名单对齐的完整对照（编码时须一致）。

**失败：** 一般不应失败；异常时 `{ "code": -1, "message": "查询失败" }`。

**缓存：** 调用方可在「打开编辑结构」时拉一次并内存缓存；字段随发版变更，无需本地写死。

---

## 4. 既有接口：语义变更

### 4.1 POST `/report-template/node/create`（METRIC）

**请求参数（METRIC 相关，相对原文档增量说明）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| statField | String | 是 | 必须为字典 `value`（或可归一的已知别名） |

**请求示例：**

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

**成功：** `{ "code": 0, "message": "创建成功", "id": 11, "level": 2 }`

**失败 message 示例（`code=-1`）：**

- `统计字段不能为空`
- `统计字段不在可选列表中`
- （其余校验同原模版 API）

### 4.2 POST `/report-template/node/update`（METRIC）

传 `statField` 时规则同创建；归一后落库。

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

**成功：** `{ "code": 0, "message": "更新成功" }`

### 4.3 POST `/report-template/saveTree`

树中每个 `nodeType=METRIC` 的 `statField` 校验与 create 相同；任一非法则整单失败（`code=-1`），不部分提交。

---

## 5. 调用方修改点

### 5.1 拉取字典并渲染下拉

```js
// 打开编辑结构时
const res = await fetch("/report-template/stat-fields").then((r) => r.json());
if (res.code !== 0) {
  // 提示失败，禁止保存指标
}
const fields = res.fields; // [{ value, label }, ...]

// 渲染 <select>：option.value = value，option.text = label
// 编辑回显：selected = node.statField
// 若 node.statField 不在 fields[].value：
//   追加 <option value="{原值}">未知字段({原值})</option> 并 selected
```

### 5.2 提交

```js
// code === 0 表示保存成功
// 提交 body.statField 必须是下拉选中的 value（库列名），不要传中文 label
// 若当前仍是「未知字段」选项：前端拦截，提示先改选合法字段
```

### 5.3 响应处理

```js
if (res.code === -1 && /统计字段/.test(res.message || "")) {
  // 展示 message；引导重新选择下拉项
}
```

### 5.4 进度 / 状态

无异步约定；本功能无 Redis key。

---

## 6. 注意事项

- `label` 仅用于展示；落库与生成只认 `value`（`statField`）。
- 三列「单位名称」`label` 可相同，`value` 不同（`employer_name` / `unit_name_after_occupation` / `further_study_school_name`）；UI 靠选中值区分，可选 `title=value` 悬停辅助（非契约必选）。
- 字典与生成白名单同源；勿在前端维护第二份硬编码列表（除非离线兜底，且须与发版同步）。
- 历史脏 `statField`：`detail` 仍原样返回；编辑须改选后才能再保存成功。
- 不改 `POST /report/createReport`。

---

## 附录 A：统计字段对照表（契约清单）

编码实现 Catalog / 本接口 `fields` 须与下表一致（顺序建议保持）。

| value | label |
|-------|-------|
| student_no | 学号 |
| student_name | 姓名 |
| graduation_year | 毕业年份 |
| gender_name | 性别 |
| education_name | 学历 |
| college_name | 学院名称 |
| major_name | 专业名称 |
| political_status_name | 政治面貌 |
| ethnicity_name | 民族 |
| hardship_type_name | 困难生类别 |
| source_province | 生源省 |
| source_city | 生源市 |
| source_in_out_province | 省内外生源 |
| source_geo3 | 三大地理区域生源 |
| source_econ4 | 四大经济区域生源 |
| source_geo7 | 七大地理区域生源 |
| source_econ8 | 八大经济区生源 |
| source_gd_region | 广东省生源区域 |
| destination_raw | 原始毕业去向 |
| destination_category | 毕业去向类别 |
| destination_major_category | 毕业去向大类 |
| flow_analysis_group | 就业流向分析群体 |
| employer_name | 单位名称 |
| employer_location | 单位所在地 |
| job_province | 就业省 |
| job_city | 就业市 |
| job_geo3 | 三大地理区域就业 |
| job_econ4 | 四大经济区域就业 |
| job_geo7 | 七大地理区域就业 |
| job_northeast | 东北地区就业 |
| job_econ8 | 八大经济区就业 |
| job_econ8_2 | 八大经济区就业2 |
| job_city_tier | 就业城市类别 |
| job_gd_region | 广东省就业区域 |
| job_in_out_province | 省内外就业 |
| job_school_city | 学校属地市就业 |
| job_gba | 粤港澳大湾区就业 |
| job_west | 西部地区就业 |
| job_belt_road | 一带一路地区就业 |
| job_jjj | 京津冀地区就业 |
| job_yangtze_belt | 长江经济带就业 |
| job_yellow_river | 黄河流域就业 |
| job_chengyu | 成渝经济圈就业 |
| job_in_out_province_2 | 省内外就业-2 |
| in_province_source_job_cross | 省内生源就业交叉 |
| employer_nature | 单位性质 |
| employer_major_type | 单位大类 |
| employer_industry | 单位所属行业 |
| job_occupation | 就业职业 |
| unit_name_after_occupation | 单位名称 |
| further_study_school_name | 单位名称 |
| abroad_country_region | 留学国家/地区 |
| qs_rank | QS排名 |
| us_rank | US排名 |
| further_study_level | 升学院校层次 |

> 不含系统列：`id` / `school_id` / `batch_id` / `created_at` / `updated_at` / 导入用「学校名称」。

---

*本文为 API 设计 + 调用方对接说明。Task 拆分 / 编码须用户另行点名后产出。*
```
