# MaxKB4j RAG 分段与检索能力提升改进方案（零接口变更版）

> **核心约束：所有提升在不改变任何对外接口与参数的前提下实现。**
> 用户可见的 API、配置项、页面行为保持完全不变；提升全部来自内部算法、索引组织
> 与工程实现的优化。基于当前代码（索引、检索、注入全链路）盘点编写，
> 所有引用均为仓库内真实代码位置，可直接定位。
>
> **任务清单使用方式**：第三节按阶段组织全部 13 项优化任务，每个任务含唯一编号、
> checkbox 子任务（`- [ ]` 待办 / `- [x]` 已完成）、验收标准与回退开关；
> 落地一项勾选一项，配合 3.0 任务总览表跟踪整体进度。

---

## 一、约束边界定义

| 类别 | 内容 | 是否允许变更 |
|:-----|:-----|:-----------|
| 对外接口 | HTTP 端点、请求/响应参数与结构（含 hit_test、文档分段预览接口的 `patterns/limit/withFilter` 入参） | ❌ 不变 |
| 用户配置 | `KnowledgeSetting` 字段集与语义（onDemandEnable/topN/maxParagraphCharNumber/searchMode/similarity/rerank* 等）、`searchMode` 枚举取值 | ❌ 不变 |
| 前端行为 | 段落管理页展示的分段结果、命中测试交互、对话引用展示 | ❌ 不变（details 结构仅允许**增量**添加字段） |
| 内部实现 | 内部类/内部 DTO（`SearchRequest` 等）、算法与排序策略、索引内容的组织方式 | ✅ 允许 |
| 部署配置 | `application.yml` 部署级配置项及默认值（`vector.store.*`、`knowledge.split.*`、`knowledge.search.*`） | ✅ 允许（新增配置均有安全默认值，不配置即维持现状或直接受益） |
| 数据结构 | 数据库内部辅助表/列（本方案**尽量规避**；唯一涉及新增辅助表的是任务 1 的 `rag_evaluation` 评估表，纯内部运维用途、对外无感；任务 7 子块化**不动**数据库——子块只存在于索引层） | ⚠️ 谨慎允许 |

> 判定原则：升级部署后，用户不修改任何应用配置、不学习任何新概念，检索与分段效果直接变好；
> 需要调优的场合仅通过部署级 yml（运维属性），不触及产品接口。

---

## 二、当前基线（第一期已落地回顾）

以下能力已在代码中实现，构成本期优化的起点：

| 能力 | 实现位置 |
|:-----|:--------|
| 应用管线 Rerank + 3 倍召回超采样 | `application/pipeline/step/rerankstep/impl/RerankStep.java`、`searchdatasetstep/impl/SearchDatasetStep.java` |
| 混合检索 RRF 融合（k=60，名次融合不依赖分值量纲）；minScore 阈值过滤在 [0,1] 归一化空间内比较 | `store/impl/CompositeStoreImpl.java`、`FullTextStoreImpl.java`、`PgVectorEmbeddingStoreImpl.normalizeScore` |
| 段落路 + 问题路双路召回，按「最高分优先、总分次之」去重排序 | `retriever/SearchOrchestrator.java`（`recall-multiplier:3`） |
| 分块重叠（句子级回填，`overlap-percent:10`）、长表格按行分组 + 表头复制 | `util/TextSplitter.java`、`DocumentSplitServiceImpl.splitTable` |
| 标题路径切分（heading stack 维护 H1~H6） | `DocumentSplitServiceImpl.splitByHeadings` |
| 上下文增强嵌入（`文档名 \| 标题路径\n标题+正文` 前缀） | `service/ParagraphIndexBatchExecutor.buildEmbeddingText` |
| 多轮改写 + 原文/改写并行召回融合 | `ResetProblemStep` + `SearchDatasetStep.retrieval` |
| Prompt 引用编号、紧凑 JSON、预算级截断 | `core/support/RagContentInjector` |
| 命中直答对齐、多模态图片嵌入（SSRF 加固） | `AbsChatStep.java:82`、`PgVectorEmbeddingStoreImpl` |

> **盘点中发现的既有量纲问题（任务 6 顺手修复）**：`PgVectorEmbeddingStoreImpl.toTextChunkVOs`
> 输出反归一化余弦分 `[-1,1]`，`FullTextStoreImpl` 输出 `[0,1]` 归一化分——RRF 名次融合本身
> 不受影响，但融合后附着到 `TextChunkVO` 的"两路最高分"在两路上语义不一致，下游 similarity
> 阈值比较与前端展示分数随之失真。统一量纲工作量极小，随任务 6 一并落地与验证。

