package com.example.laterealmenu;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

public class DiagnosticoPlantaFragment extends Fragment {


    private static final int PICK_IMAGE = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int PERMISSION_REQUEST_CAMERA = 102;
    private static final int PERMISSION_REQUEST_GALLERY = 103;

    // MÚLTIPLES API KEYS para rotación automática
    private static final String[] PLANT_ID_API_KEYS = {
            "G6Rx6DXluRXyqYdHJQ1ikopRobsttDzubTebrA2NCCJ2rw9RpC",
            "HUtpjB4Zl0ajb10opUfb9NEsKZF0FWpz1avBktJzwvojoGJQ54"
    };

    private static final String PLANT_ID_API_URL = "https://api.plant.id/v2/identify";

    // Variables para gestión de API keys
    private int currentApiKeyIndex = 0;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 2;

    private ImageView imgPlanta;
    private Button btnTomarFoto, btnGaleria, btnAnalizar;
    private ProgressBar progressBar;
    private CardView cardProgress;
    private CardView resultadoLayout;
    private TextView tvResultado, tvEstado, tvPlagas, tvRecomendaciones, tvNombreCientifico;
    private Bitmap imagenBitmap;
    private Uri imagenUri;

    private OkHttpClient client;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Variables para almacenar los datos del diagnóstico
    private String nombreComunDiagnostico;
    private String nombreCientificoDiagnostico;
    private String descripcionDiagnostico;
    private float probabilidadDiagnostico;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diagnostico_planta, container, false);

        // Configurar cliente HTTP
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)  // Reducido de 30 a 15
                .readTimeout(15, TimeUnit.SECONDS)     // Reducido de 30 a 15
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews(view);
        setupClickListeners();

        return view;
    }
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Exception e) {
            Log.e("Network", "Error verificando conexión: " + e.getMessage());
            return false;
        }
    }
    private void initViews(View view) {
        imgPlanta = view.findViewById(R.id.imgPlanta);
        btnTomarFoto = view.findViewById(R.id.btnTomarFoto);
        btnGaleria = view.findViewById(R.id.btnGaleria);
        btnAnalizar = view.findViewById(R.id.btnAnalizar);
        progressBar = view.findViewById(R.id.progressBar);
        resultadoLayout = view.findViewById(R.id.resultadoLayout);
        tvResultado = view.findViewById(R.id.tvResultado);
        tvEstado = view.findViewById(R.id.tvEstado);
        tvPlagas = view.findViewById(R.id.tvPlagas);
        tvRecomendaciones = view.findViewById(R.id.tvRecomendaciones);
        tvNombreCientifico = view.findViewById(R.id.tvNombreCientifico);
        cardProgress = view.findViewById(R.id.cardProgress);
    }

    private void setupClickListeners() {
        btnTomarFoto.setOnClickListener(v -> tomarFoto());
        btnGaleria.setOnClickListener(v -> abrirGaleria());
        btnAnalizar.setOnClickListener(v -> analizarPlantaConAPI());
    }

    // MÉTODOS NUEVOS PARA GESTIÓN DE API KEYS
    private String getCurrentApiKey() {
        return PLANT_ID_API_KEYS[currentApiKeyIndex];
    }

    private void rotateToNextApiKey() {
        int oldIndex = currentApiKeyIndex;
        currentApiKeyIndex = (currentApiKeyIndex + 1) % PLANT_ID_API_KEYS.length;
        retryCount = 0;

        Log.d("APIDebug", "🔄 Rotando API Key: " + oldIndex + " → " + currentApiKeyIndex);
        mostrarMensaje("🔄 Usando clave API alternativa...");
    }

    private boolean shouldRotateApiKey() {
        return retryCount >= MAX_RETRIES;
    }

    private void handleApiKeyRotation() {
        if (shouldRotateApiKey()) {
            rotateToNextApiKey();
            // Reintentar automáticamente con la nueva key
            analizarPlantaConAPI();
        }
    }

    // MÉTODO MODIFICADO: analizarPlantaConAPI()
    private void analizarPlantaConAPI() {
        if (imagenBitmap == null) {
            mostrarMensaje("⚠️ Primero selecciona una imagen");
            return;
        }
        if (!isNetworkAvailable()) {
            mostrarMensaje("❌ Sin conexión a internet");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnAnalizar.setEnabled(false);
        resultadoLayout.setVisibility(View.GONE);

        String currentApiKey = getCurrentApiKey();

        Log.d("APIDebug", "=== INICIANDO ANÁLISIS ===");
        Log.d("APIDebug", "Usando API Key índice: " + currentApiKeyIndex);
        Log.d("APIDebug", "Reintento: " + retryCount);

        // Verificar que la API Key no sea placeholder
        if (currentApiKey.equals("tu_segunda_api_key_aqui") ||
                currentApiKey.equals("tu_tercera_api_key_aqui") ||
                currentApiKey.equals("tu_cuarta_api_key_aqui")) {

            Log.e("APIDebug", "❌ ERROR: API Key de placeholder detectada en índice " + currentApiKeyIndex);
            mostrarMensaje("❌ Configura tus API Keys adicionales");
            progressBar.setVisibility(View.GONE);
            btnAnalizar.setEnabled(true);
            return;
        }

        if (currentApiKey.isEmpty()) {
            Log.e("APIDebug", "❌ ERROR: API Key vacía");
            mostrarMensaje("❌ Error: API Key no configurada");
            progressBar.setVisibility(View.GONE);
            btnAnalizar.setEnabled(true);
            return;
        }

        // Convertir bitmap a base64
        String imageBase64 = bitmapToBase64(imagenBitmap);
        Log.d("APIDebug", "Imagen convertida a Base64");

        // Crear JSON request
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("api_key", currentApiKey);

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

            Log.d("APIDebug", "Enviando solicitud a Plant.id con key índice: " + currentApiKeyIndex);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("APIDebug", "❌ Error de conexión: " + e.getMessage());
                    requireActivity().runOnUiThread(() -> {
                        retryCount++;

                        if (shouldRotateApiKey() && currentApiKeyIndex < PLANT_ID_API_KEYS.length - 1) {
                            // Rotar automáticamente a la siguiente key
                            handleApiKeyRotation();
                        } else {
                            tvResultado.setText("❌ Error de conexión: " + e.getMessage());
                            progressBar.setVisibility(View.GONE);
                            btnAnalizar.setEnabled(true);
                            mostrarMensaje("Error de conexión con el servidor");
                        }
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d("APIDebug", "✅ Respuesta recibida. Código: " + response.code());

                    requireActivity().runOnUiThread(() -> {
                        if (response.isSuccessful()) {

                            retryCount = 0;
                            Log.d("APIDebug", "✅ Análisis exitoso, procesando respuesta...");
                            procesarRespuestaAPI(responseBody);
                        } else {
                            retryCount++;
                            String errorMsg = "Error del servidor: " + response.code();

                            // Detectar errores específicos de API key
                            if (response.code() == 401 || response.code() == 403) {
                                errorMsg = "Clave API inválida o sin créditos (Error " + response.code() + ")";

                                if (currentApiKeyIndex < PLANT_ID_API_KEYS.length - 1) {
                                    errorMsg += " - Probando con clave alternativa...";
                                    handleApiKeyRotation();
                                    return; // Salir, ya que se reintentará automáticamente
                                } else {
                                    errorMsg += " - No hay más claves disponibles";
                                }
                            }

                            if (responseBody.contains("error")) {
                                try {
                                    JsonObject errorJson = new Gson().fromJson(responseBody, JsonObject.class);
                                    if (errorJson.has("error")) {
                                        errorMsg = "Error: " + errorJson.get("error").getAsString();
                                    }
                                    if (errorJson.has("message")) {
                                        errorMsg += " - " + errorJson.get("message").getAsString();
                                    }
                                } catch (Exception e) {
                                    Log.e("APIDebug", "No se pudo parsear error response");
                                }
                            }

                            tvResultado.setText("❌ " + errorMsg);
                            progressBar.setVisibility(View.GONE);
                            btnAnalizar.setEnabled(true);

                            if (!shouldRotateApiKey() || currentApiKeyIndex >= PLANT_ID_API_KEYS.length - 1) {
                                mostrarMensaje(errorMsg);
                            }
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e("APIDebug", "❌ Error creando request: " + e.getMessage());
            progressBar.setVisibility(View.GONE);
            btnAnalizar.setEnabled(true);
            mostrarMensaje("Error creando la solicitud");
        }
    }

    // MÉTODO NUEVO: Para mostrar información de las API keys
    private void mostrarInfoAPIKeys() {
        StringBuilder info = new StringBuilder();
        info.append("🔑 API Keys Configuradas:\n\n");

        for (int i = 0; i < PLANT_ID_API_KEYS.length; i++) {
            String status = (i == currentApiKeyIndex) ? "✅ ACTUAL" : "⏳ DISPONIBLE";
            String keyPreview = PLANT_ID_API_KEYS[i].substring(0, Math.min(10, PLANT_ID_API_KEYS[i].length())) + "...";

            // Detectar placeholders
            if (PLANT_ID_API_KEYS[i].contains("tu_") && PLANT_ID_API_KEYS[i].contains("_aqui")) {
                status = "❌ NO CONFIGURADA";
                keyPreview = "PLACEHOLDER";
            }

            info.append((i + 1) + ". ").append(status).append("\n");
            info.append("   ").append(keyPreview).append("\n\n");
        }

        info.append("Reintentos actuales: ").append(retryCount).append("/").append(MAX_RETRIES);

        new AlertDialog.Builder(requireContext())
                .setTitle("Información de API Keys")
                .setMessage(info.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    // MÉTODO NUEVO: Para forzar rotación manual
    private void rotarClaveManual() {
        if (currentApiKeyIndex < PLANT_ID_API_KEYS.length - 1) {
            rotateToNextApiKey();
            mostrarMensaje("🔄 Clave API rotada manualmente a índice: " + (currentApiKeyIndex + 1));
        } else {
            mostrarMensaje("⚠️ Ya estás en la última clave API disponible");
        }
    }

    // MODIFICAR el método setupClickListeners para agregar botón de info

    private void procesarRespuestaAPI(String responseBody) {
        try {
            JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
            Log.d("APIDebug", "Procesando respuesta de la API...");

            if (jsonResponse.has("suggestions")) {
                JsonArray suggestions = jsonResponse.getAsJsonArray("suggestions");
                Log.d("APIDebug", "Número de sugerencias: " + suggestions.size());

                if (suggestions.size() > 0) {
                    JsonObject firstSuggestion = suggestions.get(0).getAsJsonObject();
                    JsonObject plantDetails = firstSuggestion.getAsJsonObject("plant_details");

                    // Extraer información con verificaciones
                    String nombreComun = "No identificado";
                    if (plantDetails.has("common_names") && plantDetails.getAsJsonArray("common_names").size() > 0) {
                        nombreComun = plantDetails.getAsJsonArray("common_names").get(0).getAsString();
                    }

                    String nombreCientifico = plantDetails.has("scientific_name") ?
                            plantDetails.get("scientific_name").getAsString() : "No disponible";

                    String descripcion = "Sin descripción disponible";
                    if (plantDetails.has("description") && plantDetails.getAsJsonObject("description").has("value")) {
                        descripcion = plantDetails.getAsJsonObject("description").get("value").getAsString();
                    }

                    float probabilidad = firstSuggestion.has("probability") ?
                            firstSuggestion.get("probability").getAsFloat() * 100 : 0;

                    Log.d("APIDebug", "Planta identificada: " + nombreComun + " (" + probabilidad + "%)");

                    // Guardar datos para posible guardado
                    nombreComunDiagnostico = nombreComun;
                    nombreCientificoDiagnostico = nombreCientifico;
                    descripcionDiagnostico = descripcion;
                    probabilidadDiagnostico = probabilidad;

                    generarDiagnosticoCompletoAPI(nombreComun, nombreCientifico, descripcion, probabilidad);

                } else {
                    Log.d("APIDebug", "No se encontraron sugerencias");
                    tvResultado.setText("🔍 No se pudo identificar la planta en la imagen");
                    progressBar.setVisibility(View.GONE);
                    btnAnalizar.setEnabled(true);
                    mostrarMensaje("No se pudo identificar la planta");
                }

            } else {
                Log.e("APIDebug", "Formato de respuesta inválido");
                tvResultado.setText("❌ Formato de respuesta inválido");
                progressBar.setVisibility(View.GONE);
                btnAnalizar.setEnabled(true);
                mostrarMensaje("Error en la respuesta del servidor");
            }

        } catch (Exception e) {
            Log.e("APIDebug", "❌ Error procesando respuesta: " + e.getMessage());
            tvResultado.setText("❌ Error procesando respuesta de la API");
            progressBar.setVisibility(View.GONE);
            btnAnalizar.setEnabled(true);
            mostrarMensaje("Error procesando la respuesta");
        }
    }

    private void generarDiagnosticoCompletoAPI(String nombreComun, String nombreCientifico, String descripcion, float probabilidad) {
        // Información detallada basada en la planta identificada
        String estado = generarEstadoDetallado(nombreComun);
        String plagas = generarAnalisisPlagasDetallado(nombreComun);
        String recomendaciones = generarRecomendacionesDetalladas(nombreComun);

        // Mostrar resultados COMPLETOS
        tvResultado.setText(String.format("🌱 %s (%.1f%% de confianza)", nombreComun, probabilidad));
        tvNombreCientifico.setText(String.format("📚 Nombre científico: %s", nombreCientifico));
        tvEstado.setText(String.format("📖 Descripción: %s\n\n%s",
                descripcion.length() > 200 ? descripcion.substring(0, 200) + "..." : descripcion,
                estado));
        tvPlagas.setText(plagas);
        tvRecomendaciones.setText(recomendaciones);

        resultadoLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        btnAnalizar.setEnabled(true);

        // ✅ GUARDAR LOS DATOS EN LAS VARIABLES GLOBALES
        nombreComunDiagnostico = nombreComun;
        nombreCientificoDiagnostico = nombreCientifico;
        descripcionDiagnostico = descripcion;
        probabilidadDiagnostico = probabilidad;

        Log.d("APIDebug", "✅ Análisis completado para: " + nombreComun);
        mostrarMensaje("✅ Análisis completado con Plant.id API");

        // ✅ MOSTRAR DIÁLOGO PARA GUARDAR CONSULTA
        mostrarDialogoGuardarConsulta();
    }

    private void mostrarDialogoGuardarConsulta() {
        if (nombreComunDiagnostico == null) {
            Toast.makeText(requireContext(), "❌ Primero analiza una planta", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("🌱 ¿Guardar Diagnóstico?")
                .setMessage("¿Qué deseas hacer con el diagnóstico de \"" + nombreComunDiagnostico + "\"?")
                .setPositiveButton("💾 Guardar en Consultas", (dialog, which) -> {
                    Log.d("Diagnostico", "Usuario eligió guardar solo en consultas");
                    guardarConsultaEnFirebase(false);
                })
                .setNegativeButton("❌ Cancelar", (dialog, which) -> {
                    Log.d("Diagnostico", "Usuario canceló guardar consulta");
                    Toast.makeText(requireContext(), "Diagnóstico no guardado", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("📄 Guardar con PDF", (dialog, which) -> {
                    Log.d("Diagnostico", "Usuario eligió guardar con PDF");
                    guardarConsultaEnFirebase(true);
                })
                .show();
    }

    private void guardarConsultaEnFirebase(boolean generarPDF) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "❌ Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nombreComunDiagnostico == null || imagenBitmap == null) {
            Toast.makeText(requireContext(), "❌ No hay imagen para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("Diagnostico", "Guardando consulta con imagen para: " + nombreComunDiagnostico);

        // Mostrar progreso
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Guardando consulta con imagen...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Convertir imagen a Base64 con compresión
        String imagenBase64 = bitmapToBase64(imagenBitmap);
        Log.d("Diagnostico", "Imagen convertida a Base64, longitud: " + imagenBase64.length());

        // Crear objeto consulta para Firestore
        Map<String, Object> consulta = new HashMap<>();
        consulta.put("nombrePlanta", nombreComunDiagnostico);
        consulta.put("nombreCientifico", nombreCientificoDiagnostico != null ? nombreCientificoDiagnostico : "No disponible");
        consulta.put("descripcion", descripcionDiagnostico != null ? descripcionDiagnostico : "Sin descripción");
        consulta.put("diagnostico", tvEstado != null ? tvEstado.getText().toString() : "Diagnóstico no disponible");
        consulta.put("plagas", tvPlagas != null ? tvPlagas.getText().toString() : "Análisis de plagas no disponible");
        consulta.put("recomendaciones", tvRecomendaciones != null ? tvRecomendaciones.getText().toString() : "Recomendaciones no disponibles");
        consulta.put("imagenBase64", imagenBase64); // ✅ AQUÍ SE GUARDA LA IMAGEN
        consulta.put("usuarioId", user.getUid());
        consulta.put("diasRiegoRecomendados", calcularDiasRiego(nombreComunDiagnostico));
        consulta.put("diasFertilizanteRecomendados", calcularDiasFertilizante(nombreComunDiagnostico));
        consulta.put("fechaDiagnostico", com.google.firebase.Timestamp.now());
        consulta.put("probabilidadIdentificacion", probabilidadDiagnostico);
        consulta.put("notificacionesActivadas", true);
        consulta.put("tipo", "diagnostico_ia");
        consulta.put("tieneImagen", true);

        // Guardar en colección "consultas"
        db.collection("consultas")
                .add(consulta)
                .addOnSuccessListener(documentReference -> {
                    progressDialog.dismiss();
                    String consultaId = documentReference.getId();
                    Log.d("Diagnostico", "✅ Consulta con imagen guardada con ID: " + consultaId);

                    Toast.makeText(requireContext(), "✅ Diagnóstico guardado con imagen", Toast.LENGTH_SHORT).show();

                    if (generarPDF) {
                        generarPDFDiagnostico(consultaId);
                    } else {
                        // Redirigir a Mis Consultas
                        if (getActivity() instanceof MainActivity) {
                            MainActivity mainActivity = (MainActivity) getActivity();
                            mainActivity.loadFragment(new MisConsultasFragment(), R.id.nav_mis_consultas);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("Diagnostico", "❌ Error al guardar consulta: " + e.getMessage());
                    Toast.makeText(requireContext(), "❌ Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void generarPDFDiagnostico(String consultaId) {
        try {
            // Crear nombre del archivo
            String fileName = "Diagnostico_" +
                    (nombreComunDiagnostico != null ? nombreComunDiagnostico.replace(" ", "_") : "Planta") + "_" +
                    System.currentTimeMillis() + ".pdf";

            File file;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10+ - Usar MediaStore para guardar en Downloads público
                file = guardarPDFConMediaStore(fileName);
            } else {
                // Android 9 y anteriores - usar Downloads público directamente
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                file = new File(downloadsDir, fileName);
            }

            // Crear el documento PDF
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(12);

            // Contenido del PDF
            int yPos = 50;

            // Título
            paint.setTextSize(20);
            paint.setFakeBoldText(true);
            paint.setColor(Color.parseColor("#2E7D32"));
            canvas.drawText("🌱 Diagnóstico de Planta - AgroExpert", 50, yPos, paint);

            paint.setColor(Color.BLACK);
            yPos += 40;

            // Información básica
            paint.setTextSize(14);
            paint.setFakeBoldText(true);
            canvas.drawText("INFORMACIÓN BÁSICA", 50, yPos, paint);

            paint.setTextSize(12);
            paint.setFakeBoldText(false);
            yPos += 25;
            canvas.drawText("• Planta: " + nombreComunDiagnostico, 50, yPos, paint);
            yPos += 20;
            canvas.drawText("• Nombre científico: " + nombreCientificoDiagnostico, 50, yPos, paint);
            yPos += 20;
            canvas.drawText("• Probabilidad de identificación: " + String.format("%.1f", probabilidadDiagnostico) + "%", 50, yPos, paint);
            yPos += 20;
            canvas.drawText("• Fecha: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()), 50, yPos, paint);
            yPos += 30;

            // Descripción
            paint.setFakeBoldText(true);
            canvas.drawText("DESCRIPCIÓN", 50, yPos, paint);
            paint.setFakeBoldText(false);
            yPos += 20;

            String descripcion = descripcionDiagnostico != null ? descripcionDiagnostico : "Sin descripción disponible";
            String[] descLines = dividirTexto(descripcion, 70);
            for (String line : descLines) {
                if (yPos > 750) break;
                canvas.drawText(line, 50, yPos, paint);
                yPos += 15;
            }

            yPos += 20;

            // Cuidados
            paint.setFakeBoldText(true);
            canvas.drawText("CUIDADOS RECOMENDADOS", 50, yPos, paint);
            paint.setFakeBoldText(false);
            yPos += 20;

            String cuidados = tvEstado != null ? tvEstado.getText().toString() : "Información no disponible";
            String[] cuidadoLines = dividirTexto(cuidados, 70);
            for (String line : cuidadoLines) {
                if (yPos > 750) break;
                canvas.drawText(line, 50, yPos, paint);
                yPos += 15;
            }

            yPos += 20;

            // Plagas
            paint.setFakeBoldText(true);
            canvas.drawText("ANÁLISIS DE PLAGAS", 50, yPos, paint);
            paint.setFakeBoldText(false);
            yPos += 20;

            String plagas = tvPlagas != null ? tvPlagas.getText().toString() : "Información no disponible";
            String[] plagasLines = dividirTexto(plagas, 70);
            for (String line : plagasLines) {
                if (yPos > 750) break;
                canvas.drawText(line, 50, yPos, paint);
                yPos += 15;
            }

            yPos += 20;

            // Recomendaciones
            paint.setFakeBoldText(true);
            canvas.drawText("RECOMENDACIONES", 50, yPos, paint);
            paint.setFakeBoldText(false);
            yPos += 20;

            String recomendaciones = tvRecomendaciones != null ? tvRecomendaciones.getText().toString() : "Información no disponible";
            String[] recomendacionesLines = dividirTexto(recomendaciones, 70);
            for (String line : recomendacionesLines) {
                if (yPos > 750) break;
                canvas.drawText(line, 50, yPos, paint);
                yPos += 15;
            }

            document.finishPage(page);

            // Guardar el PDF
            FileOutputStream fos;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                fos = (FileOutputStream) requireContext().getContentResolver().openOutputStream(getUriFromMediaStore(fileName));
            } else {
                fos = new FileOutputStream(file);
            }

            document.writeTo(fos);
            document.close();
            fos.close();

            // Mostrar mensaje de éxito
            requireActivity().runOnUiThread(() -> {
                String ubicacion = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) ?
                        "Carpeta de Descargas pública" : file.getAbsolutePath();

                new AlertDialog.Builder(requireContext())
                        .setTitle("✅ PDF Generado")
                        .setMessage("El diagnóstico se guardó en:\n" + ubicacion + "\n\n¿Qué quieres hacer ahora?")
                        .setPositiveButton("📤 Compartir", (dialog, which) -> {
                            compartirPDF(file);
                        })
                        .setNeutralButton("📱 Abrir", (dialog, which) -> {
                            abrirPDF(file);
                        })
                        .setNegativeButton("✅ Continuar", (dialog, which) -> {
                            // Redirigir a Mis Consultas después de generar PDF
                            if (getActivity() instanceof MainActivity) {
                                MainActivity mainActivity = (MainActivity) getActivity();
                                mainActivity.loadFragment(new MisConsultasFragment(), R.id.nav_mis_consultas);
                            }
                        })
                        .show();
            });

        } catch (Exception e) {
            Log.e("PDF", "Error generando PDF: " + e.getMessage());
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "❌ Error generando PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                // Redirigir a Mis Consultas incluso si falla el PDF
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.loadFragment(new MisConsultasFragment(), R.id.nav_mis_consultas);
                }
            });
        }
    }

    // Método para Android 10+ - Guardar en Downloads público usando MediaStore
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private File guardarPDFConMediaStore(String fileName) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            }
        } catch (Exception e) {
            Log.e("PDF", "Error con MediaStore: " + e.getMessage());
        }
        return new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
    }

    // Método para obtener URI desde MediaStore
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private Uri getUriFromMediaStore(String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        return requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
    }

    private void compartirPDF(File file) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            Uri uri;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                uri = getUriFromMediaStore(file.getName());
            } else {
                uri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".provider", file);
            }

            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Diagnóstico: " + nombreComunDiagnostico);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(shareIntent, "Compartir PDF");
            startActivity(chooser);

        } catch (Exception e) {
            Log.e("PDF", "Error compartiendo: " + e.getMessage());
            Toast.makeText(requireContext(), "Error al compartir", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirPDF(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                uri = getUriFromMediaStore(file.getName());
            } else {
                uri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".provider", file);
            }

            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "No hay app para abrir PDFs", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No se pudo abrir el PDF", Toast.LENGTH_SHORT).show();
        }
    }

    // MÉTODOS AUXILIARES CORREGIDOS
    private int calcularDiasRiego(String nombrePlanta) {
        if (nombrePlanta == null) return 4;

        switch (nombrePlanta.toLowerCase()) {
            case "tomate":
            case "tomato":
                return 2;
            case "rosa":
            case "rose":
                return 3;
            case "orquídea":
            case "orchid":
                return 7;
            case "suculenta":
            case "succulent":
                return 10;
            case "cactus":
                return 14;
            default:
                return 4;
        }
    }

    private int calcularDiasFertilizante(String nombrePlanta) {
        if (nombrePlanta == null) return 30;

        switch (nombrePlanta.toLowerCase()) {
            case "tomate":
            case "tomato":
                return 15;
            case "rosa":
            case "rose":
                return 30;
            case "orquídea":
            case "orchid":
                return 45;
            case "suculenta":
            case "succulent":
                return 60;
            case "cactus":
                return 90;
            default:
                return 30;
        }
    }

    private String generarEstadoDetallado(String nombrePlanta) {
        if (nombrePlanta == null) return getEstadoDefault();

        switch (nombrePlanta.toLowerCase()) {
            case "tomate":
            case "tomato":
                return "🌡️ Condiciones ideales:\n" +
                        "• Temperatura: 18-25°C\n" +
                        "• Humedad: 60-70%\n" +
                        "• Luz: 6-8 horas sol directo\n" +
                        "• Suelo: Bien drenado, pH 6.0-6.8\n\n" +
                        "💧 Riego:\n" +
                        "• Frecuencia: Cada 2-3 días\n" +
                        "• Cantidad: 500ml por planta\n" +
                        "• Mejor momento: Mañana temprano";

            case "rosa":
            case "rose":
                return "🌡️ Condiciones ideales:\n" +
                        "• Temperatura: 15-25°C\n" +
                        "• Humedad: 40-60%\n" +
                        "• Luz: 6 horas sol directo\n" +
                        "• Suelo: Rico en materia orgánica, pH 6.5\n\n" +
                        "💧 Riego:\n" +
                        "• Frecuencia: 2-3 veces por semana\n" +
                        "• Cantidad: 1-2 litros por planta\n" +
                        "• Evitar mojar hojas y flores";

            case "orquídea":
            case "orchid":
                return "🌡️ Condiciones ideales:\n" +
                        "• Temperatura: 18-28°C\n" +
                        "• Humedad: 60-80%\n" +
                        "• Luz: Indirecta brillante\n" +
                        "• Sustrato: Corteza de pino, sphagnum\n\n" +
                        "💧 Riego:\n" +
                        "• Frecuencia: Cada 7-10 días\n" +
                        "• Método: Inmersión 15 minutos\n" +
                        "• Dejar escurrir completamente";

            default:
                return getEstadoDefault();
        }
    }

    private String getEstadoDefault() {
        return "🌡️ Condiciones generales:\n" +
                "• Temperatura: 15-25°C\n" +
                "• Humedad: 40-70%\n" +
                "• Luz: Indirecta a sol parcial\n" +
                "• Suelo: Bien drenado\n\n" +
                "💧 Riego general:\n" +
                "• Frecuencia: Cuando suelo seque\n" +
                "• Método: Riego profundo\n" +
                "• Evitar encharcamientos";
    }

    private String generarAnalisisPlagasDetallado(String nombrePlanta) {
        if (nombrePlanta == null) return getPlagasDefault();

        switch (nombrePlanta.toLowerCase()) {
            case "tomate":
            case "tomato":
                return "🦠 Plagas comunes:\n" +
                        "• Mosca blanca: Pequeñas moscas blancas bajo hojas\n" +
                        "• Pulgones: Insectos verdes/negros en brotes\n" +
                        "• Oídio: Manchas blancas polvorientas\n\n" +
                        "🔍 Prevención:\n" +
                        "• Rotación de cultivos cada año\n" +
                        "• Aireación adecuada\n" +
                        "• Control de malezas\n\n" +
                        "🌿 Tratamiento orgánico:\n" +
                        "• Jabón potásico (10ml por litro)\n" +
                        "• Aceite de neem (5ml por litro)\n" +
                        "• Infusión de ajo para prevención";

            case "rosa":
            case "rose":
                return "🦠 Plagas comunes:\n" +
                        "• Oídio: Cubierta blanca en hojas\n" +
                        "• Mancha negra: Puntos negros circulares\n" +
                        "• Pulgones: En brotes tiernos\n\n" +
                        "🔍 Prevención:\n" +
                        "• Podas sanitarias regulares\n" +
                        "• Riego al suelo, no a las hojas\n" +
                        "• Espaciamiento adecuado\n\n" +
                        "🌿 Tratamiento:\n" +
                        "• Azufre en polvo para hongos\n" +
                        "• Fungicida de cobre\n" +
                        "• Insecticida sistémico si es grave";

            default:
                return getPlagasDefault();
        }
    }

    private String getPlagasDefault() {
        return "🔍 Análisis preventivo:\n" +
                "• Revisar underside de hojas semanalmente\n" +
                "• Observar coloración y textura hojas\n" +
                "• Monitorear crecimiento anormal\n\n" +
                "🌿 Prevención general:\n" +
                "• Buen drenaje del suelo\n" +
                "• Fertilización balanceada\n" +
                "• Control natural con mariquitas";
    }

    private String generarRecomendacionesDetalladas(String nombrePlanta) {
        if (nombrePlanta == null) return getRecomendacionesDefault();

        switch (nombrePlanta.toLowerCase()) {
            case "tomate":
            case "tomato":
                return "🌱 Fertilización:\n" +
                        "• Tipo: NPK 5-10-10 (más fósforo)\n" +
                        "• Frecuencia: Cada 15 días\n" +
                        "• Época: Floración y fructificación\n\n" +
                        "✂️ Poda y mantenimiento:\n" +
                        "• Eliminar chupones laterales\n" +
                        "• Tutorar para soporte\n" +
                        "• Deshojar base para aireación\n\n" +
                        "💡 Consejos expertos:\n" +
                        "• Rotar cultivos anualmente\n" +
                        "• Asociar con albahaca para plagas\n" +
                        "• Cosechar en horas frescas";

            case "rosa":
            case "rose":
                return "🌱 Fertilización:\n" +
                        "• Tipo: Especial para rosas (NPK 6-12-6)\n" +
                        "• Frecuencia: Mensual en crecimiento\n" +
                        "• Aplicar después de poda\n\n" +
                        "✂️ Poda y mantenimiento:\n" +
                        "• Poda principal en invierno\n" +
                        "• Deadheading (eliminar flores muertas)\n" +
                        "• Forma de vaso para aireación\n\n" +
                        "💡 Consejos expertos:\n" +
                        "• Acolchado con corteza pino\n" +
                        "• Proteger de vientos fuertes\n" +
                        "• Podar por encima de yema exterior";

            default:
                return getRecomendacionesDefault();
        }
    }

    private String getRecomendacionesDefault() {
        return "🌱 Fertilización general:\n" +
                "• Tipo: Balanceado 10-10-10\n" +
                "• Frecuencia: Mensual en crecimiento\n" +
                "• Suspender en invierno\n\n" +
                "✂️ Mantenimiento básico:\n" +
                "• Limpieza de hojas secas\n" +
                "• Control de crecimiento\n" +
                "• Observación constante\n\n" +
                "💡 Consejos generales:\n" +
                "• Registrar cambios en diario\n" +
                "• Fotografiar evolución\n" +
                "• Consultar especialista si hay dudas";
    }

    // Método auxiliar para dividir texto en líneas
    private String[] dividirTexto(String texto, int maxLength) {
        if (texto == null) return new String[]{"Texto no disponible"};

        List<String> lines = new ArrayList<>();
        while (texto.length() > maxLength) {
            int breakPoint = texto.lastIndexOf(' ', maxLength);
            if (breakPoint == -1) breakPoint = maxLength;
            lines.add(texto.substring(0, breakPoint));
            texto = texto.substring(breakPoint).trim();
        }
        lines.add(texto);
        return lines.toArray(new String[0]);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // MÉTODOS DE CÁMARA Y GALERÍA
    private void tomarFoto() {
        Log.d("Camara", "Solicitando permiso de cámara...");

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("Camara", "Permiso de cámara no concedido, solicitando...");
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            Log.d("Camara", "Permiso de cámara ya concedido, abriendo cámara...");
            abrirCamara();
        }
    }

    private void abrirGaleria() {
        Log.d("Galeria", "Solicitando permiso de galería...");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - Solo necesita READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("Galeria", "Permiso READ_MEDIA_IMAGES no concedido, solicitando...");
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_GALLERY);
            } else {
                Log.d("Galeria", "Permiso READ_MEDIA_IMAGES ya concedido, abriendo galería...");
                abrirGaleriaConPermiso();
            }
        } else {
            // Android 12 y anteriores - READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("Galeria", "Permiso READ_EXTERNAL_STORAGE no concedido, solicitando...");
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_GALLERY);
            } else {
                Log.d("Galeria", "Permiso READ_EXTERNAL_STORAGE ya concedido, abriendo galería...");
                abrirGaleriaConPermiso();
            }
        }
    }

    private void abrirCamara() {
        Log.d("Camara", "Intentando abrir cámara...");

        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // Verificar si hay alguna app de cámara disponible
            if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                Log.d("Camara", "App de cámara encontrada, iniciando...");
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                Log.e("Camara", "No se encontró ninguna aplicación de cámara");
                mostrarMensaje("No se encontró una aplicación de cámara");
            }
        } catch (Exception e) {
            Log.e("Camara", "Error al abrir cámara: " + e.getMessage());
            mostrarMensaje("Error al abrir la cámara: " + e.getMessage());
        }
    }

    private void abrirGaleriaConPermiso() {
        Log.d("Galeria", "Intentando abrir galería...");

        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");

            // Crear un chooser para asegurar que funcione en todos los dispositivos
            Intent chooser = Intent.createChooser(intent, "Seleccionar imagen de planta");

            // Verificar si hay alguna app que pueda manejar esta acción
            if (chooser.resolveActivity(requireActivity().getPackageManager()) != null) {
                Log.d("Galeria", "Selector de imágenes encontrado, iniciando...");
                startActivityForResult(chooser, PICK_IMAGE);
            } else {
                Log.e("Galeria", "No se encontró ninguna aplicación para seleccionar imágenes");

                // Fallback: intentar abrir cualquier selector de archivos
                try {
                    Intent fallbackIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    fallbackIntent.setType("image/*");
                    Intent fallbackChooser = Intent.createChooser(fallbackIntent, "Seleccionar imagen");

                    if (fallbackChooser.resolveActivity(requireActivity().getPackageManager()) != null) {
                        Log.d("Galeria", "Usando fallback para seleccionar archivos");
                        startActivityForResult(fallbackChooser, PICK_IMAGE);
                    } else {
                        mostrarMensaje("No se pudo encontrar ninguna aplicación para seleccionar imágenes");
                    }
                } catch (Exception fallbackEx) {
                    Log.e("Galeria", "Error en fallback: " + fallbackEx.getMessage());
                    mostrarMensaje("Error al abrir el selector de archivos");
                }
            }
        } catch (Exception e) {
            Log.e("Galeria", "Error al abrir galería: " + e.getMessage());
            mostrarMensaje("Error al abrir la galería");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d("Permisos", "Resultado de permisos - Código: " + requestCode);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Permisos", "Permiso concedido");
            switch (requestCode) {
                case PERMISSION_REQUEST_CAMERA:
                    Log.d("Permisos", "Permiso de cámara concedido, abriendo cámara...");
                    abrirCamara();
                    break;
                case PERMISSION_REQUEST_GALLERY:
                    Log.d("Permisos", "Permiso de galería concedido, abriendo galería...");
                    abrirGaleriaConPermiso();
                    break;
            }
        } else {
            Log.d("Permisos", "Permiso denegado");
            mostrarMensaje("Permiso denegado. La función no puede usarse.");

            // Explicar por qué se necesita el permiso
            if (requestCode == PERMISSION_REQUEST_CAMERA) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Permiso de Cámara Requerido")
                        .setMessage("Necesitamos acceso a la cámara para tomar fotos de las plantas y realizar diagnósticos precisos.")
                        .setPositiveButton("Entendido", null)
                        .show();
            } else if (requestCode == PERMISSION_REQUEST_GALLERY) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Permiso de Galería Requerido")
                        .setMessage("Necesitamos acceso a la galería para seleccionar imágenes existentes de plantas para el análisis.")
                        .setPositiveButton("Entendido", null)
                        .show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("ActivityResult", "Código: " + requestCode + ", Resultado: " + resultCode);

        if (resultCode == getActivity().RESULT_OK) {
            Log.d("ActivityResult", "Resultado OK");

            if (requestCode == PICK_IMAGE && data != null) {
                Log.d("ActivityResult", "Procesando imagen de galería...");
                imagenUri = data.getData();

                if (imagenUri != null) {
                    Log.d("ActivityResult", "URI obtenida: " + imagenUri.toString());
                    try {
                        imagenBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imagenUri);
                        imgPlanta.setImageBitmap(imagenBitmap);
                        imgPlanta.setVisibility(View.VISIBLE);
                        resultadoLayout.setVisibility(View.GONE);
                        mostrarMensaje("✅ Imagen cargada de galería");
                        Log.d("ActivityResult", "Imagen cargada correctamente");
                    } catch (IOException e) {
                        Log.e("ActivityResult", "Error cargando imagen: " + e.getMessage());
                        e.printStackTrace();
                        mostrarMensaje("❌ Error al cargar la imagen");
                    }
                } else {
                    Log.e("ActivityResult", "URI de imagen es null");
                    mostrarMensaje("❌ No se pudo obtener la imagen");
                }

            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Log.d("ActivityResult", "Procesando foto de cámara...");
                Bundle extras = data.getExtras();

                if (extras != null && extras.get("data") != null) {
                    imagenBitmap = (Bitmap) extras.get("data");
                    imgPlanta.setImageBitmap(imagenBitmap);
                    imgPlanta.setVisibility(View.VISIBLE);
                    resultadoLayout.setVisibility(View.GONE);
                    mostrarMensaje("✅ Foto tomada exitosamente");
                    Log.d("ActivityResult", "Foto procesada correctamente");
                } else {
                    Log.e("ActivityResult", "Datos de foto son null");
                    mostrarMensaje("❌ No se pudo obtener la foto");
                }
            }
        } else if (resultCode == getActivity().RESULT_CANCELED) {
            Log.d("ActivityResult", "Usuario canceló la selección");
        } else {
            Log.d("ActivityResult", "Resultado desconocido: " + resultCode);
        }
    }

    private void mostrarMensaje(String mensaje) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
    }

    // Método simplificado para programar notificaciones
    private void programarNotificaciones(String plantaId, int diasRiego) {
        String mensaje = "🔔 Recordatorios programados cada " + diasRiego + " días";
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
        Log.d("Notificaciones", "Notificaciones programadas para: " + nombreComunDiagnostico);
    }
}