package com.adnaan525.medme.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * All persisted date/time values are plain ISO-8601 strings (see model package) so the
 * exported JSON stays human-readable and dependency-free. This class is the single place
 * that converts between those strings and java.time types.
 */
public final class DateTimeUtils {
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    public static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    public static final DateTimeFormatter DISPLAY_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DISPLAY_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, HH:mm");

    private DateTimeUtils() {
    }

    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    public static LocalDate parseDate(String date) {
        return LocalDate.parse(date, DATE_FORMAT);
    }

    public static String formatTime(LocalTime time) {
        return time.format(TIME_FORMAT);
    }

    public static LocalTime parseTime(String time) {
        return LocalTime.parse(time, TIME_FORMAT);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMAT);
    }

    public static LocalDateTime parseDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime, DATE_TIME_FORMAT);
    }

    public static String todayIso() {
        return formatDate(LocalDate.now());
    }
}
