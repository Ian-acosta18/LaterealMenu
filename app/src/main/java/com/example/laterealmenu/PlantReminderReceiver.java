package com.example.laterealmenu;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Calendar;

public class PlantReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "PlantReminderReceiver";
    private FirebaseFirestore db;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "🔔 Receiver activado: " + intent.getAction());

        db = FirebaseFirestore.getInstance();

        if ("ACTION_CHECK_PLANT_REMINDERS".equals(intent.getAction()) ||
                Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            checkAllPlants(context);
        }
        // ✅ NUEVO: Manejar recordatorios específicos
        else if ("ACTION_SINGLE_REMINDER".equals(intent.getAction())) {
            String plantaNombre = intent.getStringExtra("planta_nombre");
            int diasRiego = intent.getIntExtra("dias_riego", 7);
            int diasFertilizante = intent.getIntExtra("dias_fertilizante", 30);
            String plantaId = intent.getStringExtra("planta_id");

            if (plantaNombre != null) {
                enviarRecordatorioPersonalizado(context, plantaNombre, diasRiego, diasFertilizante, plantaId);
            }
        }
    }


    private void enviarRecordatorioPersonalizado(Context context, String plantaNombre, int diasRiego, int diasFertilizante, String plantaId) {
        try {
            NotificationHelper notificationHelper = new NotificationHelper(context);

            String titulo = "🌿 Recordatorio de " + plantaNombre;
            String mensaje = "No olvides cuidar tu planta:\n\n";
            mensaje += "💧 Riego cada: " + diasRiego + " días\n";
            mensaje += "🌱 Fertilización cada: " + diasFertilizante + " días\n\n";
            mensaje += "¡Tu planta te lo agradecerá! 🌟";

            notificationHelper.sendPlantNotification(
                    titulo,
                    mensaje,
                    (int) System.currentTimeMillis(),
                    plantaId,
                    "reminder"
            );

            Log.d(TAG, "✅ Recordatorio personalizado enviado para: " + plantaNombre);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error enviando recordatorio personalizado: " + e.getMessage());
        }
    }

    private void checkAllPlants(Context context) {
        Log.d(TAG, "🔍 Verificando todas las plantas...");

        db.collection("plantas")
                .whereEqualTo("notificacionesActivadas", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    NotificationHelper notificationHelper = new NotificationHelper(context);
                    int notificationId = 1000;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String plantId = document.getId();
                            String plantName = document.getString("nombreComun");
                            String tituloRegistro = document.getString("tituloRegistro");
                            String displayName = tituloRegistro != null ? tituloRegistro : plantName;

                            Integer diasRiego = getIntValue(document, "diasRiego", 7);
                            Integer diasFertilizante = getIntValue(document, "diasFertilizante", 30);
                            Long ultimoRiego = getLongValue(document, "ultimoRiego");
                            Long ultimaFertilizacion = getLongValue(document, "ultimaFertilizacion");
                            Long fechaRegistro = getLongValue(document, "fechaRegistro");

                            if (displayName != null) {
                                // Verificar riego
                                WateringStatus waterStatus = calculateWateringStatus(ultimoRiego, fechaRegistro, diasRiego);
                                if (waterStatus.needsWatering) {
                                    String mensaje = "💧 Es hora de regar \"" + displayName + "\". ";
                                    mensaje += "Configurado para riego cada " + diasRiego + " días.";

                                    notificationHelper.sendPlantNotification(
                                            "Recordatorio de Riego 🌱",
                                            mensaje,
                                            notificationId++,
                                            plantId,
                                            "water"
                                    );
                                    Log.d(TAG, "💧 Notificación de riego para: " + displayName);
                                }

                                // Verificar fertilización
                                FertilizationStatus fertStatus = calculateFertilizationStatus(ultimaFertilizacion, fechaRegistro, diasFertilizante);
                                if (fertStatus.needsFertilization) {
                                    String mensaje = "🌱 Es hora de fertilizar \"" + displayName + "\". ";
                                    mensaje += "Configurado para fertilización cada " + diasFertilizante + " días.";

                                    notificationHelper.sendPlantNotification(
                                            "Recordatorio de Fertilización 🌿",
                                            mensaje,
                                            notificationId++,
                                            plantId,
                                            "fertilizer"
                                    );
                                    Log.d(TAG, "🌱 Notificación de fertilización para: " + displayName);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error procesando planta: " + e.getMessage());
                        }
                    }

                    Log.d(TAG, "✅ Verificación de notificaciones completada");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al verificar plantas: " + e.getMessage());
                });
    }

    // Clases auxiliares para estado (mantener igual)
    private static class WateringStatus {
        boolean needsWatering;
        int daysSince;
        WateringStatus(boolean needsWatering, int daysSince) {
            this.needsWatering = needsWatering;
            this.daysSince = daysSince;
        }
    }

    private static class FertilizationStatus {
        boolean needsFertilization;
        int daysSince;
        FertilizationStatus(boolean needsFertilization, int daysSince) {
            this.needsFertilization = needsFertilization;
            this.daysSince = daysSince;
        }
    }

    private WateringStatus calculateWateringStatus(Long lastWatering, Long registrationDate, int interval) {
        long lastTime = (lastWatering != null) ? lastWatering * 1000 :
                (registrationDate != null) ? registrationDate * 1000 : System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        int daysSince = (int) ((currentTime - lastTime) / (1000 * 60 * 60 * 24));

        return new WateringStatus(daysSince >= interval, daysSince);
    }

    private FertilizationStatus calculateFertilizationStatus(Long lastFertilization, Long registrationDate, int interval) {
        long lastTime = (lastFertilization != null) ? lastFertilization * 1000 :
                (registrationDate != null) ? registrationDate * 1000 : System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        int daysSince = (int) ((currentTime - lastTime) / (1000 * 60 * 60 * 24));

        return new FertilizationStatus(daysSince >= interval, daysSince);
    }

    private int getIntValue(QueryDocumentSnapshot document, String field, int defaultValue) {
        try {
            Long value = document.getLong(field);
            return value != null ? value.intValue() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Long getLongValue(QueryDocumentSnapshot document, String field) {
        try {
            return document.getLong(field);
        } catch (Exception e) {
            return null;
        }
    }


    public static void scheduleDailyCheck(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, PlantReminderReceiver.class);
            intent.setAction("ACTION_CHECK_PLANT_REMINDERS");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    1001,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Programar para las 9:00 AM todos los días
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            // Si ya pasaron las 9:00 AM, programar para mañana
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            // Usar setInexactRepeating para mejor eficiencia
            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );

            Log.d(TAG, "✅ Verificación diaria programada para las 9:00 AM");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error programando verificación: " + e.getMessage());
        }
    }


    public static void cancelDailyCheck(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, PlantReminderReceiver.class);
            intent.setAction("ACTION_CHECK_PLANT_REMINDERS");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    1001,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "❌ Verificación diaria cancelada");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error cancelando verificación: " + e.getMessage());
        }
    }
}