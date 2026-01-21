package com.tarzan.maxkb4j.module.system.user.enums;

import com.tarzan.maxkb4j.module.system.user.constants.Operate;
import com.tarzan.maxkb4j.module.system.user.constants.Permission;
import com.tarzan.maxkb4j.module.system.user.constants.ResourceType;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum PermissionEnum {


    APPLICATION_CREATE(ResourceType.APPLICATION, "APPLICATION", Operate.CREATE, Permission.MANAGE),
    APPLICATION_EDIT(ResourceType.APPLICATION, "APPLICATION", Operate.EDIT, Permission.MANAGE),
    APPLICATION_DELETE(ResourceType.APPLICATION, "APPLICATION", Operate.DELETE, Permission.MANAGE),
    APPLICATION_READ(ResourceType.APPLICATION, "APPLICATION", Operate.READ, Permission.VIEW),
    APPLICATION_EXPORT(ResourceType.APPLICATION, "APPLICATION", Operate.EXPORT, Permission.MANAGE),
    APPLICATION_IMPORT(ResourceType.APPLICATION, "APPLICATION", Operate.IMPORT, Permission.MANAGE),

    APPLICATION_OVERVIEW_PUBLIC_ACCESS(ResourceType.APPLICATION, "APPLICATION_OVERVIEW",Operate.PUBLIC_ACCESS, Permission.MANAGE),
    APPLICATION_OVERVIEW_ACCESS(ResourceType.APPLICATION, "APPLICATION_OVERVIEW", Operate.ACCESS, Permission.MANAGE),
    APPLICATION_OVERVIEW_EMBED(ResourceType.APPLICATION, "APPLICATION_OVERVIEW", Operate.EMBED, Permission.MANAGE),
    APPLICATION_OVERVIEW_API_KEY(ResourceType.APPLICATION, "APPLICATION_OVERVIEW", Operate.API_KEY, Permission.MANAGE),
    APPLICATION_OVERVIEW_DISPLAY(ResourceType.APPLICATION, "APPLICATION_OVERVIEW", Operate.DISPLAY, Permission.MANAGE),
    APPLICATION_OVERVIEW_READ(ResourceType.APPLICATION, "APPLICATION_OVERVIEW", Operate.READ, Permission.VIEW),

    APPLICATION_CHAT_LOG_ADD_KNOWLEDGE(ResourceType.APPLICATION, "APPLICATION_CHAT_LOG", Operate.ADD_KNOWLEDGE, Permission.MANAGE),
    APPLICATION_CHAT_LOG_CLEAR_POLICY(ResourceType.APPLICATION, "APPLICATION_CHAT_LOG", Operate.CLEAR_POLICY, Permission.MANAGE),
    APPLICATION_CHAT_LOG_ANNOTATION(ResourceType.APPLICATION, "APPLICATION_CHAT_LOG", Operate.ANNOTATION, Permission.MANAGE),
    APPLICATION_CHAT_LOG_EXPORT(ResourceType.APPLICATION, "APPLICATION_CHAT_LOG", Operate.EXPORT, Permission.MANAGE),
    APPLICATION_CHAT_LOG_READ(ResourceType.APPLICATION, "APPLICATION_CHAT_LOG", Operate.READ, Permission.VIEW),

    APPLICATION_ACCESS_READ(ResourceType.APPLICATION, "APPLICATION_ACCESS", Operate.READ, Permission.VIEW),
    APPLICATION_ACCESS_EDIT(ResourceType.APPLICATION, "APPLICATION_ACCESS", Operate.EDIT, Permission.MANAGE),

    KNOWLEDGE_CREATE(ResourceType.KNOWLEDGE, "KNOWLEDGE", Operate.CREATE, Permission.MANAGE),
    KNOWLEDGE_EDIT(ResourceType.KNOWLEDGE, "KNOWLEDGE", Operate.EDIT, Permission.MANAGE),
    KNOWLEDGE_DELETE(ResourceType.KNOWLEDGE, "KNOWLEDGE", Operate.DELETE, Permission.MANAGE),
    KNOWLEDGE_READ(ResourceType.KNOWLEDGE, "KNOWLEDGE", Operate.READ, Permission.VIEW),
    KNOWLEDGE_EXPORT(ResourceType.KNOWLEDGE, "KNOWLEDGE", Operate.EXPORT, Permission.MANAGE),
    KNOWLEDGE_VECTOR(ResourceType.KNOWLEDGE, "KNOWLEDGE", Operate.VECTOR, Permission.MANAGE),
    KNOWLEDGE_GENERATE(ResourceType.KNOWLEDGE, "KNOWLEDGE", Operate.GENERATE, Permission.MANAGE),
    KNOWLEDGE_SYNC(ResourceType.KNOWLEDGE, "KNOWLEDGE", Operate.SYNC, Permission.MANAGE),
    KNOWLEDGE_HIT_TEST_READ(ResourceType.KNOWLEDGE, "KNOWLEDGE_HIT_TEST", Operate.READ, Permission.VIEW),

    KNOWLEDGE_WORKFLOW_READ(ResourceType.KNOWLEDGE, "KNOWLEDGE_WORKFLOW", Operate.READ, Permission.VIEW),
    KNOWLEDGE_WORKFLOW_EDIT(ResourceType.KNOWLEDGE, "KNOWLEDGE_WORKFLOW", Operate.EDIT, Permission.MANAGE),

    KNOWLEDGE_DOCUMENT_CREATE(ResourceType.KNOWLEDGE, "KNOWLEDGE_DOCUMENT", Operate.CREATE, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_EDIT(ResourceType.KNOWLEDGE, "KNOWLEDGE_DOCUMENT", Operate.EDIT, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_DELETE(ResourceType.KNOWLEDGE, "KNOWLEDGE_DOCUMENT", Operate.DELETE, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_VECTOR(ResourceType.KNOWLEDGE, "KNOWLEDGE_DOCUMENT", Operate.VECTOR, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_GENERATE(ResourceType.KNOWLEDGE, "KNOWLEDGE_DOCUMENT", Operate.GENERATE, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_SYNC(ResourceType.KNOWLEDGE, "KNOWLEDGE_DOCUMENT", Operate.SYNC, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_MIGRATE(ResourceType.KNOWLEDGE, "KNOWLEDGE_DOCUMENT", Operate.MIGRATE, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_DOWNLOAD(ResourceType.KNOWLEDGE, "KNOWLEDGE_DOCUMENT", Operate.DOWNLOAD, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_REPLACE(ResourceType.KNOWLEDGE, "KNOWLEDGE_DOCUMENT", Operate.REPLACE, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_EXPORT(ResourceType.KNOWLEDGE, "KNOWLEDGE_DOCUMENT", Operate.EXPORT, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_READ(ResourceType.KNOWLEDGE, "KNOWLEDGE_DOCUMENT", Operate.READ, Permission.VIEW),

    KNOWLEDGE_PROBLEM_CREATE(ResourceType.KNOWLEDGE, "KNOWLEDGE_PROBLEM", Operate.CREATE, Permission.MANAGE),
    KNOWLEDGE_PROBLEM_EDIT(ResourceType.KNOWLEDGE, "KNOWLEDGE_PROBLEM", Operate.EDIT, Permission.MANAGE),
    KNOWLEDGE_PROBLEM_DELETE(ResourceType.KNOWLEDGE, "KNOWLEDGE_PROBLEM", Operate.DELETE, Permission.MANAGE),
    KNOWLEDGE_PROBLEM_RELATE(ResourceType.KNOWLEDGE, "KNOWLEDGE_PROBLEM", Operate.RELATE, Permission.MANAGE),
    KNOWLEDGE_PROBLEM_READ(ResourceType.KNOWLEDGE, "KNOWLEDGE_PROBLEM", Operate.READ, Permission.VIEW),

    TOOL_CREATE(ResourceType.TOOL, "TOOL", Operate.CREATE, Permission.MANAGE),
    TOOL_DEBUG(ResourceType.TOOL, "TOOL", Operate.DEBUG, Permission.MANAGE),
    TOOL_EDIT(ResourceType.TOOL, "TOOL", Operate.EDIT, Permission.MANAGE),
    TOOL_DELETE(ResourceType.TOOL, "TOOL", Operate.DELETE, Permission.MANAGE),
    TOOL_READ(ResourceType.TOOL, "TOOL", Operate.READ, Permission.VIEW),
    TOOL_IMPORT(ResourceType.TOOL, "TOOL", Operate.IMPORT, Permission.MANAGE),
    TOOL_EXPORT(ResourceType.TOOL, "TOOL", Operate.EXPORT, Permission.MANAGE),

    MODEL_CREATE(ResourceType.MODEL, "MODEL", Operate.CREATE, Permission.MANAGE),
    MODEL_EDIT(ResourceType.MODEL, "MODEL", Operate.EDIT, Permission.MANAGE),
    MODEL_DELETE(ResourceType.MODEL, "MODEL", Operate.DELETE, Permission.MANAGE),
    MODEL_READ(ResourceType.MODEL, "MODEL", Operate.READ, Permission.VIEW),
    ;

    private final String resourceType;
    private final String resource;
    private final String operate;
    private final String permission;

    PermissionEnum(String resourceType, String resource, String operate, String permission) {
        this.resourceType = resourceType;
        this.resource = resource;
        this.operate = operate;
        this.permission = permission;
    }


    private static List<PermissionEnum> getPermissions(String resourceType) {
        return Arrays.stream(values()).filter(e -> e.resourceType.equals(resourceType))
                .collect(Collectors.toList());
    }

    public static List<PermissionEnum> getPermissions(String resourceType, List<String> permissionList) {
        return getPermissions(resourceType).stream().filter(e -> permissionList.contains(e.getPermission())).toList();
    }


    public  String getWorkspaceResourcePerm(String workspaceId,String targetId) {
        return   resource + ":" + operate + ":/WORKSPACE/" + workspaceId + "/" + resourceType + "/" + targetId;
    }

}


