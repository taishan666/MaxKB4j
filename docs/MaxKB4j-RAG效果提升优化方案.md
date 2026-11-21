# MaxKB4j RAG 分段与检索能力提升改进方案（零接口变更版）

> **核心约束：所有提升在不改变任何对外接口与参数的前提下实现。**
> 用户可见的 API、配置项、页面行为保持完全不变；提升全部来自内部算法、索引组织
> 与工程实现的优化。基于当前代码（索引、检索、注入全链路）盘点编写，
> 所有引用均为仓库内真实代码位置，可直接定位。

---

## 一、约束边界定义

| 类别 | 内容 | 是否允许变更 |
|:-----|:-----|:-----------|
| 对外接口 | HTTP 端点、请求/响应参数与结构（含 hit_test、文档分段预览接口的 `patterns/limit/withFilter` 入参） | ❌ 不变 |
| 用户配置 | `KnowledgeSetting` 字段集与语义（onDemandEnable/topN/maxParagraphCharNumber/searchMode/similarity/rerank* 等）、`searchMode` 枚举取值 | ❌ 不变 |
| 前端行为 | 段落管理页展示的分段结果、命中测试交互、对话引用展示 | ❌ 不变（details 结构仅允许**增量**添加字段） |
| 内部实现 | 内部类/内部 DTO（`SearchRequest` 等）、算法与排序策略、索引内容的组织方式 | ✅ 允许 |
| 部署配置 | `application.yml` 部署级配置项及默认值（`vector.store.*`、`knowledge.split.*`、`knowledge.search.*`） | ✅ 允许（新增配置均有安全默认值，不配置即维持现状或直接受益） |
| 数据结构 | 数据库内部辅助表/列（本方案**尽量规避**；唯一例外见 P1-1，且对外无感） | ⚠️ 谨慎允许 |

> 判定原则：升级部署后，用户不修改任何应用配置、不学习任何新概念，检索与分段效果直接变好；
> 需要调优的场合仅通过部署级 yml（运维属性），不触及产品接口。

---

## 二、当前基线（第一期已落地回顾）

以下能力已在代码中实现，构成本期优化的起点：

| 能力 | 实现位置 |
|:-----|:--------|
| 应用管线 Rerank + 3 倍召回超采样 | `application/pipeline/step/rerankstep/impl/RerankStep.java`、`searchdatasetstep/impl/SearchDatasetStep.java` |
| 混合检索 RRF 融合（k=60）+ 两路分数量纲统一 [0,1] | `store/impl/CompositeStoreImpl.java`、`FullTextStoreImpl.java`、`PgVectorEmbeddingStoreImpl.normalizeScore` |
| 段落路 + 问题路双路召回，按「最高分优先、总分次之」去重排序 | `retriever/SearchOrchestrator.java`（`recall-multiplier:3`） |
| 分块重叠（句子级回填，`overlap-percent:10`）、长表格按行分组 + 表头复制 | `util/TextSplitter.java`、`DocumentSplitServiceImpl.splitTable` |
| 标题路径切分（heading stack 维护 H1~H6） | `DocumentSplitServiceImpl.splitByHeadings` |
| 上下文增强嵌入（`文档名 \| 标题路径\n标题+正文` 前缀） | `service/ParagraphIndexBatchExecutor.buildEmbeddingText` |
| 多轮改写 + 原文/改写并行召回融合 | `ResetProblemStep` + `SearchDatasetStep.retrieval` |
| Prompt 引用编号、紧凑 JSON、预算级截断 | `core/support/RagContentInjector` |
| 命中直答对齐、多模态图片嵌入（SSRF 加固） | `AbsChatStep.java:82`、`PgVectorEmbeddingStoreImpl` |

---

## 三、零接口改进清单

### 3.1 分段侧

