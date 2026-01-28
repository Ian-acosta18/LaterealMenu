package com.example.laterealmenu;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "PLANT_CARE_CHANNEL";
    private static final String CHANNEL_NAME = "Cuidado de Plantas";
    private static final String CHANNEL_DESCRIPTION = "Notificaciones para riego y fertilización de plantas";

    private Context context;
    private NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "✅ Canal de notificaciones creado: " + CHANNEL_ID);
        }
    }

    public void sendPlantNotification(String title, String message, int notificationId, String plantId, String type) {
        try {
            // Intent para abrir la app cuando se toque la notificación
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("fragment", "mis_plantas");
            intent.putExtra("plant_id", plantId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Crear notificación
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(getNotificationIcon(type))
                    .setColor(getNotificationColor(type))
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER);

            // Agregar vibración
            builder.setVibrate(new long[]{0, 500, 200, 500});

            // Mostrar notificación
            notificationManager.notify(notificationId, builder.build());

            Log.d(TAG, "📲 Notificación enviada: " + title);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error enviando notificación: " + e.getMessage());
        }
    }

    // Métodos legacy para compatibilidad
    public void sendWaterNotification(String plantName, int daysSince, int interval, String plantId) {
        String title = "💧 Recordatorio de Riego";
        String message = "Es hora de regar \"" + plantName + "\". Han pasado " + daysSince + " días.";
        sendPlantNotification(title, message, generateNotificationId(), plantId, "water");
    }

    public void sendFertilizerNotification(String plantName, int daysSince, int interval, String plantId) {
        String title = "🌱 Recordatorio de Fertilización";
        String message = "Es hora de fertilizar \"" + plantName + "\". Han pasado " + daysSince + " días.";
        sendPlantNotification(title, message, generateNotificationId(), plantId, "fertilizer");
    }

    private int getNotificationIcon(String type) {

        switch (type) {
            case "water":
                return android.R.drawable.ic_menu_help;
            case "fertilizer":
                return android.R.drawable.ic_menu_info_details;
            case "info":
                return android.R.drawable.ic_dialog_info;
            default:
                return android.R.drawable.ic_dialog_alert;
        }
    }

    private int getNotificationColor(String type) {
        switch (type) {
            case "water":
                return Color.parseColor("#2196F3");
            case "fertilizer":
                return Color.parseColor("#4CAF50");
            default:
                return Color.parseColor("#FF9800");
        }
    }

    private int generateNotificationId() {
        return (int) (System.currentTimeMillis() / 1000);
    }
}