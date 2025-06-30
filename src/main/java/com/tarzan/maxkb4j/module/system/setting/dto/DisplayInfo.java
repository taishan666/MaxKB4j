package com.tarzan.maxkb4j.module.system.setting.dto;

import lombok.Data;

@Data
public class DisplayInfo {
    private Boolean showUserManual;
    private String userManualUrl;
    private Boolean showForum;
    private String forumUrl;
    private Boolean showProject;
    private String projectUrl;
    private String theme;
    private String title;
    private String slogan;
}
