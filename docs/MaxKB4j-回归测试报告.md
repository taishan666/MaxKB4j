# MaxKB4j 回归测试报告

> 报告日期：2026-07-28
> 范围：工作流引擎核心流程 + 公共工具链 + 持久层 TypeHandler + 历史消息管理回归测试集
> 测试类型：纯单元测试（JUnit 5 + AssertJ + Mockito），不依赖数据库 / LLM / HTTP / Spring 容器

---

## 一、概述

本次为 MaxKB4j 项目补建了一份针对「关键流程」的回归测试集，目标是固化核心逻辑的预期行为，防止后续重构引入回归缺陷。

**结论摘要**

- 回归测试集现覆盖 **22 个测试类、158 个测试用例**，分布在 `maxkb4j-common` 与 `maxkb4j-service/maxkb4j-workflow` 两个模块。
- 第二轮扩展新增 **6 个测试类、57 个测试用例**：日期时间工具、空安全/类型判断、Bean 拷贝与分页、字符串列表 `TypeHandler`、历史消息管理。
- 全部用例 **通过**（0 失败、0 错误、0 跳过）。
- 编写与验证过程中发现并修复 **2 处实现层面的问题**（1 处契约违反、1 处不可达死代码），均附回归用例守护。

| 指标 | 数值 |
| --- | --- |
| 测试类数 | 22 |
| 测试用例总数 | 158 |
| 通过 | 158 |
| 失败 / 错误 / 跳过 | 0 / 0 / 0 |
| 通过率 | 100% |

---

## 二、测试环境

| 项 | 版本 / 说明 |
| --- | --- |
| 操作系统 | Windows |
| Shell | PowerShell |
| JDK | Oracle JDK 21.0.9 (LTS) |
| 构建工具 | Apache Maven 3.9.9 |
| 父 POM | spring-boot-starter-parent 3.5.1 |
| 测试框架 | JUnit 5 (Jupiter)，随 `spring-boot-starter-test` 引入 |
| 断言库 | AssertJ |
| Mock 库 | Mockito（inline mock maker，自附加 agent） |
| 关键业务依赖 | langchain4j 1.17.2、mybatis-plus 3.5.9、fastjson、commons-lang3 |

> 备注：`maxkb4j-workflow` 模块原先无测试依赖，本次在其 `pom.xml` 增补 `spring-boot-starter-test`（test scope）。

---

## 三、测试范围与策略

### 策略

- **纯单元测试**：直接 `new` 被测对象，避免启动 Spring 上下文，执行快、稳定、可重复。
- **隔离外部依赖**：不连接数据库、不调用 LLM、不发 HTTP；对 `Workflow` 接口使用 Mockito 打桩。
- **聚焦可测的纯逻辑**：选择无副作用的算法 / 解析 / 转换 / 校验类流程作为回归锚点。

### 覆盖的关键流程矩阵

| 关键流程 | 覆盖测试类 | 用例数 |
| --- | --- | --- |
| 条件分支比较算子（eq/ne/contain/数值/长度/空值/真假） | `CompareImplTest`、`CompareOperatorTest`、`CompareBuilderTest` | 19 |
| 条件断言组合（and/or、字段校验、未知算子降级） | `ConditionUtilTest` | 7 |
| 工作流变量解析（global/chat/loop/node 合并、引用字段） | `VariableResolverTest` | 6 |
| 模板渲染（`{{var}}` 替换、覆盖、空安全、缺省抛错） | `TemplateRendererTest` | 6 |
| 工作流上下文管理（节点追加/替换、查找、转发） | `WorkflowContextTest` | 3 |
| 工作流图导航（上下游边查找、空安全） | `EdgeNavigatorTest` | 4 |
| 节点生命周期与 CAS 抢占（防菱形汇聚重复执行） | `AbsNodeTest`、`NodeIdGeneratorTest` | 12 |
| 历史消息转换（成对裁剪、多模态、标签清理、最近 N 轮） | `MessageConverterTest` | 10 |
| 敏感数据脱敏（手机/身份证/邮箱/银行卡/API Key） | `DataMaskUtilTest` | 12 |
| 批量分片处理（999 分片、自定义批、Consumer/Function） | `BatchUtilTest` | 8 |
| 聊天渲染标签清理（tool_calls / form_render） | `RenderTagsTest` | 6 |
| 摘要与 MIME 映射 | `MD5UtilTest`、`MimeTypeUtilsTest` | 8 |
| 日期时间工具（格式化/解析/转换/下一周期时间点/参数校验） | `DateTimeUtilTest` | 19 |
| 空安全相等与简单类型判断（含基本类型数组） | `ObjectUtilTest` | 11 |
| Bean 拷贝 / 列表分页拷贝 / 对象转 Map | `BeanUtilTest` | 9 |
| 分页对象拷贝与记录类型转换 | `PageUtilTest` | 3 |
| 持久层字符串列表 TypeHandler（数组 <-> List） | `StringListTypeHandlerTest` | 8 |
| 历史消息管理（节点/全局装配、多模态、空安全） | `HistoryManagerTest` | 7 |
| **合计** |  | **158** |

