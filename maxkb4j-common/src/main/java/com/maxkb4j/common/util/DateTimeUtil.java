package com.maxkb4j.common.util;


import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Date;

public class DateTimeUtil {
    public static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public DateTimeUtil() {
    }


    public static String format(LocalDateTime  dateTime, String pattern) {
        return DateTimeFormatter.ofPattern(pattern).format(dateTime);
    }

    public static String now() {
        return DATETIME_FORMAT.format(LocalDateTime.now());
    }

    public static LocalDateTime parseDateTime(String dateStr, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return parseDateTime(dateStr, formatter);
    }

    public static LocalDateTime parseDateTime(String dateStr, DateTimeFormatter formatter) {
        return LocalDateTime.parse(dateStr, formatter);
    }

    public static LocalDateTime parseDateTime(String dateStr) {
        return parseDateTime(dateStr, DATETIME_FORMAT);
    }

    public static LocalDate parseDate(String dateStr, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return parseDate(dateStr, formatter);
    }

    public static LocalDate parseDate(String dateStr, DateTimeFormatter formatter) {
        return LocalDate.parse(dateStr, formatter);
    }

    public static LocalDate parseDate(String dateStr) {
        return parseDate(dateStr, DATE_FORMAT);
    }

    public static LocalTime parseTime(String dateStr, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return parseTime(dateStr, formatter);
    }

    public static LocalTime parseTime(String dateStr, DateTimeFormatter formatter) {
        return LocalTime.parse(dateStr, formatter);
    }

    public static LocalTime parseTime(String dateStr) {
        return parseTime(dateStr, TIME_FORMAT);
    }

    public static Instant toInstant(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    public static LocalDateTime toDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static Date toDate(LocalDateTime dateTime) {
        return Date.from(toInstant(dateTime));
    }

    public static LocalDate toLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public static Duration between(Temporal startInclusive, Temporal endExclusive) {
        return Duration.between(startInclusive, endExclusive);
    }

    public static Period between(LocalDate startDate, LocalDate endDate) {
        return Period.between(startDate, endDate);
    }

    /**
     * 获取第二天的指定时间
     * @param time 指定时间
     * @return 第二天指定时间的LocalDateTime
     */
    public static LocalDateTime getNextDay(LocalTime time) {
        return LocalDate.now().plusDays(1).atTime(time);
    }

    /**
     * 获取下一个到达的指定时间点：若今天该时刻尚未过去则返回今天，否则返回明天。
     * @param hour 小时
     * @param minute 分钟
     * @param second 秒
     * @return 下一次该时刻的LocalDateTime
     */
    public static LocalDateTime getNextDayAtTime(int hour, int minute, int second) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.withHour(hour).withMinute(minute).withSecond(second).withNano(0);
        if (today.isAfter(now)) {
            return today;
        }
        return today.plusDays(1);
    }

    /**
     * 获取下一个到达指定周几的时间点：若本周该周几的指定时刻尚未过去则返回本周，否则返回下周同一周几。
     * @param day 参数为周几 1 2 3 4 5 6 7
     * @param hour 小时
     * @param minute 分钟
     * @param second 秒
     * @return 下一次该周几该时刻的LocalDateTime对象
     */
    public static LocalDateTime getSameDayNextWeek(int day, int hour, int minute, int second) {
        if (day < 1 || day > 7) {
            throw new IllegalArgumentException("day参数必须在1-7之间");
        }
        LocalDateTime now = LocalDateTime.now();
        int currentDay = now.getDayOfWeek().getValue();
        // 距离下一次该周几的天数（0 表示今天就是目标周几）
        int daysToAdd = (day - currentDay + 7) % 7;
        LocalDateTime candidate = now.toLocalDate().plusDays(daysToAdd)
                .atTime(hour, minute, second);
        // 若该时刻已过（含恰好为当前时刻），顺延到下周
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusWeeks(1);
        }
        return candidate;
    }

    /**
     * 获取下一个到达指定日期的时间点：若本月该日的指定时刻尚未过去则返回本月，否则返回下月同一日（下月无该日时取月末）。
     * @param day 天参数为1-31
     * @param hour 小时
     * @param minute 分钟
     * @param second 秒
     * @return 下一次该日该时刻的LocalDateTime对象
     */
    public static LocalDateTime getSameDayNextMonth(int day, int hour, int minute, int second) {
        if (day < 1 || day > 31) {
            throw new IllegalArgumentException("day参数必须在1-31之间");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        // 先尝试本月
        int thisMonthLength = today.lengthOfMonth();
        int targetDayThis = Math.min(day, thisMonthLength);
        LocalDateTime candidate = today.withDayOfMonth(targetDayThis)
                .atTime(hour, minute, second);
        if (candidate.isAfter(now)) {
            return candidate;
        }
        // 本月该时刻已过，顺延到下月
        LocalDate nextMonth = today.plusMonths(1);
        int nextMonthLength = nextMonth.lengthOfMonth();
        // 处理下个月没有相同天数的情况，取下个月的最后一天
        int targetDayNext = Math.min(day, nextMonthLength);
        return nextMonth.withDayOfMonth(targetDayNext).atTime(hour, minute, second);
    }

    /**
     *
     * 获取指定周期下一个时间点
     * @param intervalValue 1
     * @param intervalUnit 单位hours 或者minutes
     * @param second 秒
     */
    public static LocalDateTime getSameDayNextInterval(String intervalValue, String intervalUnit, int second) {
        int interval = Integer.parseInt(intervalValue);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime baseTime = now.withSecond(second);
        // 计算下一个间隔时间点
        if ("hours".equals(intervalUnit)) {
            return baseTime.plusHours(interval);
        } else if ("minutes".equals(intervalUnit)) {
            return baseTime.plusMinutes(interval);
        } else {
            throw new IllegalArgumentException("Invalid interval unit: " + intervalUnit);
        }
    }
}