---

## 三、优化任务清单

> 分期原则：P0 不动索引（部署即生效）；P1 改变索引写入内容（重建索引后生效，段落表/接口/前端全程无感）；P2 进阶选做。
> **任务 1 先行**：先有度量，后续每一项都有基线可验证。

### 3.0 任务总览

| # | 任务 | 对应问题 | 阶段 | 工作量 | 重建索引 | 预期收益 |
|:--|:-----|:--------|:----|:------|:-------|:--------|
| 1 | 评估与观测体系（含黄金集标注，与开发并行） | R8、R9 | P0 | 中 | 否 | 优化可度量 |
| 2 | embedding 复用 + 结果缓存 | R1、R11 | P0 | 小 | 否 | 检索延迟/成本显著下降 |
| 3 | 命中测试对齐真实链路 | R7 | P0 | 小 | 否 | 调参如实反映效果 |
| 4 | 命中邻块扩展注入 | R4 | P0 | 小 | 否 | 答案上下文更完整 |
| 5 | MMR 多样性去重 | R3 | P0 | 小 | 否 | topN 覆盖面提升 |
| 6 | blend 模式内部自适应 + 量纲统一 | R2 | P0 | 小 | 否 | 关键词/型号查询召回提升 |
| 7 | **索引层子块化（核心，含召回配额联动放大）** | I1 | P1 | 中偏高 | **是** | 大块库检索精度大幅提升 |
| 8 | 代码块/列表原子性保护 | I3、I4 | P1 | 小 | **是** | 代码类文档可读召回 |
| 9 | token 估算保护 | I2 | P1 | 小 | **是** | 英文库向量质量稳定 |
| 10 | 查询增强多路化 | R5 | P1 | 中 | 否 | 多跳/长尾问题召回提升 |
| 11 | Contextual Retrieval | I5 | P2 | 中 | **是** | 表格/短块区分度提升 |
| 12 | 语义分段 | I6 | P2 | 中 | **是** | 语义完整块 |
| 13 | 问题路权重 + 工程调优 | R6、R10 | P2 | 小 | 否 | 融合可调、吞吐提升 |

---

### P0 阶段：部署即生效（1~2 周，不重建索引）

#### 任务 1：评估与观测体系

**问题**：R8 无评估体系（检索改动无 Recall@k / MRR / nDCG 基线）；R9 观测不足（details 只有总耗时与最终列表）
**工作量**：中｜**重建索引**：否｜**改动范围**：新增内部评估模块、`SearchOrchestrator`、`getDetails`

- [ ] **1.1 黄金集标注（前置任务，与开发并行启动）**——评估效力完全取决于标注集质量。每个重点库标注 50~100 条「问题 → 期望命中段落」，覆盖短关键词/型号、自然语言长句、表格、代码等场景；标注需要业务方投入，是本项最大的人工成本。
- [ ] 1.2 新增内部评估模块（`rag_evaluation` 辅助表：question、expected_paragraph_ids、knowledge_id），批量回放 hit_test 链路（含任务 3 对齐后的 rerank），输出 Recall@k / MRR / nDCG；指标实现 ≤ 200 行，不引入外部框架。
- [ ] 1.3 观测埋点：`SearchOrchestrator.search` 记录段落路/问题路召回数、融合前后 topK 分数、分段耗时；`RerankStep` 记录候选数 → 保留数与降级标记。
- [ ] 1.4 `getDetails()` 增量透出观测字段（details 仅加字段，旧前端不受影响）。

**验收**：任一检索改动可跑评估集对比；线上 details 能回答"这次为什么没召回"。
**回退**：纯增量模块，评估表停用不影响任何链路。

#### 任务 2：query embedding 复用与结果缓存

**问题**：R1 query embedding 重复计算（双路召回 + 原文/改写并行，单次对话最多 4 次 embedding 调用）；R11 重复查询无缓存（FAQ 场景相同 query 反复全链路检索）
**工作量**：小｜**重建索引**：否｜**改动范围**：`SearchRequest`、`PgVectorEmbeddingStoreImpl`

