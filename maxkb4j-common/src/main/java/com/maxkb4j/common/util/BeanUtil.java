package com.maxkb4j.common.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.FeatureDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Bean拷贝工具类
 *
 * @author tarzan liu
 * @since JDK1.8
 * @date 2021年5月11日
 */
@Slf4j
public class BeanUtil {


    /**
     * 方法描述 不copy为null的属性
     *
     * @param source
     * @param target
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
            Constructor<T> constructor = target.getConstructor();
            // 使用构造函数实例化对象
            T newInstance = constructor.newInstance();
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
            return new ArrayList<>();
        }
        return source.stream().map(e -> copy(e, target)).collect(Collectors.toList());
    }

    public static <T, R> List<R> copyList(List<T> source, Function<? super T, ? extends R> mapper) {
        if (null == source || source.isEmpty()) {
            return new ArrayList<>();
        }
        return source.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }


    public static <T> Map<String, T> toMap(Object requestParameters) {
        if (requestParameters == null) {
            return new HashMap<>();
        }

        Map<String, T> map = new HashMap<>();
        Class<?> clazz = requestParameters.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            try {
                // 尝试设置为可访问
                field.setAccessible(true);

                Object value = field.get(requestParameters);
                String key = field.getName();

                // 检查值是否非空且不为空白字符串
                if (value != null && !value.toString().trim().isEmpty()) {
                    map.put(key, (T) value);
                }
            } catch (IllegalAccessException e) {
                // 处理访问失败的情况
                log.error("Failed to access field {}: {}", field.getName(), e.getMessage());
            } catch (Exception e) {
                // 捕获其他可能的异常，如InaccessibleObjectException
                log.error("Unexpected error with field {}: {}", field.getName(), e.getMessage());
            }
        }

        return map;
    }


    /**
     * 复制分页对象：将 IPage<S> 的记录逐条拷贝为 target 类型，保留分页元数据。
     */
    public static <S, T> IPage<T> copyPage(IPage<S> source, Class<T> target) {
        return copyPage(source, r -> copy(r, target));
    }

    public static <S, T> IPage<T>  copyPage(IPage<S> source, Function<? super S, ? extends T> mapper) {
        Page<T> page = new Page<>();
        if (source == null) {
            return page;
        }
        page.setCurrent(source.getCurrent());
        page.setSize(source.getSize());
        page.setTotal(source.getTotal());
        // 对数据列表进行转换
        List<S> originalList = source.getRecords();
        List<T> transformedList = originalList.stream()
                .map(mapper)
                .collect(Collectors.toList());
        // 设置转换后的列表到新页面
        page.setRecords(transformedList);
        return page;
    }
}