package com.tarzan.maxkb4j.core.workflow.node.application.input;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ApplicationNodeParams {
    @NotBlank(message = "应用图标")
    private String icon;
    @NotBlank(message = "应用名称")
    private String name;
    @NotBlank(message = "应用id不能为空")
    private String applicationId;
    @NotNull(message = "用户问题不能为空")
    private List<String> questionReferenceAddress;

    private List<String> apiInputFieldList; // Optional

    private List<String> userInputFieldList; // Optional,

    private List<String> imageList; // Optional

    private List<String> documentList; // Optional

    private List<String> audioList; // Optional

    private List<String> otherList;


}
