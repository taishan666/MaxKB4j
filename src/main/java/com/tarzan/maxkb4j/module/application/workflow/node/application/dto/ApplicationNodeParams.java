package com.tarzan.maxkb4j.module.application.workflow.node.application.dto;

import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationNodeParams extends BaseParams {
    @NotBlank(message = "应用id不能为空")
    private String applicationId;

    @NotNull(message = "用户问题不能为空")
    private List<String> questionReferenceAddress;

    private List<String> apiInputFieldList; // Optional

    private List<String> userInputFieldList; // Optional,

    private List<String> imageList; // Optional

    private List<String> documentList; // Optional

    private List<String> audioList; // Optional

    private Map<String, Object> childNode; // Optional, allowing null

    private Map<String, Object> nodeData; // Optional, allowing null

    @Override
    public boolean isValid() {
        return false;
    }
}