---

## 四、测试结果总览

| 模块 | 测试类数 | 用例数 | 通过 | 失败 | 错误 | 跳过 |
| --- | --- | --- | --- | --- | --- | --- |
| `maxkb4j-common` | 11 | 94 | 94 | 0 | 0 | 0 |
| `maxkb4j-service/maxkb4j-workflow` | 11 | 64 | 64 | 0 | 0 | 0 |
| **合计** | **22** | **158** | **158** | **0** | **0** | **0** |

构建状态：`BUILD SUCCESS`（`mvn test` 退出码 0）。

---

## 五、详细用例清单

### 5.1 maxkb4j-common（94 用例）

#### `BatchUtilTest`（8）— `com.maxkb4j.common.util.BatchUtil`
| 用例 | 覆盖点 |
| --- | --- |
| protectBach_consumer_smallListExecutedAsSingleBatch | 小列表整批执行 |
| protectBach_consumer_customBatchSize | 自定义批大小分片（2/2/1） |
| protectBach_consumer_largeListSplitBy999 | 超过 999 自动分片（999/1） |
| protectBach_consumer_nullAndEmptyAreNoOps | null / 空列表为 no-op |
| protectBach_function_aggregatesResults | Function 变体结果聚合 |
| protectBach_function_largeListAggregatesInOrder | 大列表保序聚合 |
| protectBach_function_nullAndEmptyReturnEmpty | null / 空返回空 |
| protectBach_function_toleratesNullChunkResult | 子批返回 null 容错 |

#### `DataMaskUtilTest`（12）— `com.maxkb4j.common.util.DataMaskUtil`
| 用例 | 覆盖点 |
| --- | --- |
| maskMobile_masksMiddleFourDigits | 手机号 138****1234 |
| maskMobile_keepsInvalidLengthAndNullAsIs | 非法长度 / null 原样返回 |
| maskIdCard_keepsFirstThreeAndLastFour | 身份证前3 + 后4 |
| maskIdCard_keepsShortAndNullAsIs | 短串 / null 原样 |
| maskEmail_masksLongUsername | 长用户名脱敏 |
| maskEmail_collapsesShortUsername | 短用户名折叠为 * |
| maskEmail_keepsNullAndMissingAtAsIs | null / 缺 @ 原样 |
| maskBankCard_keepsFirstSixAndLastFour | 银行卡前6后4 |
| maskBankCard_keepsShortAndNullAsIs | 短串 / null 原样 |
| maskApiKey_keepsPrefixAndSuffix | API Key 前后缀保留 |
| maskString_keepsShortStringUnmaskedByStarsForDots | 通用短串处理 |
| maskString_masksMiddleForLongString | 通用长串中间掩码 |

#### `MD5UtilTest`（4）— `com.maxkb4j.common.util.MD5Util`
| 用例 | 覆盖点 |
| --- | --- |
| encrypt_matchesKnownDigest | MD5("hello") 标准值校验 |
| encrypt_isDeterministic | 同输入确定性 |
| encrypt_differsForDifferentInput | 不同输入差异 |
| encrypt_withRange_returnsSubstring | 区间截取 |

