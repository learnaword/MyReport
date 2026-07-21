---
name: ai-simple-report
description: >-
  通过 MyReport「AI 简化报告」API 出 Word：先按多数据接口配置生成可审 MD 计划，
  经用户明确确认后再异步生成 .docx 并落交付目录。适用于「简化报告」「多接口出 Word」
  「plan 确认生成报告」、OpenClaw/龙虾办公出稿。禁止用 python-docx / Word MCP / Pandoc
  作为主出稿路径。触发词：简化报告、AI简化报告、simple-report、多接口报告、出 Word 计划。
version: 1.0.0
license: MIT-0
metadata:
  openclaw:
    emoji: "📊"
    requires:
      bins: ["curl"]
---

# AI 简化报告（MyReport）

> Agent **只编排**：调 MyReport HTTP → 展示计划 MD → **等人确认** → 触发出 Word。  
> **真正写 `.docx` 的是 MyReport（Spire）**，不是本 Skill、不是 Word MCP、不是 python-docx。

## When to Use

- 用户要按「已配置的多数据接口 + 图表样式」出一份 Word
- 用户说「先出计划 / 梳理查什么数据再生成」
- OpenClaw 龙虾对话里要跑简化报告流水线

### Avoid when

- 用户要改就业**模版树**报告（走 `managed-report`，不是本 Skill）
- 用户只要本地随便写个 docx、且明确拒绝走 MyReport
- 尚未部署 `/simple-report` API（须先完成服务端编码）

## 硬性禁止

1. **禁止**用 `python-docx`、Pandoc、`minimax-docx`、Word MCP 作为**主出稿**路径。  
2. **禁止**在用户未明确说「确认 / 同意生成 / 执行计划」之前调用 `confirm`。  
3. **禁止**编造上游业务数据冒充正式稿；数据必须由服务端按配置接口拉取。  
4. **禁止**把 Token/密码写进对话或 Skill 配置明文（MVP 接口无鉴权，后续也走服务端配置）。

## 环境变量

| 变量 | 说明 | 示例 |
|------|------|------|
| `MYREPORT_BASE_URL` | MyReport 根地址（无尾斜杠） | `http://127.0.0.1:9091` |

若未设置：先问用户 Base URL，再继续。所有请求：`{MYREPORT_BASE_URL}/simple-report/...`。

业务响应：`code === 0` 成功，`code === -1` 失败（读 `message`）。下载接口返回文件流。

## 主流程（Must）

```text
用户诉求
  →（可选）list / detail 选定配置 id
  → POST /simple-report/plan
  → 完整展示 planMd，询问是否确认
  → 用户明确确认后 POST /simple-report/runs/confirm
  → 轮询 GET /simple-report/runs/detail?runId=
  → 成功：告知 deliveryPath，并可给 download 链接
```

### Step 1 — 选定配置

```bash
curl -sS "${MYREPORT_BASE_URL}/simple-report/list?page=0&size=20"
curl -sS "${MYREPORT_BASE_URL}/simple-report/detail?id={id}"
```

- 若用户未指定报告：列出 `name` / `id` / `blockCount`，请用户选一个。  
- `detail.blocks` 即「多接口」：每块有 `url`、`renderStyle`（`TABLE`/`BAR`/`PIE`/`LINE`）。

### Step 2 — 生成计划（不出 Word）

```bash
curl -sS -X POST "${MYREPORT_BASE_URL}/simple-report/plan" \
  -H "Content-Type: application/json" \
  -d '{"id":1,"userPrompt":"用户原话摘要"}'
```

成功时拿到：`runId`、`planMd`、`runStatus=1`（待确认）。

**必须**把 `planMd` 原文展示给用户（可略作排版，勿删接口/样式关键行），然后问：

> 以上计划是否确认生成 Word？回复「确认生成」后我才会提交。

### Step 3 — 确认生成（仅在用户明确同意后）

用户明确同意的例子：`确认`、`确认生成`、`同意`、`执行计划`。  
含糊语气（如「看看再说」）→ **不要** confirm。

```bash
curl -sS -X POST "${MYREPORT_BASE_URL}/simple-report/runs/confirm" \
  -H "Content-Type: application/json" \
  -d '{"runId":100}'
```

成功：`runStatus=2`，`engineReportId`（一般为 `1000000000 + runId`）。  
任务已异步提交；**不要**声称 Word 已生成完毕。

### Step 4 — 轮询终态

每隔 2–5 秒：

```bash
curl -sS "${MYREPORT_BASE_URL}/simple-report/runs/detail?runId={runId}"
```

