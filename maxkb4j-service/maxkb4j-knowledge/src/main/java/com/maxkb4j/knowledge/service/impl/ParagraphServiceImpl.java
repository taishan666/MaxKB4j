package com.maxkb4j.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.knowledge.dto.ParagraphDTO;
import com.maxkb4j.knowledge.dto.ProblemDTO;
import com.maxkb4j.knowledge.event.GenerateProblemEvent;
import com.maxkb4j.knowledge.event.ParagraphIndexEvent;
import com.maxkb4j.knowledge.dto.GenerateProblemDTO;
import com.maxkb4j.knowledge.dto.ParagraphAddDTO;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;
import com.maxkb4j.knowledge.mapper.DocumentMapper;
import com.maxkb4j.knowledge.mapper.ParagraphMapper;
import com.maxkb4j.knowledge.service.IParagraphInternalService;
import com.maxkb4j.knowledge.service.IProblemParagraphService;
import com.maxkb4j.knowledge.service.IProblemService;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.ProblemSimpleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-27 11:13:27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParagraphServiceImpl extends ServiceImpl<ParagraphMapper, ParagraphEntity> implements IParagraphInternalService {

    private final IProblemService problemService;
    private final IProblemParagraphService problemParagraphService;
    private final IDataStore compositeStore;
    private final DocumentMapper documentMapper;
    private final ApplicationEventPublisher eventPublisher;

    public void updateStatusById(String id, int type, int status) {
        baseMapper.updateStatusByIds(List.of(id),type,status,type-1,type+1);
    }

    public void updateStatusByIds(List<String> paragraphIds, int type, int status) {
        baseMapper.updateStatusByIds(paragraphIds,type,status,type-1,type+1);
    }

    @Override
    public List<ParagraphDTO> listDtoByIds(List<String> ids) {
        return BeanUtil.copyList(baseMapper.selectByIds(ids), ParagraphDTO.class);
    }

    public List<String> getNoActiveParagraphIds(List<String> knowledgeIds, List<String> excludeDocIds) {
        LambdaQueryWrapper<ParagraphEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(ParagraphEntity::getId);
        queryWrapper.in(ParagraphEntity::getKnowledgeId, knowledgeIds);
        queryWrapper.eq(ParagraphEntity::getIsActive, false);
        if (!CollectionUtils.isEmpty(excludeDocIds)){
            queryWrapper.notIn(ParagraphEntity::getDocumentId, excludeDocIds);
        }
        List<ParagraphEntity> paragraphs = super.list(queryWrapper);
        return paragraphs.stream().map(ParagraphEntity::getId).toList();
    }


    @Transactional
    public void updateParagraphById(String knowledgeId,String docId,ParagraphEntity paragraph) {
        this.updateById(paragraph);
        if (Objects.nonNull(paragraph.getContent())){
            documentMapper.updateCharLengthById(docId);
            eventPublisher.publishEvent(new ParagraphIndexEvent(this, knowledgeId,docId,List.of(paragraph.getId())));
        }
        // isActive 不再回写到各 store；检索时统一通过 noActiveList() 在搜索阶段排除非活跃段落。
    }


    @Transactional
    public Boolean deleteBatchByIds(String knowledgeId,String docId, List<String> paragraphIds) {
        compositeStore.deleteByParagraphIds(knowledgeId,paragraphIds);
        this.removeByIds(paragraphIds);
        return documentMapper.updateCharLengthById(docId);
    }


    @Transactional
    public boolean saveParagraphAndProblem(String knowledgeId, String docId, ParagraphAddDTO addDTO) {
        ParagraphDTO paragraph= new ParagraphDTO(knowledgeId, docId, addDTO.getTitle(), addDTO.getContent(),addDTO.getPosition());
        List<ProblemDTO> problemList = addDTO.getProblemList();
        List<String> problems = new ArrayList<>();
        if (!CollectionUtils.isEmpty(problemList)) {
            problems =problemList.stream().map(ProblemDTO::getContent).toList();
        }
        return saveParagraphAndProblem(paragraph, problems);
    }


    @Override
    @Transactional
    public boolean saveParagraphAndProblem(ParagraphDTO paragraph, List<String> problems) {
        this.save(BeanUtil.copy(paragraph, ParagraphEntity.class));
        if (!CollectionUtils.isEmpty(problems)) {
            List<ProblemParagraphEntity> problemParagraphMappingEntities = new ArrayList<>();
            for (String problem : problems) {
                ProblemEntity problemEntity = problemService.lambdaQuery().eq(ProblemEntity::getContent, problem).one();
                if (problemEntity == null) {
                    problemEntity = ProblemEntity.createDefault();
                    problemEntity.setHitNum(0);
                    problemEntity.setContent(problem);
                    problemEntity.setKnowledgeId(paragraph.getKnowledgeId());
                    problemService.save(problemEntity);
                }
                ProblemParagraphEntity entity = new ProblemParagraphEntity();
                entity.setKnowledgeId(paragraph.getKnowledgeId());
                entity.setProblemId(problemEntity.getId());
                entity.setParagraphId(paragraph.getId());
                entity.setDocumentId(paragraph.getDocumentId());
                problemParagraphMappingEntities.add(entity);
            }
            problemParagraphService.saveBatch(problemParagraphMappingEntities);
        }
        eventPublisher.publishEvent(new ParagraphIndexEvent(this, paragraph.getKnowledgeId(),paragraph.getDocumentId(),List.of(paragraph.getId())));
        return documentMapper.updateCharLengthById(paragraph.getDocumentId());
    }


    public ParagraphEntity createParagraph(String knowledgeId, String docId, String title, String content,Integer  position) {
        ParagraphEntity paragraph = new ParagraphEntity();
        paragraph.setId(IdWorker.get32UUID());
        paragraph.setTitle(title == null ? "" : title);
        paragraph.setContent(content == null ? "" : content);
        paragraph.setKnowledgeId(knowledgeId);
        paragraph.setStatus("nn0");
        paragraph.setHitNum(0);
        paragraph.setIsActive(true);
        paragraph.setPosition(position==null?1:position);
        paragraph.setDocumentId(docId);
        return paragraph;
    }


    @Transactional
    public boolean save(ParagraphEntity paragraph) {
        List<ParagraphEntity> list = this.lambdaQuery().eq(ParagraphEntity::getKnowledgeId, paragraph.getKnowledgeId()).eq(ParagraphEntity::getDocumentId, paragraph.getDocumentId()).list();
        List<ParagraphEntity> updateList=list.stream().filter(e->e.getPosition()>=paragraph.getPosition()).peek(e-> e.setPosition(e.getPosition()+1)).toList();
        if (!CollectionUtils.isEmpty(updateList)){
             super.updateBatchById(updateList);
        }
        return super.save(paragraph);
    }


    @Override
    @Transactional
    public boolean saveDtoBatch(List<ParagraphDTO> paragraphs) {
        Map<String,List<ParagraphDTO>> knowledgeGroup = paragraphs.stream()
                .filter(e -> e.getKnowledgeId() != null)
                .collect(Collectors.groupingBy(ParagraphDTO::getKnowledgeId));
        knowledgeGroup.forEach((knowledgeId,knowledgeParagraphs)->{
            Map<String,List<ParagraphDTO>> docGroup = knowledgeParagraphs.stream()
                    .filter(e -> e.getDocumentId() != null)
                    .collect(Collectors.groupingBy(ParagraphDTO::getDocumentId));
            docGroup.forEach((docId,docParagraphs)->{
                long count = this.lambdaQuery().eq(ParagraphEntity::getKnowledgeId, knowledgeId).eq(ParagraphEntity::getDocumentId, docId).count();
                int position= (int) (count+1);
                for (ParagraphDTO paragraph : docParagraphs) {
                    if (paragraph.getTitle()!=null&&paragraph.getTitle().trim().length()>256){
                        paragraph.setTitle(paragraph.getTitle().substring(0,256));
                    }
                    paragraph.setPosition(position);
                    position++;
                }
            });
        });
       return super.saveBatch(BeanUtil.copyList(paragraphs, ParagraphEntity.class));
    }


    public IPage<ParagraphEntity> pageParagraphByDocId(String docId, int current, int size, String title, String content) {
        Page<ParagraphEntity> paragraphPage = new Page<>(current, size);
        LambdaQueryWrapper<ParagraphEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ParagraphEntity::getDocumentId, docId);
        if (StringUtils.isNotBlank(title)) {
            wrapper.like(ParagraphEntity::getTitle, title);
        }
        if (StringUtils.isNotBlank(content)) {
            wrapper.like(ParagraphEntity::getContent, content);
        }
        wrapper.orderByAsc(ParagraphEntity::getPosition);
        return this.page(paragraphPage, wrapper);
    }

    public List<ProblemSimpleVO> getProblemsByParagraphId(String paragraphId) {
        List<ProblemParagraphEntity> list = problemParagraphService.lambdaQuery()
                .select(ProblemParagraphEntity::getProblemId).eq(ProblemParagraphEntity::getParagraphId, paragraphId).list();
        if (!CollectionUtils.isEmpty(list)) {
            List<String> problemIds = list.stream().map(ProblemParagraphEntity::getProblemId).toList();
            List<ProblemEntity> problems = problemService.lambdaQuery().in(ProblemEntity::getId, problemIds).list();
            return BeanUtil.copyList(problems, ProblemSimpleVO.class);
        }
        return Collections.emptyList();
    }

    public Boolean batchGenerateRelated(String knowledgeId, String docId, GenerateProblemDTO dto) {
        this.updateStatusByIds(dto.getParagraphIdList(), 2, 0);
        eventPublisher.publishEvent(new GenerateProblemEvent(this, knowledgeId,List.of(docId),dto.getModelId(),dto.getModelParamsSetting(),dto.getNumber(),List.of("0")));
        return true;
    }

    @Transactional
    public boolean adjustPosition(String knowledgeId, String documentId, String paragraphId, Integer newPosition, Integer targetIndex) {
        if (newPosition == null || newPosition < 1) {
            return false;
        }
        if (targetIndex != null && targetIndex < 0) {
            newPosition = targetIndex + 1;
        }
        // 2. 一次性查询所需数据，避免二次查库
        List<ParagraphEntity> paragraphs = this.lambdaQuery()
                .select(ParagraphEntity::getId, ParagraphEntity::getPosition)
                .eq(ParagraphEntity::getKnowledgeId, knowledgeId)
                .eq(ParagraphEntity::getDocumentId, documentId)
                .orderByAsc(ParagraphEntity::getPosition)
                .list();

        if (CollectionUtils.isEmpty(paragraphs)) {
            return false;
        }

        // 3. 从已查询的列表中查找目标段落，替代额外的 getById
        ParagraphEntity target = paragraphs.stream()
                .filter(p -> paragraphId.equals(p.getId()))
                .findFirst()
                .orElse(null);

        if (target == null) {
            return false;
        }

        int oldPosition = target.getPosition();

        // 4. 位置未变化，直接返回
        if (oldPosition == newPosition) {
            return true;
        }

        // 5. 边界检查：防止targetIndex超出实际段落范围
        int maxPosition = paragraphs.size();
        if (newPosition > maxPosition) {
            newPosition = maxPosition;
        }
        if (oldPosition == newPosition) {
            return true;
        }

        // 6. 精确构建需要更新的实体列表，只修改受影响的记录
        List<ParagraphEntity> changeList = new ArrayList<>();
        boolean isMoveUp = oldPosition > newPosition;

        for (ParagraphEntity p : paragraphs) {
            int pos = p.getPosition();
            if (p.getId().equals(paragraphId)) {
                // 目标段落：直接设置新位置
                p.setPosition(newPosition);
                changeList.add(p);
            } else if (isMoveUp && pos >= newPosition && pos < oldPosition) {
                // 上移：新旧位置之间的段落整体后移一位
                p.setPosition(pos + 1);
                changeList.add(p);
            } else if (!isMoveUp && pos > oldPosition && pos <= newPosition) {
                // 下移：新旧位置之间的段落整体前移一位
                p.setPosition(pos - 1);
                changeList.add(p);
            }
        }

        if (changeList.isEmpty()) {
            return true;
        }

        return this.updateBatchById(changeList);
    }


    //type 1 向量化 2 问题生成 3 网络同步
    public List<String> listParagraphIdsByStates(String docId,int type, List<String> stateList) {
        return baseMapper.listParagraphIdsByStates(docId, (4-type),stateList);
    }

    @Override
    @Transactional
    public boolean deleteById(String knowledgeId,String paragraphId) {
        compositeStore.deleteByParagraphId(knowledgeId, paragraphId);
        problemParagraphService.lambdaUpdate().eq(ProblemParagraphEntity::getParagraphId, paragraphId).remove();
        return this.removeById(paragraphId);
    }
}
