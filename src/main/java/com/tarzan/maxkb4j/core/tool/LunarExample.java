package com.tarzan.maxkb4j.core.tool;

import cn.hutool.core.date.ChineseDate;
import java.time.LocalDate;

public class LunarExample {
    public static void main(String[] args) {
        // 指定一个公历日期（例如今天：2025-12-10）
        LocalDate solarDate = LocalDate.now();

        // 创建 ChineseDate 对象（自动转换为农历）
        ChineseDate lunar = new ChineseDate(solarDate);

        // 输出农历信息
        System.out.println("农历日期：" + lunar.getTerm());           // 如：二〇二五年十一月十一
        System.out.println("农历年：" + lunar.getChineseYear());       // 如：二〇二五
        System.out.println("农历月：" + lunar.getChineseMonth());      // 如：十一月
        System.out.println("农历日：" + lunar.getChineseDay());        // 如：十一
        System.out.println("生肖：" + lunar.getChineseZodiac());       // 如：蛇
        System.out.println("天干地支年：" + lunar.getCyclicalYMD());     // 如：乙巳
        System.out.println("是否闰月：" + lunar.isLeapMonth());       // true/false
    }
}
