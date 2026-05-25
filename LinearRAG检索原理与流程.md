# LinearRAG 检索原理与流程

## 一、整体架构

LinearRAG 是一个**基于异构图的检索增强生成（RAG）系统**，核心思想是把语料库构建成一张包含 **段落节点、实体节点、句子节点** 的异构图，然后在图上通过 **BFS 扩散 + Personalized PageRank（PPR）** 实现检索，相比纯向量检索能捕获更丰富的结构化语义关系。

整个系统分为三个阶段：**索引（Index）→ 检索（Retrieve）→ 生成（QA）**。

---

## 二、索引阶段（`index()`）

索引阶段的目标是把原始语料构建成一张异构图。

### 步骤 1：段落向量化

```
passages → EmbeddingModel.encode() → passageEmbeddingStore
```

每个段落文本经过 Embedding 模型编码，以 `{hashId, text, embedding}` 的形式存入 `passageEmbeddingStore`。

### 步骤 2：命名实体识别（NER）

```
passages → SpacyNER.batchNer() → {passage→entities, sentence→entities}
```

使用 Stanford CoreNLP 对每个段落做 NER，产出两份映射：
- `passageHashId → [实体列表]`：每个段落包含哪些实体
- `sentence → [实体列表]`：每个句子包含哪些实体

过滤掉 `ORDINAL / CARDINAL / NUMBER / MONEY / PERCENT / QUANTITY` 等数值类实体。NER 结果持久化到 `ner_results.json`，支持增量更新。

### 步骤 3：构建句子 & 实体 Embedding 库

```
所有句子文本 → sentenceEmbeddingStore
所有实体文本 → entityEmbeddingStore
```

### 步骤 4：构建实体 ↔ 句子双向索引

```
entityHashIdToSentenceHashIds:  实体 → 包含该实体的句子列表
sentenceHashIdToEntityHashIds:  句子 → 句子中包含的实体列表
```

这是后续 BFS 图扩散的核心数据结构。

### 步骤 5：建图（边与权重）

图中有三类边：

| 边类型 | 构建逻辑 | 权重 |
|---|---|---|
| **实体 ↔ 段落** | 统计实体在段落中的出现次数，归一化为频率 | `count(entity, passage) / total_entity_count_in_passage` |
| **段落 ↔ 段落** | 按段落前缀的序号（如 `0:`, `1:`）建立相邻段落连接 | 固定 `1.0` |
| **实体 ↔ 句子** | 通过实体↔句子双向索引隐式关联（不在图中显式建边，而是在 BFS 中使用） | — |

图使用 JGraphT 的 `SimpleWeightedGraph` 存储，同时导出为 `LinearRAG.graphml` 文件供可视化。

---

## 三、检索阶段（`retrieve()`）

这是 LinearRAG 的核心，流程如下：

```
question
   │
   ▼
getSeedEntities()  ← NER + 向量相似度
   │
   ├── 找到种子实体 → graphSearchWithSeedEntities()  → PPR → Top-K passages
   │
   └── 无种子实体   → densePassageRetrieval()        → Top-K passages（降级兜底）
```

### 步骤 1：种子实体匹配（`getSeedEntities()`）

```
question → SpacyNER.questionNer() → 问题实体
问题实体 → EmbeddingModel.encode() → 问题实体向量
entityEmbeddings × 问题实体向量ᵀ → 相似度矩阵
对每个问题实体，取最相似的库中实体作为种子
```

每个种子实体携带：`hashId`、`text`、`similarityScore`、在图顶点列表中的 `index`。

### 步骤 2：图搜索（`graphSearchWithSeedEntities()`）

这是 LinearRAG 的核心创新，分为三部分：

#### 2a. BFS 实体分数扩散（`calculateEntityScores()`）

从种子实体出发，沿着 **实体 → 句子 → 实体** 的路径做广度优先扩散：

```
种子实体 (tier=1)
   │
   ▼ 找该实体关联的所有句子（过滤已使用）
   │
   ▼ 计算句子与问题的向量相似度，取 Top-K 句子
   │
   ▼ 对每个 Top-K 句子，找出其中的所有实体
   │
   ▼ 新实体分数 = 当前实体分数 × 句子相似度
   │
   ▼ 若新分数 < iterationThreshold，剪枝
   │
   ▼ 加入下一轮扩散队列（tier=2, 3, ...）
```

关键参数：
- `maxIterations = 3`：最多扩散 3 轮
- `iterationThreshold = 0.5`：分数低于阈值则停止扩散
- `topKSentence = 1`：每个实体每轮只取最相关的 1 个句子

分数是**乘法衰减**的：离种子越远的实体，分数越低。

#### 2b. 段落分数计算（`calculatePassageScores()`）

段落分数由三部分叠加：

```
passageScore = passageRatio × DPR_normalized_score + log(1 + entityBonus)
```

**① DPR 稠密检索分数**

```
passageEmbeddings × questionEmbedding → 余弦相似度 → min-max 归一化
```

**② 实体奖励（entityBonus）**

```
对每个已激活实体：
  entityBonus += entityScore × log(1 + occurrences_in_passage) / tier
passageScore += log(1 + totalEntityBonus)
```

实体在段落中出现次数越多、实体本身分数越高、tier 越小（越接近种子），奖励越大。

