package com.example.laterealmenu;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.google.android.gms.tasks.Tasks;
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
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public void onCreate() {
        // Inicialización
    }

    @Override
    public void onDataSetChanged() {
        // Descargar datos sincrónicamente (es un hilo de fondo)
        listaPlantas.clear();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            try {
                // Bloqueamos el hilo hasta tener respuesta (válido en Widget Factory)
                QuerySnapshot querySnapshot = Tasks.await(
                        db.collection("plantas")
                                .whereEqualTo("usuarioId", user.getUid())
                                .get()
                );

                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Planta p = doc.toObject(Planta.class);
                    p.setId(doc.getId()); // Aseguramos tener el ID para clicks
                    listaPlantas.add(p);
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
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
        views.setTextViewText(R.id.widgetItemInfo, "Riego: cada " + planta.getDiasRiego() + " días");

        // Intent para abrir detalle al hacer click en la fila
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra("plant_id", planta.getId());
        fillInIntent.putExtra("fragment", "mis_plantas");
        views.setOnClickFillInIntent(R.id.widgetItemName, fillInIntent); // Click en nombre abre detalle

        // Intent para acción rápida (regar)
        Intent actionIntent = new Intent();
        actionIntent.setAction("ACTION_WIDGET_WATER");
        actionIntent.putExtra("plant_id", planta.getId());
        actionIntent.putExtra("plant_name", planta.getNombreComun());
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