- [ ] 2.1 `SearchRequest`（内部类）增加请求级 `queryEmbedding`；`PgVectorEmbeddingStoreImpl.searchBySource` 优先复用，miss 才调用——单次对话 embedding 调用从最多 4 次降为 2 次。
- [ ] 2.2 进程内 LRU 缓存（key = 嵌入模型 + query hash；容量/TTL yml 可配、默认保守），高频 FAQ 降为 0 次。
- [ ] 2.3 检索结果缓存：key = query + knowledgeIds + 关键设置摘要；短 TTL（如 60s）；默认容量 0（关闭）或小容量灰度。
- [ ] 2.4 缓存失效：写入口分散在 `DocumentIndexListener`/`ParagraphIndexListener` 等多个监听器——失效钩子挂在 store 写操作（`upsert`/`deleteBy*`）一层最稳妥，一处挂载覆盖全部入口；段落 isActive 状态翻转同样纳入（见第五节风险 4）。

**验收**：重复 query 检索延迟下降；段落删除/停用后缓存不返回脏数据。
**回退**：yml 容量/TTL 配 0 即全部关闭，回到无缓存现状。

#### 任务 3：命中测试对齐真实链路

**问题**：R7 命中测试与真实链路脱节（hit_test 不含 rerank 与改写召回）
**工作量**：小｜**重建索引**：否｜**改动范围**：`ParagraphRetriever`、抽 `RerankExecutor` 公共组件

- [ ] 3.1 `RerankStep` 打分逻辑抽为公共组件（`RerankExecutor`），管线与 hit_test 共用一份实现。
- [ ] 3.2 `KnowledgeController.hitTest` 内部读取该库已有设置：`rerankEnable=true 且 rerankModelId 非空` 时自动对命中结果执行 rerank——**不加任何请求参数**，用户在库里开了 rerank，命中测试即如实反映。
- [ ] 3.3 响应结构增量加 `rerankApplied` 标记与前后分数（旧前端忽略新字段，无感）。

**验收**：库内开启 rerank 后，hit_test 分数排序与对话管线检索一致。
**回退**：无需开关——行为由库内已有 rerank 设置驱动，库未开启时与现状完全一致。

#### 任务 4：命中邻块扩展注入

**问题**：R4 命中后无邻块补全（`position` 字段已存在但未用于上下文回补）
**工作量**：小｜**重建索引**：否｜**改动范围**：`SearchDatasetStep` 内部步骤

- [ ] 4.1 `SearchDatasetStep` 在 rerank 后、注入前增加内部扩展步骤：按命中块 `documentId + position` 回补 ±1 相邻块；扩展块只注入上下文、不参与排序与引用计分；总预算受已有 `maxParagraphCharNumber` 硬顶。`RagContentInjector` 已按文档分组 + position 排序，邻块自然拼接。
- [ ] 4.2 **扩展块不得混入 `paragraphList`**：`SearchDatasetStep.getDetails` 把 paragraphList 原样透出前端当命中列表展示，扩展块混入会改变命中数与引用展示（违反零变更承诺，见第五节风险 7）。实现上以独立 context key（如 `neighborBlocks`）保存，仅注入阶段合并，标记不参与引用计分。
- [ ] 4.3 yml 开关（默认开，评估集不达标可关）。

**验收**：评估集答案完整性提升；前端命中列表条数与引用数与开关前一致。
**回退**：yml 开关一键关闭，回到无邻块现状。

#### 任务 5：MMR 多样性去重

**问题**：R3 无多样性控制（同文档相邻块集体入选挤占 topN）
**工作量**：小｜**重建索引**：否｜**改动范围**：`SearchOrchestrator`、`TextChunkVO`、两个 store 的结果映射

- [ ] 5.1 前置小改造：`TextChunkVO` 目前只有 sourceId + score，无 documentId——同文档惩罚需在 `TextChunkVO` 增量加 documentId 字段，并由 `PgVectorEmbeddingStoreImpl.toTextChunkVOs`（metadata 已有该字段）与 `FullTextStoreImpl` 聚合投影透传；改动小但触及两个 store 的结果映射。
- [ ] 5.2 `SearchOrchestrator.dedupAndRank` 截断前增加 MMR：`argmax [λ·rel(d) − (1−λ)·max_sim(d, S)]`，λ 默认 0.7（yml 可调）；零向量成本版本用「同文档连续入选递减」近似相似度惩罚。
- [ ] 5.3 仅在 topN 内计算，开销可忽略；与子块化（任务 7）组合时按父段落去重已消解大部分重复，MMR 作为第二道控制。

