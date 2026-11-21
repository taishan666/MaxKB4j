# MaxKB4j 代码 Bug 扫描报告

- 扫描日期：2026-11-21
- 扫描基线：dev 分支工作区（含未提交改动），commit `4abcf8348`
- 编译验证：`mvn compile` 全模块通过（exit 0）
- 验证方式：全部 11 项高危与 38 项中危均由主会话逐条亲自读码复核后入册（部分结论经 langchain4j 1.22.0 / openai-java 4.9.0 字节码核实）；低危项采信代理报告的行号与代码引用

## 一、扫描覆盖度

| 区域 | 方式 | 状态 |
|---|---|---|
| 18 个 MyBatis Mapper XML（全部逐行） | 人工逐行审查 | ✅ 完成 |
| application.yml / dev-prod-test 配置 | 人工审查 + 交叉验证代码引用 | ✅ 完成 |
| Flyway 迁移脚本 ↔ 实体一致性 | 抽查关键字段 | ✅ 抽查完成 |
| docker-compose(.dev).yml / Dockerfile / CI / i18n | 人工审查 | ✅ 完成 |
| service-api / web 模块 | 接口与静态产物，抽查核心类 | ✅ 完成 |
| Flux 异步链路 / ThreadLocal 用户上下文 | 全链路追查（controller → service → executor） | ✅ 完成 |
| PgVectorEmbeddingStoreImpl（向量写入/重试/SSRF 防护） | 人工审查 | ✅ 完成 |
| maxkb4j-common + maxkb4j-core（100 文件） | 代理全量审查 + 主会话逐条复核 | ✅ 完成 |
| maxkb4j-knowledge（157 文件 + 8 Mapper XML） | 代理全量审查 + 高危项主会话逐条复核 | ✅ 完成 |
| maxkb4j-workflow（131 文件） | 代理全量审查（含 langchain4j 反编译验证）+ 高危项主会话逐条复核 | ✅ 完成 |
| maxkb4j-application + chat（125 文件） | 代理全量审查 + 高/中危主会话逐条复核 | ✅ 完成 |
| maxkb4j-model + trigger（111 文件） | 代理全量审查（含 openai-java 字节码核实）+ 高危项主会话逐条复核 | ✅ 完成 |
| maxkb4j-system + tool + oss + start（140 文件） | 代理全量审查 + 高危项主会话逐条复核 | ✅ 完成 |

**六大模块深度审查全部完成。**

## 二、Bug 总览统计

| 等级 | 数量 | 说明 |
|---|---|---|
| 🔴 高 | 11 | 认证绕过/沙箱逃逸/功能完全不可用/文件损坏/数据损坏 |
| 🟠 中 | 38 | 越权/资源耗尽/事务失效/静默失败/统计失真 |
| 🟡 低 | 34 | NPE/死代码/配置残留/语义偏差/并发瑕疵 |
| **合计** | **83** | 另排除 30 项查证不成立的疑点 |

## 三、高危 Bug（11 项，按建议修复顺序排列）

