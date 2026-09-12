package com.adnaan525.medme.util;

import com.adnaan525.medme.model.DurationType;
import com.adnaan525.medme.model.Inventory;
import com.adnaan525.medme.model.Medication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
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

    /** Last day of a FIXED_DAYS course; meaningless for RECURRING (no end date). */
    public static LocalDate courseEndDate(Medication med) {
        LocalDate start = DateTimeUtils.parseDate(med.getStartDate());
        return start.plusDays(Math.max(0, med.getTotalDays() - 1));
    }

    /** True only once a FIXED_DAYS course's last day is in the past (not for a course that hasn't started yet). */
    public static boolean isCourseFinished(Medication med) {
        if (med.getDurationType() != DurationType.FIXED_DAYS) {
            return false;
        }
        return LocalDate.now().isAfter(courseEndDate(med));
    }

    /** True once a FIXED_DAYS course's start date is still in the future. */
    public static boolean isCourseUpcoming(Medication med) {
        if (med.getDurationType() != DurationType.FIXED_DAYS) {
            return false;
        }
        return LocalDate.now().isBefore(DateTimeUtils.parseDate(med.getStartDate()));
    }

    /**
     * Doses still needed to finish a FIXED_DAYS course from {@code from} (inclusive) onward -
     * dose-times-per-day x days remaining. Returns 0 once the course has finished, and -1 for
     * RECURRING medications, where "enough to finish" doesn't apply (no end date).
     */
    public static int dosesNeededForRestOfCourse(Medication med, LocalDate from) {
        if (med.getDurationType() != DurationType.FIXED_DAYS) {
            return -1;
        }
        LocalDate start = DateTimeUtils.parseDate(med.getStartDate());
        LocalDate end = courseEndDate(med);
        LocalDate effectiveFrom = from.isBefore(start) ? start : from;
        if (effectiveFrom.isAfter(end)) {
            return 0;
        }
        long daysLeft = ChronoUnit.DAYS.between(effectiveFrom, end) + 1;
        return (int) (daysLeft * med.getDoseTimes().size());
    }

    /**
     * Whether a medication needs a restock nudge. For a FIXED_DAYS course this compares stock
     * against what's actually needed to finish the remaining days - e.g. a 7-day, once-daily
     * course with 7 left needs no reminder even if that's below the configured threshold. For
     * RECURRING medications (no end date to size the comparison against) it falls back to the
     * user-configured threshold.
     */
    public static boolean isLowStock(Medication med) {
        Inventory inventory = med.getInventory();
        if (inventory == null) {
            return false;
        }
        if (med.getDurationType() == DurationType.FIXED_DAYS) {
            return inventory.getQuantityRemaining() < dosesNeededForRestOfCourse(med, LocalDate.now());
        }
        return inventory.isLowStock();
    }

    /**
     * Whether a medication belongs in the Archive tab: either its FIXED_DAYS course has run
     * its course, or the user manually archived it (Medication.active is false).
     */
    public static boolean isArchived(Medication med) {
        return isCourseFinished(med) || !med.isActive();
    }
}
