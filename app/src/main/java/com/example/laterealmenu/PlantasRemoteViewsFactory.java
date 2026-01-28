package com.example.laterealmenu;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PlantasRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context context;
    private List<Planta> listaPlantas = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public PlantasRemoteViewsFactory(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        // Inicializar Firebase si es necesario
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public void onDataSetChanged() {
        // Esto se ejecuta en segundo plano al actualizar el widget
        listaPlantas.clear();

        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            try {
                Log.d("Widget", "Cargando plantas para: " + user.getEmail());
                // Consulta síncrona (bloqueante) permitida aquí
                QuerySnapshot querySnapshot = Tasks.await(
                        db.collection("plantas")
                                .whereEqualTo("usuarioId", user.getUid())
                                .get()
                );

                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Planta p = doc.toObject(Planta.class);
                    p.setId(doc.getId());
                    listaPlantas.add(p);
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                agregarPlantaError("Error de conexión");
            }
        } else {
            // Si no hay usuario logueado, mostramos aviso
            agregarPlantaError("Inicia sesión en la App");
        }

        // SI LA LISTA ESTÁ VACÍA (Usuario nuevo), agregamos un ejemplo visual
        if (listaPlantas.isEmpty() && user != null) {
            agregarPlantaError("Sin plantas aún. ¡Agrega una!");
        }
    }

    // Método auxiliar para mostrar mensajes en la lista si algo falla
    private void agregarPlantaError(String mensaje) {
        Planta p = new Planta();
        p.setNombreComun(mensaje);
        p.setDiasRiego(0);
        listaPlantas.add(p);
    }

    @Override
    public void onDestroy() {
        listaPlantas.clear();
    }

    @Override
    public int getCount() {
        return listaPlantas.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position >= listaPlantas.size()) return null;

        Planta planta = listaPlantas.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_item_planta);

        // Configurar Textos
        views.setTextViewText(R.id.widgetItemName, planta.getNombreComun());

        if (planta.getDiasRiego() > 0) {
            views.setTextViewText(R.id.widgetItemInfo, "Riego: cada " + planta.getDiasRiego() + " días");
        } else {
            views.setTextViewText(R.id.widgetItemInfo, "Toca para abrir");
        }

        // Click en el item -> Abrir App
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra("fragment", "mis_plantas");
        views.setOnClickFillInIntent(R.id.widgetItemName, fillInIntent);

        // Click en la gota -> Regar
        Intent actionIntent = new Intent();
        actionIntent.setAction("ACTION_WIDGET_WATER");
        if (planta.getId() != null) {
            actionIntent.putExtra("plant_id", planta.getId());
            actionIntent.putExtra("plant_name", planta.getNombreComun());
        }
        views.setOnClickFillInIntent(R.id.widgetAction, actionIntent);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}