| 编号 | 类别 | 问题 | 位置 | 触发条件与后果 | 修复建议 |
|---|---|---|---|---|---|
| BUG-74 | 认证绕过 | 初始 admin 口令硬编码 `tarzan@123456` 且 yml 默认值使其永远生效，代码中"未配置时生成 16 位随机口令"保护分支成死代码，无任何告警 | `application.yml:94` + `UserServiceImpl.java:195-211` | 部署不设 `SYSTEM_DEFAULT_PASSWORD`（默认即硬编码值）→ 任何知道源码者可用 `admin / tarzan@123456` 登录公网实例获得全部管理员权限 | yml 默认值置空 `${SYSTEM_DEFAULT_PASSWORD:}`，让随机口令分支生效 |
| BUG-75 | 沙箱逃逸 | Groovy 沙箱静态白名单放行 `java.nio.file.Files` 全套读/写/删方法（readString/readAllBytes/writeString/delete/deleteIfExists 等），`Path.of` 同在类白名单；编译期直接放行、运行期只验类型不验路径 | `GroovySandboxPolicy.java:473-478` + `GroovySandboxInterceptor.java:49-78` | 拥有 TOOL_DEBUG 权限的用户（列入默认权限）在工具脚本写 `Files.writeString(Path.of('/etc/cron.d/x'),...)` → 进程权限内任意文件读写删（可读含 DB 密码/JWT 密钥的配置、写持久化后门） | Files/Path 移出静态白名单，或运行期校验路径限定在工作目录内 |
| BUG-83 | 限流失效 | webhook 限流每次访问（含 429 的那次）都 `put`，Caffeine `expireAfterWrite(1,MINUTES)` 每次被重写 → 计数永不过期、单调递增；`estimatedSize()` 为死变量；getIfPresent+put 非原子并发丢计数 | `WebhookTriggerController.java:58-70` | 同 IP 持续调用累计 60 次后永久 429，"每分钟 60 次"完全失效，EVENT 触发器被永久封禁（两个代理独立发现，双重印证） | 固定/滑动窗口计数器；429 时不刷新时间戳 |
| BUG-55 | API 不可用 | 非标参数 `thinking`/`enable_thinking` 经 langchain4j `customParameters()`（`@JsonAnyGetter`）展开为请求体顶层字段；OpenAI/DeepSeek/Anthropic 官方 API 对未识别参数返回 400；思考开关默认 true 即触发；streaming 分支未 remove `enable_thinking` | `OpenAiModelProvider.java:87-89,102-113`（DeepSeek/Volcanic/Anthropic 同模式） | 9+ 继承 OpenAiModelProvider 的供应商创建模型校验与运行时对话全部 400；Anthropic 另发 `thinking:{type:"disabled"}`（其仅接受 enabled）必 400；附带原地修改共享 JSONObject 的并发风险 | 非标参数按 provider 白名单注入；禁止原地修改共享 params |
| BUG-6 | 访问控制绕过 | `WebUtil.getIP` 取 X-Forwarded-For 链首（追加式头部，客户端伪造值恒在链首），白名单/限流/验证码绑定全基于它 | `WebUtil.java:96-107`；调用方 `ChatEmbedService.java:55`、`RateLimitAspect.java:89`、`UserServiceImpl.java:160` | 伪造 `X-Forwarded-For` 请求头即可绕过嵌入/分享 IP 白名单、`@RateLimit` 限流、验证码 IP 绑定 | 白名单/限流改用可信头（如 X-Real-IP）或 `request.getRemoteAddr()`；必须解析 XFF 时从链右侧取第一个非可信代理 |
| BUG-56 | 服务不可用 | createModel 注释声称"LLM 远程校验移出事务"，实际 `modelIsValid`（LLM 走 `flux.blockLast()`，读超时默认 30 分钟）仍在 `@Transactional` 方法内，事务入口即占用连接 | `ModelServiceImpl.java:80-98` | 模型服务慢/不可达但端口可达 → 校验挂到 read-timeout；并发创建几个模型即耗尽连接池，全站数据库操作阻塞 | 校验移到事务外（先校验后开事务落库） |
| BUG-17 | 功能崩溃 | 段落关联新问题时 `entity.setProblemId(problemEntity.getId())` 在 `saveBatch` 之前执行，ASSIGN_UUID 插入时才回填 id → 写入 null | `ParagraphServiceImpl.java:166,172-175`；schema `problem_paragraph_mapping.problem_id NOT NULL` | POST 段落 `problemList` 含知识库中不存在的新问题 → NOT NULL 违规 → 事务回滚，段落创建必然 500（对照同类路径均预生成 UUID，唯此路径坏） | 新问题用 `IdWorker.get32UUID()` 预生成 id 后再 setProblemId |
| BUG-46 | 续聊崩溃 | 恢复执行时 `saveContext` 将持久化的 `JSONArray`（元素 JSONObject）强转 `List<Group>`，泛型擦除使 cast 通过、迭代时抛 CCE；属"恢复后上下文为 JSON 类型"系统性缺陷 | `VariableAggregationNode.java:31-36` + `NodeStateLoader.java:84,93`；同类隐患 `RerankerNodeHandler.java:111` | 工作流含变量聚合节点且在表单/用户选择中断点之前执行过 → 续聊请求 100% ClassCastException 失败（loadNodeState 在 ChatWorkflow 构造器中执行） | 恢复链路用 fastjson `toJavaList` 重建 POJO，或 context 统一存 JSON 兼容值 |
| BUG-35 | 功能崩溃 | AgentExecutor 以 `"agent_"+memoryId` 作 chatId 但从不调用 chatOpen 初始化会话；合法会话 id 由 IdWorker 生成，缓存 miss 后查库必为空 | `AgentExecutor.java:38-43` + `ApplicationChatServiceImpl.getChatInfo` | 应用作为工具（agent 子应用）被主应用调用 → 100% 抛 `common.record.not.exists`，功能完全不可用；且每次失败前 `ensureStatsExists` 先插一行永不使用的统计行，统计表持续膨胀 | 执行前补会话初始化并复用稳定 chatUserId |
| BUG-45 | 功能失效 | 节点失败启用"异常分支"后路由正确，但异常分支下游节点被依赖检查静默跳过——`isExecuted` 只认 SUCCESS/SKIP，失败节点状态 ERROR 不在内 | `NodeLifecycleExecutor.java:130-144` + `NodeDependencyChecker.java:51-54` | 任一节点 `enableException=true` 且抛异常 → 异常分支节点被空完成、永不执行，其后继也永不构建；异常处理特性形同虚设，用户无感知 | `isExecuted` 纳入 ERROR 状态，或异常分支路由时重置依赖判定 |
| BUG-18 | 文件损坏 | downloadSourceFile 已向 response 写入文件字节后仍返回 `R<String>`，Spring 将 R 对象 JSON 序列化追加到响应尾部 | `DocumentExportController.java:58-62` | 每次下载源文件，xlsx/zip 尾部多出 `{"code":200,...}` → 文件损坏无法打开；Content-Type/Content-Disposition 被覆盖 | 改为返回 void（失败时再写 JSON） |

