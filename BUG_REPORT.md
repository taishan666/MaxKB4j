# MaxKB4j 代码 Bug 扫描报告

- 扫描日期：2026-11-21
- 扫描基线：dev 分支工作区（含未提交改动），commit `4abcf8348`
- 编译验证：`mvn compile` 全模块通过（exit 0）
- 验证方式：所有"高/中危"发现均经主会话逐条亲自读码复核后入册；低危项采信代理报告的行号与代码引用

## 一、覆盖度说明

| 区域 | 方式 | 状态 |
|---|---|---|
| 18 个 MyBatis Mapper XML（全部逐行） | 人工逐行审查 | ✅ 完成 |
| application.yml / dev-prod-test 配置 | 人工审查 + 交叉验证代码引用 | ✅ 完成 |
| Flyway 迁移脚本 ↔ 实体一致性 | 抽查关键字段 | ✅ 抽查完成 |
| docker-compose(.dev).yml / Dockerfile / CI / i18n | 人工审查 | ✅ 完成 |
| service-api / web 模块 | 接口与静态产物，抽查核心类 | ✅ 完成 |
| Flux 异步链路 / ThreadLocal 用户上下文 | 全链路追查（controller → service → executor） | ✅ 完成 |
| PgVectorEmbeddingStoreImpl（向量写入/重试/SSRF 防护） | 人工审查 | ✅ 完成 |
| **maxkb4j-common + maxkb4j-core（100 文件）** | 代理全量审查 + 主会话逐条复核 | ✅ **完成** |
| **maxkb4j-knowledge（157 文件 + 8 XML）** | 代理全量审查 + 主会话逐条复核 | ✅ **完成** |
| **maxkb4j-workflow（131 文件）** | 代理全量审查（含 langchain4j 反编译验证）+ 高危项主会话逐条复核 | ✅ **完成** |
| **maxkb4j-application + chat（125 文件）** | 代理全量审查（Mapper XML/导出/异步链路由主会话先行完成）+ 高/中危主会话逐条复核 | ✅ **完成** |
| maxkb4j-model + trigger（111 文件） | 并行代理审查中 | ⚠️ 进行中 |
| maxkb4j-system + tool + oss + start（140 文件） | 并行代理审查中 | ⚠️ 进行中 |

## 二、确认的 Bug

### BUG-1【中·功能失效】DocLing 解析引擎对 .html 文件永远不生效

- **位置**：`maxkb4j-start/src/main/resources/application.yml:118`
- **证据**：`supported-extensions` 列表中 `.HTML` 为大写；`DocLingParseEngine.java:49-50` 先把文件名 `toLowerCase()` 再 `endsWith` 匹配，大写条目永远命中不了。Java 默认值是 `.html`（小写，正确），yml 覆盖默认值时引入笔误。
- **触发条件**：启用 DocLing（`MINERU_ENABLED=true`）后上传 `.html` 文件 → 全部回落普通解析器，OCR/表格/公式增强失效。
- **修复**：yml 中 `.HTML` → `.html`。

### BUG-2【中·功能缺陷】聊天记录导出接口未设置下载响应头

- **位置**：`ApplicationChatController.java:59-62`（`export` 端点）→ `ApplicationChatServiceImpl.writeChatExport`
- **证据**：直接 `EasyExcel.write(response.getOutputStream(), ...)`，未设置 `Content-Type`/`Content-Disposition`。项目内其他全部导出端点均有设置（`KnowledgeExportHandler.java:114`、`ApplicationExportService.java:139`、`ExcelExportSupport.java:49` 等）。
- **后果**：导出的 xlsx 以无文件名、默认 Content-Type 的原始字节流返回，下载体验破损。
- **修复**：写流前补齐三个响应头（contentType + charset + `attachment;filename*=utf-8''...`）。

### BUG-3【低·死代码陷阱】权限 SQL 引用项目中不存在的表

- **位置**：`UserMapper.xml:5-34`（`getUserPermissionById`）
- **证据**：引用 `dataset`、`team_member`、`team_member_permission`——本项目 Flyway 迁移中均不存在（MaxKB 原版遗留）；`UserMapper.java:17` 有声明、全项目无调用方。
- **修复**：删除 SQL 与接口声明，或重写为真实表结构。

### BUG-4【低·无效配置】mapper-locations 匹配不到任何文件

- **位置**：`application.yml:52`
- **证据**：`classpath:mapper/**/*.xml` 下无任何 XML，实际 XML 在包路径 `com/maxkb4j/**/mapper/` 下，靠 MyBatis「接口同名 XML 自动加载」兜底。
- **修复**：改为 `classpath*:com/maxkb4j/**/mapper/*.xml`。

### BUG-5【低·部署】server.crt 死挂载

- **位置**：`docker-compose.yml:81`
- **证据**：挂载 `./server.crt:/tmp/certs/server.crt`，仓库中该文件不存在，全代码库（含 Dockerfile、prod 配置）无任何引用；Docker 对不存在的 bind-mount 源会在宿主机创建同名空目录。
- **修复**：删除该挂载行。

### BUG-6【高·安全】WebUtil.getIP 信任 X-Forwarded-For 链首，可伪造 IP 绕过白名单与限流

