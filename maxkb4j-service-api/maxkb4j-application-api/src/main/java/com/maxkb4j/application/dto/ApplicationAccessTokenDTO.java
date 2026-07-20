package com.maxkb4j.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class ApplicationAccessTokenDTO {
    private String applicationId;

    private String accessToken;

    private Boolean isActive;

    private Integer accessNum;

    private Boolean whiteActive;

    private List<String> whiteList;

    private Boolean showSource;

    private Boolean showExec;

    private Boolean authentication;

    private String language;

    private Boolean accessTokenReset;
}
