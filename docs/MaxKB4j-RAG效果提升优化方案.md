# MaxKB4j RAG 效果提升代码优化方案

> 基于当前代码实现（索引、检索、注入、生成全链路）梳理，按"现状 → 问题 → 分级优化方案"组织。
> 所有引用均为仓库内真实代码位置，可直接定位。

---

## 一、现状：RAG 链路盘点

### 1.1 索引侧（写入链路）

| 环节 | 实现 | 位置 |
| :--- | :--- | :--- |
| 文档解析 | PDFBox 逐行解析 + 字号/粗体推断标题层级 + PPOCR 扫描件 OCR + 图片抽取上传；可选 DocLing 引擎 | `maxkb4j-service/maxkb4j-knowledge/src/main/java/com/maxkb4j/knowledge/parser/impl/PdfParser.java`、`DocumentParseServiceImpl.java` |
| 分块 | Markdown 标题层级切分 → 超长按句子合并到 512 字符；表格整体保留（`{{TABLE}}` 标记）；BreakIterator 分句 | `service/impl/DocumentSplitServiceImpl.java`、`util/SentenceSplitter.java`、`util/TextSplitter.java` |
| 向量化 | 嵌入输入 = `title + content`；双写 PgVector（batch=10 + 指数退避重试）与 MongoDB 全文库（jieba 预分词） | `service/ParagraphIndexBatcher.java:102`、`store/impl/PgVectorEmbeddingStoreImpl.java`、`store/impl/FullTextStoreImpl.java` |
| 问题路 | 问题内容单独建索引，检索命中后经 `problem_paragraph` 映射回段落 | `service/impl/ProblemServiceImpl.java`、`retriever/SearchOrchestrator.java` |

### 1.2 检索侧（应用对话管线，`ChatSimpleServiceImpl` 组装）

```
[可选] ResetProblemStep          多轮问题改写（CompressingQueryAssistant）
        ↓
SearchDatasetStep                原问题 + 改写问题并行检索，按最大相似度合并 → topN
        ↓
DataRetriever → store 选择        vector / fullText / composite(hybrid)
        ↓
SearchOrchestrator               段落路 + 问题路双路召回 → 去重排序截断
        ↓
GenerateHumanMessageStep         RagContentInjector 将命中段落拼成 JSON 注入 prompt
        ↓
ChatStep                         AiServices 流式生成（记忆 / 工具）
```

- 工作流侧另有 `SearchKnowledgeNode`（检索 + 命中直答）与 `RerankerNode`（ScoringModel 重排）。
- 按需模式（`onDemandEnable`）把知识库包装成 Tool，由 `KnowledgeExecutor` 执行多 query 并行检索。

---

## 二、核心问题清单

### 索引侧

| # | 问题 | 位置 | 影响 |
| :--- | :--- | :--- | :--- |
| I1 | 分块无重叠（overlap=0），跨块边界信息丢失 | `TextSplitter.mergeChunksIntoParts` | 边界问答召回差 |
| I2 | 嵌入输入仅 `title + content`，缺文档名/标题路径等上下文，短块语义稀疏 | `ParagraphIndexBatcher.java:102` | 向量区分度低，同名术语混淆 |
| I3 | limit 固定 512 字符且按字符非 token 计；长表格切块不复制表头 | `DocumentSplitServiceImpl.java:51` | 块长与嵌入模型窗口不匹配；表格块丢表头语义 |
| I4 | 分块 title 只取最近一级标题，未继承父级标题路径 | `DocumentSplitServiceImpl.splitByHeadings` | 脱离文档结构后块语义不完整 |

### 检索侧

| # | 问题 | 位置 | 影响 |
| :--- | :--- | :--- | :--- |
| R1 | 混合检索融合 = 两路取 max 分，但两路量纲不同：向量路余弦 [-1,1]，全文路按当前 query top 分相对归一 [0,1] | `CompositeStoreImpl.java:121` | hybrid 排序失真，效果常不如单路 |
| R2 | 全文路归一化相对 top 命中，`minScore` 过滤时 top 命中恒过阈值，阈值语义失效 | `FullTextStoreImpl.java:39,155` | 低质结果漏入上下文 |
| R3 | 应用对话主链路无 rerank；ScoringModel 重排仅存在于工作流 `RerankerNode` | `RerankerNodeHandler.java`（workflow） | 检索噪声直接进 prompt，是效果最大短板 |
| R4 | 无召回超采样：每路 topK = 最终 topK，融合/重排没有候选余量 | `SearchRequest.topK` 全链路透传 | 融合后有效候选过少 |
| R5 | 按需知识库路由代码被注释（RouterAssistant 整块注释），多知识库始终全量检索 | `SearchDatasetStep.execute` 注释块 | 噪声库稀释召回 |
| R6 | 查询侧仅单路改写，无多查询扩展 / HyDE；检索模式固定由配置决定，不随 query 特征自适应 | `ResetProblemStep`、`DataRetriever.getStore` | 短关键词查询向量召回弱，反之亦然 |

