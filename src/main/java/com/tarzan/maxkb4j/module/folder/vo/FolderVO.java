package com.tarzan.maxkb4j.module.folder.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class FolderVO {
    private String id;
    private String name;
    private String parentId;
    private String workspaceId;
    private List<FolderVO> children;
}
