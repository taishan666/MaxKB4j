package com.tarzan.maxkb4j.common.util;


import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
/**
 *  批处理工具类
 *
 * @version 1.0
 * @since JDK1.8
 * @author tarzan
 * @date 2022年01月28日 16:39:50
 */
public class BatchUtil {

    /**
     * 批次数量
     */
    public static final Integer NUMBER_BACH_PROTECT = 999;


    /**
     * 批量中包含有关联查询，可能因为数据过长出现数据查询的的问题，
     * 这里进行分批处理
     * @param list
     * 需要处理的list
     * @param bach
     * 批量处理的函数
     * @param <T>
     *     元素类型
     */
    public static <T> void protectBach(List<T> list, Consumer<List<T>> bach){
        if (isEmpty(list)){return;}
        if (list.size() > NUMBER_BACH_PROTECT) {
            for (int i = 0; i < list.size(); i += NUMBER_BACH_PROTECT) {
                int lastIndex = Math.min(i + NUMBER_BACH_PROTECT, list.size());
                bach.accept(list.subList(i, lastIndex));
            }
        }else {
            bach.accept(list);
        }
    }

    /**
     * 批量中包含有关联查询，可能因为数据过长出现数据查询的的问题
     * 这里进行分批，合并数据处理。
     * @param list
     *  原数据
     * @param bach
     *  处理方法
     * @param <T>
     *     原数据类型
     * @param <R>
     *     处理结果类型
     * @return
     *  处理结果汇总
     */
    public static <T, R> List<R> protectBach(List<T> list, Function<List<T>, List<R>> bach){
        if (isEmpty(list)){return Collections.emptyList();}
        if (list.size() > NUMBER_BACH_PROTECT) {
            List<R> end = new LinkedList<>();
            for (int i = 0; i < list.size(); i += NUMBER_BACH_PROTECT) {
                int lastIndex = Math.min(i + NUMBER_BACH_PROTECT, list.size());
                Optional.ofNullable(bach.apply(list.subList(i, lastIndex)))
                        .ifPresent(end::addAll);
            }
            return end;
        }
        return bach.apply(list);
    }

    private static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }


}