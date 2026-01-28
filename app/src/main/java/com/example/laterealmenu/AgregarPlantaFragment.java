package com.example.laterealmenu;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AgregarPlantaFragment extends Fragment {

    private static final int PICK_IMAGE = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int PERMISSION_REQUEST_CAMERA = 102;
    private static final int PERMISSION_REQUEST_GALLERY = 103;

    private static final String PLANT_ID_API_KEY = "0rVcm58Hq5Wwr1rT43saHBD7xoBEzATrZ9Y2aZzebEVEQFiao1";
    private static final String PLANT_ID_API_URL = "https://api.plant.id/v2/identify";

    // Views
    private ImageView imgPlantaManual;
    private Button btnTomarFotoManual, btnGaleriaManual, btnAnalizarIA, btnGuardarPlantaManual, btnUsarDatosIA;
    private ProgressBar progressBarManual;
    private LinearLayout layoutResultadoIA;
    private TextInputEditText etTituloRegistro, etNombrePlanta, etDescripcionBreve;
    private AutoCompleteTextView actvCategoria, actvPrioridad;
    private TextView tvFechaCreacion, tvDiasRiego, tvDiasFertilizante, tvPlantaIdentificada;
    private SeekBar seekBarRiegoManual, seekBarFertilizante;
    private Switch switchNotificacionesManual;

    private Bitmap imagenBitmap;
    private Uri imagenUri;
    private OkHttpClient client;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Datos de la planta identificada por IA
    private String nombrePlantaIA;
    private String nombreCientificoIA;
    private String descripcionIA;
    private float probabilidadIA;
    private int diasRiegoIA;
    private int diasFertilizanteIA;

    // Arrays para los dropdowns
    private String[] categorias = {"Interior", "Exterior", "Suculentas", "Cactus", "Hierbas", "Flores", "Vegetales", "Árboles", "Arbustos", "Orquídeas"};
    private String[] prioridades = {"Baja", "Media", "Alta"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("AgregarPlanta", "onCreateView: Inflando layout");
        View view = inflater.inflate(R.layout.fragment_agregar_planta, container, false);

        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews(view);
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        Log.d("AgregarPlanta", "initViews: Iniciando búsqueda de vistas");

        try {
            // Vistas existentes
            imgPlantaManual = view.findViewById(R.id.imgPlantaManual);
            btnTomarFotoManual = view.findViewById(R.id.btnTomarFotoManual);
            btnGaleriaManual = view.findViewById(R.id.btnGaleriaManual);
            btnAnalizarIA = view.findViewById(R.id.btnAnalizarIA);
            btnGuardarPlantaManual = view.findViewById(R.id.btnGuardarPlantaManual);
            btnUsarDatosIA = view.findViewById(R.id.btnUsarDatosIA);
            progressBarManual = view.findViewById(R.id.progressBarManual);
            layoutResultadoIA = view.findViewById(R.id.layoutResultadoIA);
            etNombrePlanta = view.findViewById(R.id.etNombrePlanta);
            tvPlantaIdentificada = view.findViewById(R.id.tvPlantaIdentificada);
            switchNotificacionesManual = view.findViewById(R.id.switchNotificacionesManual);

            // NUEVAS VISTAS
            etTituloRegistro = view.findViewById(R.id.etTituloRegistro);
            etDescripcionBreve = view.findViewById(R.id.etDescripcionBreve);
            actvCategoria = view.findViewById(R.id.actvCategoria);
            actvPrioridad = view.findViewById(R.id.actvPrioridad);
            tvFechaCreacion = view.findViewById(R.id.tvFechaCreacion);
            tvDiasFertilizante = view.findViewById(R.id.tvDiasFertilizante);
            seekBarFertilizante = view.findViewById(R.id.seekBarFertilizante);

            // BUSCAR EL SEEKBAR CON MÁS DETALLE
            seekBarRiegoManual = view.findViewById(R.id.seekBarRiegoManual);
            Log.d("AgregarPlanta", "Búsqueda directa de seekBarRiegoManual: " + (seekBarRiegoManual != null));

            // Si no se encuentra, intentar de otra forma
            if (seekBarRiegoManual == null) {
                Log.w("AgregarPlanta", "SeekBar no encontrado en primera búsqueda, intentando alternativa...");
                View seekBarView = view.findViewById(R.id.seekBarRiegoManual);
                if (seekBarView instanceof SeekBar) {
                    seekBarRiegoManual = (SeekBar) seekBarView;
                    Log.d("AgregarPlanta", "✅ SeekBar encontrado mediante casting");
                }
            }

            tvDiasRiego = view.findViewById(R.id.tvDiasRiego);

            // Configurar nuevos componentes
            configurarDropdowns();
            configurarFechaCreacion();
            configurarSeekBarFertilizante();

            // Debug final
            Log.d("AgregarPlanta", "=== RESUMEN VISTAS ===");
            Log.d("AgregarPlanta", "seekBarRiegoManual: " + (seekBarRiegoManual != null ? "✅ ENCONTRADO" : "❌ NULL"));
            Log.d("AgregarPlanta", "tvDiasRiego: " + (tvDiasRiego != null ? "✅ ENCONTRADO" : "❌ NULL"));
            Log.d("AgregarPlanta", "etTituloRegistro: " + (etTituloRegistro != null ? "✅ ENCONTRADO" : "❌ NULL"));
            Log.d("AgregarPlanta", "seekBarFertilizante: " + (seekBarFertilizante != null ? "✅ ENCONTRADO" : "❌ NULL"));
            Log.d("AgregarPlanta", "Botones principales: " + (btnAnalizarIA != null ? "✅ OK" : "❌ FALTAN"));

            if (seekBarRiegoManual == null) {
                Log.e("AgregarPlanta", "❌ VERIFICACIÓN: seekBarRiegoManual NO SE ENCONTRÓ");
                Toast.makeText(requireContext(), "Error: No se pudo cargar el control de días de riego", Toast.LENGTH_LONG).show();
            } else {
                Log.d("AgregarPlanta", "✅ VERIFICACIÓN: Todas las vistas cargadas correctamente");
            }

        } catch (Exception e) {
            Log.e("AgregarPlanta", "❌ Error crítico en initViews: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error crítico al cargar la interfaz", Toast.LENGTH_LONG).show();
        }
    }

    private void configurarDropdowns() {
        // Configurar categorías
        ArrayAdapter<String> categoriaAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categorias);
        actvCategoria.setAdapter(categoriaAdapter);

        // Configurar prioridades
        ArrayAdapter<String> prioridadAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, prioridades);
        actvPrioridad.setAdapter(prioridadAdapter);

        // Establecer valor por defecto para prioridad
        actvPrioridad.setText("Media", false);
    }

    private void configurarFechaCreacion() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String fechaActual = sdf.format(new Date());
        tvFechaCreacion.setText(fechaActual);

        // Hacer clickeable para cambiar fecha
        tvFechaCreacion.setOnClickListener(v -> mostrarDatePicker());
    }

    private void configurarSeekBarFertilizante() {
        if (seekBarFertilizante != null) {
            seekBarFertilizante.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int dias = progress + 1; // Rango: 1-90 días
                    tvDiasFertilizante.setText(dias + " días");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            // Valor inicial
            int diasInicial = seekBarFertilizante.getProgress() + 1;
            tvDiasFertilizante.setText(diasInicial + " días");
        }
    }

    private void mostrarDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String fechaSeleccionada = String.format(Locale.getDefault(),
                            "%02d/%02d/%d", dayOfMonth, month + 1, year);
                    tvFechaCreacion.setText(fechaSeleccionada);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    private void setupClickListeners() {
        Log.d("AgregarPlanta", "setupClickListeners: Iniciando configuración");

        try {
            // Configurar listeners básicos primero
            if (btnTomarFotoManual != null) {
                btnTomarFotoManual.setOnClickListener(v -> {
                    Log.d("AgregarPlanta", "Botón tomar foto clickeado");
                    tomarFoto();
                });
            } else {
                Log.e("AgregarPlanta", "btnTomarFotoManual es NULL");
            }

            if (btnGaleriaManual != null) {
                btnGaleriaManual.setOnClickListener(v -> {
                    Log.d("AgregarPlanta", "Botón galería clickeado");
                    abrirGaleria();
                });
            } else {
                Log.e("AgregarPlanta", "btnGaleriaManual es NULL");
            }

            if (btnAnalizarIA != null) {
                btnAnalizarIA.setOnClickListener(v -> {
                    Log.d("AgregarPlanta", "Botón analizar IA clickeado");
                    analizarConIA();
                });
            } else {
                Log.e("AgregarPlanta", "btnAnalizarIA es NULL");
            }

            if (btnGuardarPlantaManual != null) {
                btnGuardarPlantaManual.setOnClickListener(v -> {
                    Log.d("AgregarPlanta", "Botón guardar clickeado");
                    guardarPlantaManual();
                });
            } else {
                Log.e("AgregarPlanta", "btnGuardarPlantaManual es NULL");
            }

            if (btnUsarDatosIA != null) {
                btnUsarDatosIA.setOnClickListener(v -> {
                    Log.d("AgregarPlanta", "Botón usar datos IA clickeado");
                    usarDatosIA();
                });
            } else {
                Log.e("AgregarPlanta", "btnUsarDatosIA es NULL");
            }

            // CONFIGURAR SEEKBAR - CON VERIFICACIÓN EXTRA
            Log.d("AgregarPlanta", "Buscando seekBarRiegoManual...");
            if (seekBarRiegoManual != null) {
                Log.d("AgregarPlanta", "✅ seekBarRiegoManual encontrado, configurando listener...");

                // Configurar el listener DEL SEEKBAR
                seekBarRiegoManual.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        int dias = progress + 1;
                        Log.d("AgregarPlanta", "SeekBar cambiado: " + dias + " días");
                        if (tvDiasRiego != null) {
                            tvDiasRiego.setText(dias + " días");
                        } else {
                            Log.e("AgregarPlanta", "tvDiasRiego es NULL en onProgressChanged");
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        Log.d("AgregarPlanta", "SeekBar empezó a moverse");
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        Log.d("AgregarPlanta", "SeekBar dejó de moverse");
                    }
                });

                // Establecer valor inicial
                int diasInicial = seekBarRiegoManual.getProgress() + 1;
                if (tvDiasRiego != null) {
                    tvDiasRiego.setText(diasInicial + " días");
                    Log.d("AgregarPlanta", "✅ Valor inicial establecido: " + diasInicial + " días");
                } else {
                    Log.e("AgregarPlanta", "❌ tvDiasRiego es NULL - no se puede establecer valor inicial");
                }

                Log.d("AgregarPlanta", "✅ SeekBar de riego configurado exitosamente");

            } else {
                Log.e("AgregarPlanta", "❌ ERROR CRÍTICO: seekBarRiegoManual es NULL - no se puede configurar listener");

                // Intentar buscar el SeekBar nuevamente
                View view = getView();
                if (view != null) {
                    SeekBar seekBar = view.findViewById(R.id.seekBarRiegoManual);
                    if (seekBar != null) {
                        Log.d("AgregarPlanta", "✅ SeekBar encontrado en segunda búsqueda");
                        seekBarRiegoManual = seekBar;
                        // Llamar recursivamente para configurar
                        setupClickListeners();
                        return;
                    }
                }

                Toast.makeText(requireContext(), "Error: Control de riego no disponible", Toast.LENGTH_LONG).show();
            }

            // ✅ CONFIGURAR SEEKBAR DE FERTILIZANTE (si existe)
            if (seekBarFertilizante != null) {
                Log.d("AgregarPlanta", "✅ seekBarFertilizante encontrado, configurando listener...");

                seekBarFertilizante.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        int dias = progress + 1;
                        Log.d("AgregarPlanta", "SeekBar fertilizante cambiado: " + dias + " días");
                        if (tvDiasFertilizante != null) {
                            tvDiasFertilizante.setText(dias + " días");
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        Log.d("AgregarPlanta", "SeekBar fertilizante empezó a moverse");
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        Log.d("AgregarPlanta", "SeekBar fertilizante dejó de moverse");
                    }
                });

                // Establecer valor inicial para fertilizante
                int diasInicialFert = seekBarFertilizante.getProgress() + 1;
                if (tvDiasFertilizante != null) {
                    tvDiasFertilizante.setText(diasInicialFert + " días");
                    Log.d("AgregarPlanta", "✅ Valor inicial fertilizante establecido: " + diasInicialFert + " días");
                }

                Log.d("AgregarPlanta", "✅ SeekBar de fertilizante configurado exitosamente");
            } else {
                Log.w("AgregarPlanta", "⚠️ seekBarFertilizante no encontrado");
            }

            Log.d("AgregarPlanta", "✅ setupClickListeners completado exitosamente");

        } catch (Exception e) {
            Log.e("AgregarPlanta", "❌ Error en setupClickListeners: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error al configurar controles", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("AgregarPlanta", "onViewCreated ejecutado");

        // Verificación final después de que la vista está completamente creada
        new android.os.Handler().postDelayed(() -> {
            Log.d("AgregarPlanta", "=== VERIFICACIÓN FINAL ===");
            Log.d("AgregarPlanta", "seekBarRiegoManual: " + (seekBarRiegoManual != null));
            Log.d("AgregarPlanta", "seekBarFertilizante: " + (seekBarFertilizante != null));
            Log.d("AgregarPlanta", "¿Vista creada?: " + (getView() != null));

            if (seekBarRiegoManual == null && getView() != null) {
                Log.e("AgregarPlanta", "❌ VERIFICACIÓN FINAL: seekBarRiegoManual sigue NULL");
                // Último intento
                SeekBar lastTry = getView().findViewById(R.id.seekBarRiegoManual);
                Log.d("AgregarPlanta", "Último intento: " + (lastTry != null));
            }
        }, 1000);
    }

    private void analizarConIA() {
        if (imagenBitmap == null) {
            Toast.makeText(requireContext(), "⚠️ Primero selecciona una imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarManual.setVisibility(View.VISIBLE);
        btnAnalizarIA.setEnabled(false);

        String imageBase64 = bitmapToBase64(imagenBitmap);

        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("api_key", PLANT_ID_API_KEY);

            JsonArray imagesArray = new JsonArray();
            imagesArray.add(imageBase64);
            requestBody.add("images", imagesArray);

            JsonArray modifiersArray = new JsonArray();
            modifiersArray.add("crops_fast");
            modifiersArray.add("similar_images");
            requestBody.add("modifiers", modifiersArray);

            requestBody.addProperty("plant_language", "es");

            JsonArray plantDetailsArray = new JsonArray();
            plantDetailsArray.add("common_names");
            plantDetailsArray.add("url");
            plantDetailsArray.add("description");
            plantDetailsArray.add("taxonomy");
            requestBody.add("plant_details", plantDetailsArray);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestBody.toString()
            );

            Request request = new Request.Builder()
                    .url(PLANT_ID_API_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    requireActivity().runOnUiThread(() -> {
                        progressBarManual.setVisibility(View.GONE);
                        btnAnalizarIA.setEnabled(true);
                        Toast.makeText(requireContext(), "❌ Error de conexión", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body().string();
                    requireActivity().runOnUiThread(() -> {
                        progressBarManual.setVisibility(View.GONE);
                        btnAnalizarIA.setEnabled(true);

                        if (response.isSuccessful()) {
                            procesarRespuestaIA(responseBody);
                        } else {
                            Toast.makeText(requireContext(), "❌ Error en el análisis", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            progressBarManual.setVisibility(View.GONE);
            btnAnalizarIA.setEnabled(true);
            Toast.makeText(requireContext(), "❌ Error creando solicitud", Toast.LENGTH_SHORT).show();
        }
    }

    private void procesarRespuestaIA(String responseBody) {
        try {
            Log.d("AgregarPlanta", "Procesando respuesta IA: " + responseBody);

            JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);

            if (jsonResponse.has("suggestions")) {
                JsonArray suggestions = jsonResponse.getAsJsonArray("suggestions");

                if (suggestions.size() > 0) {
                    JsonObject firstSuggestion = suggestions.get(0).getAsJsonObject();

                    // ✅ CORRECCIÓN: Hacer las variables final o efectivamente final
                    final float probabilidad = firstSuggestion.has("probability") ?
                            firstSuggestion.get("probability").getAsFloat() * 100 : 0;

                    // ✅ CORRECCIÓN: Extraer detalles de la planta correctamente
                    final String nombreComun;
                    final String nombreCientifico;
                    final String descripcion;

                    if (firstSuggestion.has("plant_details")) {
                        JsonObject plantDetails = firstSuggestion.getAsJsonObject("plant_details");

                        // Nombre común
                        if (plantDetails.has("common_names") && plantDetails.getAsJsonArray("common_names").size() > 0) {
                            nombreComun = plantDetails.getAsJsonArray("common_names").get(0).getAsString();
                        } else {
                            nombreComun = "No identificado";
                        }

                        // Nombre científico
                        if (plantDetails.has("scientific_name")) {
                            nombreCientifico = plantDetails.get("scientific_name").getAsString();
                        } else {
                            nombreCientifico = "No disponible";
                        }

                        // Descripción
                        if (plantDetails.has("description") && plantDetails.getAsJsonObject("description").has("value")) {
                            descripcion = plantDetails.getAsJsonObject("description").get("value").getAsString();
                        } else {
                            descripcion = "Sin descripción disponible";
                        }
                    } else {
                        nombreComun = "No identificado";
                        nombreCientifico = "No disponible";
                        descripcion = "Sin descripción disponible";
                    }

                    // ✅ CORRECCIÓN: Guardar datos de IA en variables de instancia
                    nombrePlantaIA = nombreComun;
                    nombreCientificoIA = nombreCientifico;
                    descripcionIA = descripcion;
                    probabilidadIA = probabilidad;
                    diasRiegoIA = calcularDiasRiego(nombreComun);
                    diasFertilizanteIA = calcularDiasFertilizante(nombreComun);

                    // ✅ CORRECCIÓN: Usar variables final en el lambda
                    requireActivity().runOnUiThread(() -> {
                        String resultado = String.format("🌱 %s (%.1f%%)\n📚 %s",
                                nombreComun, probabilidad, nombreCientifico);
                        tvPlantaIdentificada.setText(resultado);
                        layoutResultadoIA.setVisibility(View.VISIBLE);

                        Toast.makeText(requireContext(), "✅ Planta identificada", Toast.LENGTH_SHORT).show();
                        Log.d("AgregarPlanta", "✅ Planta identificada: " + nombreComun + " (" + probabilidad + "%)");
                    });

                } else {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "🔍 No se pudo identificar la planta", Toast.LENGTH_SHORT).show();
                        Log.d("AgregarPlanta", "❌ No hay sugerencias en la respuesta");
                    });
                }
            } else {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "❌ Error en respuesta de la API", Toast.LENGTH_SHORT).show();
                    Log.d("AgregarPlanta", "❌ No hay campo 'suggestions' en la respuesta");
                });
            }
        } catch (Exception e) {
            Log.e("AgregarPlanta", "❌ Error procesando respuesta IA: " + e.getMessage());
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "❌ Error procesando respuesta", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void usarDatosIA() {
        if (nombrePlantaIA != null) {
            // Establecer título del registro con el nombre de la planta
            etTituloRegistro.setText("Mi " + nombrePlantaIA);
            etNombrePlanta.setText(nombrePlantaIA);
            etDescripcionBreve.setText(descripcionIA);

            // Intentar determinar categoría automáticamente
            determinarCategoriaAutomatica(nombrePlantaIA);

            // Actualizar SeekBars
            if (seekBarRiegoManual != null) {
                seekBarRiegoManual.setProgress(diasRiegoIA - 1);
            }
            if (seekBarFertilizante != null) {
                seekBarFertilizante.setProgress(diasFertilizanteIA - 1);
            }

            if (tvDiasRiego != null) {
                tvDiasRiego.setText(diasRiegoIA + " días");
            }

            layoutResultadoIA.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "✅ Datos de IA aplicados", Toast.LENGTH_SHORT).show();
        }
    }

    private void determinarCategoriaAutomatica(String nombrePlanta) {
        String nombreLower = nombrePlanta.toLowerCase();

        if (nombreLower.contains("cactus") || nombreLower.contains("suculent")) {
            actvCategoria.setText("Cactus", false);
        } else if (nombreLower.contains("rosa") || nombreLower.contains("orquídea") || nombreLower.contains("flower")) {
            actvCategoria.setText("Flores", false);
        } else if (nombreLower.contains("hierba") || nombreLower.contains("albahaca") || nombreLower.contains("menta")) {
            actvCategoria.setText("Hierbas", false);
        } else if (nombreLower.contains("tomate") || nombreLower.contains("lechuga") || nombreLower.contains("vegetal")) {
            actvCategoria.setText("Vegetales", false);
        } else {
            actvCategoria.setText("Interior", false);
        }
    }

    private void guardarPlantaManual() {
        String titulo = etTituloRegistro.getText().toString().trim();
        String nombre = etNombrePlanta.getText().toString().trim();
        String descripcion = etDescripcionBreve.getText().toString().trim();
        String categoria = actvCategoria.getText().toString().trim();
        String prioridad = actvPrioridad.getText().toString().trim();
        String fechaCreacion = tvFechaCreacion.getText().toString().trim();

        // Validaciones
        if (titulo.isEmpty() || nombre.isEmpty() || descripcion.isEmpty() || prioridad.isEmpty()) {
            Toast.makeText(requireContext(), "⚠️ Completa todos los campos obligatorios (*)", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener días de riego y fertilizante
        int diasRiego = seekBarRiegoManual != null ? seekBarRiegoManual.getProgress() + 1 : 7;
        int diasFertilizante = seekBarFertilizante != null ? seekBarFertilizante.getProgress() + 1 : 30;
        boolean notificaciones = switchNotificacionesManual.isChecked();

        if (imagenBitmap == null) {
            Toast.makeText(requireContext(), "⚠️ Agrega una imagen de la planta", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "❌ Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convertir imagen
        String imagenBase64 = bitmapToBase64(imagenBitmap);

        // ✅ CORREGIDO: Obtener timestamp actual en milisegundos y calcular próximas fechas
        long timestampActual = System.currentTimeMillis();
        long timestampActualSegundos = timestampActual / 1000;

        // Calcular próximas fechas de riego y fertilización
        Calendar calendar = Calendar.getInstance();

        // Próximo riego: hoy + días de riego
        calendar.add(Calendar.DAY_OF_YEAR, diasRiego);
        long proximoRiego = calendar.getTimeInMillis() / 1000;

        // Próxima fertilización: hoy + días de fertilizante
        calendar.setTimeInMillis(timestampActual);
        calendar.add(Calendar.DAY_OF_YEAR, diasFertilizante);
        long proximaFertilizacion = calendar.getTimeInMillis() / 1000;

        // Crear planta con todos los campos - ✅ ACTUALIZADO PARA NUEVO SISTEMA DE NOTIFICACIONES
        Map<String, Object> planta = new HashMap<>();

        // Información básica
        planta.put("tituloRegistro", titulo);
        planta.put("nombreComun", nombre);
        planta.put("nombreCientifico", nombreCientificoIA != null ? nombreCientificoIA : "No identificado");
        planta.put("descripcion", descripcion);
        planta.put("categoria", categoria.isEmpty() ? "General" : categoria);
        planta.put("prioridad", prioridad);
        planta.put("fechaCreacion", fechaCreacion);
        planta.put("cuidados", "Configurado " + (nombrePlantaIA != null ? "con IA" : "manualmente"));
        planta.put("plagas", "Por observar");
        planta.put("recomendaciones", "Riego cada " + diasRiego + " días, fertilización cada " + diasFertilizante + " días");
        planta.put("imagenBase64", imagenBase64);
        planta.put("usuarioId", user.getUid());

        // Configuración de cuidados
        planta.put("diasRiego", diasRiego);
        planta.put("diasFertilizante", diasFertilizante);

        // ✅ CORREGIDO: Fechas para notificaciones (en segundos)
        planta.put("fechaRegistro", timestampActualSegundos);
        planta.put("ultimoRiego", timestampActualSegundos);
        planta.put("ultimaFertilizacion", timestampActualSegundos);
        planta.put("proximoRiego", proximoRiego);
        planta.put("proximaFertilizacion", proximaFertilizacion);

        // Identificación por IA
        planta.put("probabilidadIdentificacion", probabilidadIA);
        planta.put("agregadoManual", nombrePlantaIA == null);

        // ✅ MEJORADO: Sistema de notificaciones
        planta.put("notificacionesActivadas", notificaciones);
        planta.put("regadoHoy", false);
        planta.put("fertilizadoHoy", false);
        planta.put("totalRiegos", 0);
        planta.put("totalFertilizaciones", 0);

        // Estado y seguimiento
        planta.put("estadoSeguimiento", "Activo");
        planta.put("progreso", 0);
        planta.put("requiereSeguimiento", true);
        planta.put("notasAdicionales", "");
        planta.put("necesitaAtencion", false);
        planta.put("salud", 100); // Salud inicial al 100%

        // Guardar en Firestore
        db.collection("plantas")
                .add(planta)
                .addOnSuccessListener(documentReference -> {
                    String plantaId = documentReference.getId();

                    Log.d("AgregarPlanta", "✅ Planta guardada - ID: " + plantaId);
                    Log.d("AgregarPlanta", "📅 Configuración - Riego: " + diasRiego + " días, Fertilización: " + diasFertilizante + " días");
                    Log.d("AgregarPlanta", "🔔 Notificaciones: " + (notificaciones ? "ACTIVADAS" : "DESACTIVADAS"));

                    Toast.makeText(requireContext(), "✅ Planta guardada exitosamente", Toast.LENGTH_SHORT).show();

                    // ✅ MEJORADO: Integración con nuevo sistema de notificaciones
                    if (notificaciones) {
                        try {
                            // Programar notificaciones para esta planta específica
                            programarNotificacionesParaPlanta(plantaId, nombre, diasRiego, diasFertilizante);

                            // Verificar inmediatamente si necesita notificaciones
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).checkPlantsNow();
                            }

                            String mensajeNotif = "🔔 Notificaciones activadas para \"" + nombre + "\"";
                            mensajeNotif += "\n💧 Riego cada " + diasRiego + " días";
                            mensajeNotif += "\n🌱 Fertilización cada " + diasFertilizante + " días";

                            Toast.makeText(requireContext(), mensajeNotif, Toast.LENGTH_LONG).show();
                            Log.d("AgregarPlanta", "🔔 Sistema de notificaciones configurado para: " + nombre);

                        } catch (Exception e) {
                            Log.e("AgregarPlanta", "❌ Error configurando notificaciones: " + e.getMessage());
                            Toast.makeText(requireContext(), "✅ Planta guardada, pero error en notificaciones", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "✅ Planta guardada - 🔕 Notificaciones desactivadas", Toast.LENGTH_SHORT).show();
                        Log.d("AgregarPlanta", "🔕 Notificaciones desactivadas para: " + nombre);
                    }

                    // ✅ MEJORADO: Redirigir a MIS PLANTAS con confirmación
                    new android.os.Handler().postDelayed(() -> {
                        if (getActivity() instanceof MainActivity) {
                            final MainActivity activity = (MainActivity) getActivity();
                            activity.loadFragment(new MisPlantasFragment(), R.id.nav_mis_plantas);

                            // Opcional: Mostrar mensaje adicional en MainActivity
                            activity.runOnUiThread(() ->
                                    Toast.makeText(activity, "🌿 " + nombre + " agregada a tu colección", Toast.LENGTH_SHORT).show()
                            );
                        }
                    }, 1500); // Pequeño delay para que el usuario vea el mensaje

                })
                .addOnFailureListener(e -> {
                    Log.e("AgregarPlanta", "❌ Error al guardar planta: " + e.getMessage(), e);
                    Toast.makeText(requireContext(), "❌ Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ✅ NUEVO MÉTODO MEJORADO: Programar notificaciones con información de días
    private void programarNotificacionesParaPlanta(String plantaId, String nombrePlanta, int diasRiego, int diasFertilizante) {
        try {
            Log.d("AgregarPlanta", "🔔 Programando notificaciones para: " + nombrePlanta);
            Log.d("AgregarPlanta", "   - Riego cada: " + diasRiego + " días");
            Log.d("AgregarPlanta", "   - Fertilización cada: " + diasFertilizante + " días");

            // El sistema principal (PlantReminderReceiver) ya está programado
            // Solo necesitamos asegurar que esté activo
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();

                // Verificar que el sistema de notificaciones esté activo
                activity.schedulePlantReminders();

                // ✅ NUEVO: Enviar notificación de confirmación con información detallada
                enviarNotificacionConfirmacion(nombrePlanta, diasRiego, diasFertilizante);

                // ✅ NUEVO: Programar notificación de recordatorio futuro
                programarRecordatorioFuturo(nombrePlanta, diasRiego, diasFertilizante, plantaId);
            }

        } catch (Exception e) {
            Log.e("AgregarPlanta", "❌ Error programando notificaciones: " + e.getMessage());
        }
    }

    // ✅ NUEVO MÉTODO: Programar recordatorio futuro
    // ✅ MÉTODO CORREGIDO Y SIMPLIFICADO
    private void programarRecordatorioFuturo(String nombrePlanta, int diasRiego, int diasFertilizante, String plantaId) {
        try {
            Log.d("AgregarPlanta", "⏰ Configurando recordatorios para: " + nombrePlanta);

            // Calcular fecha para el recordatorio (mañana a las 9:00 AM)
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            long recordatorioTimestamp = calendar.getTimeInMillis();

            // Obtener AlarmManager
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(android.content.Context.ALARM_SERVICE);

            // Crear intent para el receiver
            Intent intent = new Intent(requireContext(), PlantReminderReceiver.class);
            intent.setAction("ACTION_SINGLE_REMINDER");
            intent.putExtra("planta_nombre", nombrePlanta);
            intent.putExtra("dias_riego", diasRiego);
            intent.putExtra("dias_fertilizante", diasFertilizante);
            intent.putExtra("planta_id", plantaId);

            // Crear PendingIntent
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    (int) System.currentTimeMillis(), // ID único
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Programar la alarma
            if (alarmManager != null) {
                // Usar set() que es compatible con todas las versiones
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        recordatorioTimestamp,
                        pendingIntent
                );
                Log.d("AgregarPlanta", "✅ Recordatorio programado para: " +
                        new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(recordatorioTimestamp)));
            } else {
                Log.e("AgregarPlanta", "❌ AlarmManager no disponible");
            }

        } catch (Exception e) {
            Log.e("AgregarPlanta", "❌ Error programando recordatorio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ MÉTODO ACTUALIZADO: Notificación de confirmación con información detallada
    private void enviarNotificacionConfirmacion(String nombrePlanta, int diasRiego, int diasFertilizante) {
        try {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                NotificationHelper notificationHelper = new NotificationHelper(requireContext());

                // ✅ MEJORADO: Mensaje más informativo con días específicos
                String titulo = "🌱 " + nombrePlanta + " Agregada";
                String mensaje = "✅ Configuración guardada exitosamente\n\n";
                mensaje += "📅 Próximo riego: en " + diasRiego + " días\n";
                mensaje += "🌱 Próxima fertilización: en " + diasFertilizante + " días\n\n";
                mensaje += "💡 Te notificaremos cuando sea tiempo de cuidar tu planta";

                notificationHelper.sendPlantNotification(
                        titulo,
                        mensaje,
                        (int) System.currentTimeMillis(),
                        "confirmacion",
                        "info"
                );

                Log.d("AgregarPlanta", "📲 Notificación de confirmación enviada con información de días");
            }
        } catch (Exception e) {
            Log.e("AgregarPlanta", "❌ Error enviando notificación de confirmación: " + e.getMessage());
        }
    }

    private int calcularDiasRiego(String nombrePlanta) {
        switch (nombrePlanta.toLowerCase()) {
            case "tomate":
            case "tomato": return 2;
            case "rosa":
            case "rose": return 3;
            case "orquídea":
            case "orchid": return 7;
            case "suculenta":
            case "succulent": return 10;
            case "cactus": return 14;
            default: return 4;
        }
    }

    private int calcularDiasFertilizante(String nombrePlanta) {
        switch (nombrePlanta.toLowerCase()) {
            case "tomate":
            case "tomato": return 14;
            case "rosa":
            case "rose": return 21;
            case "orquídea":
            case "orchid": return 30;
            case "suculenta":
            case "succulent": return 60;
            case "cactus": return 90;
            default: return 30;
        }
    }

    // Métodos para cámara y galería
    private void tomarFoto() {
        // Verificar permisos primero
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            abrirCamara();
        }
    }

    private void abrirGaleria() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_GALLERY);
        } else {
            abrirGaleriaConPermiso();
        }
    }

    private void abrirCamara() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // Verificar que hay una app de cámara disponible
            if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                Toast.makeText(requireContext(), "No hay aplicación de cámara disponible", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("AgregarPlanta", "Error al abrir cámara: " + e.getMessage());
            Toast.makeText(requireContext(), "Error al abrir cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirGaleriaConPermiso() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case PERMISSION_REQUEST_CAMERA: abrirCamara(); break;
                case PERMISSION_REQUEST_GALLERY: abrirGaleriaConPermiso(); break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == PICK_IMAGE && data != null) {
                imagenUri = data.getData();
                try {
                    imagenBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imagenUri);
                    imgPlantaManual.setImageBitmap(imagenBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                imagenBitmap = (Bitmap) extras.get("data");
                imgPlantaManual.setImageBitmap(imagenBitmap);
            }
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
}