package com.maxkb4j.model.dto;

import com.alibaba.fastjson.JSONArray;
import com.maxkb4j.common.mp.entity.ModelCredential;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建模型入参：仅包含客户端可提供的字段，
 * userId / meta / status 等由服务端强制设置。
 *
 * @author tarzan
 */
@Data
public class ModelCreateDTO {

    @NotBlank(message = "模型名称不能为空")
    private String name;

    @NotBlank(message = "模型类型不能为空")
    private String modelType;

    @NotBlank(message = "基础模型不能为空")
    private String modelName;

    @NotBlank(message = "供应商不能为空")
    private String provider;

    private ModelCredential credential;

    private JSONArray modelParamsForm;
}
