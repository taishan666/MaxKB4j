package com.maxkb4j.application.dto;

import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
public class ApplicationBatchEditDTO {
    @NonNull
    private List<String> idList;
    private Integer cleanTime;
    private Integer fileCleanTime;
}
