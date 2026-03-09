package com.maxkb4j.application.dto;

import lombok.Data;

@Data
public class ApplicationQuery {
    private String name;
    private String publishStatus;
    private String createUser;
    private String folderId;
    private String type;
}
