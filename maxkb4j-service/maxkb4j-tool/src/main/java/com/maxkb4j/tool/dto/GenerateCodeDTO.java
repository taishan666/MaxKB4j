package com.maxkb4j.tool.dto;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.MessageDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class GenerateCodeDTO {
   private JSONArray inputFieldList;
   private JSONArray initFieldList;
   @NotBlank
   private String modelId;
   private JSONObject modelParamsSetting;
   private List<MessageDTO> messages;
   @NotBlank
   private String prompt;
}
