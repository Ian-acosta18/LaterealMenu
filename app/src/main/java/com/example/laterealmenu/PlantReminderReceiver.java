package com.example.laterealmenu;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth; // ✅ Importante
import com.google.firebase.auth.FirebaseUser; // ✅ Importante
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
            String mensaje = "No olvides cuidar tu planta:\n\n" +
                    "💧 Riego cada: " + diasRiego + " días\n" +
                    "🌱 Fertilización cada: " + diasFertilizante + " días\n\n" +
                    "¡Tu planta te lo agradecerá! 🌟";

            notificationHelper.sendPlantNotification(titulo, mensaje, (int) System.currentTimeMillis(), plantaId, "reminder");
            Log.d(TAG, "✅ Recordatorio personalizado enviado para: " + plantaNombre);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error enviando recordatorio personalizado: " + e.getMessage());
        }
    }

    private void checkAllPlants(Context context) {
        Log.d(TAG, "🔍 Verificando plantas...");

        // ✅ 1. OBTENER EL USUARIO ACTUAL
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // ✅ 2. SI NO HAY USUARIO, NO HACER NADA (Evita notificar plantas ajenas)
        if (user == null) {
            Log.d(TAG, "⚠️ No hay usuario logueado, cancelando verificación de fondo.");
            return;
        }

        String userId = user.getUid();
        Log.d(TAG, "👤 Verificando solo plantas del usuario: " + userId);

        db.collection("plantas")
                .whereEqualTo("usuarioId", userId) // ✅ 3. FILTRAR POR ID DE USUARIO
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

                            // Usamos getDateValue para evitar el bug de fechas null
                            Long ultimoRiego = getDateValue(document, "ultimoRiego");
                            Long ultimaFertilizacion = getDateValue(document, "ultimaFertilizacion");
                            Long fechaRegistro = getDateValue(document, "fechaRegistro");

                            if (displayName != null) {
                                // Riego
                                WateringStatus waterStatus = calculateWateringStatus(ultimoRiego, fechaRegistro, diasRiego);
                                if (waterStatus.needsWatering) {
                                    String mensaje = "💧 Es hora de regar \"" + displayName + "\". Configurado cada " + diasRiego + " días.";
                                    notificationHelper.sendPlantNotification("Recordatorio de Riego 🌱", mensaje, notificationId++, plantId, "water");
                                }

                                // Fertilización
                                FertilizationStatus fertStatus = calculateFertilizationStatus(ultimaFertilizacion, fechaRegistro, diasFertilizante);
                                if (fertStatus.needsFertilization) {
                                    String mensaje = "🌱 Es hora de fertilizar \"" + displayName + "\". Configurado cada " + diasFertilizante + " días.";
                                    notificationHelper.sendPlantNotification("Recordatorio de Fertilización 🌿", mensaje, notificationId++, plantId, "fertilizer");
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error procesando planta: " + e.getMessage());
                        }
                    }
                    Log.d(TAG, "✅ Verificación completada para el usuario.");
                })
                .addOnFailureListener(e -> Log.e(TAG, "❌ Error al verificar plantas: " + e.getMessage()));
    }

    // ✅ MÉTODO CRÍTICO: Maneja tanto Timestamp como Long
    private Long getDateValue(QueryDocumentSnapshot document, String field) {
        try {
            Timestamp timestamp = document.getTimestamp(field);
            if (timestamp != null) {
                return timestamp.getSeconds();
            }
            return document.getLong(field);
        } catch (Exception e) {
            return null;
        }
    }

    // Clases auxiliares y métodos de cálculo
    private static class WateringStatus {
        boolean needsWatering;
        int daysSince;
        WateringStatus(boolean needsWatering, int daysSince) { this.needsWatering = needsWatering; this.daysSince = daysSince; }
    }

    private static class FertilizationStatus {
        boolean needsFertilization;
        int daysSince;
        FertilizationStatus(boolean needsFertilization, int daysSince) { this.needsFertilization = needsFertilization; this.daysSince = daysSince; }
    }

    private WateringStatus calculateWateringStatus(Long lastWatering, Long registrationDate, int interval) {
        long lastTime = (lastWatering != null) ? lastWatering * 1000 :
                (registrationDate != null) ? registrationDate * 1000 : System.currentTimeMillis();
        int daysSince = (int) ((System.currentTimeMillis() - lastTime) / (1000 * 60 * 60 * 24));
        return new WateringStatus(daysSince >= interval, daysSince);
    }

    private FertilizationStatus calculateFertilizationStatus(Long lastFertilization, Long registrationDate, int interval) {
        long lastTime = (lastFertilization != null) ? lastFertilization * 1000 :
                (registrationDate != null) ? registrationDate * 1000 : System.currentTimeMillis();
        int daysSince = (int) ((System.currentTimeMillis() - lastTime) / (1000 * 60 * 60 * 24));
        return new FertilizationStatus(daysSince >= interval, daysSince);
    }

    private int getIntValue(QueryDocumentSnapshot document, String field, int defaultValue) {
        try {
            Long value = document.getLong(field);
            return value != null ? value.intValue() : defaultValue;
        } catch (Exception e) { return defaultValue; }
    }

    public static void scheduleDailyCheck(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, PlantReminderReceiver.class);
            intent.setAction("ACTION_CHECK_PLANT_REMINDERS");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
            Log.d(TAG, "✅ Verificación diaria programada (9:00 AM)");
        } catch (Exception e) { Log.e(TAG, "❌ Error programando: " + e.getMessage()); }
    }

    public static void cancelDailyCheck(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, PlantReminderReceiver.class);
            intent.setAction("ACTION_CHECK_PLANT_REMINDERS");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "❌ Verificación cancelada");
        } catch (Exception e) { Log.e(TAG, "❌ Error cancelando: " + e.getMessage()); }
    }
}