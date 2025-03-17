package com.tarzan.maxkb4j.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class QueryDTO {
    private String name;
    private String selectUserId;
}
