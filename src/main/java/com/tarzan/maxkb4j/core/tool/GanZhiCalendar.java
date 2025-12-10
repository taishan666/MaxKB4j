package com.tarzan.maxkb4j.core.tool;

import cn.hutool.core.date.ChineseDate;
import cn.hutool.core.date.chinese.GanZhi;

import java.time.LocalDateTime;
import java.util.Arrays;

public class GanZhiCalendar {

    // 十天干
    private static final String[] TIAN_GAN = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};

    // 十二地支
    private static final String[] DI_ZHI = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};

    public static String toDayGan(LocalDateTime date) {
        ChineseDate lunar = new ChineseDate(date.toLocalDate());
        return GanZhi.getGanzhiOfDay(lunar.getChineseYear(), lunar.getMonth(), lunar.getDay()).substring(0, 1);
    }

    public static String toLunarDate(LocalDateTime date) {
        ChineseDate lunar = new ChineseDate(date.toLocalDate());
        return lunar.getChineseYear() + "年" + lunar.getChineseMonth() + "月" + lunar.getChineseDay() + "日"+getDiZhiHour(date)+"时";
    }


    public static String toGanZhi(LocalDateTime date) {
        ChineseDate lunar = new ChineseDate(date.toLocalDate());
        String ganZhi = lunar.getCyclicalYMD();
        ganZhi = ganZhi.replace("年", "年 ");
        ganZhi = ganZhi.replace("月", "月 ");
        return ganZhi + " " + getGanZhiHour(date);
    }


    private static String getDiZhiHour(LocalDateTime date) {
        int hour = date.getHour();
        int shichen = (hour + 1) / 2 % 12; // 0=子,1=丑,...,11=亥
        if (hour == 23) shichen = 0; // 特殊处理23点属于子时
        return DI_ZHI[shichen];
    }

    private static String getGanZhiHour(LocalDateTime date) {
        int hour = date.getHour();
        int shichen = (hour + 1) / 2 % 12; // 0=子,1=丑,...,11=亥
        if (hour == 23) shichen = 0; // 特殊处理23点属于子时
        String dayGan = toDayGan(date);
        int dayGanIndex = Arrays.asList(TIAN_GAN).indexOf(dayGan);
        // 五鼠遁口诀：
        // 甲己还加甲，乙庚丙作初，
        // 丙辛从戊起，丁壬庚子居，
        // 戊癸何方发，壬子是真途。
        int ziShiGanIndex = switch (dayGanIndex) {
            case 0, 5 -> // 甲、己
                    0; // 甲
            case 1, 6 -> // 乙、庚
                    2; // 丙
            case 2, 7 -> // 丙、辛
                    4; // 戊
            case 3, 8 -> // 丁、壬
                    6; // 庚
            case 4, 9 -> // 戊、癸
                    8; // 壬
            default -> 0;
        };
        int ganIndex = (ziShiGanIndex + shichen) % 10;
        return TIAN_GAN[ganIndex] + getDiZhiHour(date) + "时";
    }


}
