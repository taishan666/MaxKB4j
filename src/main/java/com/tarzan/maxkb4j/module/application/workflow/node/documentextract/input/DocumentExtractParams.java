package com.tarzan.maxkb4j.module.application.workflow.node.documentextract.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DocumentExtractParams extends BaseParams {
    @JsonProperty("document_list")
    List<String> documentList;

    @Override
    public boolean isValid() {
        return false;
    }
}
