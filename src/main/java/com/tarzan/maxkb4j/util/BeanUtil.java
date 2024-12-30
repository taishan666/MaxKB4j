package com.tarzan.maxkb4j.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.FeatureDescriptor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Bean拷贝工具类
 *
 * @author tarzan liu
 * @since JDK1.8
 * @date 2021年5月11日
 */
public class BeanUtil {


    /**
     * 方法描述 不copy为null的属性
     *
     * @param @param source
     * @param @param target
     * @return void
     * @throws
     */
    public static void copyPropertiesExcludeNull(Object source, Object target) {
        BeanWrapperImpl wrappedSource = new BeanWrapperImpl(source);
        String[] ignoreProperties = Stream.of(wrappedSource.getPropertyDescriptors()).map(FeatureDescriptor::getName).filter(propertyName -> wrappedSource.getPropertyValue(propertyName) == null)
                .toArray(String[]::new);
        BeanUtils.copyProperties(source, target, ignoreProperties);
    }

    /**
     * 复制bean的属性
     *
     * @param source 源 要复制的对象
     * @param target 目标 复制到此对象
     */
    public static void copyProperties(Object source, Object target) {
        BeanUtils.copyProperties(source, target);
    }

    /**
     * 复制对象
     *
     * @param source 源 要复制的对象
     * @param target 目标 复制到此对象
     * @param <T>
     * @return
     */
    public static <T> T copy(Object source, Class<T> target) {
        try {
            T newInstance = target.newInstance();
            BeanUtils.copyProperties(source, newInstance);
            return newInstance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 复制list
     *
     * @param source
     * @param target
     * @param <T>
     * @param <K>
     * @return
     */
    public static <T, K> List<K> copyList(List<T> source, Class<K> target) {
        if (null == source || source.isEmpty()) {
            return Collections.emptyList();
        }
        return source.stream().map(e -> copy(e, target)).collect(Collectors.toList());
    }

    public static <T> Map<String, T> objectToMap(Object requestParameters) {
        Map<String, T> map = new HashMap<>(10);
        // 获取f对象对应类中的所有属性域
        Field[] fields = requestParameters.getClass().getDeclaredFields();
        for (Field field : fields) {
            String varName = field.getName();
            // 获取原来的访问控制权限
            boolean accessFlag = field.isAccessible();
            // 修改访问控制权限
            field.setAccessible(true);
            // 获取在对象f中属性fields[i]对应的对象中的变量
            Object o = null;
            try {
                o = field.get(requestParameters);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (o != null && StringUtils.isNotBlank(o.toString().trim())) {
                map.put(varName, (T) o);
                // 恢复访问控制权限
                field.setAccessible(accessFlag);
            }
        }
        return map;
    }
}