**验收**：topN 中同文档连续块占比下降，Recall@topN 不降。
**回退**：λ=1 退化为纯相关度排序（等价现状），或 yml disable。

#### 任务 6：blend 模式内部自适应 + 量纲统一

**问题**：R2 检索模式静态（短关键词/型号/错误码查询在向量路几乎无效，反之长句全文路弱）
**工作量**：小｜**重建索引**：否｜**改动范围**：`CompositeStoreImpl`

- [ ] 6.1 `CompositeStoreImpl` 融合改为 weighted RRF：`score(d) = Σ w_route/(k + rank_route(d))`。
- [ ] 6.2 权重由 query 特征决定（内部规则，零模型调用，jieba `Tokenizer` 现成可用）：分词后词数 ≤ 4 或含型号/错误码特征（连续 ≥4 位字母数字）→ 全文路 w 提升；自然语言长句 → 向量路 w 提升；默认等权（即现状）。
- [ ] 6.3 单路模式（embed/keywords）是用户的显式选择，**不做任何干预**——自适应只发生在 blend 内部。
- [ ] 6.4 顺手统一分数量纲（修复第二节末尾指出的既有问题）：`mergeByRrf` 附着的"两路最高分"统一在 [0,1] 归一化空间取 max——RRF 名次不变，仅修复展示分与 similarity 阈值过滤的语义一致性。

**验收**：型号/错误码类 query 在 blend 模式下 Recall 提升，自然语言长句不回退；评估集前后对比量纲统一无负效应。
**回退**：权重规则 yml disable 后等权融合（即现状）；量纲统一保留（纯正确性修复）。

---

### P1 阶段：索引质量（2~4 周，需重建索引，对外全程无感）

#### 任务 7：索引层子块化 small-to-big（本期最大单项）

**问题**：I1 检索单元 = 注入单元（用户选大块——长 limit/QA 长内容——时向量区分度低，选小块时上下文不足）
**工作量**：中偏高｜**重建索引**：**是**｜**改动范围**：`ParagraphIndexBatchExecutor`、`SearchOrchestrator`（召回配额）、yml

**核心思路：段落表不动、接口不动、前端段落管理看到的分段不变；只在索引写入层细化检索粒度。**

- [ ] 7.1 子块切分与写入：`ParagraphIndexBatchExecutor.createIndexBatch` 写入前，对超过内部阈值（如 1024 字符，yml 可配）的段落按子块尺寸（如 256 字符，yml 可配）切子块；每个子块的 `EmbeddingEntity.sourceId` **仍为父段落 id**，嵌入文本继续带 `文档名 | 标题路径` 前缀（子块粒度更细，前缀增益更大）。
- [ ] **7.2 必须同步解决：store 层召回挤占（本项最关键的配套设计）**。store 层截断——向量路 `maxResults`、`mergeByRrf` 的 `limit(topK)`、全文路 `limit`——全部发生在 `dedupAndRank` 按 sourceId 聚合**之前**：同一父段落的多个子块在截断层互相占位（默认 1024/256 配置下单段最多 4 个），强匹配父段会把其他父段挤出候选，子块化收益被漏召回直接抵消；现有 `recall-multiplier:3` 小于子块数上限，已现挤占。解法（二选一或组合）：
  - **召回配额联动放大**（默认方案）：`SearchOrchestrator` 将 store 层 topK 再乘以 `ceil(父块阈值 / 子块尺寸)`（子块化开启时自动推导，yml 可覆盖）；
  - store 层 sourceId 预去重：截断前按 sourceId 保留最高名次分（改动更大，备选）。
- [ ] 7.3 **成本同步评估**：索引条目数最多放大 `父块阈值 / 子块尺寸` 倍（默认 ×4）——embedding 调用、pgvector 存储量、MongoDB 文档量同比增长，重建索引窗口时长与费用按库预估；仅超阈值段落切子块，超阈值占比低的库实际放大远小于理论上限。
- [ ] 7.4 零改造兼容性逐链路核对（全部已对照现有代码验证成立）：
  - [ ] `SearchOrchestrator.dedupAndRank` 按 sourceId 聚合取最高分/总分——多子块命中天然聚合回父段落，现有算法直接复用；
  - [ ] `deleteByParagraphIds` 按 sourceId 过滤——多子块一并删除，删除链路不变；
  - [ ] 全文库（MongoDB）子块文档同 sourceId 写入，检索命中后同样聚合，同构；
  - [ ] `ParagraphRetriever` 按 sourceId 回查段落表返回父段落全文——返回结构不变；
  - [ ] 段落管理页、命中测试、对话引用全部展示父段落——前端零变更。
