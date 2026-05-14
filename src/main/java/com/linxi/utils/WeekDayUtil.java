package com.linxi.utils;

import cn.hutool.core.date.DateUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 星期几工具类
 */
public class WeekDayUtil {

    private static final String[] WEEK_DAYS = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};

    public static String getWeekDay(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            int dayOfWeek = date.getDayOfWeek().getValue();
            return WEEK_DAYS[dayOfWeek - 1];
        } catch (Exception e) {
            return "";
        }
    }

    public static String getWeekDay(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        return WEEK_DAYS[dayOfWeek - 1];
    }
}
