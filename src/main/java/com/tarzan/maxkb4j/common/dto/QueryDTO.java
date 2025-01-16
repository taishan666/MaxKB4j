package com.tarzan.maxkb4j.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class QueryDTO {
    private String name;
    @JsonProperty("select_user_id")
    private UUID selectUserId;
}
