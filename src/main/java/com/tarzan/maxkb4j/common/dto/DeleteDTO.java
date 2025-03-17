package com.tarzan.maxkb4j.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DeleteDTO {

    private List<String> idList;
}