| #   | 问题 | 零接口改进方向 | 变更范围 |
|:----|:-----|:-------------|:--------|
| I1 | 检索单元 = 注入单元：用户选大块（长 limit/QA 长内容）时向量区分度低，选小块时上下文不足 | **索引层子块化（small-to-big）**：段落表与接口不动，索引写入时对超阈值段落切子块，子块 `sourceId` = 父段落 id | 纯索引层，重建索引生效 |
| I2 | limit 按字符计数：英文 512 字符 ≈ 130 token，中文 ≈ 512 token，同配置下英文块信息密度仅 1/4 | 内部 token 估算保护：估算超嵌入模型窗口时**提前**切分（limit 语义不变，只是更保守地执行） | 纯算法 |
| I3 | 代码块无原子性保护：表格有 `{{TABLE}}` 保护，```` ``` ```` 代码块会被句子切分从中间切碎 | `getStringList` 增加代码围栏检测，超长代码块按空行/函数边界分组 | 纯算法 |
| I4 | overlap 覆盖不全：`splitTable` 行组间、`lineSplit` 路径无重叠 | 表格行组间可选回填上一组末行；与已有 `overlap-percent` 复用 | 纯算法 |
| I5 | 无块级语义补全：表格块、无标题短块向量区分度低（静态前缀已做） | Contextual Retrieval：索引期生成 1~2 句块定位摘要作前缀（yml 开关，默认关） | 索引层 + yml |
| I6 | 无语义分段：字符预算 + 硬边界合并，话题转折处易切断 | 语义断点优先、预算兜底（`SentenceSplitter` 作为 fallback） | 纯算法（索引期 embedding 成本） |

### 3.2 检索侧

| #   | 问题 | 零接口改进方向 | 变更范围 |
|:----|:-----|:-------------|:--------|
| R1 | query embedding 重复计算：双路召回 + 原文/改写并行，单次对话最多 4 次 embedding 调用 | 请求内复用 + 进程内 LRU 缓存 | 纯内部 |
| R2 | 检索模式静态：短关键词/型号/错误码查询在向量路几乎无效，反之长句全文路弱 | **blend 模式内部加权自适应**：weighted RRF 按 query 特征动态调两路权重与召回配额；embed/keywords 单路选择尊重用户设置不动 | 纯算法 |
| R3 | 无多样性控制：同文档相邻块集体入选挤占 topN | MMR（固定 λ=0.7，yml 可调）+ 同文档连续入选递减惩罚 | 纯算法 |
| R4 | 命中后无邻块补全：`position` 字段已存在但未用于上下文回补 | rerank 后按 `documentId + position` 回补 ±1 邻块注入（预算复用已有 `maxParagraphCharNumber`） | 纯内部步骤 |
| R5 | 查询增强单路：仅 ResetProblemStep 一路改写，多跳/对比问题覆盖不足 | **单次 LLM 调用产出多路查询**（改写 + 关键词化 + 分解），复用 `SearchDatasetStep.retrieval` 已有 N 路并行骨架；开关走 yml，流式首包延迟不回退 | 内部 + yml |
| R6 | 问题路分数无权重：与段落路直接竞争排序，无法按标注质量调权 | yml 内部权重（默认 1.0，行为不变），`mapProblemsToParagraphs` 乘权后融合 | 内部 + yml |
| R7 | 命中测试与真实链路脱节：hit_test 不含 rerank 与改写召回 | hit_test **内部自动应用库设置里已有的** `rerankEnable/rerankModelId`（无需新参数）；RerankStep 核心逻辑抽公共组件复用 | 纯内部 |
| R8 | 无评估体系：检索改动无 Recall@k / MRR / nDCG 基线 | 内部评估模块：黄金集回放 hit_test 链路（含对齐后的 rerank），输出指标报告 | 新增内部模块 |
| R9 | 观测不足：details 只有总耗时与最终列表 | 各步记录分路召回数、融合前后分数、分段耗时；details **增量**加字段（旧前端无感） | 纯内部 |
| R10 | 向量库工程参数：batch-size 默认 10；HNSW 参数未暴露 | 默认值调优 + yml 暴露 `m/ef_construction/ef_search`（建表 SQL 层管理） | yml |
| R11 | 重复查询无缓存：FAQ 场景相同 query 反复全链路检索 | query+库集合 级 LRU 结果缓存（短 TTL，命中即跳过检索；写索引时按库失效） | 纯内部 |

---

## 四、分期实施方案

> 分期原则：P0 不动索引（部署即生效）；P1 改变索引写入内容（重建索引后生效，段落表/接口/前端全程无感）；P2 进阶选做。
> **P0-1 先行**：先有度量，后续每一项都有基线可验证。

### P0 部署即生效（1~2 周，不重建索引）

#### P0-1 评估与观测体系（对应 R8、R9）

- 新增内部评估模块（`rag_evaluation` 辅助表：question、expected_paragraph_ids、knowledge_id），
  批量回放 hit_test 链路，输出 Recall@k / MRR / nDCG；指标实现 ≤ 200 行，不引入外部框架。
- 观测：`SearchOrchestrator.search` 记录段落路/问题路召回数、融合前后 topK 分数、分段耗时；
  `RerankStep` 记录候选数→保留数与降级标记；`getDetails()` 增量透出（旧前端不受影响）。
- 验收：任一检索改动可跑评估集对比；线上 details 能回答"这次为什么没召回"。

#### P0-2 query embedding 复用与结果缓存（对应 R1、R11）

- `SearchRequest`（内部类）增加请求级 `queryEmbedding`；`PgVectorEmbeddingStoreImpl.searchBySource`
  优先复用，miss 才调用——单次对话 embedding 调用从最多 4 次降为 2 次。
- 进程内 LRU（key = 嵌入模型 + query hash；容量/TTL yml 可配、默认保守），高频 FAQ 降为 0 次。
- 检索结果缓存：key = query + knowledgeIds + 关键设置摘要；短 TTL（如 60s）；
  段落/文档增删改时按 knowledgeId 主动失效。默认容量 0（关闭）或小容量灰度。

#### P0-3 命中测试对齐真实链路（对应 R7）

- `KnowledgeController.hitTest` 内部读取该库已有设置：`rerankEnable=true 且 rerankModelId 非空`
  时自动对命中结果执行 rerank——**不加任何请求参数**，用户在库里开了 rerank，命中测试即如实反映。
- RerankStep 打分逻辑抽为公共组件（`RerankExecutor`），管线与 hit_test 共用一份实现。
- 响应结构增量加 `rerankApplied` 标记与前后分数（旧前端忽略新字段，无感）。

#### P0-4 命中邻块扩展注入（对应 R4）

- `SearchDatasetStep` 在 rerank 后、注入前增加内部扩展步骤：按命中块 `documentId + position`
  回补 ±1 相邻块；扩展块只注入上下文、不参与排序与引用计分；总预算受已有
  `maxParagraphCharNumber` 硬顶。`RagContentInjector` 已按文档分组 + position 排序，邻块自然拼接。
- yml 开关（默认开，评估集不达标可关）。

#### P0-5 MMR 多样性去重（对应 R3）

- `SearchOrchestrator.dedupAndRank` 截断前增加 MMR：`argmax [λ·rel(d) − (1−λ)·max_sim(d, S)]`，
  λ 默认 0.7（yml 可调）；零向量成本版本用「同文档连续入选递减」近似相似度惩罚。
- 仅在 topN 内计算，开销可忽略；与子块化（P1-1）组合时按父段落去重已消解大部分重复，MMR 作为第二道控制。

#### P0-6 blend 模式内部自适应（对应 R2）

- `CompositeStoreImpl` 融合改为 weighted RRF：`score(d) = Σ w_route/(k + rank_route(d))`。
  权重由 query 特征决定（内部规则，零模型调用）：分词后词数 ≤ 4 或含型号/错误码特征
  （连续 ≥4 位字母数字）→ 全文路 w 提升；自然语言长句 → 向量路 w 提升；默认等权（即现状）。
- 单路模式（embed/keywords）是用户的显式选择，**不做任何干预**——自适应只发生在 blend 内部。
- 效果等价于"blend 模式自动变聪明"，而 `searchMode` 取值与语义完全不变。

### P1 索引质量（2~4 周，需重建索引，对外全程无感）

#### P1-1 索引层子块化 small-to-big（对应 I1）——本期最大单项

- **核心思路：段落表不动、接口不动、前端段落管理看到的分段不变；只在索引写入层细化检索粒度。**
- `ParagraphIndexBatchExecutor.createIndexBatch` 写入前，对超过内部阈值（如 1024 字符，yml 可配）
  的段落按子块尺寸（如 256 字符，yml 可配）切子块；每个子块的 `EmbeddingEntity.sourceId`
  **仍为父段落 id**，嵌入文本继续带 `文档名 | 标题路径` 前缀（子块粒度更细，前缀增益更大）。
- **零改造兼容性**（逐一核对现有链路）：
  - `SearchOrchestrator.dedupAndRank` 按 sourceId 聚合取最高分/总分——多子块命中天然聚合回父段落，**现有算法直接复用**；
  - `deleteByParagraphIds` 按 sourceId 过滤——多子块一并删除，**删除链路不变**；
  - 全文库（MongoDB）子块文档同 sourceId 写入，检索命中后同样聚合，**同构**；
  - `ParagraphRetriever` 按 sourceId 回查段落表返回父段落全文——**返回结构不变**；
  - 段落管理页、命中测试、对话引用全部展示父段落，**前端零变更**。
- 收益场景：用户为保证上下文完整选了大 limit（如 2048）或导入长 QA 内容时，
  检索粒度自动细化到子块级（区分度高），注入仍回完整父段落——**同一组参数下精度与完整性兼得**。
- 可选增强：rerank 输入改用命中子块文本（`ParagraphRagVO` 内部透传字段标记 `@JsonIgnore`，
  不进入前端响应），短文本精排更敏感。
- 存量库：执行"重建索引"即生成子块（复用现有 reindex 链路）；不重建则维持现状，平滑灰度。

#### P1-2 代码块/列表原子性保护（对应 I3、I4）

- `getStringList` 在 `{{TABLE}}` 检测之外增加代码围栏（```` ``` ```` / `~~~`）检测为 `{{CODE}}` 块：
  不超长整体原子保留；超长按空行分组切分，保留块首围栏语言标注，组间不带 overlap。
