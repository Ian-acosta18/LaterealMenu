package com.example.laterealmenu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class InicioFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextView tvWelcome, tvPlantCount, tvConsultasCount, tvDailyTip;
    private TextView tvPlantCountMain, tvConsultasCountMain;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inicio, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Inicializar todas las vistas
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvPlantCount = view.findViewById(R.id.tvPlantCount);
        tvConsultasCount = view.findViewById(R.id.tvConsultasCount);
        tvDailyTip = view.findViewById(R.id.tvDailyTip);
        tvPlantCountMain = view.findViewById(R.id.tvPlantCountMain);
        tvConsultasCountMain = view.findViewById(R.id.tvConsultasCountMain);

        setupDashboard(view);
        loadUserData();
        loadRealDataFromFirebase();

        return view;
    }

    private void setupDashboard(View view) {
        CardView cardDiagnostico = view.findViewById(R.id.cardDiagnostico);
        CardView cardMisPlantas = view.findViewById(R.id.cardMisPlantas);
        CardView cardConsultas = view.findViewById(R.id.cardConsultas);
        CardView cardCalendario = view.findViewById(R.id.cardCalendario);
        CardView cardConsejos = view.findViewById(R.id.cardConsejos);
        CardView cardAgregar = view.findViewById(R.id.cardAgregar);

        if (cardDiagnostico != null) {
            cardDiagnostico.setOnClickListener(v -> navigateToDiagnostico());
        }
        if (cardMisPlantas != null) {
            cardMisPlantas.setOnClickListener(v -> navigateToMisPlantas());
        }
        if (cardConsultas != null) {
            cardConsultas.setOnClickListener(v -> navigateToConsultas());
        }
        if (cardCalendario != null) {
            cardCalendario.setOnClickListener(v -> navigateToCalendario());
        }
        if (cardConsejos != null) {
            cardConsejos.setOnClickListener(v -> navigateToConsejos());
        }
        if (cardAgregar != null) {
            cardAgregar.setOnClickListener(v -> navigateToAgregar());
        }
    }

    private void loadUserData() {
        String userEmail = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "Usuario";
        if (tvWelcome != null) {
            if (userEmail != null && !userEmail.equals("Usuario")) {
                // Extraer la primera parte del correo (antes del @)
                String userName = userEmail.split("@")[0];
                // Capitalizar la primera letra
                userName = userName.substring(0, 1).toUpperCase() + userName.substring(1);
                tvWelcome.setText("Bienvenido, " + userName + "!");
            } else {
                tvWelcome.setText("Bienvenido Usuario");
            }
        }
    }

    private void loadRealDataFromFirebase() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        System.out.println("DEBUG - User ID: " + userId);

        if (userId == null) {
            System.out.println("DEBUG - No user logged in");
            updateCounts(0, 0);
            return;
        }

        // CORREGIDO: Usar el campo correcto "usuarioId" en lugar de "userId"
        // Cargar conteo real de plantas
        db.collection("plantas")
                .whereEqualTo("usuarioId", userId) // ✅ CORREGIDO: Cambiado de "userId" a "usuarioId"
                .get()
                .addOnCompleteListener(task -> {
                    System.out.println("DEBUG - Plantas query completed: " + task.isSuccessful());
                    int plantCount = 0;

                    if (task.isSuccessful() && task.getResult() != null) {
                        plantCount = task.getResult().size();
                        System.out.println("DEBUG - Plantas count: " + plantCount);
                    } else {
                        plantCount = 0;
                        if (task.getException() != null) {
                            System.out.println("DEBUG - Plantas error: " + task.getException().getMessage());
                        }
                    }

                    final int finalPlantCount = plantCount;

                    // CORREGIDO: Usar el campo correcto "usuarioId" en lugar de "userId"
                    // Cargar conteo real de consultas
                    db.collection("consultas")
                            .whereEqualTo("usuarioId", userId) // ✅ CORREGIDO: Cambiado de "userId" a "usuarioId"
                            .get()
                            .addOnCompleteListener(task2 -> {
                                System.out.println("DEBUG - Consultas query completed: " + task2.isSuccessful());
                                int consultasCount = 0;

                                if (task2.isSuccessful() && task2.getResult() != null) {
                                    consultasCount = task2.getResult().size();
                                    System.out.println("DEBUG - Consultas count: " + consultasCount);
                                } else {
                                    if (task2.getException() != null) {
                                        System.out.println("DEBUG - Consultas error: " + task2.getException().getMessage());
                                    }
                                }

                                // Actualizar la UI con los conteos
                                updateCounts(finalPlantCount, consultasCount);
                            });
                })
                .addOnFailureListener(e -> {
                    System.out.println("DEBUG - Error en consulta de plantas: " + e.getMessage());
                    updateCounts(0, 0);
                });
    }

    private void updateCounts(int plantCount, int consultasCount) {
        System.out.println("DEBUG - Updating counts: " + plantCount + " plantas, " + consultasCount + " consultas");

        // Actualizar los TextViews pequeños en el header
        if (tvPlantCount != null) {
            String plantText = plantCount + " " + (plantCount == 1 ? "planta" : "plantas");
            tvPlantCount.setText(plantText);
            System.out.println("DEBUG - Setting tvPlantCount: " + plantText);
        }

        if (tvConsultasCount != null) {
            String consultasText = consultasCount + " " + (consultasCount == 1 ? "consulta" : "consultas");
            tvConsultasCount.setText(consultasText);
            System.out.println("DEBUG - Setting tvConsultasCount: " + consultasText);
        }

        // Actualizar los TextViews principales
        if (tvPlantCountMain != null) {
            tvPlantCountMain.setText(String.valueOf(plantCount));
            System.out.println("DEBUG - Setting tvPlantCountMain: " + plantCount);
        }

        if (tvConsultasCountMain != null) {
            tvConsultasCountMain.setText(String.valueOf(consultasCount));
            System.out.println("DEBUG - Setting tvConsultasCountMain: " + consultasCount);
        }

        // Actualizar tip del día basado en los datos
        updateDailyTip(plantCount);
    }

    private void updateDailyTip(int plantCount) {
        String dailyTip;

        if (plantCount == 0) {
            dailyTip = "🌱 ¡Comienza agregando tu primera planta! Usa el botón 'Agregar' para registrar tus plantas.";
        } else if (plantCount <= 2) {
            dailyTip = "💧 Riega tus plantas temprano en la mañana para mejores resultados y prevención de hongos.";
        } else if (plantCount <= 5) {
            dailyTip = "📅 Programa recordatorios para el cuidado de cada planta según sus necesidades específicas.";
        } else {
            dailyTip = "🌿 ¡Excelente trabajo! Tienes un jardín diverso. Considera rotar las plantas periódicamente.";
        }

        if (tvDailyTip != null) {
            tvDailyTip.setText(dailyTip);
        }
    }

    // Métodos de navegación
    private void navigateToDiagnostico() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.contenedor, new DiagnosticoPlantaFragment())
                .addToBackStack(null)
                .commit();
    }

    private void navigateToMisPlantas() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.contenedor, new MisPlantasFragment())
                .addToBackStack(null)
                .commit();
    }

    private void navigateToConsultas() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.contenedor, new MisConsultasFragment())
                .addToBackStack(null)
                .commit();
    }

    private void navigateToCalendario() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.contenedor, new CalendarioFragment())
                .addToBackStack(null)
                .commit();
    }

    private void navigateToConsejos() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.contenedor, new ConsejosFragment())
                .addToBackStack(null)
                .commit();
    }

    private void navigateToAgregar() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.contenedor, new AgregarPlantaFragment())
                .addToBackStack(null)
                .commit();
    }
}