package com.adnaan525.medme.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.adnaan525.medme.R;
import com.adnaan525.medme.model.DoseLog;
import com.adnaan525.medme.model.Medication;
import com.adnaan525.medme.model.Patient;
import com.adnaan525.medme.ui.patients.MedicationDetailActivity;
import com.adnaan525.medme.util.DateTimeUtils;

public final class NotificationHelper {
    public static final String CHANNEL_DOSE_REMINDERS = "dose_reminders";
    public static final String CHANNEL_LOW_STOCK = "low_stock_alerts";

    private NotificationHelper() {
    }

    public static void createChannels(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel doseChannel = new NotificationChannel(
                CHANNEL_DOSE_REMINDERS,
                context.getString(R.string.channel_dose_reminders_name),
                NotificationManager.IMPORTANCE_HIGH);
        doseChannel.setDescription(context.getString(R.string.channel_dose_reminders_description));

        NotificationChannel lowStockChannel = new NotificationChannel(
                CHANNEL_LOW_STOCK,
                context.getString(R.string.channel_low_stock_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        lowStockChannel.setDescription(context.getString(R.string.channel_low_stock_description));

        manager.createNotificationChannel(doseChannel);
        manager.createNotificationChannel(lowStockChannel);
    }

    private static boolean canPostNotifications(Context context) {
        return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void showDoseReminder(Context context, Patient patient, Medication med, DoseLog log) {
        if (!canPostNotifications(context)) {
            return;
        }
        int notificationId = AlarmScheduler.requestCodeFor(med.getId(), 0) ^ log.getScheduledDateTime().hashCode();

        Intent contentIntent = new Intent(context, MedicationDetailActivity.class);
        contentIntent.putExtra(MedicationDetailActivity.EXTRA_PATIENT_ID, patient.getId());
        contentIntent.putExtra(MedicationDetailActivity.EXTRA_MEDICATION_ID, med.getId());
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context, notificationId, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent markTakenIntent = new Intent(context, DoseActionReceiver.class);
        markTakenIntent.setAction(DoseActionReceiver.ACTION_MARK_TAKEN);
        markTakenIntent.putExtra(DoseActionReceiver.EXTRA_MEDICATION_ID, med.getId());
        markTakenIntent.putExtra(DoseActionReceiver.EXTRA_DOSE_LOG_ID, log.getId());
        markTakenIntent.putExtra(DoseActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
        PendingIntent markTakenPendingIntent = PendingIntent.getBroadcast(
                context, notificationId, markTakenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent deleteIntent = new Intent(context, DoseActionReceiver.class);
        deleteIntent.setAction(DoseActionReceiver.ACTION_DISMISSED);
        deleteIntent.putExtra(DoseActionReceiver.EXTRA_MEDICATION_ID, med.getId());
        deleteIntent.putExtra(DoseActionReceiver.EXTRA_DOSE_LOG_ID, log.getId());
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(
                context, notificationId + 1, deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String scheduledTimeLabel = DateTimeUtils.parseDateTime(log.getScheduledDateTime())
                .format(DateTimeUtils.DISPLAY_TIME_FORMAT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_DOSE_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification_pill)
                .setContentTitle(context.getString(R.string.notification_dose_title, med.getName()))
                .setContentText(context.getString(R.string.notification_dose_text, patient.getName(), scheduledTimeLabel))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .addAction(0, context.getString(R.string.action_mark_taken), markTakenPendingIntent);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    public static void showLowStock(Context context, Patient patient, Medication med) {
        if (!canPostNotifications(context)) {
            return;
        }
        int notificationId = ("lowstock_" + med.getId()).hashCode();

        Intent contentIntent = new Intent(context, MedicationDetailActivity.class);
        contentIntent.putExtra(MedicationDetailActivity.EXTRA_PATIENT_ID, patient.getId());
        contentIntent.putExtra(MedicationDetailActivity.EXTRA_MEDICATION_ID, med.getId());
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context, notificationId, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_LOW_STOCK)
                .setSmallIcon(R.drawable.ic_notification_pill)
                .setContentTitle(context.getString(R.string.notification_low_stock_title, med.getName()))
                .setContentText(context.getString(R.string.notification_low_stock_text, patient.getName(),
                        med.getInventory() != null ? med.getInventory().getQuantityRemaining() : 0))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }
}