#### `MessageConverterTest`（10）— `com.maxkb4j.common.util.MessageConverter`
| 用例 | 覆盖点 |
| --- | --- |
| formatHistoryMessages_emptyOrNull | 空输入返回空 |
| formatHistoryMessages_singleTextContentSerializedAsString | 纯文本序列化为字符串 |
| formatHistoryMessages_multimodalContentSerializedAsList | 多模态序列化为内容数组 |
| formatHistoryMessages_dropsTrailingUnpairedMessage | 末尾不成对消息裁剪 |
| toHistoryMessages_nullReturnsEmpty | null 入参 |
| toHistoryMessages_skipsFormRenderAndStripsToolCallsRender | 跳过 form_render、剥离 tool_calls_render |
| toHistoryMessages_keepsOnlyLastRounds | 最近 N 轮截取 |
| lastRounds_handlesNullEmptyAndNonPositiveRounds | null/空/非正轮数 |
| lastRounds_returnsLastRoundsCopyAndClampsOverflow | 返回独立副本、溢出截断 |
| toChatMessageVO_populatesFieldsWithChildNode | VO 字段（含子节点）填充 |

#### `MimeTypeUtilsTest`（4）— `com.maxkb4j.common.util.MimeTypeUtils`
| 用例 | 覆盖点 |
| --- | --- |
| getMimeType_knownExtensions | 已知扩展名映射 |
| getMimeType_isCaseInsensitive | 大小写不敏感 |
| getMimeType_unknownExtensionReturnsDefault | 未知扩展名默认值 |
| getMimeType_nullOrEmptyReturnsDefault | null / 空默认值 |

#### `RenderTagsTest`（6）— `com.maxkb4j.common.util.RenderTags`
| 用例 | 覆盖点 |
| --- | --- |
| stripToolCallsRender_removesTagContent | 剥离 tool_calls 标签 |
| stripToolCallsRender_handlesMultilineWithDotall | 多行（DOTALL） |
| stripToolCallsRender_isNullSafe | null 安全 |
| stripToolCallsRender_keepsPlainContent | 纯文本原样 |
| containsFormRender_detectsTag | form_render 识别 |
| containsFormRender_isNullSafeAndNegative | null 安全与阴性 |

