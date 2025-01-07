package com.tarzan.maxkb4j.module.dataset.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.dataset.dto.GenerateProblemDTO;
import com.tarzan.maxkb4j.module.dataset.dto.ProblemDTO;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.vo.ProblemVO;
import com.tarzan.maxkb4j.module.embedding.service.EmbeddingService;
import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.module.systemSetting.entity.SystemSettingEntity;
import com.tarzan.maxkb4j.module.systemSetting.service.SystemSettingService;
import com.tarzan.maxkb4j.util.BeanUtil;
import com.tarzan.maxkb4j.util.RSAUtil;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.dashscope.QwenChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.tarzan.maxkb4j.module.dataset.mapper.ProblemMapper;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author tarzan
 * @date 2024-12-26 10:45:40
 */
@Service
public class ProblemService extends ServiceImpl<ProblemMapper, ProblemEntity>{

    @Autowired
    private ProblemParagraphService problemParagraphService;
    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private SystemSettingService systemSettingService;

    public IPage<ProblemVO> getProblemsByDatasetId(Page<ProblemEntity> problemPage, UUID id) {
        return baseMapper.getProblemsByDatasetId(problemPage, id);
    }

    @Async
    public void batchGenerateRelated(UUID datasetId,List<ParagraphEntity> paragraphs, GenerateProblemDTO dto) {
        ModelEntity model=modelService.getById(dto.getModel_id());
        SystemSettingEntity systemSetting=systemSettingService.lambdaQuery().eq(SystemSettingEntity::getType,1).one();
        try {
            String credential= RSAUtil.rsaLongDecrypt(model.getCredential(),systemSetting.getMeta().getString("value"));
            JSONObject json=JSONObject.parseObject(credential);
            ChatLanguageModel chatModel = QwenChatModel.builder()
                    .apiKey(json.getString("api_key"))
                    .modelName(model.getModelName())
                    .build();
            List<ProblemDTO> problemDTOS = new ArrayList<>();
            for (ParagraphEntity paragraph : paragraphs) {
                UserMessage userMessage = UserMessage.from(dto.getPrompt().replace("{data}", paragraph.getContent()));
                ChatRequest chatRequest = ChatRequest.builder()
                        .messages(userMessage)
                        .build();
                ChatResponse chatResponse = chatModel.chat(chatRequest);
                String output = chatResponse.aiMessage().text();
                List<String> problems = extractProblems(output);
                for (String problem : problems) {
                    ProblemDTO problemDTO = new ProblemDTO();
                    problemDTO.setId(UUID.randomUUID());
                    problemDTO.setDatasetId(paragraph.getDatasetId());
                    problemDTO.setContent(problem);
                    problemDTO.setHitNum(0);
                    problemDTO.setDocumentId(paragraph.getDocumentId());
                    problemDTO.setParagraphId(paragraph.getId());
                    problemDTOS.add(problemDTO);
                }
            }
            List<ProblemEntity> problemEntities=BeanUtil.copyList(problemDTOS, ProblemEntity.class);
            baseMapper.insert(problemEntities);
            if (!CollectionUtils.isEmpty(problemDTOS)) {
                List<ProblemParagraphEntity> problemParagraphs=new ArrayList<>();
                for (ProblemDTO problem : problemDTOS) {
                    ProblemParagraphEntity problemParagraph=new ProblemParagraphEntity();
                    problemParagraph.setProblemId(problem.getId());
                    problemParagraph.setParagraphId(problem.getParagraphId());
                    problemParagraph.setDatasetId(problem.getDatasetId());
                    problemParagraph.setDocumentId(problem.getDocumentId());
                    problemParagraphs.add(problemParagraph);
                }
                problemParagraphService.saveBatch(problemParagraphs);
                embeddingService.createProblems(datasetId,problemDTOS);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }

    public List<String> extractProblems(String output) {
        String[] problems = output.split("\n");
        List<String> result = new ArrayList<>(problems.length);
        for (String problem : problems) {
            result.add(extractProblem(problem));
        }
        return result;
    }

    public String extractProblem(String problem) {
        // 移除开头的数字和句点
        if (problem != null && !problem.isEmpty()) {
            problem = problem.replaceAll("^\\d+\\.\\s*", "");

            // 定义匹配<question>标签内容的模式
            Pattern pattern = Pattern.compile("<question>(.*?)</question>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(problem);

            // 查找匹配并提取内容
            if (matcher.find()) {
                problem = matcher.group(1);
                // 如果提取的内容为空或不存在，则返回null
                if (problem == null || problem.trim().isEmpty()) {
                    return null;
                }
            } else {
                // 如果没有找到匹配项，则返回null
                return null;
            }
        } else {
            // 如果输入字符串为空或null，则返回null
            return null;
        }

        return problem;
    }
}
