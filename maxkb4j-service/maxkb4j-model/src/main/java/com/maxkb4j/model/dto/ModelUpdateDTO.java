package com.maxkb4j.model.dto;

import com.maxkb4j.common.mp.entity.ModelCredential;
import lombok.Data;

/**
 * 更新模型入参：仅包含可编辑字段；
 * provider / userId 等归属字段不允许从客户端变更。
 *
 * @author tarzan
 */
@Data
public class ModelUpdateDTO {

    private String name;

    private String modelType;

    private String modelName;

    private ModelCredential credential;
}
