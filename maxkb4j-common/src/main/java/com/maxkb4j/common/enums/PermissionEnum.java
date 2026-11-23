package com.maxkb4j.common.enums;

import com.maxkb4j.common.constant.*;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum PermissionEnum {


    APPLICATION_CREATE(ResourceType.APPLICATION, ResourceGroup.APPLICATION, Operate.CREATE, Permission.MANAGE),
    APPLICATION_EDIT(ResourceType.APPLICATION, ResourceGroup.APPLICATION, Operate.EDIT, Permission.MANAGE),
    APPLICATION_DELETE(ResourceType.APPLICATION, ResourceGroup.APPLICATION, Operate.DELETE, Permission.MANAGE),
    APPLICATION_BATCH_DELETE(ResourceType.APPLICATION, ResourceGroup.APPLICATION, Operate.BATCH_DELETE, Permission.MANAGE),
    APPLICATION_READ(ResourceType.APPLICATION, ResourceGroup.APPLICATION, Operate.READ, Permission.VIEW),
    APPLICATION_EXPORT(ResourceType.APPLICATION, ResourceGroup.APPLICATION, Operate.EXPORT, Permission.MANAGE),
    APPLICATION_IMPORT(ResourceType.APPLICATION, ResourceGroup.APPLICATION, Operate.IMPORT, Permission.MANAGE),
    APPLICATION_RELATE_RESOURCE_VIEW(ResourceType.APPLICATION, ResourceGroup.APPLICATION, Operate.RELATE_VIEW, Permission.MANAGE),
    APPLICATION_OVERVIEW_PUBLIC_ACCESS(ResourceType.APPLICATION, ResourceGroup.APPLICATION_OVERVIEW, Operate.PUBLIC_ACCESS, Permission.MANAGE),
    APPLICATION_OVERVIEW_ACCESS(ResourceType.APPLICATION, ResourceGroup.APPLICATION_OVERVIEW, Operate.ACCESS, Permission.MANAGE),
    APPLICATION_OVERVIEW_EMBED(ResourceType.APPLICATION, ResourceGroup.APPLICATION_OVERVIEW, Operate.EMBED, Permission.MANAGE),
    APPLICATION_OVERVIEW_API_KEY(ResourceType.APPLICATION, ResourceGroup.APPLICATION_OVERVIEW, Operate.API_KEY, Permission.MANAGE),
    APPLICATION_OVERVIEW_DISPLAY(ResourceType.APPLICATION, ResourceGroup.APPLICATION_OVERVIEW, Operate.DISPLAY, Permission.MANAGE),
    APPLICATION_OVERVIEW_READ(ResourceType.APPLICATION, ResourceGroup.APPLICATION_OVERVIEW, Operate.READ, Permission.VIEW),

    APPLICATION_CHAT_LOG_ADD_KNOWLEDGE(ResourceType.APPLICATION, ResourceGroup.APPLICATION_CHAT_LOG, Operate.ADD_KNOWLEDGE, Permission.MANAGE),
    APPLICATION_CHAT_LOG_CLEAR_POLICY(ResourceType.APPLICATION, ResourceGroup.APPLICATION_CHAT_LOG, Operate.CLEAR_POLICY, Permission.MANAGE),
    APPLICATION_CHAT_LOG_ANNOTATION(ResourceType.APPLICATION, ResourceGroup.APPLICATION_CHAT_LOG, Operate.ANNOTATION, Permission.MANAGE),
    APPLICATION_CHAT_LOG_EXPORT(ResourceType.APPLICATION, ResourceGroup.APPLICATION_CHAT_LOG, Operate.EXPORT, Permission.MANAGE),
    APPLICATION_CHAT_LOG_READ(ResourceType.APPLICATION, ResourceGroup.APPLICATION_CHAT_LOG, Operate.READ, Permission.VIEW),

    APPLICATION_ACCESS_READ(ResourceType.APPLICATION, ResourceGroup.APPLICATION_ACCESS,  Operate.READ, Permission.VIEW),
    APPLICATION_ACCESS_EDIT(ResourceType.APPLICATION,  ResourceGroup.APPLICATION_ACCESS,  Operate.EDIT, Permission.MANAGE),

    KNOWLEDGE_CREATE(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE, Operate.CREATE, Permission.MANAGE),
    KNOWLEDGE_EDIT(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE, Operate.EDIT, Permission.MANAGE),
    KNOWLEDGE_DELETE(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE, Operate.DELETE, Permission.MANAGE),
    KNOWLEDGE_BATCH_DELETE(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE, Operate.BATCH_DELETE, Permission.MANAGE),
    KNOWLEDGE_READ(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE, Operate.READ, Permission.VIEW),
    KNOWLEDGE_EXPORT(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE, Operate.EXPORT, Permission.MANAGE),
    KNOWLEDGE_VECTOR(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE, Operate.VECTOR, Permission.MANAGE),
    KNOWLEDGE_GENERATE(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE, Operate.GENERATE, Permission.MANAGE),
    KNOWLEDGE_SYNC(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE, Operate.SYNC, Permission.MANAGE),
    KNOWLEDGE_HIT_TEST_READ(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_HIT_TEST, Operate.READ, Permission.VIEW),
    KNOWLEDGE_RELATE_RESOURCE_VIEW(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE, Operate.RELATE_VIEW, Permission.MANAGE),
    KNOWLEDGE_WORKFLOW_READ(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_WORKFLOW, Operate.READ, Permission.VIEW),
    KNOWLEDGE_WORKFLOW_EDIT(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_WORKFLOW,  Operate.EDIT, Permission.MANAGE),

    KNOWLEDGE_DOCUMENT_READ(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_DOCUMENT, Operate.READ, Permission.VIEW),
    KNOWLEDGE_DOCUMENT_CREATE(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_DOCUMENT, Operate.CREATE, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_EDIT(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_DOCUMENT, Operate.EDIT, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_DELETE(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_DOCUMENT, Operate.DELETE, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_VECTOR(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_DOCUMENT, Operate.VECTOR, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_GENERATE(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_DOCUMENT, Operate.GENERATE, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_SYNC(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_DOCUMENT, Operate.SYNC, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_MIGRATE(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_DOCUMENT, Operate.MIGRATE, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_DOWNLOAD(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_DOCUMENT, Operate.DOWNLOAD, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_REPLACE(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_DOCUMENT, Operate.REPLACE, Permission.MANAGE),
    KNOWLEDGE_DOCUMENT_EXPORT(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_DOCUMENT, Operate.EXPORT, Permission.MANAGE),
    KNOWLEDGE_PROBLEM_CREATE(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_PROBLEM, Operate.CREATE, Permission.MANAGE),
    KNOWLEDGE_PROBLEM_EDIT(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_PROBLEM, Operate.EDIT, Permission.MANAGE),
    KNOWLEDGE_PROBLEM_DELETE(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_PROBLEM, Operate.DELETE, Permission.MANAGE),
    KNOWLEDGE_PROBLEM_RELATE(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_PROBLEM, Operate.RELATE, Permission.MANAGE),
    KNOWLEDGE_PROBLEM_READ(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_PROBLEM, Operate.READ, Permission.VIEW),

    KNOWLEDGE_TAG_READ(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_TAG, Operate.READ, Permission.VIEW),
    KNOWLEDGE_TAG_CREATE(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_TAG, Operate.CREATE, Permission.MANAGE),
    KNOWLEDGE_TAG_EDIT(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_TAG, Operate.EDIT, Permission.MANAGE),
    KNOWLEDGE_TAG_DELETE(ResourceType.KNOWLEDGE, ResourceGroup.KNOWLEDGE_TAG, Operate.DELETE, Permission.MANAGE),

    TOOL_READ(ResourceType.TOOL, ResourceGroup.TOOL,Operate.READ, Permission.VIEW),
    TOOL_CREATE(ResourceType.TOOL, ResourceGroup.TOOL, Operate.CREATE, Permission.MANAGE),
    TOOL_DEBUG(ResourceType.TOOL, ResourceGroup.TOOL,Operate.DEBUG, Permission.MANAGE),
    TOOL_EDIT(ResourceType.TOOL, ResourceGroup.TOOL,Operate.EDIT, Permission.MANAGE),
    TOOL_DELETE(ResourceType.TOOL, ResourceGroup.TOOL,Operate.DELETE, Permission.MANAGE),
    TOOL_BATCH_DELETE(ResourceType.TOOL, ResourceGroup.TOOL,Operate.BATCH_DELETE, Permission.MANAGE),
    TOOL_IMPORT(ResourceType.TOOL, ResourceGroup.TOOL,Operate.IMPORT, Permission.MANAGE),
    TOOL_EXPORT(ResourceType.TOOL, ResourceGroup.TOOL,Operate.EXPORT, Permission.MANAGE),
    TOOL_RELATE_RESOURCE_VIEW(ResourceType.TOOL, ResourceGroup.TOOL, Operate.RELATE_VIEW, Permission.MANAGE),

    MODEL_READ(ResourceType.MODEL, ResourceGroup.MODEL, Operate.READ, Permission.VIEW),
    MODEL_CREATE(ResourceType.MODEL, ResourceGroup.MODEL, Operate.CREATE, Permission.MANAGE),
    MODEL_EDIT(ResourceType.MODEL, ResourceGroup.MODEL, Operate.EDIT, Permission.MANAGE),
    MODEL_DELETE(ResourceType.MODEL, ResourceGroup.MODEL, Operate.DELETE, Permission.MANAGE),
    MODEL_RELATE_RESOURCE_VIEW(ResourceType.MODEL, ResourceGroup.MODEL, Operate.RELATE_VIEW, Permission.MANAGE),
    ;

    /**
     * 按资源类型分组的权限枚举索引，类加载时构建一次，避免每次权限解析都遍历全部枚举。
     */
    private static final Map<String, List<PermissionEnum>> PERMISSIONS_BY_RESOURCE_TYPE =
            Arrays.stream(values()).collect(Collectors.groupingBy(PermissionEnum::getResourceType));
    private final String resourceType;
    private final String resourceGroup;
    private final String operate;
    private final String permission;


    PermissionEnum(String resourceType, String resourceGroup, String operate, String permission) {
        this.resourceType = resourceType;
        this.resourceGroup = resourceGroup;
        this.operate = operate;
        this.permission = permission;
    }

    private static List<PermissionEnum> getPermissions(String resourceType) {
        return PERMISSIONS_BY_RESOURCE_TYPE.getOrDefault(resourceType, List.of());
    }

    public static List<PermissionEnum> getPermissions(String resourceType, List<String> permissionList) {
        if (permissionList == null || permissionList.isEmpty()) {
            return List.of();
        }
        Set<String> grantedPermissions = new HashSet<>(permissionList);
        return getPermissions(resourceType).stream()
                .filter(e -> grantedPermissions.contains(e.getPermission()))
                .toList();
    }

    public String getResourcePerm() {
        return resourceGroup + ":" + operate + ":/WORKSPACE/" + AppConst.DEFAULT_ID;
    }

    public String getResourcePerm(String targetId) {
        return resourceGroup + ":" + operate + ":/WORKSPACE/" + AppConst.DEFAULT_ID + "/" + resourceType + "/" + targetId;
    }

}


