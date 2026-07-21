# 学校 Excel 导入（已废弃）

> **状态：已下线。** 学校管理产品能力已移除；数据默认归属武汉大学。  
> 请改用就业去向导入：`docs/grad_employment_excel_import.md`。  
> 契约说明：`docs/remove_school_manage/API设计.md`。

## 涉及调用方

- 原管理台 `#school`、静态页 `/school/import.html` → 现重定向至 `/admin/index.html#data`
- 其它系统请停止调用下方接口

## API（下线行为）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /school/import | 重定向到管理台 `#data` |
| GET | /school/list | `{ "code": -1, "message": "学校管理已下线" }` |
| POST | /school/importExcel | 同上 |
| GET | /school/importProgress | 同上 |

## 调用方修改点

1. 删除对 `/school/list`、`/school/importExcel` 的调用  
2. 就业数据直接 `POST /grad-employment/importExcel`（无需先导入学校）  
3. 报告创建不再传 `schoolId`，见 `docs/report_manage.md`

## 注意事项

- 库表 `school` 仍保留，应用会自动确保存在「武汉大学」记录
- 勿再依赖学校 Excel 导入任务进度
