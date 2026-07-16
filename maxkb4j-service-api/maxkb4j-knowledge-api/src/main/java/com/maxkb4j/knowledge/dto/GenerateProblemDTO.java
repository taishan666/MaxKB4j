package com.maxkb4j.knowledge.dto;

import com.alibaba.fastjson.JSONObject;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class GenerateProblemDTO {
    private List<String> documentIdList;
    private List<String> paragraphIdList;
    @NotBlank(message = "模型ID不能为空")
    private String modelId;
    private JSONObject modelParamsSetting;
    private Integer number;
    private String prompt;
    private List<String> stateList;
}