**③ 属性关键词加成（可选）**

若问题是属性类查询（包含 born/where/when/founded 等关键词），对段落中同时出现这些关键词的情况额外加分：

```
passageScore += attributeKeywordBoost × log(1 + overlap_count)
```

最后乘以 `passageNodeWeight = 0.05` 缩放到与实体分数相当的尺度。

#### 2c. Personalized PageRank（`runPpr()`）

将 BFS 实体分数 + 段落分数合并为图的**重置分布（reset distribution）**，然后做幂迭代 PPR：

```
p = (1 - d) × reset + d × M × p
```

- `d = damping = 0.5`（阻尼系数，比标准 PageRank 的 0.85 小，更依赖重置分布）
- `maxIter = 100`，`tolerance = 1e-6`
- 转移矩阵 `M` 由图的加权邻接矩阵归一化得到

收敛后，取出所有段落节点的 PPR 分数，降序排列，返回 Top-K 段落。

### 步骤 3：兜底：纯稠密检索（`densePassageRetrieval()`）

当问题中未识别出任何实体时，直接做段落向量与问题向量的点积相似度排序。

---

## 四、生成阶段（`qa()`）

```
Top-K 段落（按分数排序拼接）
   +
问题文本
   ↓
LLM (system prompt: 阅读理解助手)
   ↓
"Thought: ...推理过程... Answer: 最终答案"
```

- 多个问题通过线程池并行调用 LLM
- 从 `"Answer:"` 后截断提取最终预测答案

---

## 五、完整数据流图

```
┌─────────────────────────────── 索引阶段 ───────────────────────────────┐
│                                                                        │
│  chunks.json                                                           │
│      │                                                                 │
│      ▼                                                                 │
│  [段落 Embedding] ─────────────────────────────────────┐               │
│      │                                                 │               │
│      ▼                                                 │               │
│  [NER] → 段落→实体 映射                                 │               │
│      │   句子→实体 映射                                 │               │
│      │                                                 │               │
│      ├→ [句子 Embedding]                               │               │
│      ├→ [实体 Embedding]                               │               │
│      │                                                 │               │
│      └→ 构建异构图 ──────────────────────────────────→ Graph           │
│           ├ 实体↔段落边（出现频率权重）                  │               │
│           ├ 段落↔段落边（相邻序号）                      │               │
│           └ entity↔sentence 索引（BFS 用）              │               │
│                                                         │               │
└─────────────────────────────────────────────────────────┼───────────────┘
                                                          │
┌─────────────────────────────── 检索阶段 ───────────────────────────────┐
│                                                                        │
│  question                                                              │
│      │                                                                 │
│      ├→ [NER] → 问题实体 → 向量相似度 → 种子实体                        │
│      │                                                                 │
│      ├→ [BFS 扩散] 种子实体 → 句子 → 新实体（乘法衰减）                  │
│      │                                                                 │
│      ├→ [DPR] 段落向量 × 问题向量 → 段落相似度分数                       │
│      │       + 实体奖励 + 属性关键词加成                                 │
│      │                                                                 │
│      ├→ [合并] entityWeights + passageWeights → 重置分布                │
│      │                                                                 │
│      └→ [PPR] 幂迭代 Personalized PageRank → Top-K 段落                 │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
                                                          │
┌─────────────────────────────── 生成阶段 ───────────────────────────────┐
│                                                                        │
│  Top-K 段落 + question → LLM → "Thought: ... Answer: ..."              │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 六、核心超参数

| 参数 | 默认值 | 含义 |
|---|---|---|
| `maxIterations` | 3 | BFS 最大扩散轮数 |
| `iterationThreshold` | 0.5 | BFS 分数剪枝阈值 |
| `topKSentence` | 1 | 每轮每个实体取的最相关句子数 |
| `passageRatio` | 1.5 | DPR 分数在段落分中的权重系数 |
| `passageNodeWeight` | 0.05 | 段落节点在 PPR 重置分布中的缩放系数 |
| `damping` | 0.5 | PPR 阻尼系数（越小越依赖种子） |
| `retrievalTopK` | 5 | 最终返回的段落数 |
| `enableHybridAttributeFallback` | false | 是否启用属性关键词加成 |
| `attributeKeywordBoost` | 0.25 | 属性关键词加成系数 |

---

## 七、关键源文件对照

| 源文件 | 职责 |
|---|---|
| `core/LinearRAG.java` | 核心逻辑：索引、检索、图搜索、PPR |
| `ner/SpacyNER.java` | 命名实体识别（Stanford CoreNLP） |
| `embedding/EmbeddingStore.java` | 向量存储与持久化（JSON） |
| `embedding/DjlEmbeddingModel.java` | Embedding 模型封装（DJL） |
| `llm/LLMModel.java` | LLM 推理接口 |
| `config/LinearRAGConfig.java` | 全局配置参数 |
| `util/MatrixOps.java` | 矩阵/向量运算工具 |
| `util/Utils.java` | MD5 哈希、归一化、日志工具 |
| `evaluate/Evaluator.java` | 答案评估（LLM 准确率 & 包含准确率） |
| `App.java` | 主入口：加载数据集、串联索引→检索→评估流程 |
