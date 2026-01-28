package com.example.laterealmenu;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.firebase.firestore.FieldValue;
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

    private static final int MAX_DIAGNOSTICOS_GRATIS = 3;

    // ✅ CAMBIO REALIZADO: Clave actualizada a "AGRO-VIP"
    private static final String CLAVE_ESPECIAL_VIP = "AGRO-VIP";

    private static final String[] PLANT_ID_API_KEYS = {
            "G6Rx6DXluRXyqYdHJQ1ikopRobsttDzubTebrA2NCCJ2rw9RpC",
            "HUtpjB4Zl0ajb10opUfb9NEsKZF0FWpz1avBktJzwvojoGJQ54"
    };
    private static final String PLANT_ID_API_URL = "https://api.plant.id/v2/identify";

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

    private String nombreComunDiagnostico;
    private String nombreCientificoDiagnostico;
    private String descripcionDiagnostico;
    private float probabilidadDiagnostico;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diagnostico_planta, container, false);

        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();

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
        } catch (Exception e) { return false; }
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
        btnAnalizar.setOnClickListener(v -> verificarLimitesYAnalizar());
    }

    private void verificarLimitesYAnalizar() {
        if (imagenBitmap == null) {
            mostrarMensaje("⚠️ Primero selecciona una imagen");
            return;
        }

        if (!isNetworkAvailable()) {
            mostrarMensaje("❌ Sin conexión a internet");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            mostrarMensaje("❌ Usuario no autenticado");
            return;
        }

        btnAnalizar.setEnabled(false);
        btnAnalizar.setText("Verificando cuenta...");

        db.collection("usuarios").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nivel = documentSnapshot.getString("nivel");
                        boolean esPremium = "premium".equalsIgnoreCase(nivel);

                        if (esPremium) {
                            Log.d("Limites", "Usuario Premium detectado. Acceso concedido.");
                            analizarPlantaConAPI();
                        } else {
                            Long count = documentSnapshot.getLong("diagnosticosCount");
                            int actuales = (count != null) ? count.intValue() : 0;

                            if (actuales < MAX_DIAGNOSTICOS_GRATIS) {
                                analizarPlantaConAPI();
                            } else {
                                btnAnalizar.setEnabled(true);
                                btnAnalizar.setText("Analizar Planta");
                                mostrarAlertaLimiteAlcanzado();
                            }
                        }
                    } else {
                        analizarPlantaConAPI();
                    }
                })
                .addOnFailureListener(e -> {
                    btnAnalizar.setEnabled(true);
                    btnAnalizar.setText("Analizar Planta");
                    mostrarMensaje("Error verificando cuenta: " + e.getMessage());
                });
    }

    private void mostrarAlertaLimiteAlcanzado() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("🔒 Límite Alcanzado");
        builder.setMessage("Has utilizado tus " + MAX_DIAGNOSTICOS_GRATIS + " diagnósticos gratuitos.\n\n" +
                "¿Tienes una clave especial para desbloquear acceso ilimitado?");

        builder.setIcon(android.R.drawable.ic_lock_lock);
        builder.setPositiveButton("Entendido", null);

        builder.setNeutralButton("🔑 Tengo un Código", (dialog, which) -> {
            mostrarDialogoIngresarCodigo();
        });

        builder.show();
    }

    private void mostrarDialogoIngresarCodigo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Ingresa tu Clave Especial");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        // ✅ CAMBIO REALIZADO: Hint actualizado
        input.setHint("Ej: AGRO-VIP");
        builder.setView(input);

        builder.setPositiveButton("Canjear", (dialog, which) -> {
            String codigoIngresado = input.getText().toString().trim();
            validarCodigoEspecial(codigoIngresado);
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void validarCodigoEspecial(String codigo) {
        if (codigo.equals(CLAVE_ESPECIAL_VIP)) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                ProgressDialog pd = new ProgressDialog(requireContext());
                pd.setMessage("Activando modo Premium...");
                pd.show();

                db.collection("usuarios").document(user.getUid())
                        .update("nivel", "premium")
                        .addOnSuccessListener(aVoid -> {
                            pd.dismiss();
                            mostrarMensaje("🌟 ¡Felicidades! Ahora tienes escaneos ILIMITADOS.");
                            verificarLimitesYAnalizar();
                        })
                        .addOnFailureListener(e -> {
                            pd.dismiss();
                            mostrarMensaje("❌ Error al activar premium: " + e.getMessage());
                        });
            }
        } else {
            mostrarMensaje("❌ Código incorrecto. Inténtalo de nuevo.");
        }
    }

    private void incrementarContadorUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("usuarios").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        String nivel = doc.getString("nivel");
                        if (!"premium".equalsIgnoreCase(nivel)) {
                            db.collection("usuarios").document(user.getUid())
                                    .update("diagnosticosCount", FieldValue.increment(1));
                        }
                    });
        }
    }

    // --- SECCION DE API Y ANALISIS ---
    private String getCurrentApiKey() { return PLANT_ID_API_KEYS[currentApiKeyIndex]; }

    private void rotateToNextApiKey() {
        currentApiKeyIndex = (currentApiKeyIndex + 1) % PLANT_ID_API_KEYS.length;
        retryCount = 0;
        Log.d("APIDebug", "🔄 Rotando API Key a índice: " + currentApiKeyIndex);
        mostrarMensaje("🔄 Reintentando con servidor alternativo...");
    }

    private boolean shouldRotateApiKey() { return retryCount >= MAX_RETRIES; }

    private void handleApiKeyRotation() {
        if (shouldRotateApiKey()) {
            rotateToNextApiKey();
            analizarPlantaConAPI();
        }
    }

    private void analizarPlantaConAPI() {
        btnAnalizar.setText("Analizando...");
        progressBar.setVisibility(View.VISIBLE);
        resultadoLayout.setVisibility(View.GONE);

        String currentApiKey = getCurrentApiKey();
        String imageBase64 = bitmapToBase64(imagenBitmap);

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

            RequestBody body = RequestBody.create(MediaType.parse("application/json"), requestBody.toString());

            Request request = new Request.Builder()
                    .url(PLANT_ID_API_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    requireActivity().runOnUiThread(() -> {
                        retryCount++;
                        if (shouldRotateApiKey() && currentApiKeyIndex < PLANT_ID_API_KEYS.length - 1) {
                            handleApiKeyRotation();
                        } else {
                            resetUIOnError("Error de conexión: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body().string();
                    requireActivity().runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            retryCount = 0;
                            procesarRespuestaAPI(responseBody);
                        } else {
                            retryCount++;
                            if (response.code() == 401 || response.code() == 403) {
                                if (currentApiKeyIndex < PLANT_ID_API_KEYS.length - 1) {
                                    handleApiKeyRotation();
                                    return;
                                }
                            }
                            resetUIOnError("Error del servidor: " + response.code());
                        }
                    });
                }
            });
        } catch (Exception e) {
            resetUIOnError("Error creando solicitud: " + e.getMessage());
        }
    }

    private void resetUIOnError(String msg) {
        tvResultado.setText("❌ " + msg);
        progressBar.setVisibility(View.GONE);
        btnAnalizar.setEnabled(true);
        btnAnalizar.setText("Analizar Planta");
        mostrarMensaje(msg);
    }

    private void procesarRespuestaAPI(String responseBody) {
        try {
            JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
            if (jsonResponse.has("suggestions")) {
                JsonArray suggestions = jsonResponse.getAsJsonArray("suggestions");
                if (suggestions.size() > 0) {
                    JsonObject firstSuggestion = suggestions.get(0).getAsJsonObject();
                    JsonObject plantDetails = firstSuggestion.getAsJsonObject("plant_details");

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

                    incrementarContadorUsuario();

                    generarDiagnosticoCompletoAPI(nombreComun, nombreCientifico, descripcion, probabilidad);

                } else {
                    resetUIOnError("No se pudo identificar la planta");
                }
            } else {
                resetUIOnError("Formato de respuesta inválido");
            }
        } catch (Exception e) {
            resetUIOnError("Error procesando respuesta");
        }
    }

    private void generarDiagnosticoCompletoAPI(String nombreComun, String nombreCientifico, String descripcion, float probabilidad) {
        String estado = generarEstadoDetallado(nombreComun);
        String plagas = generarAnalisisPlagasDetallado(nombreComun);
        String recomendaciones = generarRecomendacionesDetalladas(nombreComun);

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
        btnAnalizar.setText("Analizar Planta");

        nombreComunDiagnostico = nombreComun;
        nombreCientificoDiagnostico = nombreCientifico;
        descripcionDiagnostico = descripcion;
        probabilidadDiagnostico = probabilidad;

        mostrarDialogoGuardarConsulta();
    }

    private void mostrarDialogoGuardarConsulta() {
        if (nombreComunDiagnostico == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("🌱 ¿Guardar Diagnóstico?")
                .setMessage("¿Qué deseas hacer con el diagnóstico de \"" + nombreComunDiagnostico + "\"?")
                .setPositiveButton("💾 Guardar", (dialog, which) -> guardarConsultaEnFirebase(false))
                .setNegativeButton("❌ Cancelar", null)
                .setNeutralButton("📄 Guardar + PDF", (dialog, which) -> guardarConsultaEnFirebase(true))
                .show();
    }

    private void guardarConsultaEnFirebase(boolean generarPDF) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Guardando...");
        progressDialog.show();
        String imagenBase64 = bitmapToBase64(imagenBitmap);
        Map<String, Object> consulta = new HashMap<>();
        consulta.put("nombrePlanta", nombreComunDiagnostico);
        consulta.put("nombreCientifico", nombreCientificoDiagnostico);
        consulta.put("descripcion", descripcionDiagnostico);
        consulta.put("diagnostico", tvEstado.getText().toString());
        consulta.put("plagas", tvPlagas.getText().toString());
        consulta.put("recomendaciones", tvRecomendaciones.getText().toString());
        consulta.put("imagenBase64", imagenBase64);
        consulta.put("usuarioId", user.getUid());
        consulta.put("fechaDiagnostico", com.google.firebase.Timestamp.now());
        consulta.put("tipo", "diagnostico_ia");
        db.collection("consultas").add(consulta)
                .addOnSuccessListener(documentReference -> {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(), "✅ Guardado exitosamente", Toast.LENGTH_SHORT).show();
                    if (generarPDF) generarPDFDiagnostico(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show();
                });
    }

    private void generarPDFDiagnostico(String consultaId) {
        try {
            String fileName = "Diagnostico_" +
                    (nombreComunDiagnostico != null ? nombreComunDiagnostico.replace(" ", "_") : "Planta") + "_" +
                    System.currentTimeMillis() + ".pdf";

            File file;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                file = guardarPDFConMediaStore(fileName);
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) downloadsDir.mkdirs();
                file = new File(downloadsDir, fileName);
            }

            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(12);

            int yPos = 50;
            paint.setTextSize(20);
            paint.setFakeBoldText(true);
            paint.setColor(Color.parseColor("#2E7D32"));
            canvas.drawText("🌱 Diagnóstico de Planta - AgroExpert", 50, yPos, paint);
            paint.setColor(Color.BLACK);
            yPos += 40;

            paint.setTextSize(14);
            paint.setFakeBoldText(true);
            canvas.drawText("INFORMACIÓN BÁSICA", 50, yPos, paint);
            paint.setTextSize(12);
            paint.setFakeBoldText(false);
            yPos += 25;
            canvas.drawText("• Planta: " + nombreComunDiagnostico, 50, yPos, paint);
            yPos += 20;
            canvas.drawText("• Probabilidad: " + String.format("%.1f", probabilidadDiagnostico) + "%", 50, yPos, paint);

            // ... (Resto del contenido PDF simplificado para brevedad, pero mantenlo en tu archivo)
            document.finishPage(page);

            FileOutputStream fos;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                fos = (FileOutputStream) requireContext().getContentResolver().openOutputStream(getUriFromMediaStore(fileName));
            } else {
                fos = new FileOutputStream(file);
            }

            document.writeTo(fos);
            document.close();
            fos.close();

            requireActivity().runOnUiThread(() -> {
                String ubicacion = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) ?
                        "Carpeta de Descargas" : file.getAbsolutePath();
                new AlertDialog.Builder(requireContext())
                        .setTitle("✅ PDF Generado")
                        .setMessage("Guardado en: " + ubicacion)
                        .setPositiveButton("OK", null)
                        .show();
            });

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Métodos auxiliares
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private File guardarPDFConMediaStore(String fileName) {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private Uri getUriFromMediaStore(String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        return requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
    }

    private String generarEstadoDetallado(String nombre) { return "Información detallada sobre el estado..."; }
    private String generarAnalisisPlagasDetallado(String nombre) { return "Análisis de plagas..."; }
    private String generarRecomendacionesDetalladas(String nombre) { return "Recomendaciones de cuidado..."; }

    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
    }

    private void tomarFoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            abrirCamara();
        }
    }

    private void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void abrirGaleria() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_GALLERY);
            } else {
                abrirGaleriaConPermiso();
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_GALLERY);
            } else {
                abrirGaleriaConPermiso();
            }
        }
    }

    private void abrirGaleriaConPermiso() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == PERMISSION_REQUEST_CAMERA) abrirCamara();
            if (requestCode == PERMISSION_REQUEST_GALLERY) abrirGaleriaConPermiso();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == getActivity().RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE) {
                imagenUri = data.getData();
                try {
                    imagenBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imagenUri);
                    imgPlanta.setImageBitmap(imagenBitmap);
                    imgPlanta.setVisibility(View.VISIBLE);
                    resultadoLayout.setVisibility(View.GONE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                imagenBitmap = (Bitmap) data.getExtras().get("data");
                imgPlanta.setImageBitmap(imagenBitmap);
                imgPlanta.setVisibility(View.VISIBLE);
                resultadoLayout.setVisibility(View.GONE);
            }
        }
    }

    private void mostrarMensaje(String mensaje) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
    }
}