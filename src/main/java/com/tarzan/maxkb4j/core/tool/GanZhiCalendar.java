package com.tarzan.maxkb4j.core.tool;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class GanZhiCalendar {

    // 已知锚点：1900年1月31日 是 庚子年 丁丑月 甲辰日
    private static final LocalDate ANCHOR_DATE = LocalDate.of(1900, 1, 31);
    // 十天干
    private static final String[] TIANGAN = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    // 十二地支
    private static final String[] DIZHI = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};

    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.now(); // 可替换为任意时间
        System.out.println(toGanZhi(now));
    }

    public static String toDayGan(LocalDateTime date) {
        int ganIndex =getDayGanIndex(date); // 防止负数
        return TIANGAN[ganIndex];
    }

    public static int getDayGanIndex(LocalDateTime date) {
        long daysBetween = ChronoUnit.DAYS.between(ANCHOR_DATE, date);
        return (int) (daysBetween % 10);
    }

    public static String toGanZhi(LocalDateTime date) {
        String ganZhiYear = getGanZhiYear(date);
        String ganZhiMonth = getGanZhiMonth(date);
        String ganZhiDay = getGanZhiDay(date);
        String ganZhiHour = getGanZhiHour(date);
        return ganZhiYear + "年　" + ganZhiMonth + "月　" + ganZhiDay + "日　" + ganZhiHour + "时";
    }


    private static String getGanZhiYear(LocalDateTime date) {
        long yearsBetween = ChronoUnit.YEARS.between(ANCHOR_DATE, date);
        int ganIndex = (int) ((yearsBetween+6)  % 10);
        int zhiIndex = (int) (yearsBetween % 12);
        return TIANGAN[ganIndex] + DIZHI[zhiIndex];
    }

    public static int getYearGanIndex(LocalDateTime date) {
        long yearsBetween = ChronoUnit.YEARS.between(ANCHOR_DATE, date);
        return  (int) ((yearsBetween+6)  % 10);
    }

    // 需要用到是节气月
    private static String getGanZhiMonth(LocalDateTime date) {
        int month=SolarTermsCalculator.getSolarTermMonth(date.toLocalDate());
        int zhiIndex = (month+1)  % 12; // 因为正月是寅月，寅是下标是2，所以 (1+1)%12=2
        if (zhiIndex == 0) zhiIndex = 11;
       // zhiIndex--;
        int yearGanIndex = getYearGanIndex(date);
        // 五鼠遁口诀：
        // 甲己还加甲，乙庚丙作初，
        // 丙辛从戊起，丁壬庚子居，
        // 戊癸何方发，壬子是真途。
        int ziShiGanIndex = switch (yearGanIndex) {
            case 0, 5 -> // 甲、己
                    2; // 丙
            case 1, 6 -> // 乙、庚
                    4; // 戊
            case 2, 7 -> // 丙、辛
                    6; // 庚
            case 3, 8 -> // 丁、壬
                    8; // 壬
            case 4, 9 -> // 戊、癸
                    0; // 甲
            default -> 0;
        };
        int ganIndex = (ziShiGanIndex + (month - 1)) % 10;
        return TIANGAN[ganIndex] + DIZHI[zhiIndex];
    }

    // 日干支：使用已知公式（适用于1900 - 2099）
    // 来自《日干支计算公式》：对于公历日期 year-month-day
    private static String getGanZhiDay(LocalDateTime date) {
        long daysBetween = ChronoUnit.DAYS.between(ANCHOR_DATE, date);
        int ganIndex = (int) (daysBetween % 10) ;
        int zhiIndex = (int) ((daysBetween+4) % 12);
        return TIANGAN[ganIndex] + DIZHI[zhiIndex];
    }

    private static String getGanZhiHour(LocalDateTime date) {
        int hour=date.getHour();
        int shichen = (hour + 1) / 2 % 12; // 0=子,1=丑,...,11=亥
        if (hour == 23) shichen = 0; // 特殊处理23点属于子时
        int dayGanIndex = getDayGanIndex(date);
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
        return TIANGAN[ganIndex] + DIZHI[shichen];
    }


}
