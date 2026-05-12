package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.entity.ApplicationLongTermMemoryEntity;
import com.maxkb4j.application.mapper.ApplicationLongTermMemoryMapper;
import com.maxkb4j.model.service.IModelProviderService;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class ApplicationLongTermMemoryService extends ServiceImpl<ApplicationLongTermMemoryMapper, ApplicationLongTermMemoryEntity> implements IApplicationLongTermMemoryService {


    String LONG_TERM_PROMPT = """
                    你是一个专业的用户长期记忆提炼引擎。你的唯一职责是：从对话中精确识别具有持久价值的用户信息，并与已有记忆进行结构化融合，输出供 AI 助手长期使用的用户画像记忆。
                    ## 输入
                    【已有记忆】：
                    {{existingMemory}}
                    【本轮新增对话】：
                    {{newConversation}}
                    ---
                    ## 提取门槛（必须同时满足，才可提取）
                    1. **跨会话复用价值**：这条信息在未来其他对话中仍然适用，而非当次临时需求
                    2. **明确可证**：可从对话原文直接支撑，不得推断、脑补或延伸
                    3. **改善回答质量**：记住这条信息后，AI 的回答会对该用户更准确或更贴合
                    **以下内容禁止提取：**
                    - 用户的一次性临时要求（如「这次用表格输出就好」）
                    - 用户提问的具体内容本身（问题不是记忆）
                    - 无法从对话原文直接证明的推断
                    - 闲聊、问候、感谢等无信息量内容
                    - AI 的回答内容（只提取用户侧信息）
                    ---
                    ## 四类记忆分类与融合规则
                    ### 【偏好】交互偏好
                    用户对「AI 如何回应」的稳定期望，需明确声明或在多轮中反复体现才可录入。
                    常见维度：回答详略 / 语言风格（正式/口语）/ 输出格式（表格/列表/段落）/ 是否要举例 / 代码风格偏好 / 回复语言
                    融合规则：
                    - 同维度出现新偏好 → **覆盖**旧值，条目末标注 `※已更新`
                    - 新维度 → 直接追加
                    - 旧偏好无新证据但未被否定 → **保留**
                    ---
                    ### 【背景】用户背景
                    用户的客观身份与环境信息，稳定性强，用户未明确更正则不主动变动。
                    常见维度：职业/角色 / 所在行业 / 技术栈与熟练度 / 使用产品或系统 / 团队规模 / 所在地区
                    融合规则：
                    - 与旧记忆冲突 → **以新对话为准**，标注 `※已更新`，删除旧值
                    - 新增信息 → 追加
                    - 信息模糊无法确认 → 追加时标注 `※待确认`
                    ---
                    ### 【约定】明确约定
                    用户明确要求 AI 固定遵守的行为规则，须有明确指令性语言支撑，不可自行解读。
                    常见维度：禁止行为 / 固定执行动作 / 特定触发词响应 / 内容边界 / 输出限制
                    融合规则：
                    - 同类新规则 → **覆盖**旧规则，标注 `※已更新`
                    - 新增规则 → 追加
                    - 用户明确取消的规则 → **直接删除**
                    ---
                    ### 【目标】当前目标
                    用户近期或长期正在推进的具体目标，有助于 AI 主动提供更相关的帮助。
                    常见维度：正在进行的项目 / 学习计划 / 待解决的核心问题 / 关键决策
                    融合规则：
                    - 已明确完成或放弃的目标 → **删除**
                    - 新目标 → 追加
                    - 已有目标有进展更新 → **覆盖**旧描述
                    ---
                    ## 输出规范
                    1. **只输出记忆内容本身**，不含任何开头语、解释、总结或分隔说明
                    2. 四个章节**全部输出**，确无内容写「暂无」，不可省略章节
                    3. 每条格式：`- [维度标签] 内容`，标签 2~5 字，精准简洁
                    4. 有变更标记（`※已更新` / `※待确认`）的条目置于各章节**最前**
                    5. 每条记忆控制在 **60 字以内**，信息密度优先，超出则拆为两条
                    6. 输出语言与【本轮新增对话】主要语言保持一致
                    ---
                    ## 输出格式
                    ### 【偏好】交互偏好
                    - [维度标签] 内容
                    （暂无则写：暂无）
                    ### 【背景】用户背景
                    - [维度标签] 内容
                    （暂无则写：暂无）
                    ### 【约定】明确约定
                    - [维度标签] 内容
                    （暂无则写：暂无）
                    ### 【目标】当前目标
                    - [维度标签] 内容
                    （暂无则写：暂无）
            """;


    private final ApplicationChatRecordService chatRecordService;
    private final IModelProviderService modelProviderService;
    private static final Pattern THINK_TAG_PATTERN = Pattern.compile("<think>.*?</think>", Pattern.DOTALL);

    @Async
    @Override
    public void saveMemory(String applicationId, String chatUserId, String modelId, int pageSize) {
        long count = chatRecordService.countByAppIdAndChatUserId(applicationId, chatUserId);
        if (count <= 0 || pageSize <= 0) {
            return;
        }
        if (count % pageSize==0){
            int page= (int) ((count-1)/pageSize);
            int offset = page * pageSize;
            List<ApplicationChatRecordEntity> chatRecords = chatRecordService.listByAppIdAndChatUserId(applicationId, chatUserId,pageSize,offset);
            List<String> lines = new ArrayList<>();
            for (ApplicationChatRecordEntity chatRecord : chatRecords) {
                lines.add("User：" + chatRecord.getProblemText() + "/n" + "AI：" + chatRecord.getAnswerText());
            }
            String newConversation = String.join("\n", lines);
            ApplicationLongTermMemoryEntity longTermMemory = getLongTermMemory(applicationId, chatUserId);
            String existingMemory = longTermMemory == null ? "" : longTermMemory.getMemory();
            String userMessage = LONG_TERM_PROMPT.replace("{{existingMemory}}", existingMemory).replace("{{newConversation}}", newConversation);
            ChatModel chatModel = modelProviderService.buildChatModel(modelId);
            if (chatModel == null){
                return;
            }
            String content = chatModel.chat(userMessage);
            content = THINK_TAG_PATTERN.matcher(content).replaceAll("").trim();
            if (Objects.isNull(longTermMemory)) {
                ApplicationLongTermMemoryEntity saveEntity = new ApplicationLongTermMemoryEntity();
                saveEntity.setApplicationId(applicationId);
                saveEntity.setMemory(content);
                saveEntity.setChatUserId(chatUserId);
                this.save(saveEntity);
            } else {
                longTermMemory.setMemory(content);
                this.updateById(longTermMemory);
            }
        }
    }

/*    public static void main(String[] args) {
        long count = 11;
        int limit=10;
        int page= (int) ((count-1)/limit);
        System.out.println(page);
    }*/

    @Override
    public String getMemory(String applicationId, String chatUserId) {
        ApplicationLongTermMemoryEntity longTermMemory = getLongTermMemory(applicationId, chatUserId);
        return longTermMemory == null ? "" : longTermMemory.getMemory();
    }

    @Async
    @Override
    public void deleteMemory(String applicationId) {
         this.lambdaUpdate().eq(ApplicationLongTermMemoryEntity::getApplicationId, applicationId).remove();
    }

    private ApplicationLongTermMemoryEntity getLongTermMemory(String applicationId, String chatUserId) {
        return this.lambdaQuery()
                .select(ApplicationLongTermMemoryEntity::getId, ApplicationLongTermMemoryEntity::getMemory)
                .eq(ApplicationLongTermMemoryEntity::getApplicationId, applicationId)
                .eq(ApplicationLongTermMemoryEntity::getChatUserId, chatUserId)
                .orderByDesc(ApplicationLongTermMemoryEntity::getCreateTime)
                .last("LIMIT 1").one();
    }
}
