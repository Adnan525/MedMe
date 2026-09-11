package com.adnaan525.medme.util;

import com.adnaan525.medme.model.DurationType;
import com.adnaan525.medme.model.Medication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class ScheduleUtils {
    private ScheduleUtils() {
    }

    public static boolean isActiveOn(Medication med, LocalDate date) {
        LocalDate start = DateTimeUtils.parseDate(med.getStartDate());
        if (date.isBefore(start)) {
            return false;
        }
        if (med.getDurationType() == DurationType.FIXED_DAYS) {
            LocalDate end = start.plusDays(Math.max(0, med.getTotalDays() - 1));
            return !date.isAfter(end);
        }
        return true;
    }

    public static List<LocalDateTime> occurrencesOn(Medication med, LocalDate date) {
        List<LocalDateTime> result = new ArrayList<>();
        if (!isActiveOn(med, date)) {
            return result;
        }
        for (String timeStr : med.getDoseTimes()) {
            result.add(LocalDateTime.of(date, DateTimeUtils.parseTime(timeStr)));
        }
        return result;
    }

    /**
     * Next occurrence strictly after {@code from} for the given dose-time index, honoring the
     * medication's active date range. Returns null once a FIXED_DAYS schedule has ended.
     */
    public static LocalDateTime nextOccurrence(Medication med, int doseIndex, LocalDateTime from) {
        LocalTime time = DateTimeUtils.parseTime(med.getDoseTimes().get(doseIndex));
        LocalDateTime candidate = LocalDateTime.of(from.toLocalDate(), time);
        if (!candidate.isAfter(from)) {
            candidate = candidate.plusDays(1);
        }
        if (!isActiveOn(med, candidate.toLocalDate())) {
            return null;
        }
        return candidate;
    }
}
