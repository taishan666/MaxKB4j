package com.tarzan.maxkb4j.module.chatpipeline;

import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;

import java.util.List;
import java.util.UUID;

public abstract class PostResponseHandler {

    public abstract void handler(UUID chatId, UUID chatRecordId, List<ParagraphVO> paragraphList, String problemText, String answerText, PipelineManage manage, String paddingProblemText, UUID clientId);
}
