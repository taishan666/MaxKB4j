package com.maxkb4j.system.util;

import java.util.List;

public record PermissionDef(String authType, List<String> permissionList) {
}