#### `DateTimeUtilTest`（19）— `com.maxkb4j.common.util.DateTimeUtil`
| 用例 | 覆盖点 |
| --- | --- |
| format_appliesCustomPattern | 自定义格式化 |
| parseDateTime_defaultFormatRoundTrips | 默认格式往返 |
| parseDateTime_withPattern | 自定义 pattern 解析 |
| parseDate_defaultFormat | 日期解析 |
| parseTime_defaultFormat | 时间解析 |
| toInstantAndToDateTime_roundTrip | Instant <-> LocalDateTime 往返 |
| toDateAndToLocalDate_roundTrip | Date <-> LocalDate 往返 |
| between_temporalReturnsDuration | 时间差 Duration |
| between_datesReturnsPeriod | 日期差 Period |
| getNextDay_returnsTomorrowAtGivenTime | 次日指定时间 |
| getNextDayAtTime_picksTodayOrTomorrowInFuture | 今天/明天取未来点 |
| getSameDayNextWeek_matchesDayOfWeekAndIsInFuture | 按周几取下一未来点 |
| getSameDayNextWeek_invalidDayThrows | 周几越界抛异常 |
| getSameDayNextMonth_matchesDayClampedToMonthLengthAndIsInFuture | 按日取下一月点（月末钳制） |
| getSameDayNextMonth_invalidDayThrows | 日越界抛异常 |
| getSameDayNextInterval_hoursAdvancesIntoFuture | 小时周期推进 |
| getSameDayNextInterval_minutesAdvancesIntoFuture | 分钟周期推进 |
| getSameDayNextInterval_invalidUnitThrows | 非法单位抛异常 |
| getSameDayNextInterval_invalidNumberThrows | 非数字抛 NumberFormatException |
#### `ObjectUtilTest`（11）— `com.maxkb4j.common.util.ObjectUtil`
| 用例 | 覆盖点 |
| --- | --- |
| nullSafeEquals_bothNullAreEqual | 双 null 相等 |
| nullSafeEquals_oneNullIsNotEqual | 单侧 null 不等 |
| nullSafeEquals_sameReferenceIsEqual | 同引用相等 |
| nullSafeEquals_equalAndUnequalStrings | 字符串等/不等 |
| nullSafeEquals_objectArrays | 对象数组比较 |
| nullSafeEquals_primitiveArrays | 基本类型数组比较 |
| nullSafeEquals_mixedArrayTypesAreNotEqual | 异构数组不等 |
| isSimpleType_nullIsFalse | null 判否 |
| isSimpleType_wrappersAndStringAreTrue | 包装类与 String 判是 |
| isSimpleType_enumIsTrue | 枚举判是 |
| isSimpleType_collectionAndCustomObjectAreFalse | 集合/数组/自定义对象判否 |
#### `BeanUtilTest`（9）— `com.maxkb4j.common.util.BeanUtil`
| 用例 | 覆盖点 |
| --- | --- |
| copy_createsNewInstanceWithCopiedProperties | 属性拷贝到新实例 |
| copyList_copiesAllElementsInOrder | 列表逐条拷贝保序 |
| copyList_nullAndEmptyReturnEmptyList | null / 空返回空 |
| copyList_withMapperTransformsElements | Function 映射 |
| copyPropertiesExcludeNull_keepsExistingTargetValuesForNullSource | null 属性不覆盖目标 |
| toMap_collectsNonNullNonBlankFieldsOnly | 仅收集非空非空白字段 |
| toMap_nullReturnsEmpty | null 入参返回空 |
| copyPage_preservesMetadataAndCopiesRecords | 分页元数据与记录拷贝 |
| copyPage_nullReturnsEmptyPage | null 返回空分页 |
#### `PageUtilTest`（3）— `com.maxkb4j.common.util.PageUtil`
| 用例 | 覆盖点 |
| --- | --- |
| copy_byClass_copiesRecordsAndMetadata | 按类型拷贝记录与元数据 |
| copy_byClassAndList_usesProvidedList | 使用传入列表拷贝 |
| copy_byMapper_transformsRecords | Function 转换记录 |
#### `StringListTypeHandlerTest`（8）— `com.maxkb4j.common.typehandler.StringListTypeHandler`
| 用例 | 覆盖点 |
| --- | --- |
| getNullableResult_byName_convertsArrayToList | 按列名转 List（含 null 元素） |
| getNullableResult_byIndex_convertsArrayToList | 按列序号转 List |
| getNullableResult_nullArrayReturnsNull | null 数组返回 null |
| getNullableResult_nullRawReturnsNull | 原始值 null 返回 null |
| getNullableResult_nonArrayRawBecomesSingleElementList | 非数组原始值单元素列表 |
| getNullableResult_callableStatement | CallableStatement 读取 |
| setNonNullParameter_bindsArrayFromList | List 写入绑定数组 |
| setNonNullParameter_nullListBindsNull | null 列表绑定 null |

### 5.2 maxkb4j-service/maxkb4j-workflow（64 用例）

#### `HistoryManagerTest`（7）— `com.maxkb4j.workflow.engine.HistoryManager`
| 用例 | 覆盖点 |
| --- | --- |
| constructor_nullHistory_initializesEmptyList | null 历史初始化空列表 |
| getHistoryMessages_nodeType_buildsMessagesFromStringQuestion | 节点级字符串问题装配 |
| getHistoryMessages_nodeType_buildsMultimodalFromListQuestion | 节点级多模态（文本+图片）装配 |
| getHistoryMessages_nodeType_skipsImageWithoutUrl | 缺 url 图片项跳过 |
| getHistoryMessages_nodeType_missingNodeReturnsEmpty | 缺失节点返回空 |
| getHistoryMessages_nodeType_absentQuestionStillRecordsAnswer | 无问题仍记录答案 |
| getHistoryMessages_workflowType_delegatesToGlobalHistory | 全局级历史委派 |

