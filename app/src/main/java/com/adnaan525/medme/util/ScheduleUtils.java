package com.adnaan525.medme.util;

import com.adnaan525.medme.model.DoseLog;
import com.adnaan525.medme.model.DoseStatus;
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
     * Doses still needed to finish a FIXED_DAYS course - total course doses (days x doses/day)
     * minus doses already accounted for (taken, which drew down stock, or missed, which won't).
     * This is log-based rather than calendar-based on purpose: a calendar calculation like
     * "days remaining x doses/day" doesn't know a dose was already taken today, so right after
     * marking today's only dose taken on a 2-day course it would still say "2 needed" (today +
     * tomorrow) instead of the 1 actually left - triggering a false low-stock alert. Counting
     * from the logs instead reflects what's actually still going to be consumed. Returns -1 for
     * RECURRING/AS_NEEDED medications, where "enough to finish" doesn't apply (no end date).
     */
    public static int dosesNeededForRestOfCourse(Medication med) {
        if (med.getDurationType() != DurationType.FIXED_DAYS) {
            return -1;
        }
        int totalCourseDoses = med.getTotalDays() * med.getDoseTimes().size();
        int accountedFor = 0;
        for (DoseLog log : med.getDoseLogs()) {
            if (log.getStatus() == DoseStatus.TAKEN || log.getStatus() == DoseStatus.MISSED) {
                accountedFor++;
            }
        }
        return Math.max(0, totalCourseDoses - accountedFor);
    }

    /**
     * Whether a medication needs a restock nudge: stock must be at or below the configured
     * threshold, AND (for a FIXED_DAYS course only) stock must not be enough to finish the
     * doses left in the course. Both conditions have to hold - a course that's nearly done
     * (say, 1-2 doses left) shouldn't nag just because that's numerically below the configured
     * threshold, since there's nothing left to run out of. For RECURRING/AS_NEEDED medications
     * there's no course to run out of, so the second condition is trivially satisfied and this
     * reduces to the flat threshold check alone.
     */
    public static boolean isLowStock(Medication med) {
        Inventory inventory = med.getInventory();
        if (inventory == null || !inventory.isLowStock()) {
            return false;
        }
        if (med.getDurationType() == DurationType.FIXED_DAYS) {
            return inventory.getQuantityRemaining() < dosesNeededForRestOfCourse(med);
        }
        return true;
    }

    /**
     * Whether a medication belongs in the Archive tab: either its FIXED_DAYS course has run
     * its course, or the user manually archived it (Medication.active is false).
     */
    public static boolean isArchived(Medication med) {
        return isCourseFinished(med) || !med.isActive();
    }

    /**
     * Days from {@code from} through a FIXED_DAYS course's end date, inclusive - clamped so a
     * course that hasn't started yet is measured from its own start, not from today. 0 once the
     * course has finished. Meaningless for RECURRING/AS_NEEDED (no end date).
     */
    public static long remainingCourseDays(Medication med, LocalDate from) {
        LocalDate start = DateTimeUtils.parseDate(med.getStartDate());
        LocalDate end = courseEndDate(med);
        LocalDate effectiveFrom = from.isBefore(start) ? start : from;
        if (effectiveFrom.isAfter(end)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(effectiveFrom, end) + 1;
    }

    /**
     * How many doses of this medication to pack for a trip of {@code tripDays} days - dose-
     * times-per-day x tripDays, capped for a FIXED_DAYS course at the doses actually still left
     * in it (no point packing for doses after it ends). That cap is log-based
     * (dosesNeededForRestOfCourse), not a calendar day-count: a day-count doesn't know a dose
     * was already taken today, so on a 2-day once-daily course with today's dose already taken,
     * it would say "2 left" (today + tomorrow) instead of the 1 actually remaining - overstating
     * both how many to pack and how many are needed to have "enough" on hand. Returns -1 for
     * AS_NEEDED medications, where there's no schedule to size a quantity against.
     */
    public static int packingQuantityForTrip(Medication med, int tripDays) {
        if (med.getDurationType() == DurationType.AS_NEEDED) {
            return -1;
        }
        int dosesPerDay = med.getDoseTimes().size();
        int tripQuantity = tripDays * dosesPerDay;
        if (med.getDurationType() == DurationType.FIXED_DAYS) {
            return Math.min(tripQuantity, dosesNeededForRestOfCourse(med));
        }
        return tripQuantity;
    }
}
