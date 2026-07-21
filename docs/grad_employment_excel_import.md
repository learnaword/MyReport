# 毕业生就业去向 Excel 导入

## 涉及调用方

- 浏览器管理台：`/admin/index.html#data`
- 其它系统可直接调用上传接口

> 数据默认归属 **武汉大学**；无需先导入学校、无需传 `schoolId`。详见 `docs/remove_school_manage/API设计.md`。

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /grad-employment/import | 跳转到管理台数据分区 |
| POST | /grad-employment/importExcel | 异步提交 Excel 导入任务 |
| GET | /grad-employment/importProgress | 查询导入进度 |

### POST /grad-employment/importExcel

**请求：** `multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | `.xlsx` / `.xls`，≤ 20MB |
| schoolId | Long | 否 | **忽略**；一律归属武汉大学 |

**Excel 现行表头（56 列，顺序如下）：**

```text
学号,学校名称,姓名,毕业年份,性别,学历,学院名称,专业名称,政治面貌,民族,困难生类别,
生源省,生源市,省内外生源,三大地理区域生源,四大经济区域生源,七大地理区域生源,八大经济区生源,广东省生源区域,
原始毕业去向,毕业去向类别,毕业去向大类,就业流向分析群体,
单位名称,单位所在地,就业省,就业市,三大地理区域就业,四大经济区域就业,七大地理区域就业,东北地区就业,
八大经济区就业,八大经济区就业2,就业城市类别,广东省就业区域,省内外就业,学校属地市就业,
粤港澳大湾区就业,西部地区就业,一带一路地区就业,京津冀地区就业,长江经济带就业,黄河流域就业,成渝经济圈就业,
省内外就业-2,省内生源就业交叉,单位性质,单位大类,单位所属行业,就业职业,
单位名称,单位名称,留学国家/地区,QS排名,US排名,升学院校层次
```

**约定：**

- 使用第一个工作表，首行为表头
- 表头须包含「学号」「毕业年份」
- 「学校名称 / 学校ID」列可读入但**不参与归属**；服务端强制写入默认校（武汉大学）
- 请求参数 `schoolId` **忽略**
- 「单位名称」出现 **3 次**，按次序映射：第 1 次→就业单位，第 2 次→就业职业后单位，第 3 次→升学院校
- 同一「学校ID + 学号 + 毕业年份」已存在则覆盖更新，否则新增（学校ID 恒为默认校）

**成功响应（任务已提交，非导入完成）：**

```json
{
  "code": 0,
  "message": "导入任务已提交",
  "taskId": "a1b2c3d4e5f6..."
}
```

**失败响应示例：**

```json
{
  "code": -1,
  "message": "请上传 Excel 文件"
}
```

**异步约定：** Redis key `import:progress:{taskId}`，TTL 24h。前端 `sessionStorage` 保存 `taskId`，刷新后恢复进度条。

### GET /grad-employment/importProgress

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | String | 是 | 提交接口返回的任务 ID |

`nState`：`1` 处理中 / `2` 成功 / `4` 失败。成功时 `result` 含 `batchId`、`inserted`、`updated`、`totalParsed`。

## 调用方修改点

### 1. 页面打开

`http://localhost:9091/admin/index.html#data`

### 2. 请求示例

```bash
curl -X POST "http://localhost:9091/grad-employment/importExcel" \
  -F "file=@grad_employment.xlsx"
# 不要再传 schoolId
curl "http://localhost:9091/grad-employment/importProgress?taskId=YOUR_TASK_ID"
```

### 3. 响应处理

```js
// POST code === 0：仅任务已提交；轮询 importProgress 至 nState 为 2/4
// nState === 2：从 result.inserted / updated / batchId 取结果
```

## 注意事项

- 学号、毕业年份为空的数据行会整批失败并返回行号提示
- 不再因「学校名称不存在」失败；无需先维护学校列表
- 列表接口仅返回武汉大学下的记录
- 旧版含代码列/500 强等列的表头将忽略；实体已按现行 56 列收缩
- 不对外暴露堆栈与本机路径
- 刷新管理台后会按 `sessionStorage` 中的 `taskId` 自动恢复进度条