#### `CompareImplTest`（11）— `com.maxkb4j.workflow.compare.impl.*`
| 用例 | 覆盖点 |
| --- | --- |
| equalCompare | eq：等值 / null 处理 |
| notEqualCompare | ne：不等 |
| containCompare_string | contain：字符串包含 |
| containCompare_list | contain：集合包含 |
| notContainCompare | not_contain：字符串 / 集合 / null |
| numericComparisons | gt/ge/lt/le：数值、null 安全、非数字回退、集合按 size |
| lengthComparisons | len_*：字符串 / 集合长度 |
| isNullCompare | is_null：null / 空串 / 空集合 |
| isNotNullCompare | is_not_null |
| isTrueCompare | is_true：Boolean / String / 其他类型 |
| isNotTrueCompare | is_not_true |

#### `CompareBuilderTest`（5）— `com.maxkb4j.workflow.builder.CompareBuilder`
| 用例 | 覆盖点 |
| --- | --- |
| getHandler_unknownOperatorThrows | 未知算子抛 IllegalArgumentException |
| registerAndLookupHandler | 注册与查找 |
| registerReplacesExistingHandler | 覆盖注册返回 replaced |
| registerNullArgsThrows | null 入参校验 |
| registerSkipsNullOperatorWithoutReplacing | 跳过 null 算子 |

#### `CompareOperatorTest`（3）— `com.maxkb4j.workflow.enums.CompareOperator`
| 用例 | 覆盖点 |
| --- | --- |
| fromCode_knownOperators | 已知 code 反查 |
| fromCode_unknownAndNullReturnNull | 未知 / null 返回 null |
| everyCodeRoundTrips | 全枚举 code 往返一致 |

#### `VariableResolverTest`（6）— `com.maxkb4j.workflow.engine.VariableResolver`
| 用例 | 覆盖点 |
| --- | --- |
| getPromptVariables_mergesAllScopes | global/chat/loop/node 合并 |
| getPromptVariables_replacesNullNodeValueWithStar | 节点 null 值转 "*" |
| getNodeVariables_returnsScopedByName | 节点变量按名作用域化 |
| getNodeVariables_handlesNullAndMissingName | null 节点 / 缺 nodeName |
| getReferenceField_resolvesByScopeAndNode | 按作用域与节点引用查找 |
| getFlowVariables_groupsByScopeKey | 按作用域分组 |

#### `TemplateRendererTest`（6）— `com.maxkb4j.workflow.engine.TemplateRenderer`
| 用例 | 覆盖点 |
| --- | --- |
| render_substitutesContextVariable | `{{var}}` 替换 |
| render_keepsPlainPrompt | 无占位原文 |
| render_blankAndNullReturnEmpty | 空白 / null 返回空串 |
| render_addVariablesOverrideContext | 额外变量覆盖上下文 |
| render_addVariablesProvideExtraKeys | 额外变量补充键 |
| render_missingVariableThrows | 缺省变量抛 IllegalArgumentException |

#### `WorkflowContextTest`（3）— `com.maxkb4j.workflow.engine.WorkflowContext`
| 用例 | 覆盖点 |
| --- | --- |
| appendNode_addsAndFindsNode | 追加与查找 |
| appendNode_replacesSameRuntimeNodeInPlace | 同 runtimeNode 原地替换 |
| render_delegatesToTemplateRenderer | render / 变量 / 引用转发 |

#### `EdgeNavigatorTest`（4）— `com.maxkb4j.workflow.engine.EdgeNavigator`
| 用例 | 覆盖点 |
| --- | --- |
| findDownstreamEdges_returnsEdgesFromNode | 下游边查找 |
| findUpstreamNodeIds_returnsSourceNodes | 上游节点查找 |
| nullEdgesYieldsEmptyNavigator | null 边列表安全 |
| sizeAndEmptyReflectEdgeCount | 计数与空判定 |

