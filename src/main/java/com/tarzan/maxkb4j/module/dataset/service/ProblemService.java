package com.tarzan.maxkb4j.module.dataset.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.dataset.dto.GenerateProblemDTO;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.mapper.ProblemMapper;
import com.tarzan.maxkb4j.module.dataset.vo.ProblemVO;
import com.tarzan.maxkb4j.module.dataset.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author tarzan
 * @date 2024-12-26 10:45:40
 */
@Slf4j
@Service
@AllArgsConstructor
public class ProblemService extends ServiceImpl<ProblemMapper, ProblemEntity> {


    private final EmbeddingService embeddingService;
    private final ProblemParagraphService problemParagraphService;


    public IPage<ProblemVO> getProblemsByDatasetId(Page<ProblemEntity> problemPage, String id, String content) {
        return baseMapper.getProblemsByDatasetId(problemPage, id, content);
    }


    @Async
    public void generateRelated(BaseChatModel chatModel, EmbeddingModel embeddingModel, String datasetId, String docId, ParagraphEntity paragraph, List<ProblemEntity> dbProblemEntities, List<ProblemEntity> docProblems, GenerateProblemDTO dto) {
        log.info("开始---->生成问题:{}", paragraph.getId());
        UserMessage userMessage = UserMessage.from(dto.getPrompt().replace("{data}", paragraph.getContent()));
        ChatResponse res = chatModel.generate(userMessage);
        String output = res.aiMessage().text();
        List<String> problems = extractProblems(output);
        List<ProblemEntity> paragraphProblems = new ArrayList<>();
        if (!CollectionUtils.isEmpty(problems)) {
            for (String problem : problems) {
                String problemId = IdWorker.get32UUID();
                ProblemEntity existingProblem = findProblem(problem, docProblems, dbProblemEntities);
                if (existingProblem == null) {
                    ProblemEntity entity = ProblemEntity.createDefault();
                    entity.setId(problemId);
                    entity.setDatasetId(datasetId);
                    entity.setContent(problem);
                    paragraphProblems.add(entity);
                    baseMapper.insert(entity);
                    docProblems.add(entity);
                } else {
                    problemId = existingProblem.getId();
                }
                ProblemParagraphEntity problemParagraph = new ProblemParagraphEntity();
                problemParagraph.setProblemId(problemId);
                problemParagraph.setParagraphId(paragraph.getId());
                problemParagraph.setDatasetId(datasetId);
                problemParagraph.setDocumentId(docId);
            }
        }
        embeddingService.createProblems(embeddingModel, paragraphProblems, docId, paragraph.getId());
        log.info("结束---->生成问题:{}", paragraph.getId());
    }

    @Transactional
    public boolean createProblemsByDatasetId(String id, List<String> problems) {
        if (!CollectionUtils.isEmpty(problems)) {
            List<ProblemEntity> dbProblemEntities = this.lambdaQuery().eq(ProblemEntity::getDatasetId, id).list();
            List<ProblemEntity> problemEntities = new ArrayList<>();
            for (String problem : problems) {
                ProblemEntity existingProblem = findProblem(problem, problemEntities, dbProblemEntities);
                if (existingProblem == null) {
                    ProblemEntity entity = new ProblemEntity();
                    entity.setDatasetId(id);
                    entity.setContent(problem);
                    entity.setHitNum(0);
                    problemEntities.add(entity);
                }
            }
            return this.saveBatch(problemEntities);
        }
        return false;
    }

    public ProblemEntity findProblem(String problem, List<ProblemEntity> problemEntities, List<ProblemEntity> dbProblemEntities) {
        ProblemEntity existingProblem = null;
        if (!CollectionUtils.isEmpty(dbProblemEntities)) {
            existingProblem = dbProblemEntities.stream()
                    .filter(e -> e.getContent().equals(problem))
                    .findFirst()
                    .orElse(null);
        }
        if (!CollectionUtils.isEmpty(problemEntities) && existingProblem == null) {
            existingProblem = problemEntities.stream()
                    .filter(e -> e.getContent().equals(problem))
                    .findFirst()
                    .orElse(null);
        }
        return existingProblem;
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

    @Transactional
    public boolean deleteProblemByIds(List<String> problemIds) {
        if (CollectionUtils.isEmpty(problemIds)) {
            return false;
        }
        problemParagraphService.lambdaUpdate().in(ProblemParagraphEntity::getProblemId, problemIds).remove();
        embeddingService.lambdaUpdate().in(EmbeddingEntity::getSourceId, problemIds.stream().map(String::toString).toList()).remove();
        return this.removeByIds(problemIds);
    }
}