- [ ] 7.5 可选增强：rerank 输入改用命中子块文本（`ParagraphRagVO` 内部透传字段标记 `@JsonIgnore`，不进入前端响应），短文本精排更敏感。
- [ ] 7.6 存量库灰度：执行"重建索引"即生成子块（复用现有 reindex 链路）；不重建则维持现状，平滑灰度。

**收益场景**：用户为保证上下文完整选了大 limit（如 2048）或导入长 QA 内容时，检索粒度自动细化到子块级（区分度高），注入仍回完整父段落——同一组参数下精度与完整性兼得。
**验收**：大 limit 库 Recall@topN 显著提升；hit_test 结果结构、引用展示与重建前一致。
**回退**：yml 关闭子块化 + 再执行一次重建索引（回退 = 再重建一次）。

#### 任务 8：代码块/列表原子性保护

**问题**：I3 代码块无原子性保护（表格有 `{{TABLE}}` 保护，``` 代码块会被句子切分从中间切碎）；I4 overlap 覆盖不全（`splitTable` 行组间、`lineSplit` 路径无重叠）
**工作量**：小｜**重建索引**：**是**｜**改动范围**：`DocumentSplitServiceImpl.getStringList`、`SentenceSplitter`

- [ ] 8.1 `getStringList` 在 `{{TABLE}}` 检测之外增加代码围栏（``` / `~~~`）检测为 `{{CODE}}` 块：不超长整体原子保留；超长按空行分组切分，保留块首围栏语言标注，组间不带 overlap。
- [ ] 8.2 **先改造快速返回路径**：`getStringList` 开头 `!content.contains("|")` 的快速返回必须调整——纯代码文档（不含表格线 `|`）目前根本走不到围栏检测，围栏检测须置于该快速路径之前或与之合并。
- [ ] 8.3 `SentenceSplitter` 分句前按行首 `- / 数字.` 识别列表行为原子 chunk，不从列表项中间切断。
- [ ] 8.4 `splitTable` 行组间可选回填上一组末行（与已有 `overlap-percent` 复用，默认关，评估集验证后定）。

**验收**：代码类文档分段结果不再被切碎，评估集代码组召回提升。
**回退**：`{{CODE}}` 检测 yml 关闭；8.4 默认关。

#### 任务 9：token 估算保护

**问题**：I2 limit 按字符计数（英文 512 字符 ≈ 130 token，中文 ≈ 512 token，同配置下英文块信息密度仅 1/4）
**工作量**：小｜**重建索引**：**是**｜**改动范围**：新增 `TokenEstimator`

- [ ] 9.1 新增 `TokenEstimator`（中文 1 字 ≈ 1 token、英文 4 字符 ≈ 1 token 的启发式）。
- [ ] 9.2 `limit` 参数语义不变（仍为字符数）；当估算 token 数超过嵌入模型窗口上限时**在 limit 之内提前切分**，避免截断丢内容或向量质量劣化。注意：模型档案目前无窗口元数据，窗口上限需 yml 按模型配置。
- [ ] 9.3 预留接口后续可替换为模型 tokenizer；对中文库行为无变化，对英文库是纯收益。

**验收**：英文库分段不再出现单块超嵌入窗口导致的截断/向量劣化。
**回退**：窗口配置缺省时不提前切分（维持现状）。

#### 任务 10：查询增强多路化

**问题**：R5 查询增强单路（仅 ResetProblemStep 一路改写，多跳/对比问题覆盖不足）
**工作量**：中｜**重建索引**：否｜**改动范围**：`ResetProblemStep`、`SearchDatasetStep`

