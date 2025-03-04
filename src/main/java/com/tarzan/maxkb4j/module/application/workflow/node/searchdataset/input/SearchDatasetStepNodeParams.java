package com.tarzan.maxkb4j.module.application.workflow.node.searchdataset.input;

import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;

import java.util.List;

@Data
public class SearchDatasetStepNodeParams extends BaseParams {
    private List<String> datasetIdList;
    private DatasetSetting datasetSetting;
    private List<String> questionReferenceAddress;

    @Override
    public boolean isValid() {
        return false;
    }
}
