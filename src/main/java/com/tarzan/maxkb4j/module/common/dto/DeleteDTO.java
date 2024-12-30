package com.tarzan.maxkb4j.module.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DeleteDTO {

    @JsonProperty("id_list")
    private List<String> idList;
}