| runStatus | 含义 | Agent 动作 |
|-----------|------|------------|
| 1 | 待确认 | 不应出现在 confirm 之后；提醒用户 |
| 2 | 生成中 | 继续等；可提示「生成中…」 |
| 3 | 成功 | 报告 `deliveryPath`；提供下载 URL |
| 4 | 失败 | 展示 `failMessage`；停止 |
| 5 | 取消 | 告知已取消 |

成功时下载：

```text
{MYREPORT_BASE_URL}/simple-report/runs/download?runId={runId}
```

```bash
curl -OJ "${MYREPORT_BASE_URL}/simple-report/runs/download?runId={runId}"
```

`warnCount > 0`：告知「部分数据块拉取失败，文档中可能含错误占位」。

### Step 5 — 取消待确认（可选）

用户说「取消计划」且仍为待确认：

```bash
curl -sS -X POST "${MYREPORT_BASE_URL}/simple-report/runs/cancel" \
  -H "Content-Type: application/json" \
  -d '{"runId":100}'
```

## 配置协助（Should）

用户要**新建/改**多接口配置时，可调：

| 动作 | 方法 | 路径 |
|------|------|------|
| 创建 | POST | `/simple-report/create` |
| 改元信息 | POST | `/simple-report/update` |
| 整表替换区块 | POST | `/simple-report/save-blocks` |
| 删除 | POST | `/simple-report/delete` |

创建示例（一份 Word = 多个接口）：

```bash
curl -sS -X POST "${MYREPORT_BASE_URL}/simple-report/create" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "周报简化",
    "deliveryDir": "simple/weekly",
    "blocks": [
      {
        "title": "学院就业人数",
        "httpMethod": "GET",
        "url": "https://data.example.com/api/emp/by-college",
        "renderStyle": "BAR",
        "promptHint": "按学院统计人数"
      },
      {
        "title": "去向分布",
        "httpMethod": "GET",
        "url": "https://data.example.com/api/emp/destination",
        "renderStyle": "TABLE"
      }
    ]
  }'
```

约束提醒用户：

- `deliveryDir` 不能含 `../`；须在服务端白名单根下  
- `url` 的 host 须在服务端 SSRF 白名单内  
- 上游接口须返回 `{ "labels": [...], "values": [...] }`（或 `code`+`data` 包装）  
- MVP **无**接口鉴权 Header  

改区块用 `save-blocks`（整表替换），不要幻想「只改第 2 个」除非你先 detail 再提交完整数组。

## 可选：润色计划文案

仅当用户要求「改一下计划说明」且 Run 仍为待确认：

```bash
curl -sS -X PUT "${MYREPORT_BASE_URL}/simple-report/runs/plan" \
  -H "Content-Type: application/json" \
  -d '{"runId":100,"planMd":"# 修订后的计划\n..."}'
```

注意：此接口**只改展示用 MD**，**不会**改变真正拉数用的接口快照。勿在 MD 里「假装」改了 URL 就以为执行会变——要改接口必须改配置并重新 `plan`。

## 进度补充（Could）

若环境可访问 Redis：进度 key 为 `report:list:{engineReportId}`。  
无 Redis 时，只轮询 `runs/detail` 即可。

## 对话话术模板

**出计划后：**

```text
已生成简化报告计划（runId={runId}），请审阅：

{planMd}

确认无误后请回复「确认生成」，我将提交 MyReport 异步出 Word。
（此时不会调用 python-docx / Word MCP。）
```

**成功后：**

```text
Word 已生成成功。
- 交付路径：{deliveryPath}
- 下载：{MYREPORT_BASE_URL}/simple-report/runs/download?runId={runId}
```

## 契约速查

完整 API：仓库内 `docs/ai_simple_report.md`、`docs/ai_simple_report/API设计.md`。

| runStatus | 含义 |
|-----------|------|
| 1 | PENDING_CONFIRM |
| 2 | GENERATING |
| 3 | SUCCESS |
| 4 | FAILED |
| 5 | CANCELLED |

| renderStyle | Word 内效果 |
|-------------|-------------|
| TABLE | 表格 |
| BAR | 柱状图 |
| PIE | 饼图 |
| LINE | 折线图 |

## 安装（OpenClaw）

1. 将本目录 `ai-simple-report/` 打成 zip 或放入 Agent 的 skills 目录。  
2. 在龙虾/OpenClaw 中安装 Skill，并设置 `MYREPORT_BASE_URL`。  
3. 对话示例：`按「周报简化」配置出一份本周概况计划` → 审 MD → `确认生成`。

## Agent 自检清单

- [ ] 未在确认前调用 confirm  
- [ ] 未使用 python-docx / Word MCP / Pandoc 主出稿  
- [ ] 已展示完整 planMd  
- [ ] confirm 后轮询至 3/4，未谎称已完成  
- [ ] 成功时给出 deliveryPath 或 download 链接  
