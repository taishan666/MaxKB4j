package com.maxkb4j.knowledge.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文档任务取消入参：type=1 取消向量化，type=2 取消问题生成。
 *
 * @author tarzan
 */
@Data
public class DocumentTaskCancelDTO {

    @NotNull(message = "任务类型不能为空")
    private Integer type;
}
