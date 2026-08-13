package com.maxkb4j.system.dto;

import com.maxkb4j.core.support.permission.PermissionScopeAware;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class HomeQuery implements PermissionScopeAware {
    @NotBlank
    private String startTime;
    @NotBlank
    private String endTime;
    private String name;
    private String applicationId;

    /* ==================== 数据权限范围（由 DataPermissionSupport.fill 注入） ==================== */

    private List<String> targetIds;
    private Boolean isAdmin = false;
}
