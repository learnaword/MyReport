# 报告下载

## 涉及调用方

- 管理台：`/admin/index.html#report`（报告管理列表「下载」）
- 外部系统：可按报告实例 ID 调用下载接口获取 `.docx`

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /managed-report/download | 按报告 ID 下载最近可下载的 Word |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 报告实例 ID（`managed_report.id`） |

**成功：** HTTP 200，响应体为 `.docx` 二进制流。

| Header | 说明 |
|--------|------|
| Content-Type | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` |
| Content-Disposition | `attachment; filename="..."; filename*=UTF-8''...`（文件名优先为报告名称） |

**失败：** HTTP 400，JSON：`{ "code": -1, "message": "..." }`

常见 `message`：

- 报告不存在或已删除
- 报告尚未生成成功，无法下载（草稿 / 生成中）
- 暂无可下载文件
- 文件不存在或已失效，请重新生成
- 文件路径非法

**规则：**

- 仅按报告 ID 取库内 `file_path`，**禁止**客户端传磁盘路径
- `generateStatus=2`（成功）且文件存在 → 可下载
- `generateStatus=3`（失败）但仍保留上次成功 `file_path` → 可下载旧稿
- 软删报告不可下载
- 路径须落在文件物理根目录下（防穿越）

列表 / 详情新增字段：`canDownload`（boolean），管理台据此显示「下载」按钮。

## 调用方修改点

### 1. 请求示例

```bash
curl -OJ "http://localhost:9091/managed-report/download?id=1"
```

### 2. 响应处理（管理台 / 前端）

```js
fetch("/managed-report/download?id=" + id).then(function (r) {
  var ct = (r.headers.get("content-type") || "").toLowerCase();
  if (!r.ok || ct.indexOf("application/json") >= 0) {
    return r.json().then(function (body) {
      // body.code === -1，提示 body.message
    });
  }
  return r.blob().then(function (blob) {
    // 触发浏览器保存 .docx
  });
});
```

### 3. 进度 / 状态

无新 Redis key。下载只看库表终态与 `file_path`：

```text
generateStatus：0草稿 1生成中 2成功 3失败
canDownload：成功，或失败但仍有旧 file_path
```

## 注意事项

- 生成成功后需等 `generateStatus=2`（或列表刷新出「下载」）再下
- 改学校/模版后未重生成时，下载仍可能是上次成功稿
- 与模版静态资源 `/files/report-template/**` 无关，勿用裸路径拼下载
- 当前管理台无登录，与现有 admin 同级（内网可信）；后续鉴权另立需求
