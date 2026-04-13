package com.maxkb4j.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class ShareChatDTO {
    private List<String> chatRecordIds;
    private Boolean isCurrentAll;
}
