package com.tarzan.maxkb4j.module.application.workflow.node.directreply.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ReplyNodeParams extends BaseParams {
    @JsonProperty("reply_type")
    private String replyType;
    private List<String> fields;
    private String content;
    @JsonProperty("is_result")
    private Boolean isResult;
    @Override
    public boolean isValid() {
        return false;
    }
}