- `SentenceSplitter` 分句前按行首 `- / 数字.` 识别列表行为原子 chunk，不从列表项中间切断。
- `splitTable` 行组间可选回填上一组末行（与 `overlap-percent` 复用，默认关，评估集验证后定）。

#### P1-3 token 估算保护（对应 I2）

- 新增 `TokenEstimator`（中文 1 字 ≈ 1 token、英文 4 字符 ≈ 1 token 的启发式），
  `limit` 参数语义不变（仍为字符数）；当估算 token 数超过嵌入模型窗口上限时
  **在 limit 之内提前切分**，避免截断丢内容或向量质量劣化。
- 预留接口后续可替换为模型 tokenizer；对中文库行为无变化，对英文库是纯收益。

#### P1-4 查询增强多路化（对应 R5）

- `ResetProblemStep` 单次 LLM 调用的 prompt 扩展为一次产出多路结果
  （改写 + 关键词化 + 多跳问题分解），解析为 ≤3 路 query；
  `SearchDatasetStep.retrieval` 的并行合并骨架不动，futureList 从 2 路扩到 N 路（上限 yml 配置）。
- 各路 recallN 相应下调避免融合候选爆炸；总 LLM 调用次数仍为 **1 次**，流式首包延迟不回退。
- yml 总开关（默认关）；开启前用评估集验证收益与延迟。

