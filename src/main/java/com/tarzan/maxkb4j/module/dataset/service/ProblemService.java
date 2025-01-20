package com.tarzan.maxkb4j.module.dataset.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.dataset.dto.GenerateProblemDTO;
import com.tarzan.maxkb4j.module.dataset.dto.ProblemDTO;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.mapper.ProblemMapper;
import com.tarzan.maxkb4j.module.dataset.vo.ProblemVO;
import com.tarzan.maxkb4j.module.embedding.service.EmbeddingService;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
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
@Slf4j
@Service
public class ProblemService extends ServiceImpl<ProblemMapper, ProblemEntity>{

    @Autowired
    private ProblemParagraphService problemParagraphService;
    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private ParagraphService paragraphService;
    @Autowired
    private ModelService modelService;

    public IPage<ProblemVO> getProblemsByDatasetId(Page<ProblemEntity> problemPage, UUID id,String content) {
        return baseMapper.getProblemsByDatasetId(problemPage, id,content);
    }

    @Async
    public void batchGenerateRelated(UUID datasetId,UUID docId, GenerateProblemDTO dto) {
        List<ProblemEntity> problemEntities=new ArrayList<>();
        List<ProblemParagraphEntity> problemParagraphs=new ArrayList<>();
        List<ProblemEntity> dbProblemEntities = this.lambdaQuery().eq(ProblemEntity::getDatasetId, datasetId).list();
        documentService.updateStatusById(docId,2,1);
        try {
            List<ParagraphEntity> paragraphs = documentService.getParagraphsByDocId(docId);
            ChatLanguageModel chatModel = modelService.getChatModelById(dto.getModel_id());
            List<ProblemDTO> problemDTOS = new ArrayList<>();
            for (ParagraphEntity paragraph : paragraphs) {
                log.info("开始---->生成问题:{}", paragraph.getId());
                UserMessage userMessage = UserMessage.from(dto.getPrompt().replace("{data}", paragraph.getContent()));
                ChatRequest chatRequest = ChatRequest.builder()
                        .messages(userMessage)
                        .build();
                ChatResponse chatResponse = chatModel.chat(chatRequest);
                String output = chatResponse.aiMessage().text();
                List<String> problems =extractProblems(output);
                if (!CollectionUtils.isEmpty(problems)) {
                    for (String problem : problems) {
                        UUID problemId=UUID.randomUUID();
                        ProblemEntity existingProblem =findProblem(problem, problemEntities,dbProblemEntities);
                        if(existingProblem==null){
                            ProblemEntity entity = new ProblemEntity();
                            entity.setId(problemId);
                            entity.setDatasetId(datasetId);
                            entity.setHitNum(0);
                            entity.setContent(problem);
                            problemEntities.add(entity);
                        }else {
                            problemId = existingProblem.getId();
                        }
                        ProblemParagraphEntity problemParagraph=new ProblemParagraphEntity();
                        problemParagraph.setProblemId(problemId);
                        problemParagraph.setParagraphId(paragraph.getId());
                        problemParagraph.setDatasetId(datasetId);
                        problemParagraph.setDocumentId(docId);
                        problemParagraphs.add(problemParagraph);
                    }
                    embeddingService.createProblems(datasetId,problemDTOS);
                }
                paragraphService.updateStatusById(paragraph.getId(),2,2);
                documentService.updateStatusMetaById(paragraph.getDocumentId());
                log.info("结束---->生成问题:{}", paragraph.getId());
            }
            baseMapper.insert(problemEntities);
            problemParagraphService.saveBatch(problemParagraphs);
            documentService.updateStatusById(docId,2,2);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }

    private ProblemEntity findProblem(String problem, List<ProblemEntity> problemEntities,List<ProblemEntity> dbProblemEntities) {
        if (!CollectionUtils.isEmpty(dbProblemEntities)) {
            return dbProblemEntities.stream()
                    .filter(e -> e.getContent().equals(problem))
                    .findFirst()
                    .orElse(null);
        }
        if (!CollectionUtils.isEmpty(problemEntities)) {
            return problemEntities.stream()
                    .filter(e -> e.getContent().equals(problem))
                    .findFirst()
                    .orElse(null);
        }
        return null;
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