#### `ConditionUtilTest`（7）— `com.maxkb4j.workflow.util.ConditionUtil`
| 用例 | 覆盖点 |
| --- | --- |
| assertion_andSingleConditionTrue | and 单条件为真 |
| assertion_andRequiresAllConditions | and 全真才真 |
| assertion_orRequiresAnyCondition | or 任一为真 |
| assertion_emptyOrNullConditionsAreSatisfied | 空 / null 条件视为满足 |
| assertion_fieldSizeNotTwoReturnsFalse | 字段长度 != 2 返回 false |
| assertion_unknownOperatorReturnsFalse | 未知算子降级 false |
| assertion_numericAndContainAndIsNullOperators | gt / contain / is_null 端到端 |

#### `AbsNodeTest`（7）— `com.maxkb4j.workflow.node.AbsNode`
| 用例 | 覆盖点 |
| --- | --- |
| freshNodeIsReadyAndClaimsOnce | READY → STARTED，仅抢占一次 |
| tryClaimRunning_returnsFalseForTerminalStates | SUCCESS / ERROR 不可抢占 |
| interruptStateCanBeClaimed | INTERRUPT 可被抢占 |
| getNodeData_returnsNodeDataWhenPresent | 取 nodeData |
| getNodeData_returnsEmptyWhenAbsent | 缺失返回空 |
| getAnswerList_emptyWhenNoAnswerText | 无答案返回空 |
| getAnswerList_buildsAnswerWithContentAndReasoning | 答案与推理内容构建 |

#### `NodeIdGeneratorTest`（5）— `com.maxkb4j.workflow.util.NodeIdGenerator`
| 用例 | 覆盖点 |
| --- | --- |
| isDeterministicForSameInput | 同输入确定性 |
| nullUpListEqualsEmptyUpList | null 上游等价空列表 |
| differsWhenUpNodeListChanges | 上游变化则不同 |
| differsWhenNodeIdChanges | 节点 ID 变化则不同 |
| producesLowercaseHexOfSha1Length | SHA-1 40 位小写十六进制 |

---

## 六、测试过程中发现的问题与修复

> 以下 2 处问题在编写回归用例时发现，均已修复并以用例守护。

### 问题 1：`CompareOperator.fromCode(null)` 抛 NPE，违反自身契约

- **位置**：`maxkb4j-service/maxkb4j-workflow/src/main/java/com/maxkb4j/workflow/enums/CompareOperator.java`
- **现象**：`fromCode(null)` 抛 `NullPointerException`。
- **根因**：`CODE_MAP` 由 `Collectors.toUnmodifiableMap` 构建为不可变 `MapN`，其 `get(null)` 不允许 null 键，直接抛 NPE；而方法 Javadoc 声明「未找到返回 null」。
- **修复**：
  ```java
  public static CompareOperator fromCode(String code) {
      return code == null ? null : CODE_MAP.get(code);
  }
  ```
- **守护用例**：`CompareOperatorTest.fromCode_unknownAndNullReturnNull` 断言 `fromCode(null)` 返回 `null`。

### 问题 2：`VariableResolver` 中存在不可达的死代码分支

- **位置**：`maxkb4j-service/maxkb4j-workflow/src/main/java/com/maxkb4j/workflow/engine/VariableResolver.java`（`getPromptVariables`）
- **现象**：`global/chat/loop` 三处 `value == null ? "*" : value` 分支永远不会触发。
- **根因**：这三个上下文为 `ConcurrentHashMap`，不允许 null 值，故 `value` 恒非 null；该 null → "*" 处理仅在节点上下文（`AbsNode` 的 `LinkedHashMap`，可存 null）中真正生效。
- **修复**：移除这三处不可达分支（行为等价清理），保留 `getNodeVariables` 中生效的 null → "*" 处理。
- **守护用例**：`VariableResolverTest.getPromptVariables_replacesNullNodeValueWithStar`（通过节点上下文验证 null → "*"）。

---

## 七、运行方式

### 运行全部相关模块

```powershell
mvn test -pl maxkb4j-common -am
mvn test -pl maxkb4j-service/maxkb4j-workflow -am
```

### 只运行单个测试类

```powershell
mvn test -pl maxkb4j-service/maxkb4j-workflow -am -Dtest=ConditionUtilTest
```

### 查看详细结果

各模块 `target/surefire-reports/` 下有 `.txt`（汇总）与 `.xml`（机器可读）报告。