### P2 进阶能力（长期选做）

#### P2-1 Contextual Retrieval（对应 I5）

- 索引期用小模型（yml 配置模型 ID，可与 rerank 模型同供应商）为每块生成 1~2 句定位摘要，
  前缀到嵌入文本。成本为索引期一次性调用；存量库重建时按需开启（yml 开关，默认关）。
- 收益集中点：表格块、无标题短块（评估集单列一组验证）。

#### P2-2 语义分段（对应 I6）

- 相邻句 embedding 相似度低谷处优先断块，字符/token 预算兜底；与 P1-1 组合时
  语义断点决定子块边界，父子结构保证注入完整性。
- 索引期每句一次 embedding（与段落向量化同模型、批处理），成本可控。

#### P2-3 问题路权重与全文检索调优（对应 R6、R10）

- yml `knowledge.search.problem-route-weight`（默认 1.0 = 现状）：`mapProblemsToParagraphs`
  分数乘权后进入融合；结合 `GenerateProblemListener` 自动生成问题的质量在评估集上标定。
- `vector.store.batch-size` 默认 10 → 50~100；为 `embedding_*` 表显式管理 HNSW 索引参数
  （`m/ef_construction/ef_search` yml 暴露，建表 SQL 层实现）。
- MongoDB 全文索引评估中英混合场景（jieba 预分词 + 默认 stop words 的交互），
  必要时引入内部同义词表扩展 query 分词（词表 yml/内部表，非产品接口）。

---

## 五、与上一版方案的差异：被约束排除项及替代路径

| 上一版方案项 | 约束冲突点 | 本版替代路径 |
|:-----------|:---------|:-----------|
| `KnowledgeSetting` 新增 `chunkSize/chunkOverlapTokens` | 用户配置字段变更 | 参数不动；P1-1 索引层子块化用内部阈值（yml）实现同等的"检索粒度与注入粒度解耦"，且比按库配置更无感 |
| `KnowledgeSetting.diversityEnable` | 用户配置字段变更 | P0-5 MMR 默认开启 + λ 走 yml（零成本算法，无需用户决策） |
| `KnowledgeSetting.problemRouteWeight` | 用户配置字段变更 | P2-3 权重走 yml 部署级配置，默认 1.0 行为不变 |
| `searchMode` 新增 `auto` 枚举值 | 参数取值语义变更 | P0-6 blend 模式内部 weighted RRF 自适应——"auto 的收益"装进既有 blend 值里 |
| hit_test 请求透传 rerank 参数 | 接口参数变更 | P0-3 hit_test 内部读库设置自动应用 rerank（设置字段本就存在） |
| `ParagraphEntity` 加 `parent_id` 列 + 子块入库 | 段落管理页会显示子块，前端行为变更 | P1-1 子块只存在于索引层（sourceId=父段落 id），段落表零变更 |

