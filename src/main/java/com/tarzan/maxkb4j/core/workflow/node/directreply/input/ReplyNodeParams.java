package com.tarzan.maxkb4j.core.workflow.node.directreply.input;

import com.tarzan.maxkb4j.core.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ReplyNodeParams extends BaseParams {
    private String replyType;
    private List<String> fields;
    private String content;
    private Boolean isResult;
    @Override
    public boolean isValid() {
        return false;
    }
}