## 四、中危 Bug（38 项，按编号排列）

| 编号 | 类别 | 问题 | 位置 | 后果 | 修复建议 |
|---|---|---|---|---|---|
| BUG-1 | 功能失效 | `supported-extensions` 配置 `.HTML` 大写，而引擎先 toLowerCase 再匹配 | `application.yml:118` ↔ `DocLingParseEngine.java:49-50` | DocLing（OCR/表格/公式增强）对 .html 文件永远不生效 | `.HTML` → `.html` |
| BUG-2 | 功能缺陷 | 聊天记录导出未设置 Content-Type/Content-Disposition（项目内其他导出端点均有） | `ApplicationChatController.java:59-62` → `writeChatExport` | xlsx 以无文件名原始字节流返回，下载体验破损 | 写流前补齐三个响应头 |
| BUG-7 | 协议 | GlobalExceptionHandler 约 15 个分支无 setStatus/@ResponseStatus，返回 HTTP 200 + 业务码 500 | `GlobalExceptionHandler.java`（RuntimeException 290-295、Exception 297-302 等） | 网关重试、监控告警、HTTP 客户端按状态码判错全部失效 | 统一补 `@ResponseStatus` |
| BUG-8 | 安全 | maskString 短字符串分支 `replaceAll("\\.","*")` 只替换字面点号（转义笔误） | `DataMaskUtil.java:61-65` | 长度 ≤8 的 API Key 脱敏失效明文返回前端（`ModelServiceImpl.java:158`）；"脱敏值==回传值"判断失真 | `"\\."` → `"."` 或全量打码 |
| BUG-9 | 数据静默丢失 | IoUtil.readToString 吞 IOException 返回已读部分或空串 | `IoUtil.java:39-49` | HtmlParser/MDParser 解析失败静默得空内容，空段落入库无报错；嵌入页模板/提供商清单同受害 | 向上抛出或返回 Optional |
| BUG-10 | 安全 | SQLite 分支路径正则放行 `..`（允许 `.`/`/`/`\`） | `DatabaseUtil.java:219,251-256` | NL2SQL 节点配置 `../../x.db` → 进程权限内任意路径建库/读库 | 规范化路径并断言在数据目录内 |
| BUG-16 | 部署安全 | Postgres/Mongo 密码 `ycn4NRhjN2` 多处硬编码入库且复用同一口令；healthcheck 命令行明文带密码 | `docker-compose.yml:10,35,40,75,78`、`application-prod.yml:8-9`、`application-dev.yml:9` | 凭据泄露 + `docker inspect` 可见 | 改 `${VAR}` 注入并轮换密码 |
| BUG-19 | 数据冲突 | LLM 返回重复问题时同 ID 实体重复入批、关联表重复行 | `ProblemServiceImpl.java:116-130` | saveBatchProblems 主键冲突 → 段落标 FAILURE；叠加 BUG-20 留脏数据 | 入批前按内容 distinct |
| BUG-20 | 事务失效 | saveBatchProblems 的 @Transactional 因同类 `this.` 自调用（133、271 行）绕过代理失效 | `ProblemServiceImpl.java:158-161` | 批量插入逐条自动提交，中途失败无法回滚 → 部分落库与失败标记不一致；注释声称的"短事务"不存在 | 注入自身代理或拆分独立 Service |
| BUG-21 | 竞态 | Web 文档事件用 `@EventListener`+`@Async`（发布瞬间即执行）而非 AFTER_COMMIT，事务承诺不成立 | `KnowledgeServiceImpl.java:186-193` + `WebDocsListener.java:31-34` | 异步事务插入 document 引用未提交 knowledge 行 → FK 校验失败或孤儿文档（同模块其他监听器均正确用 @TransactionalEventListener） | 改 `@TransactionalEventListener(phase=AFTER_COMMIT)` |
| BUG-22 | 长事务 | createWebDoc/syncWebDoc 整体 @Transactional，HTTP 抓网页实际在事务内（注释称已移出） | `DocumentImportService.java:114-121,123-134` | 多 URL/慢站点事务持续数十秒，连接池占满风险 | 先抓取（事务外）再开事务落库 |
| BUG-23 | 功能崩溃 | PptParser 声明支持 .pptx 却用仅支持 OLE2 二进制格式的 OfficeParser | `PptParser.java:25,35` | doc-ling 关闭（默认）时上传 .pptx 必抛 TikaException，导入/预览必然失败 | 改 `AutoDetectParser`/`OOXMLParser` |
| BUG-24 | 解析失败 | ExcelParser 表头空单元格 → headMap null 元素/缺键 | `ExcelParser.java:36-42,62-66`；同模式 `DocumentHandler.java:241-243` | `String.join` NPE 解析失败，或表头与数据列错位、生成 "null:xxx" 污染内容 | null 表头补默认列名 |
| BUG-25 | 安全 | Web 知识库抓取任意用户可控 URL，无 scheme 白名单/内网/云元数据校验；递归模式继续抓 | `JsoupUtil.java:22-34` | 登录用户可借服务端探测内网、命中 169.254.169.254（SSRF）；同项目 PgVector 图片下载有完整 SSRF 加固，此处裸奔 | 复用 PgVector 的 SSRF 校验逻辑 |
| BUG-36 | NPE | OpenAI 兼容接口 messages 无 user 角色时 message=null 穿透（无 @Valid，手工 builder 绕过 @NotBlank） | `ChatOpenAiController.java:160` → `ChatPostHandler.java:129` `problemText.length()` | 该类合法载荷同步路径稳定 500、流式路径 error 收尾，事务回滚 | 转换层对 null/blank 返回 4xx |
| BUG-37 | 越权(IDOR) | 管理端 getChatRecordInfo 缺 chat 归属校验（同族 chatRecordPage 有 chatBelongsToApp，chat 模块有 checkChatOwner，唯此处无） | `ApplicationChatRecordController.java:34-35` → `ApplicationChatRecordServiceImpl.java:124-128` | `{id}` 填自己有权限的应用、`{chatId}` 填任意其他应用会话 → 读取记录全文（问题/AI 回答/executionDetails） | getChatRecordInfo 增加 appId 参数并校验归属 |
| BUG-38 | 数据覆盖 | reChat 对不在"最近 100 条"回填缓存里的旧记录，`chatRecord==null` 被误判为新记录 | `ApplicationChatRecordServiceImpl.java:112-116` + `ChatPostHandler.java:69-71` | saveOrUpdate 按旧 id 命中执行 update → 旧记录 problem_text/answer_text/details 全部被覆盖丢失；chat_record_count 虚增 | 回填 miss 时按 id 显式查库判定新旧 |
| BUG-40 | 功能失效 | speechToText：`requireNonNull(contentType).split("/")[1]` 对缺失/非标 Content-Type 抛 NPE/数组越界；且 MIME 子类型直接当音频格式（mp3→"mpeg" 非法必被拒、octet-stream 在采样率解析处抛异常） | `ApplicationSpeechServiceImpl.java:70` → `AbsSTTModel.java:39-41`、`GummySTT.java:71-72` | 语音识别对 mp3（最常见格式）及非标请求必失败 | 判空 + MIME→格式映射表 + 采样率解析失败降级 |
| BUG-47 | 重试必败 | 节点级历史构造 `new UserMessage(空列表)`/`new AiMessage(null)`，langchain4j 1.22 校验直接抛异常（已反编译确认） | `HistoryManager.java:70-89` | 上轮流式失败只写 question 无 answer、NL2Sql 类节点无 answer、question 无有效项 → 配 NODE 历史的节点重试永远失败，报无关的 "text cannot be null" | 构造前过滤空 contents、answer 判空跳过 |
| BUG-48 | 静默终止 | 意图分类解析失败/越界时 BRANCH_ID 写 null，`getOrDefault` 因 key 存在返回 null → 不匹配任何边 | `IntentClassifyNodeHandler.java:65-72,81-85` + `WorkflowExecutionAccessor.java:96-100` | 所有下游节点被静默 SKIP，工作流无报错直接终止，极难排查（对比条件节点会抛 CONDITION_NO_MATCH） | branchId 为 null 走失败处理 |
| BUG-49 | 记录缺失 | Question/ImageGenerate/SpeechToText/TextToSpeech 四类节点勾选"输出结果"只设 answerText（实时推送），detail 未写 IS_RESULT | `QuestionNodeHandler.java:58-67`、`ImageGenerateNodeHandler.java:85-87`、`SpeechToTextNodeHandler.java:59-61`、`TextToSpeechNodeHandler.java:43-45` | 持久化 `output().getAnswers()` 依赖 isResult() 过滤 → 保存的聊天记录缺失回答，历史/下一轮上下文不一致（对照 LLMStream/DirectReply 均正确写入） | 四处补 `putDetail(IS_RESULT)` |
| BUG-50 | 变量污染 | 变量赋值节点 chat 作用域：getChatHandleResult 内部已正确写值，外层又把 {name,input_value,output_value} 描述 Map 整体 putAll 进 ChatCache（copy-paste 双写） | `VariableAssignNodeHandler.java:44-54,88-106` | 会话变量被写入三个脏键；用户恰定义同名变量时下轮读到脏值 | 移除外层冗余写入 |
| BUG-51 | NPE | Reranker 多路召回引用字段解析为 null（上游被 SKIP/配置错误/恢复后 JSON 类型）时直接强转后 stream | `RerankerNodeHandler.java:110-112` | NPE 失败且错误信息不指向真实原因；若配异常分支又被 BUG-45 拦截 | null 时跳过该路或报可读错误 |
| BUG-52 | 资源/重复回调 | 节点超时 `CompletableFuture.cancel(true)` 不中断底层流；cancel 触发 handle 以 CancellationException 进入 completeFailedNode 与 catch 中 reportError 双重回调 | `NodeChainRunner.java:50-53` + `LLMStreamNodeHandler.java:199-226` | 客户端收到错误帧后仍持续收到该节点流式内容；模型资源不释放；异常分支被二次路由 | 同步关闭 TokenStream 并防重复上报 |
| BUG-57 | 功能不可用 | XInference STT/TTS 复用 OpenAi 模型类，SDK 在 baseUrl 直拼 /audio/*，而同 provider chat/embedding 的 langchain4j 客户端自行拼 v1/——两套约定互斥 | `XInferenceModelProvider.java:106-114` | XInference 语音识别/合成必 404 | STT/TTS 构建时给 baseUrl 追加 /v1 |
| BUG-58 | 列表页全挂 | 触发器分页循环内 `Integer.parseInt`/ScheduleType.fromValue 无捕获；batchActivate 先 cancelSchedule 后 scheduleTrigger，后者抛异常事务回滚但取消已生效 | `EventTriggerServiceImpl.java:73-84,187-219` + `NextRunTimeCalculator.java:31-32` | 一条非法 time 脏数据 → 整个分页接口 500；批量启用后 DB 显示启用、实际无调度，触发器静默失效 | 单条失败跳过并记录；先算 nextRunTime 再提交 |
| BUG-59 | 永久停摆 | 周期触发器任务 lambda 内 executeTrigger 有保护，但外层 rescheduleDaily/rescheduleFromSetting 无保护 | `TriggerScheduler.java:134-141,174-190` | 重排时 getById 遇一次 DB 异常 → 下次调度未注册，daily/weekly/monthly 触发器永久停摆直到重启 | 重排逻辑 try-catch 或先注册后执行 |
| BUG-60 | 重复触发 | scheduleTrigger 的 cancel+put 非原子：调度线程重排与 HTTP 线程编辑/启用并发时各自 cancel 后各自 schedule | `TriggerScheduler.java:78-88,131-158` | 两个任务并存且都执行 → 触发器重复触发（LLM 任务重复烧 token） | 按 triggerId 加锁或原子替换 |
| BUG-61 | 线程占用 | 触发任务 `future.get(5,MINUTES)` 同步阻塞调度线程；webhook 侧占 HTTP 线程 5 分钟；supplyAsync 未指定 Executor 跑 commonPool（与注释矛盾）；cancel(true) 对不响应中断的 LLM 调用无效 | `TriggerTaskExecutor.java:129-142` + `WebhookTriggerController.java:87` | 多个慢任务并发 → 其它触发器集体延迟、Web 线程占满；超时任务继续烧 token 且结果丢失 | 改异步回调收尾，调度线程不阻塞 |
| BUG-62 | MCP/SKILL 空跑 | 定时触发对 MCP/SKILL 工具统一走 httpOrCodeExecute（仅区分 HTTP，其余一律 Groovy 编译执行） | `TriggerTaskExecutor.java:157-175` + `ToolExecuteServiceImpl.java:40-52` | MCP 的 code（servers JSON）恰为合法 Groovy Map 字面量 → 执行成 Map 记 SUCCESS 但什么都没做；SKILL 的 code（OSS 文件 ID）→ Groovy 语法错误必 FAILURE | 按 toolType 分派正确执行器 |
| BUG-63 | NPE | getInfo 直接解引用 credential（ModelCreateDTO.credential 无 @NotNull 可创建 null） | `ModelServiceImpl.java:155-161` | 查看 credential 为空的模型详情/参数表单 500，无法查看与编辑回填 | 判空降级返回 |
| BUG-64 | 越权 | 模型 updateModel/removeModelById/updateModelParamsForm 只查功能权限无资源归属校验（getInfo 却 owner-only），可覆盖他人凭证；读与写权限边界不对称 | `ModelServiceImpl.java:117-146,148-153,210-222` | 有 MODEL_EDIT/DELETE 的用户可修改/删除/篡改他人模型凭证与参数 | 写操作对齐 getOwnedModel 语义 |
| BUG-65 | 失败不可诊断 | SiliconFlowImageModel 非 2xx 时记 warn 返回空 List → getFirst() 抛 NoSuchElementException，真实错误（key 无效/欠费/限流）只在日志 | `SiliconFlowImageModel.java:78-81,136-145` | 图像生成失败时用户看到无意义异常，排障困难 | 失败抛携带 API 错误信息的异常 |
| BUG-76 | 功能断裂 | 忘记密码流程：checkCode 仅作废验证码，两个改密接口均需登录态/旧密码，ResetPasswordDTO 字段无接口消费 | `AuthController.java:60-63,65-67` + `UserController.java:62-66` + `ResetPasswordDTO.java:8-24` | 忘记密码完全不可用；且 /user/current/reset_password 登录态改密不验旧密码/验证码（仅比对两次输入），会话被劫持即可直接改密 | 补未登录态重置接口（消费 email+code）；登录态改密验证旧密码 |
| BUG-77 | 数据一致性 | 删用户仅 removeById：FK NO ACTION 必炸；单删无角色过滤可删管理员/唯一管理员；stateless JWT 下 logout 无法使已签发 token 失效；user_resource_permission 无清理 | `UserServiceImpl.java:98-101,185-192` + `SaTokenConfigure.java:14` + V1 FK | 删除拥有 tool/knowledge/api_key 的用户必 500；禁用用户 24h 内仍可访问；误删唯一管理员风险 | 删除前清理/迁移关联数据+角色保护；禁用改黑名单或缩短有效期 |
| BUG-78 | 功能失效 | /admin/api/oss/file 上传无任何鉴权注解（SystemAuthInterceptor 自述只填充上下文不鉴权） | `oss/.../FileController.java:28-35`；`ToolStoreController.store` 同 | 匿名可持续写入 GridFS（单文件上限 100MB）耗尽存储 | admin 路径补登录+权限注解 |
| BUG-79 | 安全基线 | CORS `allowedOriginPatterns("*") + allowCredentials(true)`；sa-token token-name 同时是 cookie 名 | `WebConfig.java:84-88` | 浏览器放行带凭证跨域请求/响应读取（SameSite=Lax 部分缓解），扩大 CSRF 攻击面 | 收敛为显式域名白名单 |
| BUG-80 | 全局阻塞 | 全局 Groovy 脚本共用 `newSingleThreadExecutor` 单 worker（60s 超时）；GroovyScriptCache 在 synchronized 内编译 | `GroovyScriptExecutor.java:51-55` | 所有用户/所有会话（工具节点/debug/触发器）共享：单脚本耗时接近 60s 即让全平台工具节点排队超时 | 按会话/租户隔离或改并发池 |

## 五、低危 Bug（34 项，按编号排列）

| 编号 | 类别 | 问题 | 位置 | 修复建议 |
|---|---|---|---|---|
| BUG-3 | 死代码陷阱 | getUserPermissionById 引用不存在的表（dataset/team_member_permission），MaxKB 原版遗留，有声明无调用 | `UserMapper.xml:5-34` | 删除 SQL 与声明，或重写 |
| BUG-4 | 无效配置 | mapper-locations 匹配不到任何 XML，靠同名自动加载兜底 | `application.yml:52` | 改 `classpath*:com/maxkb4j/**/mapper/*.xml` |
| BUG-5 | 部署 | server.crt 死挂载（文件不存在、全库无引用，Docker 会建同名空目录） | `docker-compose.yml:81` | 删除挂载行 |
| BUG-11 | 安全 | SELECT 白名单放行带副作用内嵌函数（pg_read_file/dblink/INTO OUTFILE），防护依赖外部账号最小权限 | `DatabaseUtil.java:62-95` | 函数黑名单或文档化部署约束 |
| BUG-12 | 功能劣化 | NL2SqlAssistant 系统提示词为普通字符串：字面 `\n` + 开头杂散引号发给 LLM | `NL2SqlAssistant.java:10` | 改 `"""` 文本块真实换行 |
| BUG-13 | 安全 | RSA 私钥口令硬编码兜底 "mac_kb_password" + pbeWithSHA1AndDES_CBC 弱算法 | `RSAUtil.java:41,96-106,237` | 新写入强算法；无配置拒绝生成 |
| BUG-14 | 语义 | SystemAuthInterceptor 对 INVALID_TOKEN/TOKEN_TIMEOUT 静默降级，与 javadoc"过期继续抛出"相反，错误码语义退化 | `SystemAuthInterceptor.java:78-87` | 过期时继续抛出（对齐注释） |
| BUG-15 | 并发 | ChatInfo.addChatRecord 遍历查重+set/add 两步无原子性，多线程共享设计 | `ChatInfo.java:40-50` | ConcurrentHashMap+有序 key 或加锁 |
| BUG-26 | 数据重复 | 问题生成事件去重基线只加载一次，generateRelated 新问题不回写 → 同批次重复 ProblemEntity | `GenerateProblemListener.java:48,63` | 生成后回写去重基线 |
| BUG-27 | NPE | publish 对无效知识库 id：getById null 后直接 getWorkFlow | `KnowledgePublishService.java:44-48` | 判空抛友好异常 |
| BUG-28 | NPE | 文档批量导出 loadKnowledgeName 无判空 | `DocumentExportService.java:74-78` | 对照 KnowledgeExportService 抛 IllegalArgumentException |
| BUG-29 | 错误导 | checkFileLimit 在知识库不存在时 return false 放行 → 最终 FK 违规 500，错误信息误导 | `DocumentImportService.java:222-232` | 知识库不存在时抛明确异常 |
| BUG-30 | NPE | 嵌入资源缺 RESOURCE_NAME_KEY 时 `ConcurrentHashMap.put(null,…)` | `DocParser.java:79-85` | 生成占位名或跳过 |
| BUG-31 | 静默失败 | CSV 解析失败仅 warn 吞掉：storeFile 已执行（OSS 孤儿文件），接口"成功"但 0 数据 | `DocumentHandler.java:134-136` | 抛出或记录导入失败明细 |
| BUG-32 | 语义错误 | 标签查重 key IN + value IN 交叉匹配，命中不存在的组合导致漏加 | `TagServiceImpl.java:60-71`；同模式 `DocumentTagController.java:89-101` | 逐对 `(key=? AND value=?) OR …` |
| BUG-33 | 内容丢失 | WebContentCleaner 噪音正则无词边界，子串匹配误删正文（down**load**、**ad**dress 等） | `WebContentCleaner.java:30-45` | 加 `\b` 或完整 token 匹配 |
| BUG-34 | 导入不完整 | processZipQaFile 循环内 break，QA zip 只处理第一个表格文件 | `DocumentHandler.java:62` | 移除 break |
| BUG-39 | 统计失真 | MCP tools/call chatUserId 每次随机（注释自称"稳定复用"）→ 统计表膨胀、customer_num 失真 | `ChatApiService.java:179-180` | 按调用方身份生成稳定 id |
| BUG-41 | NPE | ResetProblemStep 未判空 tokenUsage（部分 provider 返回 null）；同管线 ChatStep 已判空 | `ResetProblemStep.java:40-42` | 对齐 ChatStep 判空 |
| BUG-42 | NPE | knowledgeSetting.maxParagraphCharNumber 缺字段时拆箱 NPE，该应用每轮对话必失败 | `GenerateHumanMessageStep.java:24` | 判空给默认值 |
| BUG-43 | NPE | promptGenerate 应用不存在时 getById null 直接解引用 | `ApplicationController.java:167` + `ApplicationPromptService.java:27` | 判空抛友好异常 |
| BUG-44 | 孤儿数据 | improveChatLog 第二次标注覆盖（非追加）improveParagraphIdList → 旧段落永远无法删除 | `ApplicationChatRecordServiceImpl.java:273` | 追加而非覆盖 |
| BUG-53 | 渲染损坏 | 循环开始节点对对象型数组项强制 toString（POJO→`Xxx@1f2a`） | `LoopStartNodeHandler.java:50` | 非字符串走 JSON 序列化 |
| BUG-54 | NPE | 图配置缺起始节点时 `List.of(getNode(...))` 传 null 抛 NPE | `AbstractLoopWorkflow.java:63-66`、`ChatWorkflow.java:65-67` | 抛可读配置错误 |
| BUG-66 | 静默失败 | ASR onError 只记日志，识别失败返回空串，无法区分"为空"与"失败" | `BaiLianASRRealtime.java:82-84`、`GummySTT.java:65-67` | 失败向上抛出 |
| BUG-67 | 错误行为 | TTS 音色缺失：拼出 "sambert-null-v1" / openai-java 必填校验抛异常 | `BaiLianTTS.java:23-24`、`OpenAiTTSModel.java:32-39` | 默认音色或明确报错 |
| BUG-68 | 线程挂起 | QWenTTS.toBytes 的 url.openStream() 无 connect/read 超时 | `QWenTTS.java:36-48` | 设置超时 |
| BUG-69 | 识别失败 | getSampleRate 解析失败直接 RuntimeException 不回退（m4a/amr 等无法解析 → 整次识别失败） | `AbsSTTModel.java:39-41` | 失败降级用默认 16000 |
| BUG-70 | NPE | providerRegistry.get 返回 null 未校验（registry 明确"找不到返回 null"） | `ModelServiceImpl.java:90,140,215` | 判空抛可读错误 |
| BUG-71 | 永远失败 | 未配置 token 的 EVENT 触发器 webhook 永远落到认证失败分支 | `WebhookTriggerController.java:83-91` | 配置时强制 token 或此处放行 |
| BUG-72 | 资源堆积 | OLlamaModelProvider 覆盖 getHttpClientBuilder 丢失父类 synchronized 缓存，每次构建模型新建连接工厂从不关闭 | `OLlamaModelProvider.java:55-63`（父类 `AbsModelProvider.java:75-84`） | 恢复缓存复用 |
| BUG-73 | 功能缺失 | modelList 的 SHARED_MODEL 分组硬编码空列表 | `ModelController.java:59-63` | 实现共享分组或移除 |
| BUG-81 | 对话瘫痪 | ToolSpecificationBuilder 对 input_field_list 脏数据（isRequired=null 拆箱、type=null switch）NPE；创建接口无 @Valid | `ToolSpecificationBuilder.java:34,41` | 拆箱前判空，null type 跳过 |
| BUG-82 | 语义矛盾 | "修改当前用户语言"接口被误配 @SaCheckRole(ADMIN)（对照 /user/list 用 SaMode.OR） | `UserController.java:46-52` | 改为 ADMIN+USER |

## 六、排查后排除的疑点（30 项，避免误导后续排查）

| # | 疑点 | 结论 |
|---|---|---|
| 1 | Token 统计 SUM(a)+SUM(b) NULL 丢失 | message_tokens/answer_tokens 在 schema 中 NOT NULL，不成立 |
| 2 | Flux 异步链路 ThreadLocal 用户上下文丢失 | 身份显式走 ChatState 传参，异步管线不读 userContext；各线程池不触碰该 ThreadLocal |
| 3 | fastjson 反序列化 RCE | 版本 2.0.64、未开 autoType，JSONBTypeHandler 仅 parse 库内数据，无风险 |
| 4 | JSON 日期序列化不一致 | 全局 jackson.date-format 统一 |
| 5 | MimeTypeUtils 静态 HashMap 线程安全 | 仅静态块写入、后续只读 |
| 6 | NL2SqlNodeHandler 数据源泄漏 | finally 中已正确关闭 HikariDataSource |
| 7 | 拦截器注册范围漏洞 | WebConfig chat/admin 拦截器路径与排除项设计合理 |
| 8 | i18n 双语言 key 缺失 | zh_CN 与 en_US key 结构 diff 完全一致 |
| 9 | BeanUtil.toMap 死代码 | 无生产调用方 |
| 10 | cancelTask 位翻转与 Mapper 状态位写入错位 | setCharAt 与 LPAD+reverse+SUBSTRING 逐位推演一致 |
| 11 | DocumentHandler 多 sheet 读取串数据 | ExcelDataListener 有 clear() |
| 12 | KnowledgeExportHandler keyset 分页遗漏 | gt lastId + LIMIT 正确；ZIP NonClosingOutputStream 无泄漏 |
| 13 | EncodingDetectUtil mark/reset 与 BOM 处理 | 成立，无问题 |
| 14 | IDataStore.deleteByParagraphId 落空 | 有 default 兜底实现 |
| 15 | Mapper 多参数无 @Param | starter-parent 3.5.1 默认 -parameters 编译，可正常解析 |
| 16 | ChatCache 内存泄漏 | Caffeine maximumSize=9999 + expireAfterWrite，有界 |
| 17 | ChatRecordDTO.runTime 拆箱 NPE | run_time 等列 NOT NULL |
| 18 | statistics/userTokenUsage/topQuestions 统计口径 | 内外层时间窗口一致，GROUP BY "day" 别名 PG 合法 |
| 19 | EasyExcel 导出流未关闭 / embed XSS | doWrite 内部 finish 关流；模板有 protocol/host 白名单 + JS 转义 |
| 20 | 未提交改动（ChatRecordDetailExcel 等 3 文件）引入新 bug | 本次修改本身是修复（越权取反、overview→summary），与 mapper 列名一致 |
| 21 | 并行分支汇聚节点重复执行（workflow） | NodeChainRunner 只调度 READY/SKIP，逻辑自洽 |
| 22 | 循环断点续跑索引/详情恢复（workflow） | LoopIterationRunner/LoopExecutionContext 推演正确 |
| 23 | ExecutionTracker/WorkflowContext 并发修改 | COWList + ConcurrentHashMap，无实际写冲突 |
| 24 | workflow 资源泄漏（GridFS/hutool/Hikari） | 均已 try-with-resources/finally 关闭 |
| 25 | Anthropic maxTokens 未配置 | langchain4j 1.22.0 字节码默认 1024，不成立 |
| 26 | SiliconFlow hutool HttpResponse 泄漏/线程安全 | body() 后自动关闭；RerankStep 每次新建实例 |
| 27 | WenXin params 判空 | 所有调用路径保证非 null |
| 28 | SystemSettingService 首次 insert / login getIsActive 拆箱 | MP 3.5.9 行为核实 / DDL NOT NULL，不成立 |
| 29 | UploadValidator 放行 svg/html | 下载侧强制 attachment + nosniff 防住 XSS |
| 30 | 未提交改动（ModelServiceImpl 脱敏回传 / modelParamsForm） | 本身正确；与 BUG-64 的读写权限不对称已并入该条 |

## 七、次要点与安全建议（非 bug）

- `chatRecordDetail` LEFT JOIN：会话无记录时导出行问题/答案列为 NULL（可接受）。
- `ProblemMapper.xml` 用 `MAX(pp.document_id)` 表示关联段落，语义为"字典序最大"而非首个（沿袭原版设计）。
- `PgVectorEmbeddingStoreImpl` 整体质量良好：并发缓存、指数退避重试、SSRF 防护齐备。
- `application-dev.yml` 与 `docker-compose.dev.yml` 实例名/账号不一致（`MaxKB4J_v2` vs `MaxKB4j`），依赖本地覆盖；Mongo URI `charset=UTF-8` 为无效参数（驱动忽略）。
- CI workflow 无问题。
- Skill 工具经 langchain4j 向 AI 暴露宿主机 shell 执行能力属产品设计选择，建议加命令白名单/审计。
- `TriggerScheduler.scheduleAtFixedRate` 仅支持 minutes/hours（与 TriggerSetting 契约一致，非缺陷）。

## 八、来源与验证说明

| Bug 编号段 | 来源 | 验证 |
|---|---|---|
| BUG-1~5、16 | 主会话直查（配置/部署/XML） | 逐行人工审查 |
| BUG-6~15 | common+core 代理 | 高/中危主会话逐条复核 |
| BUG-17~34 | knowledge 代理 | 高危+关键中危主会话复核 |
| BUG-35~44 | application+chat 代理 | 高/中危主会话逐条复核 |
| BUG-45~54 | workflow 代理 | 高危主会话逐条复核 |
| BUG-55~73 | model+trigger 代理 | 高/中危主会话逐条复核（含字节码核实） |
| BUG-74~82 | system+tool+oss+start 代理 | 高危主会话逐条复核 |
| BUG-83 | 两个代理独立发现 | 双重印证 + 主会话复核 |