### 生成侧

| # | 问题 | 位置 | 影响 |
| :--- | :--- | :--- | :--- |
| G1 | 默认注入模板弱：无"仅依据上下文回答、不足则明说"约束，无引用编号要求；JSON + PrettyFormat 耗 token | `RagContentInjector.java:24` | 幻觉与 token 浪费 |
| G2 | 上下文截断按整块丢弃，预算内最后一块直接舍弃 | `RagContentInjector.formatJson` | 预算利用率低 |
| G3 | "命中直答"（`returnIfSatisfied`）仅工作流生效，应用管线未对齐 | `ParagraphRagVO.returnIfSatisfied` 仅被 `SearchKnowledgeNodeHandler` 使用 | 配置了直答的应用不生效，行为不一致 |

### 观测与评估

| # | 问题 | 影响 |
| :--- | :--- | :--- |
| O1 | 无检索质量离线评估（Recall@k / MRR / nDCG），改动好坏靠感觉 | 优化无法闭环验证 |
| O2 | details 中未记录分路召回数、分数分布、各路耗时 | 线上问题难定位 |

---

## 三、优化方案（按优先级分期）

### P0 快速见效（预计 1~2 周，改动小、收益大）

#### 1. 应用管线内置 Rerank（对应 R3、R4）
- `KnowledgeSetting` 增加 `rerankEnable / rerankModelId / rerankTopN`。
- `SearchDatasetStep` 召回量改为超采样：`topK = rerankEnable ? topN * 3 : topN`。
- 在 `SearchDatasetStep` 与 `GenerateHumanMessageStep` 之间新增 `RerankStep`：
  - 复用 `IModelProviderService.buildScoringModel`（`RerankerNodeHandler` 已有完整用法可参照）；
  - `scoreAll(segments, question)` → 按分排序 → 过滤 `similarity` → 截断 `rerankTopN`。
- `ChatSimpleServiceImpl` 按开关注册该 step。
- 预期：重排是 RAG 公认收益最大的单点，直接提升进入 prompt 的上下文精度。

#### 2. 混合检索融合改为 RRF（对应 R1、R2）
- `CompositeStoreImpl.mergeByMaxScore` 改为 Reciprocal Rank Fusion：
  `score(d) = Σ_route 1 / (k + rank_route(d))`，k=60；两路各取 topK 候选。
- RRF 只依赖名次不依赖分值，天然消除向量/全文量纲不一致问题。
- 阈值语义修正：`minScore` 不再在 `FullTextStoreImpl.java:155` 用相对分过滤；改为融合后统一判断（或在有 rerank 时以 rerank 分过滤）。

#### 3. 注入 Prompt 强化（对应 G1、G2）
- 重写 `RagContentInjector.DEFAULT_PROMPT_TEMPLATE`：
  - 明确"仅依据提供的知识库内容回答；内容不足以回答时明确告知，不得编造"；
  - 上下文条目加引用编号（如 `[1] [2]`），要求答案中标注来源编号，支撑前端溯源展示；
  - 序列化去掉 `PrettyFormat`，减少约 30% 无效 token。
- `formatJson` 截断策略：预算不足时对最后一块做字符截断而非整块丢弃（保留已排序的高分内容）。

#### 4. 应用管线对齐"命中直答"（对应 G3）
- 在 `SearchDatasetStep` 检索后检查 `ParagraphRagVO.returnIfSatisfied()`；
- 命中时在 `GenerateHumanMessageStep` 前短路（直接以段落内容作答，跳过 LLM 生成），与工作流行为一致。

### P1 索引质量（预计 2~4 周，需要重建索引）

#### 5. 分块重叠（对应 I1）
- `TextSplitter.mergeChunksIntoParts` 增加 `overlap` 参数（建议 `limit * 10%~15%`），`SentenceSplitter` 透传；
- `KnowledgeSetting` 或全局配置暴露 `chunkOverlap`。

#### 6. 上下文增强嵌入（对应 I2、I4）
- 分块时在 `ParagraphSimple/ParagraphEntity` 落库标题路径（`文档名 > H1 > H2`，`splitByHeadings` 已维护 heading stack，补充输出即可）。
- `ParagraphIndexBatcher` 嵌入输入改为：`[文档名 | 标题路径]\n标题 + 正文`；段落表已有 `documentId`，文档名可在批处理时一次性查出。
- 可选进阶（开关控制）：索引期用小模型为每块生成 1~2 句上下文摘要作为前缀（Contextual Retrieval），对短块/表格块收益明显，成本可控（仅索引期一次性调用）。

