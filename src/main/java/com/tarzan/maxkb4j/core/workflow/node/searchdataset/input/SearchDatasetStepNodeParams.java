package com.tarzan.maxkb4j.core.workflow.node.searchdataset.input;

import com.tarzan.maxkb4j.module.application.domian.entity.KnowledgeSetting;
import lombok.Data;

import java.util.List;

@Data
public class SearchDatasetStepNodeParams {
    private List<String> knowledgeIdList;
    private KnowledgeSetting knowledgeSetting;
    private List<String> questionReferenceAddress;
    private Boolean showKnowledge;

}