### 注意事项

- 首次构建或沙箱环境内运行时，Maven 需联网拉取 Spring Boot 父 POM 及测试依赖（`spring-boot-starter-test` 等）。若报 `getsockopt ... Permission denied`，需放行网络访问。
- Mockito 当前以「自附加 agent」方式启用 inline mock maker，JDK 未来版本将禁用该机制；如需消除告警，可按 Mockito 文档将 byte-buddy-agent 显式配置为 `-javaagent`。

---

## 八、局限性与后续建议

### 当前局限

- **层级为单元测试**：聚焦纯逻辑（算法 / 解析 / 转换 / 校验），尚未覆盖集成与端到端流程。
- **未覆盖外部依赖**：数据库（MyBatis-Plus / pgvector）、LLM 调用（langchain4j）、HTTP 节点、OSS、Spring 容器装配等链路未纳入。 本轮已通过 Mockito 打桩覆盖自定义 `TypeHandler`（`StringListTypeHandler`）的转换逻辑，但尚未接入真实数据库 / Mapper。
- **未覆盖 Web 层**：Controller / 鉴权（Sa-Token）/ 全局异常处理 / 拦截器等暂无测试。

### 后续建议（按优先级）

1. **补 Service 层切片测试**：对知识库检索、模型调用、工具执行等核心 Service，用 `@SpringBootTest` + 嵌入式 / Testcontainers PostgreSQL 覆盖。
2. **补 Web 层 `MockMvc` 测试**：覆盖鉴权、参数校验、统一返回结构（`R`）、全局异常处理。
3. **补持久层测试**：本轮已覆盖 `StringListTypeHandler` 的数组 <-> List 转换；待补 `JSONBTypeHandler` / `EmbeddingTypeHandler` 等其余 TypeHandler，并接入真实 Mapper 做数据正确性验证。
4. **引入覆盖率度量**：接入 JaCoCo，设定核心模块行/分支覆盖率基线，纳入 CI 门禁。
5. **补端到端工作流冒烟**：构造最小工作流（Start → AiChat → DirectReply）验证引擎编排链路，配合 Mock 模型。
6. **修复 Mockito agent 告警**：将 mockito agent 显式加入构建，面向未来 JDK 兼容。

---

## 九、文件清单

### 新增测试文件（22）

- `maxkb4j-common/src/test/java/com/maxkb4j/common/util/`
  - `BatchUtilTest.java`、`DataMaskUtilTest.java`、`MD5UtilTest.java`
  - `MessageConverterTest.java`、`MimeTypeUtilsTest.java`、`RenderTagsTest.java`
  - `DateTimeUtilTest.java`、`ObjectUtilTest.java`、`BeanUtilTest.java`、`PageUtilTest.java`
- `maxkb4j-common/src/test/java/com/maxkb4j/common/typehandler/`
  - `StringListTypeHandlerTest.java`
- `maxkb4j-service/maxkb4j-workflow/src/test/java/com/maxkb4j/workflow/`
  - `compare/CompareImplTest.java`
  - `builder/CompareBuilderTest.java`
  - `enums/CompareOperatorTest.java`
  - `engine/VariableResolverTest.java`、`engine/TemplateRendererTest.java`
  - `engine/WorkflowContextTest.java`、`engine/EdgeNavigatorTest.java`、`engine/HistoryManagerTest.java`
  - `util/ConditionUtilTest.java`、`util/NodeIdGeneratorTest.java`
  - `node/AbsNodeTest.java`

### 修改的源码与配置（3）

- `maxkb4j-service/maxkb4j-workflow/pom.xml` — 增补 `spring-boot-starter-test`（test scope）。
- `maxkb4j-service/maxkb4j-workflow/src/main/java/com/maxkb4j/workflow/enums/CompareOperator.java` — `fromCode` 改为 null 安全。
- `maxkb4j-service/maxkb4j-workflow/src/main/java/com/maxkb4j/workflow/engine/VariableResolver.java` — 移除不可达死代码分支。

---

*本报告基于 2026-07-28 的代码快照生成，全部用例在 JDK 21 + Maven 3.9.9 下通过验证。*