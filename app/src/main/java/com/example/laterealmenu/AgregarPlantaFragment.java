package com.example.laterealmenu;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
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
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AgregarPlantaFragment extends Fragment {

    // Constantes de permisos e intents
    private static final int PICK_IMAGE = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int PERMISSION_REQUEST_CAMERA = 102;
    private static final int PERMISSION_REQUEST_GALLERY = 103;

    // Configuración de API PlantNet
    private static final String PLANTNET_API_KEY = "2b10AuV0b7jnV0KQZwcpDflu";
    private static final String PLANTNET_API_URL = "https://my-api.plantnet.org/v2/identify/all?api-key=";

    // Elementos de la UI
    private ImageView imgPlantaManual;
    private Button btnTomarFotoManual, btnGaleriaManual, btnAnalizarIA, btnGuardarPlantaManual, btnUsarDatosIA;
    private ProgressBar progressBarManual;
    private LinearLayout layoutResultadoIA;
    private TextInputEditText etTituloRegistro, etNombrePlanta, etDescripcionBreve;
    private AutoCompleteTextView actvCategoria, actvPrioridad;
    private TextView tvFechaCreacion, tvDiasRiego, tvDiasFertilizante, tvPlantaIdentificada;
    private SeekBar seekBarRiegoManual, seekBarFertilizante;
    private Switch switchNotificacionesManual;

    // Variables de lógica
    private Bitmap imagenBitmap;
    private Uri imagenUri;
    private OkHttpClient client;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Variables para datos de la IA
    private String nombrePlantaIA;
    private String nombreCientificoIA;
    private String descripcionIA;
    private int diasRiegoIA;
    private int diasFertilizanteIA;

    // Datos para los dropdowns
    private String[] categorias = {"Interior", "Exterior", "Suculentas", "Cactus", "Hierbas", "Flores", "Vegetales", "Árboles", "Arbustos", "Orquídeas"};
    private String[] prioridades = {"Baja", "Media", "Alta"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_agregar_planta, container, false);

        // Configurar cliente HTTP
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews(view);
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
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

        etTituloRegistro = view.findViewById(R.id.etTituloRegistro);
        etDescripcionBreve = view.findViewById(R.id.etDescripcionBreve);
        actvCategoria = view.findViewById(R.id.actvCategoria);
        actvPrioridad = view.findViewById(R.id.actvPrioridad);
        tvFechaCreacion = view.findViewById(R.id.tvFechaCreacion);
        tvDiasFertilizante = view.findViewById(R.id.tvDiasFertilizante);
        seekBarFertilizante = view.findViewById(R.id.seekBarFertilizante);
        seekBarRiegoManual = view.findViewById(R.id.seekBarRiegoManual);
        tvDiasRiego = view.findViewById(R.id.tvDiasRiego);

        configurarDropdowns();
        configurarFechaCreacion();
        configurarSeekBarFertilizante();
    }

    private void configurarDropdowns() {
        ArrayAdapter<String> categoriaAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categorias);
        actvCategoria.setAdapter(categoriaAdapter);

        ArrayAdapter<String> prioridadAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, prioridades);
        actvPrioridad.setAdapter(prioridadAdapter);
        actvPrioridad.setText("Media", false); // Valor por defecto
    }

    private void configurarFechaCreacion() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String fechaActual = sdf.format(new Date());
        tvFechaCreacion.setText(fechaActual);
        tvFechaCreacion.setOnClickListener(v -> mostrarDatePicker());
    }

    private void configurarSeekBarFertilizante() {
        if (seekBarFertilizante != null) {
            seekBarFertilizante.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int dias = progress + 1;
                    tvDiasFertilizante.setText(dias + " días");
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
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
        btnTomarFotoManual.setOnClickListener(v -> tomarFoto());
        btnGaleriaManual.setOnClickListener(v -> abrirGaleria());
        btnAnalizarIA.setOnClickListener(v -> analizarConPlantNet());
        btnGuardarPlantaManual.setOnClickListener(v -> guardarPlantaManual());
        btnUsarDatosIA.setOnClickListener(v -> usarDatosIA());

        if (seekBarRiegoManual != null) {
            seekBarRiegoManual.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int dias = progress + 1;
                    if (tvDiasRiego != null) tvDiasRiego.setText(dias + " días");
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            int diasInicial = seekBarRiegoManual.getProgress() + 1;
            if (tvDiasRiego != null) tvDiasRiego.setText(diasInicial + " días");
        }
    }

    // --- LÓGICA DE CÁMARA Y GALERÍA (Corregida para Android 13+) ---

    private void tomarFoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            abrirCamara();
        }
    }

    private void abrirCamara() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error al abrir cámara: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ MÉTODO CRÍTICO: Soluciona el problema de no poder abrir galería
    private void abrirGaleria() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ usa READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_GALLERY);
            } else {
                abrirGaleriaConPermiso();
            }
        } else {
            // Android 12 y anteriores usan READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_GALLERY);
            } else {
                abrirGaleriaConPermiso();
            }
        }
    }

    private void abrirGaleriaConPermiso() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        } catch (Exception e) {
            Log.e("Galeria", "Error: " + e.getMessage());
            Toast.makeText(requireContext(), "No se encontró galería", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case PERMISSION_REQUEST_CAMERA:
                    abrirCamara();
                    break;
                case PERMISSION_REQUEST_GALLERY:
                    abrirGaleriaConPermiso();
                    break;
            }
        } else {
            Toast.makeText(requireContext(), "Permiso denegado. Habilítalo en ajustes.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == getActivity().RESULT_OK && data != null) {
            try {
                if (requestCode == PICK_IMAGE) {
                    imagenUri = data.getData();
                    imagenBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imagenUri);
                    imgPlantaManual.setImageBitmap(imagenBitmap);
                } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                    Bundle extras = data.getExtras();
                    imagenBitmap = (Bitmap) extras.get("data");
                    imgPlantaManual.setImageBitmap(imagenBitmap);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // --- LÓGICA DE INTELIGENCIA ARTIFICIAL (PlantNet) ---

    private void analizarConPlantNet() {
        if (imagenBitmap == null) {
            Toast.makeText(requireContext(), "⚠️ Primero selecciona una imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarManual.setVisibility(View.VISIBLE);
        btnAnalizarIA.setEnabled(false);
        btnAnalizarIA.setText("Analizando...");

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            imagenBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("organs", "auto")
                    .addFormDataPart("images", "plant_image.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                    .build();

            // URL con idioma español forzado
            String url = PLANTNET_API_URL + PLANTNET_API_KEY + "&include-related-images=true&no-reject=false&lang=es";

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    requireActivity().runOnUiThread(() -> {
                        progressBarManual.setVisibility(View.GONE);
                        btnAnalizarIA.setEnabled(true);
                        btnAnalizarIA.setText("Analizar con IA");
                        Toast.makeText(requireContext(), "Error de conexión: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body().string();
                    requireActivity().runOnUiThread(() -> {
                        progressBarManual.setVisibility(View.GONE);
                        btnAnalizarIA.setEnabled(true);
                        btnAnalizarIA.setText("Analizar con IA");

                        if (response.isSuccessful()) {
                            procesarRespuestaPlantNet(responseBody);
                        } else {
                            Toast.makeText(requireContext(), "Error en API: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            progressBarManual.setVisibility(View.GONE);
            btnAnalizarIA.setEnabled(true);
            btnAnalizarIA.setText("Analizar con IA");
            Toast.makeText(requireContext(), "Error interno: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void procesarRespuestaPlantNet(String responseBody) {
        try {
            JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
            if (jsonResponse.has("results")) {
                JsonArray results = jsonResponse.getAsJsonArray("results");
                if (results.size() > 0) {
                    JsonObject bestResult = results.get(0).getAsJsonObject();
                    JsonObject species = bestResult.getAsJsonObject("species");

                    nombreCientificoIA = species.has("scientificName") ? species.get("scientificName").getAsString() : "Desconocido";

                    if (species.has("commonNames") && species.getAsJsonArray("commonNames").size() > 0) {
                        nombrePlantaIA = species.getAsJsonArray("commonNames").get(0).getAsString();
                    } else {
                        nombrePlantaIA = nombreCientificoIA;
                    }

                    float probabilidad = bestResult.has("score") ? bestResult.get("score").getAsFloat() * 100 : 0;

                    tvPlantaIdentificada.setText(String.format("🌱 %s (%.1f%%)\n📚 %s", nombrePlantaIA, probabilidad, nombreCientificoIA));
                    layoutResultadoIA.setVisibility(View.VISIBLE);

                    // Simular obtención de cuidados (aquí conectarías con Trefle si quisieras más detalle)
                    obtenerCuidadosBasicos(nombrePlantaIA);

                } else {
                    Toast.makeText(requireContext(), "No se pudo identificar la planta", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e("PlantNet", "Error parsing: " + e.getMessage());
        }
    }

    private void obtenerCuidadosBasicos(String nombre) {
        // Lógica simple para rellenar datos basada en el nombre (puedes mejorar esto)
        descripcionIA = "Planta identificada automáticamente como " + nombre + ".";
        diasRiegoIA = 7; // Valor por defecto sugerido
        diasFertilizanteIA = 30;
    }

    private void usarDatosIA() {
        if (nombrePlantaIA != null) {
            etTituloRegistro.setText("Mi " + nombrePlantaIA);
            etNombrePlanta.setText(nombrePlantaIA);
            etDescripcionBreve.setText(descripcionIA);

            if (seekBarRiegoManual != null) seekBarRiegoManual.setProgress(diasRiegoIA - 1);
            if (seekBarFertilizante != null) seekBarFertilizante.setProgress(diasFertilizanteIA - 1);

            layoutResultadoIA.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Datos de IA aplicados", Toast.LENGTH_SHORT).show();
        }
    }

    // --- GUARDADO EN FIREBASE Y ACTUALIZACIÓN DE WIDGET ---

    private void guardarPlantaManual() {
        String titulo = etTituloRegistro.getText().toString().trim();
        String nombre = etNombrePlanta.getText().toString().trim();

        if (titulo.isEmpty() || nombre.isEmpty()) {
            Toast.makeText(requireContext(), "⚠️ Completa título y nombre", Toast.LENGTH_SHORT).show();
            return;
        }
        if (imagenBitmap == null) {
            Toast.makeText(requireContext(), "⚠️ Agrega una imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGuardarPlantaManual.setEnabled(false);
        btnGuardarPlantaManual.setText("Guardando...");

        String imagenBase64 = bitmapToBase64(imagenBitmap);
        int diasRiego = seekBarRiegoManual != null ? seekBarRiegoManual.getProgress() + 1 : 7;
        int diasFertilizante = seekBarFertilizante != null ? seekBarFertilizante.getProgress() + 1 : 30;

        Map<String, Object> planta = new HashMap<>();
        planta.put("tituloRegistro", titulo);
        planta.put("nombreComun", nombre);
        planta.put("descripcion", etDescripcionBreve.getText().toString());
        planta.put("categoria", actvCategoria.getText().toString());
        planta.put("prioridad", actvPrioridad.getText().toString());
        planta.put("fechaCreacion", tvFechaCreacion.getText().toString());
        planta.put("diasRiego", diasRiego);
        planta.put("diasFertilizante", diasFertilizante);
        planta.put("imagenBase64", imagenBase64);
        planta.put("usuarioId", user.getUid());

        // Fechas para recordatorios
        long now = System.currentTimeMillis() / 1000;
        planta.put("fechaRegistro", now);
        planta.put("ultimoRiego", now);
        planta.put("ultimaFertilizacion", now);
        planta.put("notificacionesActivadas", switchNotificacionesManual.isChecked());

        db.collection("plantas").add(planta)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "✅ Planta guardada exitosamente", Toast.LENGTH_SHORT).show();

                    // ✅✅✅ ACTUALIZACIÓN DEL WIDGET ✅✅✅
                    try {
                        RecordatorioWidget.forzarActualizacion(requireContext());
                        Log.d("Widget", "Solicitud de actualización enviada");
                    } catch (Exception e) {
                        Log.e("Widget", "Error actualizando widget: " + e.getMessage());
                    }

                    // Navegar a Mis Plantas
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).loadFragment(new MisPlantasFragment(), R.id.nav_mis_plantas);
                    }
                })
                .addOnFailureListener(e -> {
                    btnGuardarPlantaManual.setEnabled(true);
                    btnGuardarPlantaManual.setText("Guardar Planta");
                    Toast.makeText(requireContext(), "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream); // Compresión al 70% para ahorrar espacio
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
    }
}