#### 7. 分块细节增强（对应 I3）
- 长表格切块时复制表头行进入每个子块，保留列语义；
- `limit` 从字符数改为按嵌入模型 tokenizer 估算的 token 数（中文 512 字符与 512 token 差异显著）；
- 句子合并时对列表项/代码块做原子性保护，不从中间切断。

#### 8. 检索路由与按需召回（对应 R5、R6）
- 恢复 `SearchDatasetStep` 中被注释的 RouterAssistant 按需路由，修复其逻辑后灰度（当前注释块中 ID 映射判断有缺陷，恢复时需修正）；
- 检索模式自适应：短查询（<=4 token）自动偏向全文/混合，长句偏向向量；作为 `searchMode=auto` 新选项。

### P2 查询增强与评估闭环（长期）

#### 9. 多查询扩展 / HyDE（对应 R6）
- 复用 `SearchDatasetStep.retrieval` 已有的并行检索合并骨架，扩展为 N 路（改写、关键词化、HyDE 假设答案）；
- 开关 + 并发度控制，避免 LLM 调用成本失控。

#### 10. 评估与观测体系（对应 O1、O2）
- 建立黄金问答集与离线评估命令：Recall@k / MRR / nDCG，作为每次检索改动的回归基线；
- `SearchDatasetStep.getDetails()` 扩展：记录各路召回条数、融合前后分数分布、分路耗时；
- 将 RAG 评估纳入现有 `docs/MaxKB4j-回归测试报告.md` 流程。

#### 11. 向量/全文工程调优
- `vector.store.batch-size` 默认 10 偏小，建议 50~100（质量无关，纯吞吐）；
- PgVector 为 `embedding_*` 表显式建 HNSW/IVFFlat 索引并参数化（`m`、`ef_construction`），大库召回延迟与 recall 均受益；
- MongoDB 全文索引评估中文混合英文场景召回，必要时自定义权重字段。

---

## 四、兼容性与落地注意

1. **全部新能力开关化**：`KnowledgeSetting` / `application.yml` 控制，默认行为不变，逐项灰度。
2. **分数量纲变更的影响**：RRF 后相似度分值语义变化，需在文档与前端提示中说明；已有应用的 `similarity` 阈值建议重新标定（可结合评估集给出推荐值）。
3. **索引重建**：方案 5/6/7 改变嵌入输入，需对存量知识库执行重建索引（复用现有 reindex 能力），建议提供"一键重建"入口并支持后台异步。
4. **成本控制**：重排与查询扩展都会增加模型调用，默认仅对混合检索 + 高 topN 场景启用重排。

## 五、实施顺序与预期收益

| 顺序 | 措施 | 主要改动文件 | 预期收益 | 工作量 |
| :--- | :--- | :--- | :--- | :--- |
| 1 | 管线内置 Rerank + 超采样 | `KnowledgeSetting`、新增 `RerankStep`、`ChatSimpleServiceImpl`、`SearchDatasetStep` | 答案相关性显著提升（最大单点） | 中 |
| 2 | RRF 融合 + 阈值修正 | `CompositeStoreImpl`、`FullTextStoreImpl` | hybrid 模式稳定优于单路 | 小 |
| 3 | Prompt 强化 + 截断优化 | `RagContentInjector` | 幻觉下降、token 成本下降 | 小 |
| 4 | 命中直答对齐 | `SearchDatasetStep`、`GenerateHumanMessageStep` | 高频问题延迟与成本下降 | 小 |
| 5 | 分块重叠 | `TextSplitter`、`SentenceSplitter` | 边界召回提升 | 小 |
| 6 | 上下文增强嵌入 | `DocumentSplitServiceImpl`、`ParagraphIndexBatcher`、实体/表 | 向量区分度提升，全链路受益 | 中 |
| 7 | 表格/标题路径分块增强 | `DocumentSplitServiceImpl` | 表格类文档问答提升 | 中 |
| 8 | 按需路由恢复 + 模式自适应 | `SearchDatasetStep`、`DataRetriever` | 多库场景精度提升 | 中 |
| 9 | 多查询扩展 / HyDE | `SearchDatasetStep` | 长尾 query 召回提升 | 中 |
| 10 | 评估与观测体系 | 新增评估模块、`getDetails` 扩展 | 优化可度量、可回归 | 中 |
| 11 | 向量索引/批量工程调优 | `PgVectorEmbeddingStoreImpl`、配置 | 延迟与吞吐改善 | 小 |

> 建议执行节奏：先做 1~4（P0），用 10 的评估基线验证收益，再推进 5~8（P1，需重建索引），最后按业务需要选做 9~11。
