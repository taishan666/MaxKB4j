package com.tarzan.maxkb4j.core.workflow.node.documentextract.input;

import com.tarzan.maxkb4j.core.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DocumentExtractParams extends BaseParams {
    private List<String> documentList;

    @Override
    public boolean isValid() {
        return false;
    }
}
