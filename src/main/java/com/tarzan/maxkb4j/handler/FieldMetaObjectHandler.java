package com.tarzan.maxkb4j.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;


@Slf4j
@Component
public class FieldMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 检查id字段是否为空，如果为空则填充
        if (this.getFieldValByName("id", metaObject) == null) {
            this.setFieldValByName("id", UUID.randomUUID(), metaObject);
        }
        this.setFieldValByName("createTime", new Date(), metaObject);
        this.setFieldValByName("updateTime", new Date(), metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updateTime", new Date(), metaObject);
    }

    /**
     * 获取指定名称的字段值。
     *
     * @param fieldName 字段名称
     * @param metaObject 元对象
     * @return 字段值
     */
    public Object getFieldValByName(String fieldName, MetaObject metaObject) {
        return metaObject.getValue(fieldName);
    }
}


