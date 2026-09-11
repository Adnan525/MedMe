package com.adnaan525.medme.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.adnaan525.medme.data.DataRepository;
import com.adnaan525.medme.model.DoseLog;
import com.adnaan525.medme.model.DoseStatus;
import com.adnaan525.medme.util.DateTimeUtils;

import java.time.LocalDateTime;

public class DoseAlarmReceiver extends BroadcastReceiver {
    public static final String EXTRA_MEDICATION_ID = "medication_id";
    public static final String EXTRA_DOSE_INDEX = "dose_index";
    public static final String EXTRA_SCHEDULED_DATE_TIME = "scheduled_date_time";

    @Override
    public void onReceive(Context context, Intent intent) {
        String medicationId = intent.getStringExtra(EXTRA_MEDICATION_ID);
        int doseIndex = intent.getIntExtra(EXTRA_DOSE_INDEX, -1);
        String scheduledIso = intent.getStringExtra(EXTRA_SCHEDULED_DATE_TIME);
        if (medicationId == null || scheduledIso == null || doseIndex < 0) {
            return;
        }

        DataRepository repo = DataRepository.getInstance(context);
        DataRepository.MedicationLookup lookup = repo.findMedication(medicationId);
        if (lookup == null || !lookup.medication.isActive()) {
            return;
        }

        LocalDateTime scheduled = DateTimeUtils.parseDateTime(scheduledIso);
        DoseLog log = repo.ensureDoseLog(medicationId, scheduled);
        if (log != null && log.getStatus() == DoseStatus.PENDING) {
            NotificationHelper.showDoseReminder(context, lookup.patient, lookup.medication, log);
        }

        // Re-arm the same dose-time slot for its next occurrence.
        AlarmScheduler.scheduleNextForDose(context, lookup.medication, doseIndex);
    }
}
