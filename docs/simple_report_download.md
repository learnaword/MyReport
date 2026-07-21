# 简化报告下载

## 涉及调用方

- 管理台：`/admin/index.html#simple-report`（配置列表「下载」、编辑弹窗「下载本次」）
- OpenClaw / Comate Skill：可按 `runId` 或配置 `id` 拉取 `.docx`
- 外部系统：同上 HTTP 接口

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /simple-report/download | 按配置 ID 下载最近一次可下载成功稿 |
| GET | /simple-report/runs/download | 按运行 ID 下载指定稿 |

### GET /simple-report/download

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 简化报告配置 ID（未软删） |

**成功：** HTTP 200，`.docx` 二进制流。

| Header | 说明 |
|--------|------|
| Content-Type | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` |
| Content-Disposition | `attachment; filename="..."; filename*=UTF-8''...`（优先报告名称） |

**失败：** HTTP 200，JSON：`{ "code": -1, "message": "..." }`

### GET /simple-report/runs/download

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| runId | Long | 是 | `simple_report_run.id` |

成功/失败形态同配置级下载。配置已软删时，若 run 仍在且 SUCCESS，**仍可下载**。

**规则：**

- 仅按 `id` / `runId` 取库内路径，**禁止**客户端传磁盘路径
- 仅 `runStatus=3`（SUCCESS）且文件存在、路径在物理根（或交付根）下 → 可下载
- 路径优先 `deliveryPath`，否则 `filePath`
- 配置级下载 = 该配置最近一次仍可下的 SUCCESS run（`finishedTime`/`id` 降序）

**列表字段增量：**

| 接口 | 字段 | 说明 |
|------|------|------|
| GET /simple-report/list | `canDownload` | boolean |
| GET /simple-report/list | `latestSuccessRunId` | Long \| null |
| GET /simple-report/runs/list、detail | `canDownload` | boolean |

## 调用方修改点

### 1. 请求示例

```bash
curl -OJ "http://localhost:9091/simple-report/download?id=1"
curl -OJ "http://localhost:9091/simple-report/runs/download?runId=100"
```

### 2. 响应处理（管理台）

```js
fetch("/simple-report/download?id=" + id).then(function (r) {
  var ct = (r.headers.get("content-type") || "").toLowerCase();
  if (ct.indexOf("application/json") >= 0) {
    return r.json().then(function (body) {
      // body.code === -1，提示 body.message
    });
  }
  return r.blob().then(function (blob) {
    // 触发浏览器保存 .docx
  });
});
```

列表：`canDownload === true` 时展示「下载」。弹窗：本次 run 成功后「下载本次」走 `/runs/download?runId=`。

### 3. 进度 / 状态

无新 Redis key。下载只看库表：

```text
runStatus：1待确认 2生成中 3成功 4失败 5取消
canDownload：仅成功且文件有效
```

## 注意事项

- 多次成功时，配置级下载是**最近一次可下稿**；要指定版本用 `runId`
- 生成成功后需刷新列表或使用弹窗「下载本次」
- 勿用 `deliveryPath` / `filePath` 拼静态 URL
- 当前管理台无登录，与现有 admin 同级（内网可信）
