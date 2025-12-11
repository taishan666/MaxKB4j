package com.tarzan.maxkb4j.core.tool;

import cn.hutool.core.date.ChineseDate;
import cn.hutool.core.date.chinese.GanZhi;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GanZhiCalendar {

    // 十天干
    private static final String[] TIAN_GAN = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};

    // 十二地支
    private static final String[] DI_ZHI = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    // 旬首（甲X）与空亡地支的映射
    private static final Map<String, String> XUN_KONG = new HashMap<>();

    static {
        XUN_KONG.put("甲子", "戌亥");
        XUN_KONG.put("甲戌", "申酉");
        XUN_KONG.put("甲申", "午未");
        XUN_KONG.put("甲午", "辰巳");
        XUN_KONG.put("甲辰", "寅卯");
        XUN_KONG.put("甲寅", "子丑");
    }

    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.now();
        System.out.println(" 农历：" + GanZhiCalendar.toLunarDate(now));
        System.out.println(" 干支：" + GanZhiCalendar.toGanZhi(now));
        System.out.println(" 日空：" + GanZhiCalendar.getKongWang(now));
    }

    public static String getDayGanZhi(LocalDateTime date) {
        return GanZhi.getGanzhiOfDay(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }
    public static String toDayGan(LocalDateTime date) {
        return getDayGanZhi(date).substring(0, 1);
    }

    public static String toLunarDate(LocalDateTime date) {
        ChineseDate lunar = new ChineseDate(date.toLocalDate());
        return lunar.getChineseYear() + "年" + lunar.getChineseMonth() + "月" + lunar.getChineseDay() + "日" + getDiZhiHour(date) + "时";
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

    /**
     * 根据日干支（如"丙申"）计算空亡地支
     *
     * @return 空亡地支数组，如 ["辰", "巳"]
     */
    public static String getKongWang(LocalDateTime  date) {
        String dayGanZhi= getDayGanZhi(date);
        if (dayGanZhi.length() != 2) {
            throw new IllegalArgumentException("日干支格式错误，应为两个字，如'丙申'");
        }

        String gan = dayGanZhi.substring(0, 1);
        String zhi = dayGanZhi.substring(1, 2);

        // 找天干在TG中的索引
        int ganIndex = -1;
        for (int i = 0; i < TIAN_GAN.length; i++) {
            if (TIAN_GAN[i].equals(gan)) {
                ganIndex = i;
                break;
            }
        }
        if (ganIndex == -1) {
            throw new IllegalArgumentException("无效天干: " + gan);
        }

        // 找地支在DZ中的索引
        int zhiIndex = -1;
        for (int i = 0; i < DI_ZHI.length; i++) {
            if (DI_ZHI[i].equals(zhi)) {
                zhiIndex = i;
                break;
            }
        }
        if (zhiIndex == -1) {
            throw new IllegalArgumentException("无效地支: " + zhi);
        }

        // 计算旬首地支索引：当前地支索引 - 天干偏移（甲=0，所以偏移就是ganIndex）
        int xunShouZhiIndex = (zhiIndex - ganIndex + 12) % 12;
        String xunShouZhi = DI_ZHI[xunShouZhiIndex];
        String xunShou = "甲" + xunShouZhi;

        // 获取空亡
        String kong = XUN_KONG.get(xunShou);
        if (kong == null) {
            throw new IllegalStateException("未找到旬首 " + xunShou + " 对应的空亡");
        }

        return kong;
    }


}
