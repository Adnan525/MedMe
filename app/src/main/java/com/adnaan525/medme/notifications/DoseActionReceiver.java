package com.adnaan525.medme.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationManagerCompat;

import com.adnaan525.medme.data.DataRepository;

import java.time.LocalDateTime;

/**
 * Handles the notification's "Mark Taken" and "Skip" action taps, and its swipe-to-dismiss
 * delete intent. Per product decision, swiping the reminder away is treated the same as
 * tapping "Mark Taken" - it's logged as taken right now, so the delay/earliness analytics
 * capture it. "Skip" explicitly resolves the dose as not taken (MISSED) with no stock change -
 * this is the only way a dose reaches MISSED now that there's no time-based sweep guessing at it.
 */
public class DoseActionReceiver extends BroadcastReceiver {
    public static final String ACTION_MARK_TAKEN = "com.adnaan525.medme.ACTION_MARK_TAKEN";
    public static final String ACTION_DISMISSED = "com.adnaan525.medme.ACTION_DISMISSED";
    public static final String ACTION_SKIP = "com.adnaan525.medme.ACTION_SKIP";
    public static final String EXTRA_MEDICATION_ID = "medication_id";
    public static final String EXTRA_DOSE_LOG_ID = "dose_log_id";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String medicationId = intent.getStringExtra(EXTRA_MEDICATION_ID);
        String doseLogId = intent.getStringExtra(EXTRA_DOSE_LOG_ID);
        if (medicationId == null || doseLogId == null) {
            return;
        }

        String action = intent.getAction();
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);

        if (ACTION_SKIP.equals(action)) {
            DataRepository.getInstance(context).markSkipped(medicationId, doseLogId);
            if (notificationId != -1) {
                NotificationManagerCompat.from(context).cancel(notificationId);
            }
            return;
        }

        DataRepository repo = DataRepository.getInstance(context);
        DataRepository.DoseTakenResult result = repo.markTaken(medicationId, doseLogId, LocalDateTime.now());

        if (ACTION_MARK_TAKEN.equals(action) && notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId);
        }

        if (result != null && result.triggersLowStockNotification) {
            NotificationHelper.showLowStock(context, result.patient, result.medication);
        }
    }
}
