package com.maxkb4j.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.system.entity.ResourceMappingEntity;
import com.maxkb4j.system.vo.ResourceUseVO;

/**
 * 资源映射服务「对内」接口：供 Controller 使用的完整服务契约。
 *
 * <p>与 {@link IResourceMappingService}（对外跨模块契约，位于 maxkb4j-system-api）区分。
 * 本接口位于 service 模块，可引用 {@link ResourceUseVO} 等 service 模块类型，
 * 使 Controller 依赖抽象而非具体实现 {@code ResourceMappingServiceImpl}。</p>
 */
public interface IResourceMappingInternalService extends IResourceMappingService, IService<ResourceMappingEntity> {

    IPage<ResourceUseVO> selectPage(String resourceType, String resourceId, int current, int size, String resourceName, String userName, String[] sourceType);

    IPage<ResourceUseVO> selectMappingResourcePage(String resourceType, String resourceId, int current, int size, String resourceName, String userName, String[] sourceType);
}