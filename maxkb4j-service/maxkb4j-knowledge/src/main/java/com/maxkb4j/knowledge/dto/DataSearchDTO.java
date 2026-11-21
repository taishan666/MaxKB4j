package com.maxkb4j.knowledge.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DataSearchDTO {
    @NotBlank(message = "查询内容不能为空")
    private String queryText;
    private String searchMode;
    /**
     * 上限取 2 以兼容混合检索的综合得分（向量+全文相加可超过 1），与前端阈值上限一致。
     */
    @NotNull(message = "相似度阈值不能为空")
    @DecimalMin(value = "0", message = "相似度阈值不能小于0")
    @DecimalMax(value = "2", message = "相似度阈值不能大于2")
    private Float similarity;
    @NotNull(message = "引用条数不能为空")
    @Min(value = 1, message = "引用条数不能小于1")
    @Max(value = 10000, message = "引用条数不能大于10000")
    private Integer topNumber;
    private List<String> excludeParagraphIds;
}
