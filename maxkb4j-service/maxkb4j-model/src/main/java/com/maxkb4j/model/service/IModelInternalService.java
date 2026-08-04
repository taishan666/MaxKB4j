package com.maxkb4j.model.service;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.model.dto.ModelQuery;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.vo.ModelListVO;
import com.maxkb4j.model.vo.ModelVO;

import java.util.List;

/**
 * 模型服务「对内」接口：供 Controller / 本模块内部使用的完整服务契约。
 *
 * <p>与 {@link IModelService}（对外跨模块契约，位于 maxkb4j-model-api）区分：
 * <ul>
 *   <li>对外接口仅暴露跨模块所需的少量方法，且只能引用 -api 模块可见的类型；</li>
 *   <li>本对内接口位于 service 模块，可引用 {@link ModelEntity} 等 service 模块类型，
 *       并继承 {@link IService} 获得 MyBatis-Plus 通用方法（getById/updateById 等），
 *       从而让 Controller 依赖抽象而非具体实现 {@code ModelServiceImpl}。</li>
 * </ul>
 */
public interface IModelInternalService extends IModelService, IService<ModelEntity> {

    List<ModelVO> models(ModelQuery  query);

    List<ModelListVO> modelList(ModelQuery  query);

    boolean createModel(ModelEntity model);

    ModelEntity updateModel(String id, ModelEntity model);

    Boolean removeModelById(String id);

    ModelEntity getInfo(String id);

    ModelEntity getModelById(String id);

    void updateModelParamsForm(String id, JSONArray paramsForm);
}
