package com.maxkb4j.core.support;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.core.support.vo.UserResourcePermissionVO;

import java.util.Map;
import java.util.Set;

/**
 * 资源权限分页查询 SPI。
 *
 * <p>不同资源类型（应用/知识库/工具/模型）的分页查询本属各自业务领域，
 * 由各业务模块实现并在运行期注入，system 模块的
 * {@code UserResourcePermissionServiceImpl} 消费，
 * 避免 system 模块反向编译依赖各业务模块。
 *
 * @author tarzan
 */
public interface IResourcePermissionPageProvider {

    /**
     * 该 provider 支持的资源类型，对应 {@code AuthTargetType} 常量值。
     */
    String supportType();

    /**
     * 按名称与权限过滤分页查询资源，返回资源权限 VO 页。
     *
     * @param current          当前页
     * @param size             每页大小
     * @param name             资源名称模糊匹配（可空）
     * @param permissionMap    targetId -> 已解析权限（MANAGE/VIEW/NOT_AUTH）；未在 map 中的视为 NOT_AUTH
     * @param permissionFilter 权限过滤集合（VIEW/MANAGE/NOT_AUTH），空表示不限
     */
    IPage<UserResourcePermissionVO> pageResource(int current, int size, String name,
                                                 Map<String, String> permissionMap,
                                                 Set<String> permissionFilter);
}
