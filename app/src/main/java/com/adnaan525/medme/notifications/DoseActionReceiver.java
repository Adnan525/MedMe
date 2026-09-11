package com.adnaan525.medme.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationManagerCompat;

import com.adnaan525.medme.data.DataRepository;

import java.time.LocalDateTime;

/**
 * Handles both the notification's "Mark Taken" action tap and its swipe-to-dismiss delete
 * intent. Per product decision, swiping the reminder away is treated the same as tapping
 * "Mark Taken" - it's logged as taken right now, so the delay/earliness analytics capture it.
 */
public class DoseActionReceiver extends BroadcastReceiver {
    public static final String ACTION_MARK_TAKEN = "com.adnaan525.medme.ACTION_MARK_TAKEN";
    public static final String ACTION_DISMISSED = "com.adnaan525.medme.ACTION_DISMISSED";
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

        DataRepository repo = DataRepository.getInstance(context);
        DataRepository.DoseTakenResult result = repo.markTaken(medicationId, doseLogId, LocalDateTime.now());

        if (ACTION_MARK_TAKEN.equals(intent.getAction())) {
            int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
            if (notificationId != -1) {
                NotificationManagerCompat.from(context).cancel(notificationId);
            }
        }

        if (result != null && result.triggersLowStockNotification) {
            NotificationHelper.showLowStock(context, result.patient, result.medication);
        }
    }
}