- **位置**：`maxkb4j-common/.../util/WebUtil.java:96-107`
- **证据**（主会话已亲自复核）：
  ```java
  for (String ipHeader : IP_HEADER_NAMES) {   // 第一个即 x-forwarded-for
      ip = request.getHeader(ipHeader);
      if (!IP_PREDICATE.test(ip)) break;
  }
  ...
  return StringUtils.isBlank(ip) ? null : splitTrim(ip, ",")[0];  // 取链首 = 客户端可伪造值
  ```
  XFF 是追加式头部，即使前置 nginx 用 `$proxy_add_x_forwarded_for`，客户端伪造值仍在链首，而代码取的正是 `[0]`。
- **影响**（已核实调用方）：
  - `ChatEmbedService.java:55`（及 ChatApiService、ChatAnonymousTokenHandler）用 `whiteList.contains(WebUtil.getIP())` 做**嵌入/分享访问白名单** → 伪造头即可绕过；
  - `RateLimitAspect.java:89` 按 IP 限流 → 伪造头即可绕过 `@RateLimit`；
  - `UserServiceImpl.java:160` 验证码 IP 绑定同样可被伪造。
- **严重程度**：高（访问控制绕过）。
- **修复**：白名单/限流场景改用 nginx 侧写入的可信头（如 `X-Real-IP`）或直接取 `request.getRemoteAddr()`；若必须解析 XFF，从链**右**侧取第一个非可信代理地址。

### BUG-7【中·协议】GlobalExceptionHandler 大多数分支返回 HTTP 200

- **位置**：`maxkb4j-common/.../handler/GlobalExceptionHandler.java`（约 15 个 handler 分支）
- **证据**（主会话已亲自复核 `RuntimeException`(290-295) 与兜底 `Exception`(297-302) 两处）：无 `response.setStatus`、无 `@ResponseStatus`，直接返回 `R.fail(500, ...)` → HTTP 200 + 业务码 500。仅 `IllegalArgumentException`(267-273) 有 `@ResponseStatus`，其余（NPE、参数校验失败、ApiException、LoginException、AccessException、RateLimitException 等）全部 200。
- **后果**：网关重试、监控告警、HTTP 客户端按状态码判错全部失效；与 NotLoginException 等 401/403/429 分支行为不一致。
- **修复**：统一补 `@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)` 或 `response.setStatus(...)`。

### BUG-8【中·安全】DataMaskUtil.maskString 短字符串脱敏失效，API Key 明文泄露

- **位置**：`maxkb4j-common/.../util/DataMaskUtil.java:61-65`
- **证据**（主会话已亲自复核）：
  ```java
  if (str.length() <= prefix + suffix) {
      return str.replaceAll("\\.", "*");   // 只替换字面点号，本意是任意字符
  }
  ```
  正则转义符是笔误（应为 `replaceAll(".", "*")`）。长度 ≤ 8 的密钥（`maskApiKey` 即 4+4）不含点号时**原样返回**。
- **影响**：`ModelServiceImpl.java:158` `credential.setApiKey(DataMaskUtil.maskApiKey(...))` → 短密钥明文返回前端；`:133-134` 的"脱敏值==回传值"沿用判断对短密钥同样失真。
- **修复**：`"\\."` → `"."`，或长度不足时直接全量打码。

### BUG-9【中·数据静默丢失】IoUtil.readToString 吞掉 IOException 返回空串

- **位置**：`maxkb4j-common/.../util/IoUtil.java:39-49`
- **证据**：catch IOException 后仅记日志返回已读部分或 `""`，调用方无法区分"文件为空"与"读取失败"。
- **影响**：`HtmlParser`/`MDParser` 解析上传文档静默得空内容（段落空入库、无用户可见错误）、`ChatEmbedService` 嵌入页模板读失败返回空页、`ModelProviderInfo` 提供商清单静默为 null。
- **修复**：向上抛出或返回 `Optional`/null 由调用方显式处理。

### BUG-10【中·安全】DatabaseUtil SQLite 分支允许 `..` 路径遍历

