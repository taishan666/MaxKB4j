package com.tarzan.maxkb4j.common.aop;

import cn.dev33.satoken.exception.NotPermissionException;
import com.tarzan.maxkb4j.common.util.StpKit;
import com.tarzan.maxkb4j.module.system.user.enums.PermissionEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Aspect
@Component
public class SaCheckPermAspect {



    @Around("@annotation(saCheckPerm)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, SaCheckPerm saCheckPerm) throws Throwable {
        // 获取 HttpServletRequest
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String requiredPermission = getString(saCheckPerm, attributes);
        //校验权限（精确匹配）
        if (!StpKit.ADMIN.hasPermission(requiredPermission)) {
            throw new NotPermissionException(requiredPermission);
        }
        // 放行
        return joinPoint.proceed();
    }

    private String getString(SaCheckPerm saCheckPerm, ServletRequestAttributes attributes) {
        if (attributes == null) {
            throw new RuntimeException("无法获取请求上下文");
        }
        HttpServletRequest request = attributes.getRequest();
        // 获取路径变量（Spring MVC 在 HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE 中存储了路径参数）
        @SuppressWarnings("unchecked")
        Map<String, String> pathVars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        // 解析 resourcePath 模板，替换 {xxx} 为实际值
        PermissionEnum permission = saCheckPerm.value();
        String actualPath = permission.getResource() + ":" + permission.getOperate() + ":/WORKSPACE/default/" + permission.getResourceType() + "/{id}";
        if (pathVars != null) {
            for (Map.Entry<String, String> entry : pathVars.entrySet()) {
                actualPath = actualPath.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        actualPath = actualPath.replace("{id}", "default");
        return actualPath;
    }
}