- [ ] 10.1 `ResetProblemStep` 单次 LLM 调用的 prompt 扩展为一次产出多路结果（改写 + 关键词化 + 多跳问题分解），解析为 ≤3 路 query。
- [ ] 10.2 **兼容性处理**：`paddingProblemText` 目前以单 String 贯穿 `AbsResetProblemStep` → pipeline context → `SearchDatasetStep`，且 `ApplicationChatRecordServiceImpl` 会将其写入聊天记录透出前端——多路化后对外仍须序列化为单字符串（拼接各路或仅保留主改写），多路列表走独立内部 key，避免改变聊天记录字段语义（见第五节风险 7）。
- [ ] 10.3 `SearchDatasetStep.retrieval` 的并行合并骨架不动，futureList 从 2 路扩到 N 路（上限 yml 配置）；各路 recallN 相应下调避免融合候选爆炸；总 LLM 调用次数仍为 **1 次**，流式首包延迟不回退。
- [ ] 10.4 yml 总开关（默认关）；开启前用评估集验证收益与延迟。

**验收**：多跳/对比类问题 Recall 提升；流式首包延迟不回退；聊天记录展示不变。
**回退**：yml 总开关关闭回到单路改写。

---

### P2 阶段：进阶能力（长期选做）

#### 任务 11：Contextual Retrieval

**问题**：I5 无块级语义补全（表格块、无标题短块向量区分度低，静态前缀已做）
**工作量**：中｜**重建索引**：**是**｜**改动范围**：`ParagraphIndexBatchExecutor`、yml

- [ ] 11.1 索引期用小模型（yml 配置模型 ID，可与 rerank 模型同供应商）为每块生成 1~2 句定位摘要，前缀到嵌入文本。
- [ ] 11.2 成本与工程：索引期每块一次 LLM 调用，大库重建索引时长与费用显著增加——需批量并发与失败降级设计（单块摘要失败退化为无前缀，不阻断索引）。
- [ ] 11.3 yml 开关默认关；存量库重建时按需开启。收益集中点：表格块、无标题短块（评估集单列一组验证）。

**验收**：评估集表格/短块组区分度提升，其余组不回退。
**回退**：yml 关闭 + 重建索引。

#### 任务 12：语义分段

**问题**：I6 无语义分段（字符预算 + 硬边界合并，话题转折处易切断）
**工作量**：中｜**重建索引**：**是**｜**改动范围**：`SentenceSplitter` 扩展

- [ ] 12.1 相邻句 embedding 相似度低谷处优先断块，字符/token 预算兜底；与任务 7 组合时语义断点决定子块边界，父子结构保证注入完整性。
- [ ] 12.2 **与分段预览接口的冲突规避**：分段预览（`patterns/limit/withFilter` 链路）与索引共用 split 服务，split 链路引入 embedding 依赖会让同步预览接口变慢并产生嵌入费用——语义分段仅索引期启用（yml 按场景区分），预览请求一律走字符预算兜底路径，行为与现状一致。
- [ ] 12.3 索引期每句一次 embedding（与段落向量化同模型、批处理），成本可控。

**验收**：评估集语义完整块召回更稳；分段预览接口延迟与费用不变。
**回退**：yml 关闭回到字符预算分段。

#### 任务 13：问题路权重与全文检索调优

**问题**：R6 问题路分数无权重（与段落路直接竞争排序，无法按标注质量调权）；R10 向量库工程参数（batch-size 默认 10；HNSW 参数未暴露）
**工作量**：小｜**重建索引**：否（HNSW DDL 对存量表补建即可）｜**改动范围**：`SearchOrchestrator`、`PgVectorEmbeddingStoreImpl`、yml

- [ ] 13.1 yml `knowledge.search.problem-route-weight`（默认 1.0 = 现状）：`mapProblemsToParagraphs` 分数乘权后进入融合；结合 `GenerateProblemListener` 自动生成问题的质量在评估集上标定。
- [ ] 13.2 `vector.store.batch-size` 默认 10 → 50~100。
- [ ] 13.3 HNSW 参数管理（`m/ef_construction/ef_search` yml 暴露）。实施细节：langchain4j `PgVectorEmbeddingStore` 不创建 HNSW 索引，需在维度表懒创建后补幂等 DDL（`CREATE INDEX IF NOT EXISTS ... USING hnsw (embedding vector_cosine_ops)`）；`ef_search` 是会话级 GUC，经 Hikari 连接池须以 connection-init-sql 或库级 `ALTER DATABASE ... SET` 生效，单次 SET 不可靠。
- [ ] 13.4 MongoDB 全文索引评估中英混合场景（jieba 预分词 + 默认 stop words 的交互），必要时引入内部同义词表扩展 query 分词（词表 yml/内部表，非产品接口）。

