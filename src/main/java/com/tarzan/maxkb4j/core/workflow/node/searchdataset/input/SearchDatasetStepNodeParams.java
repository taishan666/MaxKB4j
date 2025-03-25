package com.tarzan.maxkb4j.core.workflow.node.searchdataset.input;

import com.tarzan.maxkb4j.module.application.entity.DatasetSetting;
import com.tarzan.maxkb4j.core.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
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
