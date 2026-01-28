package com.example.laterealmenu;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProgresoPlantaFragment extends Fragment {

    private Planta planta;
    private FirebaseFirestore db;

    // Views
    private TextView tvEstadoRiego, tvEstadoFertilizacion, tvTotalRiegos, tvTotalFertilizaciones;
    private Button btnRegar, btnFertilizar, btnGuardarNotas;
    private TextInputEditText etNotasAdicionales;

    public static ProgresoPlantaFragment newInstance(Planta planta) {
        ProgresoPlantaFragment fragment = new ProgresoPlantaFragment();
        Bundle args = new Bundle();
        args.putSerializable("planta", planta);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            planta = (Planta) getArguments().getSerializable("planta");
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progreso_planta, container, false);
        initViews(view);
        cargarDatosActuales();
        setupClickListeners();
        return view;
    }

    private void initViews(View view) {
        tvEstadoRiego = view.findViewById(R.id.tvEstadoRiego);
        tvEstadoFertilizacion = view.findViewById(R.id.tvEstadoFertilizacion);
        tvTotalRiegos = view.findViewById(R.id.tvTotalRiegos);
        tvTotalFertilizaciones = view.findViewById(R.id.tvTotalFertilizaciones);
        btnRegar = view.findViewById(R.id.btnRegar);
        btnFertilizar = view.findViewById(R.id.btnFertilizar);
        btnGuardarNotas = view.findViewById(R.id.btnGuardarNotas);
        etNotasAdicionales = view.findViewById(R.id.etNotasAdicionales);
    }

    private void cargarDatosActuales() {
        if (planta != null) {
            // Estado de riego
            if (planta.isRegadoHoy()) {
                tvEstadoRiego.setText("✅ Regado hoy");
                tvEstadoRiego.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                btnRegar.setEnabled(false);
                btnRegar.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            } else {
                tvEstadoRiego.setText("❌ Pendiente de riego");
                tvEstadoRiego.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                btnRegar.setEnabled(true);
                btnRegar.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
            }

            // Estado de fertilización
            if (planta.isFertilizadoHoy()) {
                tvEstadoFertilizacion.setText("✅ Fertilizado hoy");
                tvEstadoFertilizacion.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                btnFertilizar.setEnabled(false);
                btnFertilizar.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            } else {
                tvEstadoFertilizacion.setText("❌ Pendiente de fertilización");
                tvEstadoFertilizacion.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                btnFertilizar.setEnabled(true);
                btnFertilizar.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark));
            }

            // Totales
            tvTotalRiegos.setText("Total de riegos: " + planta.getTotalRiegos());
            tvTotalFertilizaciones.setText("Total de fertilizaciones: " + planta.getTotalFertilizaciones());

            // Notas
            if (planta.getNotasAdicionales() != null) {
                etNotasAdicionales.setText(planta.getNotasAdicionales());
            }
        }
    }

    private void setupClickListeners() {
        btnRegar.setOnClickListener(v -> registrarRiego());
        btnFertilizar.setOnClickListener(v -> registrarFertilizacion());
        btnGuardarNotas.setOnClickListener(v -> guardarNotas());
    }

    private void registrarRiego() {
        new AlertDialog.Builder(requireContext())
                .setTitle("💧 Registrar Riego")
                .setMessage("¿Confirmas que regaste la planta \"" + planta.getNombreComun() + "\"?")
                .setPositiveButton("✅ Sí, regué", (dialog, which) -> {
                    actualizarEstadoRiego();
                    Toast.makeText(requireContext(), "✅ Riego registrado exitosamente", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("❌ Cancelar", null)
                .show();
    }

    private void registrarFertilizacion() {
        new AlertDialog.Builder(requireContext())
                .setTitle("🌱 Registrar Fertilización")
                .setMessage("¿Confirmas que fertilizaste la planta \"" + planta.getNombreComun() + "\"?")
                .setPositiveButton("✅ Sí, fertilicé", (dialog, which) -> {
                    actualizarEstadoFertilizacion();
                    Toast.makeText(requireContext(), "✅ Fertilización registrada exitosamente", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("❌ Cancelar", null)
                .show();
    }

    private void actualizarEstadoRiego() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("regadoHoy", true);
        updates.put("fechaUltimoRiego", new Date());
        updates.put("totalRiegos", planta.getTotalRiegos() + 1);

        db.collection("plantas").document(planta.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    planta.setRegadoHoy(true);
                    planta.setTotalRiegos(planta.getTotalRiegos() + 1);
                    cargarDatosActuales();
                });
    }

    private void actualizarEstadoFertilizacion() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fertilizadoHoy", true);
        updates.put("fechaUltimaFertilizacion", new Date());
        updates.put("totalFertilizaciones", planta.getTotalFertilizaciones() + 1);

        db.collection("plantas").document(planta.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    planta.setFertilizadoHoy(true);
                    planta.setTotalFertilizaciones(planta.getTotalFertilizaciones() + 1);
                    cargarDatosActuales();
                });
    }

    private void guardarNotas() {
        String notas = etNotasAdicionales.getText().toString().trim();

        db.collection("plantas").document(planta.getId())
                .update("notasAdicionales", notas)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "✅ Notas guardadas", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "❌ Error al guardar notas", Toast.LENGTH_SHORT).show();
                });
    }
}