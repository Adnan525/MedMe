package com.adnaan525.medme.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.adnaan525.medme.data.DataRepository;
import com.adnaan525.medme.model.Medication;
import com.adnaan525.medme.model.Patient;
import com.adnaan525.medme.util.DateTimeUtils;
import com.adnaan525.medme.util.ScheduleUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Schedules one inexact "next occurrence" alarm per dose-time (not a full recurring series -
 * see DoseAlarmReceiver, which re-arms the following day's alarm each time one fires). Uses
 * setAndAllowWhileIdle rather than exact alarms: this is an intentional tradeoff to avoid the
 * SCHEDULE_EXACT_ALARM permission (and its Play Console justification requirement) at the cost
 * of reminders occasionally drifting a few minutes under Doze - see the About screen.
 */
public final class AlarmScheduler {
    private AlarmScheduler() {
    }

    public static void scheduleNextForDose(Context context, Medication med, int doseIndex) {
        LocalDateTime next = ScheduleUtils.nextOccurrence(med, doseIndex, LocalDateTime.now());
        if (next == null) {
            return;
        }
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = buildPendingIntent(context, med.getId(), doseIndex, next);
        long triggerAtMillis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
    }

    public static void scheduleAllForMedication(Context context, Medication med) {
        if (!med.isActive()) {
            return;
        }
        for (int i = 0; i < med.getDoseTimes().size(); i++) {
            scheduleNextForDose(context, med, i);
        }
    }

    public static void cancelAllForMedication(Context context, Medication med) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }
        for (int i = 0; i < med.getDoseTimes().size(); i++) {
            Intent intent = new Intent(context, DoseAlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCodeFor(med.getId(), i),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(pendingIntent);
        }
    }

    public static void rescheduleAll(Context context) {
        DataRepository repo = DataRepository.getInstance(context);
        for (Patient patient : repo.getPatients()) {
            for (Medication med : patient.getMedications()) {
                scheduleAllForMedication(context, med);
            }
        }
    }

    static PendingIntent buildPendingIntent(Context context, String medicationId, int doseIndex, LocalDateTime scheduled) {
        Intent intent = new Intent(context, DoseAlarmReceiver.class);
        intent.putExtra(DoseAlarmReceiver.EXTRA_MEDICATION_ID, medicationId);
        intent.putExtra(DoseAlarmReceiver.EXTRA_DOSE_INDEX, doseIndex);
        intent.putExtra(DoseAlarmReceiver.EXTRA_SCHEDULED_DATE_TIME, DateTimeUtils.formatDateTime(scheduled));
        return PendingIntent.getBroadcast(
                context,
                requestCodeFor(medicationId, doseIndex),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static int requestCodeFor(String medicationId, int doseIndex) {
        return medicationId.hashCode() * 31 + doseIndex;
    }
}