**验收**：吞吐提升（批量写入/检索延迟）；问题路权重调优在评估集上有正向或零效应。
**回退**：权重默认 1.0、batch-size/HNSW 配置缺省维持现状。

---

## 四、与上一版方案的差异：被约束排除项及替代路径

| 上一版方案项 | 约束冲突点 | 本版替代路径 |
|:-----------|:---------|:-----------|
| `KnowledgeSetting` 新增 `chunkSize/chunkOverlapTokens` | 用户配置字段变更 | 参数不动；任务 7 索引层子块化用内部阈值（yml）实现同等的"检索粒度与注入粒度解耦"，且比按库配置更无感 |
| `KnowledgeSetting.diversityEnable` | 用户配置字段变更 | 任务 5 MMR 默认开启 + λ 走 yml（零成本算法，无需用户决策） |
| `KnowledgeSetting.problemRouteWeight` | 用户配置字段变更 | 任务 13 权重走 yml 部署级配置，默认 1.0 行为不变 |
| `searchMode` 新增 `auto` 枚举值 | 参数取值语义变更 | 任务 6 blend 模式内部 weighted RRF 自适应——"auto 的收益"装进既有 blend 值里 |
| hit_test 请求透传 rerank 参数 | 接口参数变更 | 任务 3 hit_test 内部读库设置自动应用 rerank（设置字段本就存在） |
| `ParagraphEntity` 加 `parent_id` 列 + 子块入库 | 段落管理页会显示子块，前端行为变更 | 任务 7 子块只存在于索引层（sourceId=父段落 id），段落表零变更 |

> 结论：上一版全部能力目标在本版均有对应实现路径，且用户侧感知仅为"效果变好"，
> 不存在需要用户重新学习或重新配置的变更。

---

## 五、兼容性与风险

1. **对外零变更验证**：每项落地后跑一次接口契约回归（现有 controller 测试 + 前端联调冒烟），
   确认请求/响应结构 diff 为空（details 增量字段除外）。
2. **索引重建窗口**：任务 7~9、11、12 改变索引写入内容，存量库执行"重建索引"后生效
   （复用 reindex 链路）；不重建则完全维持现状——天然支持逐库灰度与回退（回退 = 再重建一次）。
3. **默认行为变更点明示**（虽然接口不变，效果会有可感知变化，需在发布说明中告知）：
   - 任务 5 MMR 默认开启：topN 的构成更多样，个别依赖"topN 全是同一段落邻块"的场景答案组织可能变化；
   - 任务 6 blend 自适应：仅权重微调，RRF 名次主导，排序变化幅度有限；
   - 任务 7 子块化：大 limit 库的召回命中率上升是最主要的可感知收益。
   以上各项均提供 yml 关闭开关，一键回退旧行为。
4. **缓存一致性**（任务 2）：结果缓存 TTL 短 + 写路径主动失效双保险；段落状态翻转
   （isActive）也纳入失效触发，避免命中测试与对话拿到已停用段落。
5. **评估先行**：任务 1 是所有后续项的前置——任何检索改动必须附评估集 before/after 数据；
   黄金集标注（业务投入）与模块开发并行启动，标注质量决定全部后续验收的效力。
6. **store 层召回挤占**（任务 7）：子块在 store 截断层互相占位，召回配额必须随子块数联动
   放大（详见任务 7 子任务 7.2）——这是子块化从"方向正确"到"落地不翻车"的关键配套设计。
7. **内部字段的对外语义污染**：任务 4 邻块扩展块不得混入 `paragraphList` 透出前端
   （getDetails 原样透出该列表）；任务 10 多路查询不得改变聊天记录 `paddingProblemText`
   字段语义——两处均以独立内部 key 隔离，详见对应任务卡。
8. **索引成本放大**（任务 7 / 11 / 12）：子块化索引条目最多 ×4、Contextual Retrieval
   每块一次 LLM 调用、语义分段每句一次 embedding——重建索引的时长与费用按库预估，
   全部默认关或由阈值联动推导。

---

## 六、执行节奏

> **任务 1 → 6（P0 全部，两周内部署即收益）→ 7/8/9（同一个重建索引窗口一并完成）
> → 10 → 按业务需要选做 11~13。**
> 每项落地跑评估集对比，不达预期即用 yml 开关回退；
> 全程无任何接口、参数、前端变更。
