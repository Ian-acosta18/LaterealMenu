package com.example.laterealmenu;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

public class RecordatorioWidget extends AppWidgetProvider {

    public static final String ACTION_WIDGET_WATER = "ACTION_WIDGET_WATER";

    // ✅ MÉTODO CLAVE: Llama a esto desde cualquier parte de la app para refrescar el widget
    public static void forzarActualizacion(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        // Obtener los IDs de todos los widgets activos de esta App
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, RecordatorioWidget.class));

        // 1. Notificar a la lista que los datos cambiaron (Esto recarga desde Firebase)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widgetListView);

        // 2. Actualizar la vista general (título, botón refrescar)
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Configurar el layout principal
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_recordatorio);

        // Configurar el Servicio del Adaptador (La lista)
        Intent intent = new Intent(context, PlantasWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widgetListView, intent);

        // Configurar vista vacía
        views.setEmptyView(R.id.widgetListView, R.id.empty_view);

        // 1. Plantilla de Click General (para abrir la app)
        Intent appIntent = new Intent(context, MainActivity.class);
        PendingIntent appPendingIntent = PendingIntent.getActivity(context, 0, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(R.id.widgetListView, appPendingIntent);

        // 2. Botón Refrescar (Manual)
        Intent refreshIntent = new Intent(context, RecordatorioWidget.class);
        refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = {appWidgetId};
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, appWidgetId,
                refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btnWidgetRefresh, refreshPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        // Asegurar que la lista se actualice al poner el widget
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widgetListView);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // Manejar el click de "Regar" desde el widget
        if (ACTION_WIDGET_WATER.equals(intent.getAction())) {
            String plantId = intent.getStringExtra("plant_id");
            String plantName = intent.getStringExtra("plant_name");

            if (plantId != null) {
                // Actualizar en Firebase (Segundo plano)
                FirebaseFirestore.getInstance().collection("plantas")
                        .document(plantId)
                        .update("ultimoRiego", System.currentTimeMillis() / 1000)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "💧 " + plantName + " regada", Toast.LENGTH_SHORT).show();

                            // ✅ AQUÍ: Refresca el widget automáticamente tras regar
                            forzarActualizacion(context);
                        });
            }
        }
    }
}