# 去掉学校管理（默认武汉大学）

## 涉及调用方

- 管理台 `/admin/index.html`（去掉学校管理与选校）
- 原依赖 `/school/*` 的脚本 / 外部系统
- 报告创建、就业导入调用方

## API

契约详情见：`docs/remove_school_manage/API设计.md`。

| 变更 | 说明 |
|------|------|
| `/school/list` 等 | 下线，`code=-1`，`message=学校管理已下线` |
| `/grad-employment/*` | `schoolId` 忽略；强制武汉大学 |
| `/managed-report/create` | 仅需 `name` + `templateId`；同模版一份 |

## 调用方修改点

### 1. 创建报告

```json
{ "name": "报告名", "templateId": 2 }
```

### 2. 就业导入

只传 `file`，不要传 `schoolId`。

### 3. 停止调用学校接口

```bash
curl -s "http://localhost:9091/school/list"
# {"code":-1,"message":"学校管理已下线"}
```

## 注意事项

- 设计文档目录：`docs/remove_school_manage/`
- 可选 DDL：`docs/remove_school_manage/ddl_mysql8.sql`
- 库表 `school` / `school_id` 仍保留作内部归属键