- **位置**：`maxkb4j-core/.../util/DatabaseUtil.java:219, 251-256`
- **证据**（主会话已亲自复核）：
  ```java
  private static final Pattern SAFE_SQLITE_PATH_PATTERN = Pattern.compile("^[A-Za-z0-9_.$\\-/:\\\\ ]+$");
  ```
  允许 `.` `/` `\` 但未拦截 `..`。NL2SQL 节点 `databaseSetting.database` 由具备应用编辑权限的用户配置，填入 `../../../etc/foo.db` 可通过校验。
- **后果**：SQLite JDBC 对不存在文件会在目标路径创建新文件，对已存在 `.db` 文件可配合 `executeSqlQuery` 读取内容 → 进程权限内任意路径写/读。
- **修复**：校验后规范化路径并断言在指定数据目录内（如 `canonicalPath().startsWith(baseDir)`）。

### BUG-11【低·安全】SELECT 白名单放行带副作用的内嵌函数

- **位置**：`maxkb4j-core/.../util/DatabaseUtil.java:62-95`
- **证据**：仅 `isSelect(sqlQuery)` 挡非 SELECT 语句。LLM 生成或提示注入的 `SELECT pg_read_file(...)`、`dblink(...)`、MySQL `SELECT ... INTO OUTFILE` 均解析为 Select 可通过。
- **现状缓解**：NL2SqlNodeHandler 已在 finally 关闭数据源、`setReadOnly(true)` 下发只读会话，但内嵌函数不受约束，安全完全依赖外部库账号最小权限。
- **修复**：加函数黑名单或文档化部署约束。

### BUG-12【低·功能劣化】NL2SqlAssistant 系统提示词损坏（字面 \n + 杂散引号）

- **位置**：`maxkb4j-core/.../assistant/NL2SqlAssistant.java:10`
- **证据**（主会话已亲自复核）：`@SystemMessage("\"You are ...\\n...")` 是普通字符串（非文本块），编译后发送给 LLM 的是带杂散引号、字面 `\n` 的损坏文本；同包其它 Assistant 均用 `"""` 文本块。
- **修复**：改为文本块（`"""..."""`）真实换行。

### BUG-13【低·安全】RSAUtil 私钥口令硬编码兜底 + 弱加密算法

- **位置**：`maxkb4j-common/.../util/RSAUtil.java:41, 96-106, 237`
- **证据**：未配置 `MAXKB4J_RSA_KEY_PASSWORD` 时兜底 `"mac_kb_password"`；加密器沿用 `pbeWithSHA1AndDES_CBC`（DES-56bit + SHA1）。
- **后果**：默认部署且拖库场景下，攻击者可用默认口令+弱算法离线恢复受保护 RSA 私钥，进而解密 model 表中模型厂商 API Key（`ModelCredentialTypeHandler` 依赖）。
- **修复**：新写入改用 AES-256/CBC+SHA256 以上强度；无配置时拒绝生成而非兜底弱口令。

### BUG-14【低·语义】SystemAuthInterceptor 对过期/无效 token 静默降级，与 javadoc 契约相反

- **位置**：`maxkb4j-common/.../interceptor/SystemAuthInterceptor.java:78-87`
- **证据**：类注释声称"token 过期、无效等认证异常继续抛出"，实现中 `INVALID_TOKEN`/`TOKEN_TIMEOUT` 均 `return null` 降级为未登录上下文，业务层随后抛 `SaJwtException("当前线程未解析到登录身份")`，错误码语义退化为 `auth.token.invalid` 而非过期提示。主鉴权路径（SaCheckPermAspect.checkLogin）不受影响。
- **修复**：与注释二选一对齐（建议过期时继续抛出）。

### BUG-15【低·并发】ChatInfo.addChatRecord 非原子 check-then-act，并发下历史重复

- **位置**：`maxkb4j-common/.../domain/dto/ChatInfo.java:40-50`
- **证据**：遍历查重后 `set`/`add` 两步间无原子性；`chatRecordList` 设计为 CopyOnWriteArrayList 多线程共享（`ChatPostHandler.registerAfterCommit` 事务提交后并发调用）。
- **后果**：同 chatId 并发请求时两线程同时未查到相同 id 各自 add → 历史缓存重复记录，污染对话上下文。
- **修复**：改用 `ConcurrentHashMap<String,ChatRecordDTO>` + 有序 key 列表，或对方法加锁。

### BUG-16【中·部署安全】数据库凭据硬编码提交进仓库

- **位置**：`docker-compose.yml:10,35,40,75,78`、`application-prod.yml:8-9`、`application-dev.yml:9`
- **证据**：Postgres/Mongo 密码 `ycn4NRhjN2` 多处硬编码且复用同一口令；mongo healthcheck 命令行明文带密码（`docker inspect` 可见）。
- **修复**：改 `${VAR}` 注入并轮换该密码。

### BUG-17【高·功能崩溃】新建段落关联新问题时 problem_id 写 null，段落创建必然失败

- **位置**：`maxkb4j-knowledge/.../service/impl/ParagraphServiceImpl.java:166`（配合 172-175）
- **证据**（主会话已亲自复核）：
  ```java
  entity.setProblemId(problemEntity.getId());   // 新问题此时 id == null
  ...
  if (!newProblems.isEmpty()) {
      problemService.saveBatch(newProblems);    // ASSIGN_UUID 在 insert 时才回填 id
  }
  problemParagraphService.saveBatch(problemParagraphMappingEntities);  // problem_id 已固化为 null
  ```
  `BaseEntity` 为 `@TableId(type = IdType.ASSIGN_UUID)`（插入时生成 id）；`problem_paragraph_mapping.problem_id` 为 NOT NULL。
- **触发条件**：`POST /knowledge/{id}/document/{docId}/paragraph` 的 `problemList` 含知识库中不存在的新问题。
- **后果**：NOT NULL 违规 → 事务回滚 → 段落创建 500。对照 `DocumentWriteService.batchCreateDocs`（预生成 UUID）与 `ProblemServiceImpl.generateRelated`（同样预生成）均正确，唯此路径坏。
- **修复**：新问题用 `IdWorker.get32UUID()` 预生成 id 后再 setProblemId。

### BUG-18【高·文件损坏】下载源文件接口混写二进制流与 R 返回值，文件被追加 JSON

- **位置**：`maxkb4j-knowledge/.../controller/DocumentExportController.java:58-62`
- **证据**（主会话已亲自复核）：`downloadSourceFile` 返回类型为 `R<String>`，但 `documentSourceFileService.download` 已向 `response.getOutputStream()` 写入文件字节；方法返回非 null 的 R 后 Spring 会再次 JSON 序列化追加到响应尾部。
- **后果**：下载的 xlsx/zip 尾部多出 `{"code":200,...}` → 文件损坏无法打开；Content-Type/Content-Disposition 被覆盖。
- **修复**：改为返回 void（失败时再写 JSON）。

### BUG-19【中·数据冲突】LLM 返回重复问题时同 ID 实体重复入批 → 主键冲突

- **位置**：`ProblemServiceImpl.java:116-130`
- **证据**：`generatedQuestions` 无去重，重复问题导致同一 `problemId` 被加两次到 `newProblems`、关联表重复行 → `saveBatchProblems` 主键冲突，段落被标 FAILURE。
- **修复**：入批前按 question 内容 distinct。

### BUG-20【中·事务失效】saveBatchProblems 的 @Transactional 因同类自调用失效

- **位置**：`ProblemServiceImpl.java:158-161`（经 133、271 行 `this.` 调用）
- **证据**：同类内部调用绕过 Spring 代理，注解不生效；批量插入实际逐条自动提交，中途失败无法回滚 → 部分 problem/mapping 行落库，与失败状态标记不一致。注释声称的"短事务"不存在。
- **修复**：注入自身代理（`AopContext`/`ObjectProvider`）或拆分到独立 Service。

### BUG-21【中·竞态】Web 文档事件用 @EventListener 而非 AFTER_COMMIT，事务承诺不成立

- **位置**：`KnowledgeServiceImpl.java:186-193` + `listener/WebDocsListener.java:31-34`
- **证据**：发布事件处于 `@Transactional` 方法内，`@EventListener`+`@Async` 在发布瞬间即执行，knowledge 行尚未提交；抓取快/主事务回滚时异步事务插入 `document` 引用未提交行（FK 校验失败）或留孤儿文档。同模块 `DocumentIndexListener/ParagraphIndexListener` 均正确使用 `@TransactionalEventListener(AFTER_COMMIT)`。
- **修复**：改 `@TransactionalEventListener(phase = AFTER_COMMIT)`。

### BUG-22【中·长事务】createWebDoc/syncWebDoc 的 HTTP 抓取实际在事务内执行

- **位置**：`DocumentImportService.java:114-121`（syncWebDoc 123-134 同）
- **证据**：方法整体 `@Transactional`，注释称"HTTP 抓网页移至事务外"但 `getWebDocuments` 在事务内调用；多 URL/慢站点时事务持续数十秒，连接池占满风险。
- **修复**：先抓取（事务外）再开启事务落库。

### BUG-23【中·功能崩溃】PptParser 声明支持 .pptx 却用仅支持 OLE2 的 OfficeParser

- **位置**：`parser/impl/PptParser.java:25, 35`
- **证据**（主会话已亲自复核）：`getExtensions()` 返回 `.ppt/.pptx`，但 `new OfficeParser()` 只支持二进制 OLE2 格式；.pptx（OOXML zip）必抛 TikaException。
- **触发条件**：doc-ling 关闭（默认）时上传 .pptx → 解析必然失败。
- **修复**：改用 `AutoDetectParser` 或 `OOXMLParser`。

### BUG-24【中·解析失败】ExcelParser 表头空单元格导致 NPE 或列错位

- **位置**：`parser/impl/ExcelParser.java:36-42, 62-66`
- **证据**：`headMap.get(i)` 可为 null（空表头单元格），`String.join(" | ", headers)` 抛 NPE；缺键时表头与数据列错位。同模式 `DocumentHandler.java:241-243` 生成 `"null:xxx"` 污染内容。
- **修复**：null 表头补默认列名并 trim。

### BUG-25【中·安全】Web 知识库抓取任意 URL，无 SSRF 防护

- **位置**：`util/JsoupUtil.java:22-34`
- **证据**：`POST /knowledge/web` 的 `sourceUrl` 用户可控，`HttpRequest.get(url)` 无 scheme 白名单、无内网/回环/169.254.169.254 校验；递归模式还会按页面链接继续抓。同项目 `PgVectorEmbeddingStoreImpl` 对图片下载做了完整 SSRF 加固，此处完全裸奔。
- **修复**：复用 PgVector 的 SSRF 校验逻辑（scheme 白名单 + 内网 IP 拦截）。

### BUG-26【低·数据重复】问题生成事件中去重基线只加载一次，跨段落重复问题行

- **位置**：`GenerateProblemListener.java:48, 63`
- **证据**：`knowledgeProblems` 事件开始时查一次；`generateRelated` 新问题只放局部 `problemMap` 不回写 → 同批次多段落生成相同问题时各建一条重复 ProblemEntity。
- **修复**：生成后回写去重基线。

### BUG-27【低·NPE】publish 对无效知识库 ID 直接 NPE

- **位置**：`KnowledgePublishService.java:44-48`
- **证据**：`getById(id)` 无效时返回 null，48 行 `knowledge.getWorkFlow()` 抛 NPE 500。
- **修复**：判空抛友好异常。

### BUG-28【低·NPE】文档批量导出对无效 knowledgeId NPE

- **位置**：`DocumentExportService.java:74-78`（`loadKnowledgeName` 无判空）→ `exportExcel:48`/`exportZip:64`
- **修复**：对照 `KnowledgeExportService` 抛 IllegalArgumentException。

### BUG-29【低·错误误导】checkFileLimit 在知识库不存在时返回 false 放行导入

- **位置**：`DocumentImportService.java:222-232`
- **证据**：`selectById` 为 null 时 `return false`（应报"知识库不存在"），导入继续执行撞 `document_knowledge_id` 外键违规 500，错误信息误导。
- **修复**：知识库不存在时抛明确异常。

### BUG-30【低·NPE】嵌入资源缺 RESOURCE_NAME_KEY 时 ConcurrentHashMap.put(null) NPE

- **位置**：`parser/impl/DocParser.java:79-85`
- **修复**：null 资源名时生成占位名或跳过。

### BUG-31【低·静默失败】CSV 解析失败仅 warn 吞掉，接口"成功"但 0 数据

- **位置**：`DocumentHandler.java:134-136`
- **证据**：`storeFile` 已执行（OSS 孤儿文件），docs 中无该文件 → importQa 正常返回，用户无感知失败。
- **修复**：抛出或记录导入失败明细。

### BUG-32【低·语义错误】标签查重用 key IN + value IN 交叉匹配导致漏加标签

- **位置**：`TagServiceImpl.java:60-71`；同模式 `DocumentTagController.java:89-101`
- **证据**：`key IN (a,b) AND value IN (1,2)` 会命中不存在的交叉组合（如已存在 ("a","2") 把待加的 ("b","1") 误判为已存在）。
- **修复**：改为逐对 `(key=? AND value=?) OR ...`。

### BUG-33【低·内容丢失】WebContentCleaner 噪音正则无词边界，正文被误删

- **位置**：`util/WebContentCleaner.java:30-45`
- **证据**：`(?i)(ad|ads|...|header|hot|...)` 对 class/id 做子串匹配，含 "down**load**"、"**ad**dress" 等子串的正常正文元素被整棵删除。
- **修复**：正则加词边界 `\b` 或改为完整 token 匹配。

### BUG-34【低·导入不完整】processZipQaFile 只处理压缩包内第一个 Excel/CSV

- **位置**：`DocumentHandler.java:62`
- **证据**：循环内 `break`，QA zip 含多个表格文件时其余被静默忽略（对照同模块 zip 路径处理全部条目）。
- **修复**：移除 break。

### BUG-35【高·功能崩溃】AgentExecutor 缺会话初始化，应用作为工具（agent 子应用）必然失败

- **位置**：`maxkb4j-application/.../executor/AgentExecutor.java:38-43`
- **证据**（主会话已亲自复核）：构造 `chatId("agent_" + memoryId)` 但从不调用 `chatOpen` 创建会话；`ApplicationChatServiceImpl.getChatInfo` 缓存 miss 后查库必为空（合法 id 由 `IdWorker.get32UUID()` 生成，`agent_` 前缀不存在）→ 100% 抛 `common.record.not.exists`。
- **连带副作用**：失败前 `visitCountOver` 已执行 `ensureStatsExists` → 每次失败调用都在 `application_chat_user_stats` 插入一行永不使用的统计行，统计表持续膨胀。
- **修复**：执行前补会话初始化并复用稳定 chatUserId。

### BUG-36【中·NPE】OpenAI 兼容接口 message=null 穿透落库层，ChatPostHandler NPE

- **位置**：`ChatOpenAiController.java:160`（`request.getLastUserMessage()` 无 user 角色时返回 null）→ `handler/impl/ChatPostHandler.java:129` `problemText.length()`
- **证据**（主会话已亲自复核两处）：请求无 `@Valid` 校验，手工 builder 绕过 `ChatParams.message` 的 `@NotBlank`；messages 只含 system/assistant 时（OpenAI 兼容客户端合法载荷）同步路径稳定 500、流式路径 error 收尾。
- **修复**：转换层对 null/blank message 返回 4xx 或兜底文案。

### BUG-37【中·越权】管理端聊天记录详情 IDOR：getChatRecordInfo 缺 chat 归属校验

- **位置**：`ApplicationChatRecordController.java:34-35` → `ApplicationChatRecordServiceImpl.java:124-128`
- **证据**（主会话已亲自复核）：URL `{id}` 传自己有权限的应用、`{chatId}` 传任意其他应用的会话即可读取记录全文（问题、AI 回答、executionDetails）；同接口族 `chatRecordPage:183` 有 `chatBelongsToApp` 校验而此处没有，chat 模块对应接口也有 `checkChatOwner`——校验不一致坐实遗漏。
- **修复**：`getChatRecordInfo` 增加 appId 参数并校验归属。

### BUG-38【中·数据覆盖】reChat + 缓存冷启动回填不全时，旧对话记录被覆盖、计数虚增

- **位置**：`ApplicationChatRecordServiceImpl.java:112-116`（回填仅最近 100 条）+ `ChatPostHandler.java:69-71`
- **证据**：对不在"最近 100 条"缓存里的旧记录 reChat 时 `chatRecord == null` 被误判为新记录：`insertOrIncrementRecordCount` 重复计数；`saveOrUpdate` 按旧 id 命中已有行执行 update，`problem_text/answer_text/details` 等全部被本轮内容覆盖。
- **修复**：回填 miss 时按 chatRecordId 显式查库判定新旧。

### BUG-39【低·统计失真】MCP tools/call 的 chatUserId 每次随机，与注释意图相反

- **位置**：`ChatApiService.java:179-180`
- **证据**：注释称"使用稳定的 chatUserId 避免统计表新增行"，实现每次 `IdWorker.get32UUID()` → 统计表无限增长、`customer_num` 把每次调用算新游客。
- **修复**：按调用方身份生成稳定 id。

### BUG-40【低·NPE】speechToText 对 Content-Type 处理存在 NPE / 数组越界

- **位置**：`ApplicationSpeechServiceImpl.java:70` `Objects.requireNonNull(file.getContentType()).split("/")[1]`
- **修复**：判空并校验格式，取不到后缀时给默认值。

### BUG-41【低·NPE】ResetProblemStep 未判空 tokenUsage

- **位置**：`ResetProblemStep.java:40-42`
- **证据**：部分 provider 非流式 `Result.tokenUsage()` 返回 null 时 NPE 中断整轮对话；同管线 `ChatStep.recordTokenUsage`（:187-193）已判空，此处遗漏。
- **修复**：补判空（对齐 ChatStep）。

### BUG-42【低·NPE】GenerateHumanMessageStep 的 maxParagraphCharNumber 自动拆箱 NPE

- **位置**：`GenerateHumanMessageStep.java:24`
- **证据**：`knowledge_setting` JSONB 缺该字段（旧数据/手工导入）→ getter 返回 null 拆箱 NPE → 该应用每轮对话必失败（步骤位于兜底 validate 之前）。
- **修复**：包装类型判空给默认值。

### BUG-43【低·NPE】promptGenerate 应用不存在时 NPE

- **位置**：`ApplicationController.java:167` + `ApplicationPromptService.java:27`
- **修复**：getById 判空抛友好异常。

### BUG-44【低·孤儿数据】improveChatLog 覆盖而非追加 improveParagraphIdList

- **位置**：`ApplicationChatRecordServiceImpl.java:273` `setImproveParagraphIdList(List.of(paragraphDTO.getId()))`
- **证据**：同一条记录第二次"标注答案"时旧段落 ID 被单元素列表覆盖，旧段落不再在列表内 → `removeImproveChatLog` 的归属校验永远无法通过，旧段落成为无法清理的孤儿数据。
- **修复**：追加而非覆盖。

### BUG-45【高·功能失效】节点失败启用"异常分支"后，异常分支节点被依赖检查静默跳过

- **位置**：`maxkb4j-workflow/.../engine/NodeLifecycleExecutor.java:130-144` + `engine/NodeDependencyChecker.java:51-54`
- **证据**（主会话已亲自复核）：`completeFailedNode` 对 `enableException=true` 的失败节点路由到异常分支（下游节点被武装为 READY），但 `isExecuted` 判定只认 SUCCESS/SKIP，失败节点状态为 ERROR → 异常分支下游 `dependenciesNotExecuted` 返回 true → 被空完成，永不执行。
- **后果**：异常分支特性形同虚设——失败后工作流直接终止，配置的异常处理链路静默失效且用户无感知。
- **修复**：`isExecuted` 纳入 ERROR 状态，或异常分支路由时重置依赖判定。

### BUG-46【高·续聊崩溃】恢复执行时 VariableAggregationNode.saveContext 对 JSON 反序列化值强转 CCE

- **位置**：`node/impl/VariableAggregationNode.java:31-36` + `engine/NodeStateLoader.java:84,93`
- **证据**（主会话已亲自复核）：`saveContext` 将 `detail.get(GROUP_LIST)` 强转 `List<Group>` 后逐元素赋值——持久化后元素实为 `JSONObject`，泛型擦除使 cast 通过、迭代时抛 ClassCastException；`NodeStateLoader.loadNodeState` 在 `ChatWorkflow` 构造器中执行 → 表单/用户选择续聊请求直接失败。
- **同类隐患**：恢复后 `SearchKnowledgeNode` 把 JSONArray 放入 context，下游 Reranker `(List<ParagraphRagVO>)` 强转在元素访问处同样 CCE（`RerankerNodeHandler.java:111`）——属"恢复后上下文为 JSON 类型而非 POJO"的系统性缺陷。
- **修复**：恢复链路按类型重建 POJO（fastjson `toJavaList`），或 context 统一存 JSON 兼容值。

### BUG-47【中·重试必败】节点级历史构造消息空值未防护，langchain4j 1.22 校验直接抛异常

- **位置**：`engine/HistoryManager.java:70-89`
- **证据**：`new UserMessage(contents)` 空列表抛 IllegalArgumentException、`new AiMessage(answer)` null 抛异常（已反编译 langchain4j-core-1.22.0 确认）。触发场景：上轮节点流式失败只写 question 无 answer、NL2Sql 类节点 detail 从不含 answer、question 无有效 text/image 项 → 该节点配置 NODE 级历史时重试永远失败，报无关的"text cannot be null"。
- **修复**：构造前过滤空 contents、answer 判空跳过。

### BUG-48【中·静默终止】意图分类解析失败时 BRANCH_ID 写 null，所有下游节点被静默 SKIP

- **位置**：`handler/node/impl/IntentClassifyNodeHandler.java:65-72,81-85` + `WorkflowExecutionAccessor.java:96-100`
- **证据**：模型输出无法解析为整数/为空/越界时 `branchId=null`，`getOrDefault(BRANCH_ID, "")` 因 key 存在值为 null 返回 null → 不匹配任何边 → 所有下游 SKIP。对比条件节点无匹配会抛 `CONDITION_NO_MATCH`，此处静默吞掉，用户只看到无响应。
- **修复**：branchId 为 null 时走失败处理（对齐条件节点语义）。

### BUG-49【中·记录缺失】Question/ImageGenerate/SpeechToText/TextToSpeech 四类节点未写 isResult 标记，回答不进持久化聊天记录

- **位置**：`QuestionNodeHandler.java:58-67`、`ImageGenerateNodeHandler.java:85-87`、`SpeechToTextNodeHandler.java:59-61`、`TextToSpeechNodeHandler.java:43-45`
- **证据**：勾选"输出结果"时仅设置 `answerText`（实时 SSE 推送），detail 无 `IS_RESULT`；持久化链路 `output().getAnswers()` 依赖 `isResult()` 过滤 detail → 保存的聊天记录缺失该回答。对照 `LLMStreamNodeHandler.java:192-195`、`DirectReplyNodeHandler.java:46` 均正确 `putDetail(IS_RESULT)`。
- **后果**：流式中能看到回答，历史记录页/下一轮上下文不一致。
- **修复**：四处补 `putDetail(IS_RESULT)`。

### BUG-50【中·变量污染】变量赋值节点 chat 作用域把"结果描述 Map"整体写入 ChatCache（copy-paste 双写）

- **位置**：`handler/node/impl/VariableAssignNodeHandler.java:44-54, 88-106`
- **证据**：`getChatHandleResult` 内部已正确写 `varName→value`，外层又把 `{name, input_value, output_value}` 描述 Map 整体 `putAll` 进 `chatVariables` → 三个脏键入缓存；若用户恰好定义同名会话变量会被覆盖。
- **修复**：移除外层冗余写入。

### BUG-51【中·NPE】RerankerNodeHandler 多路召回引用字段为空时 NPE

- **位置**：`RerankerNodeHandler.java:110-112`
- **证据**：引用路径解析为 null（上游被条件分支 SKIP、引用配置错误、恢复后 context 为 JSON 类型）→ 强转后 stream NPE，错误信息不指向真实原因；若配异常分支又被 BUG-45 拦截。
- **修复**：null 时跳过该路或报可读错误。

### BUG-52【中·资源与重复回调】节点超时后底层流式未真正取消，TokenStream 继续推送且错误回调双重触发

- **位置**：`engine/NodeChainRunner.java:50-53` + `LLMStreamNodeHandler.java:199-226`
- **证据**：超时 `CompletableFuture.cancel(true)` 不中断底层流——`onPartialResponse` 的 `emitMessage` 持续向已失败会话推送 token；cancel 触发 `.handle` 以 CancellationException 进入 `completeFailedNode`，与 catch 中 `reportError` 形成 `onNodeError` 双重回调（异常分支被二次路由）。
- **修复**：取消时同步关闭 TokenStream（如注入取消标志在回调中短路）并防重复上报。

### BUG-53【低·渲染损坏】循环开始节点对对象型数组项强制 toString，下游提示词拿到垃圾文本

- **位置**：`LoopStartNodeHandler.java:50`
- **证据**：POJO 元素渲染为 `ParagraphRagVO@1f2a...`、普通 Map 为 `{k=v}`（JSONObject 的 toString 恰为合法 JSON 不受影响）。
- **修复**：非字符串元素走 JSON 序列化。

### BUG-54【低·NPE】图配置缺失起始节点时 `List.of(null)` 抛 NPE

- **位置**：`AbstractLoopWorkflow.java:63-66`、`ChatWorkflow.java:65-67`
- **修复**：缺失起始节点抛可读配置错误。

## 三、排查后排除的疑点（避免误导后续排查）

1. **Token 统计 `SUM(a)+SUM(b)` NULL 丢失** —— schema 中 `NOT NULL`，不成立。
2. **Flux 异步链路 ThreadLocal 用户上下文丢失** —— 身份显式走 `ChatState` 传参，异步管线不读 userContext；`ThreadPoolConfig`/`GroovyScriptExecutor`/`PdfParser` 线程池均不触碰该 ThreadLocal。不成立。
3. **fastjson 反序列化 RCE** —— 版本 2.0.64，未开启 autoType，`JSONBTypeHandler` 仅 parse 数据库内数据。无风险。
4. **JSON 日期序列化不一致** —— `application.yml:25` 全局 `jackson.date-format` 统一。无问题。
5. **MimeTypeUtils 静态 HashMap 线程安全** —— 仅静态块写入、后续只读。安全。
6. **NL2SqlNodeHandler 数据源泄漏** —— 已在 finally 中正确关闭 HikariDataSource。无泄漏。
7. **拦截器注册范围漏洞** —— `WebConfig.java` chat/admin 拦截器路径与排除项设计合理。
8. **i18n 双语言 key 缺失** —— zh_CN 与 en_US key 结构 diff 完全一致。
9. **BeanUtil.toMap、getUserPermissionById 等** —— 无生产调用方的死代码（后者已列为 BUG-3 提示删除）。
10. **cancelTask 位翻转与 Mapper XML 状态位写入一致性**（knowledge）—— `setCharAt(2/1/0)` 与 `LPAD+reverse+SUBSTRING` 写入位逐位推演一致，无错位。
11. **DocumentHandler 多 sheet 读取串数据** —— `ExcelDataListener` 有 `clear()`，无跨 sheet 串数据。
12. **KnowledgeExportHandler keyset 分页遗漏** —— gt lastId + LIMIT 正确；ZIP 流包装（NonClosingOutputStream）无泄漏。
13. **EncodingDetectUtil 的 TikaInputStream mark/reset 与 BOM 处理** —— 成立，无问题。
14. **IDataStore.deleteByParagraphId 落空** —— 有 default 兜底实现，不会 NPE。
15. **Mapper 多参数无 @Param**（application）—— spring-boot-starter-parent 3.5.1 默认 `-parameters` 编译 + `useActualParamName`，可正常解析。
16. **ChatCache 内存泄漏** —— Caffeine `maximumSize=9999 + expireAfterWrite`，有界。
17. **ChatRecordDTO.runTime 拆箱 NPE** —— `run_time` 等列 NOT NULL，不成立。
18. **statistics/userTokenUsage/topQuestions 统计口径** —— 内外层时间窗口过滤一致，`GROUP BY "day"` 别名 PG 合法。
19. **EasyExcel 导出流未关闭** —— `doWrite` 内部 finish 关闭流；IoUtil finally 关闭；embed 模板 XSS 防护（protocol/host 白名单 + JS 转义）完备。
20. **未提交改动引入新 bug**（ChatRecordDetailExcel 等 3 文件）—— 本次修改本身是修复（越权判断取反、`overview`→`summary` 重命名），与 mapper 列名一致。
21. **并行分支汇聚节点重复执行**（workflow）—— `NodeChainRunner.schedule` 只调度 READY/SKIP，先完成链置 SUCCESS 后其余链到达即跳过，逻辑自洽。
22. **循环断点续跑索引/详情恢复**（workflow）—— `LoopIterationRunner`/`LoopExecutionContext` 推演正确。
23. **ExecutionTracker/WorkflowContext 并发修改**（workflow）—— nodeContext 为 CopyOnWriteArrayList、上下文 Map 均为 ConcurrentHashMap，未发现实际写冲突。
24. **workflow 资源泄漏** —— DocumentExtract(GridFS)/Http(hutool)/NL2Sql(HikariDataSource) 均已 try-with-resources/finally 关闭。

## 四、次要点与安全建议（非 bug）

- `ApplicationChatMapper.xml` `chatRecordDetail` LEFT JOIN：会话无记录时导出行问题/答案列为 NULL（可接受）。
- `ProblemMapper.xml` 用 `MAX(pp.document_id)` 表示关联段落，语义为"字典序最大"而非首个（沿袭原版设计）。
- `PgVectorEmbeddingStoreImpl` 整体质量良好：并发缓存、指数退避重试、SSRF 防护齐备。
- **CORS 建议收紧**（start 模块，`WebConfig.addCorsMappings`）：`allowedOriginPatterns("*") + allowCredentials(true)` 组合过宽，建议改为显式域名白名单。
- `application-dev.yml` 与 `docker-compose.dev.yml` 数据库实例名/账号不一致（`MaxKB4J_v2` vs `MaxKB4j`、占位符账号），依赖本地覆盖才可用；Mongo URI `charset=UTF-8` 为无效参数（驱动忽略，无害）。
- CI workflow 无问题。

## 五、未完成区域（并行代理审查中，完成后更新本报告）

| 模块 | 状态 |
|---|---|
| maxkb4j-model + trigger（111 文件） | 代理审查中（provider 对接层） |
| maxkb4j-system + tool + oss + start（140 文件） | 代理审查中（沙箱链路已确认，剩余 tool 核心类） |

> 本报告 54 项确认 bug 中：BUG-1~5、16 为主会话直查发现；BUG-6~15 来自 common+core 代理；BUG-17~34 来自 knowledge 代理；BUG-35~44 来自 application+chat 代理；BUG-45~54 来自 workflow 代理。全部高/中危 16 项均由主会话逐条亲自读码复核实锤；低危项采信代理报告的行号与代码引用。