> 结论：上一版全部能力目标在本版均有对应实现路径，且用户侧感知仅为"效果变好"，
> 不存在需要用户重新学习或重新配置的变更。

---

## 六、兼容性与风险

1. **对外零变更验证**：每项落地后跑一次接口契约回归（现有 controller 测试 + 前端联调冒烟），
   确认请求/响应结构 diff 为空（details 增量字段除外）。
2. **索引重建窗口**：P1 各项改变索引写入内容，存量库执行"重建索引"后生效（复用 reindex 链路）；
   不重建则完全维持现状——天然支持逐库灰度与回退（回退 = 再重建一次）。
3. **默认行为变更点明示**（虽然接口不变，效果会有可感知变化，需在发布说明中告知）：
   - P0-5 MMR 默认开启：topN 的构成更多样，个别依赖"topN 全是同一段落邻块"的场景答案组织可能变化；
   - P0-6 blend 自适应：仅权重微调，RRF 名次主导，排序变化幅度有限；
   - P1-1 子块化：大 limit 库的召回命中率上升是最主要的可感知收益。
   以上各项均提供 yml 关闭开关，一键回退旧行为。
4. **缓存一致性**（P0-2）：结果缓存 TTL 短 + 写路径主动失效双保险；段落状态翻转
   （isActive）也纳入失效触发，避免命中测试与对话拿到已停用段落。
5. **评估先行**：P0-1 是所有后续项的前置——任何检索改动必须附评估集 before/after 数据。

---

## 七、实施顺序与预期收益

| 顺序 | 措施 | 主要改动文件 | 预期收益 | 工作量 | 重建索引 | 用户感知 |
|:----|:-----|:-----------|:--------|:------|:-------|:--------|
| 1 | 评估与观测体系 | 新增内部评估模块、`SearchOrchestrator`、`getDetails` | 优化可度量 | 中 | 否 | 无（details 增量字段） |
| 2 | embedding 复用 + 结果缓存 | `SearchRequest`、`PgVectorEmbeddingStoreImpl` | 检索延迟/成本显著下降 | 小 | 否 | 响应更快 |
| 3 | 命中测试对齐链路 | `ParagraphRetriever`、抽 `RerankExecutor` | 调参如实反映效果 | 小 | 否 | 测试结果更准 |
| 4 | 邻块扩展注入 | `SearchDatasetStep` 内部步骤 | 答案上下文更完整 | 小 | 否 | 答案更完整 |
| 5 | MMR 多样性 | `SearchOrchestrator` | topN 覆盖面提升 | 小 | 否 | 答案覆盖更广 |
| 6 | blend 自适应 | `CompositeStoreImpl` | 关键词/型号查询召回提升 | 小 | 否 | blend 更准 |
| 7 | **索引层子块化（核心）** | `ParagraphIndexBatchExecutor`、yml | 大块库检索精度大幅提升 | 中 | **是** | 同参数下召回更准 |
| 8 | 代码块/列表保护 | `DocumentSplitServiceImpl.getStringList` | 代码类文档可读召回 | 小 | **是** | 代码问答可用 |
| 9 | token 估算保护 | 新增 `TokenEstimator` | 英文库向量质量稳定 | 小 | **是** | 英文库更准 |
| 10 | 查询增强多路化 | `ResetProblemStep`、`SearchDatasetStep` | 多跳/长尾问题召回提升 | 中 | 否 | 复杂问题更好 |
| 11 | Contextual Retrieval | `ParagraphIndexBatchExecutor`、yml | 表格/短块区分度提升 | 中 | **是** | 表格问答更准 |
| 12 | 语义分段 | `SentenceSplitter` 扩展 | 语义完整块 | 中 | **是** | 召回更稳 |
| 13 | 问题路权重 + 工程调优 | `SearchOrchestrator`、`PgVectorEmbeddingStoreImpl`、yml | 融合可调、吞吐提升 | 小 | 否 | 无/更快 |

> 执行节奏：**1 → 6（P0 全部，两周内部署即收益）→ 7/8/9（同一个重建索引窗口一并完成）
> → 10 → 按业务需要选做 11~13**。每项落地跑评估集对比，不达预期即用 yml 开关回退；
> 全程无任何接口、参数、前端变更。
