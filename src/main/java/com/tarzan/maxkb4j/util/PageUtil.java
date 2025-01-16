package com.tarzan.maxkb4j.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PageUtil {


    /**
     * @Description:转换为 IPage 对象
     * @Author: tarzan
     * @Date: 2025/01/16 9:40
     */
    public static <T, E> IPage<T> copy(IPage<E> page, List<E> sourceList, Class<T> targetClazz) {
        IPage<T> pageResult = new Page<>(page.getCurrent(),page.getSize(),page.getTotal());
        pageResult.setTotal(page.getTotal());
        pageResult.setSize(page.getSize());
        List<T> records = Collections.singletonList(BeanUtil.copy(sourceList, targetClazz));
        pageResult.setRecords(records);
        return pageResult;
    }

    /**
     * @Description:转换为 IPage 对象
     * @Author: tarzan
     * @Date: 2025/01/16 9:40
     */
    public static <T, E> IPage<T> copy(IPage<E> page, Class<T> targetClazz) {
        return copy(page,page.getRecords(),targetClazz);
    }

    public static <T, R> IPage<R> copy(IPage<T> page, Function<? super T, ? extends R> mapper) {
         // 创建新的页面对象
        IPage<R> newPage = new Page<>();
        // 设置分页信息
        newPage.setCurrent(page.getCurrent());
        newPage.setSize(page.getSize());
        newPage.setTotal(page.getTotal());

        // 对数据列表进行转换
        List<T> originalList = page.getRecords();
        List<R> transformedList = originalList.stream()
                .map(mapper)
                .collect(Collectors.toList());

        // 设置转换后的列表到新页面
        newPage.setRecords(transformedList);
        return newPage;
    }


}
