package com.tarzan.maxkb4j.module.folder.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class FolderEntity {
    private String id;
    private String name;
    private String parentId;
    private String userId;
    private String workspaceId;
    private List<FolderEntity> children;
}
