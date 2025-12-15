package com.tarzan.maxkb4j.module.folder.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@NoArgsConstructor
@Data
public class FolderVO {
    private String id;
    private String name;
    private String desc;
    private String parentId;
    private String workspaceId;
    private List<FolderVO> children;

    public FolderVO(String id, String name) {
        this.id = id;
        this.name = name;
        this.desc = "";
        this.parentId = null;
        this.workspaceId = "default";
        this.children = List.of();
    }
}
