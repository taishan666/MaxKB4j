package com.tarzan.maxkb4j.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DeleteDTO {

    @JsonProperty("id_list")
    private List<UUID> idList;
}
