# Trigger 模块重构设计

**日期:** 2026-03-19
**状态:** 待审核

## 背景

`maxkb4j-trigger` 模块负责事件触发器的管理，当前 `EventTriggerService` 类承担了过多职责（约456行），包括：
- CRUD 操作
- 任务数据处理与组装
- 下次执行时间计算

这导致代码难以维护、测试困难，且存在重复代码和 Bug。

## 目标

1. 职责分离，提高代码可读性和可测试性
2. 消除代码重复
3. 修复 `getDetailBySourceId()` 中的 Bug
4. 保持简单，不过度设计

## 重构方案

采用**服务拆分**方案，将 `EventTriggerService` 按职责拆分为三个类：

### 1. EventTriggerService（核心服务）

保留 CRUD 相关方法，职责更聚焦：

```
EventTriggerService
├── pageList()          # 分页查询（调用 TaskProcessor 和 Calculator）
├── saveTrigger()       # 保存/更新触发器
├── batchActivate()     # 批量启用/禁用
├── batchDelete()       # 批量删除
├── getDetailById()     # 获取详情（调用 TaskProcessor）
├── getDetailBySourceId() # 按源获取详情（修复 Bug）
└── listBySource()      # 按源列表查询
```

依赖注入：
- `IEventTriggerTaskService`
- `IUserService`
- `EventTriggerTaskProcessor`（新增）
- `NextRunTimeCalculator`（新增）

### 2. EventTriggerTaskProcessor（任务处理器）

负责任务数据的查询、组装和填充：

```
EventTriggerTaskProcessor
├── processForPage(List<EventTriggerTaskEntity> tasks)
│   └── 为分页列表处理任务数据，返回 TaskProcessResult
├── processForDetail(String triggerId)
│   └── 为详情页处理任务数据，返回 TaskDetailResult
├── buildSourceMap(List<Map<String, Object>> list)
│   └── 构建源数据映射
└── enrichTaskInfo(EventTriggerTaskEntity task, Map<String, Object> source)
    └── 填充任务名称、图标等信息
```

内部记录类：
- `TaskProcessResult(List<EventTriggerTaskEntity> tasks, String taskString)`
- `TaskDetailResult(List<EventTriggerTaskEntity> tasks, String taskString, List<ApplicationEntity> apps, List<ToolEntity> tools)`

依赖注入：
- `IApplicationService`
- `IToolService`

### 3. NextRunTimeCalculator（时间计算器）

负责下次执行时间的计算：

```
NextRunTimeCalculator
├── calculate(JSONObject triggerSetting)
│   └── 根据调度类型计算下次执行时间
├── calculateWeekly(JSONObject setting, int hour, int minute)
├── calculateMonthly(JSONObject setting, int hour, int minute)
├── calculateInterval(JSONObject setting, int hour, int minute)
└── getStringList(Object obj)
    └── 安全地将对象转换为字符串列表
```

## Bug 修复

### getDetailBySourceId Bug

**位置:** `EventTriggerService.java` 第 424-426 行

**问题:** TOOL 类型错误地调用 `applicationService.getById()`

**修复前:**
```java
if (SourceType.APPLICATION.name().equals(sourceType)){
    vo.setApplicationTask(applicationService.getById(sourceTask.get().getSourceId()));
}
if (SourceType.TOOL.name().equals(sourceType)){
    vo.setApplicationTask(applicationService.getById(sourceTask.get().getSourceId())); // Bug!
}
```

**修复后:**
```java
if (SourceType.APPLICATION.name().equals(sourceType)){
    vo.setApplicationTask(applicationService.getById(sourceTask.get().getSourceId()));
}
if (SourceType.TOOL.name().equals(sourceType)){
    vo.setToolTask(toolService.getById(sourceTask.get().getSourceId()));
}
```

同时需要在 `SourceEventTriggerVO` 中添加 `toolTask` 字段。

## 文件结构

```
maxkb4j-trigger/
└── src/main/java/com/maxkb4j/trigger/
    ├── controller/
    │   └── TriggerController.java          # 保持不变
    ├── service/
    │   ├── EventTriggerService.java        # 精简重构
    │   ├── EventTriggerTaskService.java    # 保持不变
    │   ├── EventTriggerTaskProcessor.java  # 新增
    │   └── NextRunTimeCalculator.java      # 新增
    └── enums/
        ├── SourceType.java
        ├── ScheduleType.java
        └── TriggerType.java
```

## 代码行数变化预估

| 文件 | 重构前 | 重构后 |
|------|--------|--------|
| EventTriggerService.java | ~456 | ~180 |
| EventTriggerTaskProcessor.java | 0 | ~120 |
| NextRunTimeCalculator.java | 0 | ~80 |
| **总计** | ~456 | ~380 |

## 影响范围

- 仅影响 `maxkb4j-trigger` 模块内部实现
- API 接口不变，对外部调用者无影响
- 无数据库变更

## 测试要点

1. 分页查询功能正常
2. 触发器创建/编辑/删除功能正常
3. 批量启用/禁用功能正常
4. 下次执行时间计算正确
5. `getDetailBySourceId` 返回正确的应用/工具数据