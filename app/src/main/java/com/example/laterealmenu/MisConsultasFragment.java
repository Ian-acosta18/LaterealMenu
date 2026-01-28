package com.example.laterealmenu;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MisConsultasFragment extends Fragment {

    private static final String TAG = "MisConsultasFragment";
    private static final int PERMISSION_REQUEST_WRITE_STORAGE = 1001;

    private RecyclerView recyclerViewConsultas;
    private ConsultasAdapter consultasAdapter;
    private List<Consulta> listaConsultas;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private LinearLayout emptyState;
    private TextView tvEmptyMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mis_consultas, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Log.d(TAG, "=== INICIANDO MIS CONSULTAS ===");

        initViews(view);
        cargarConsultas();

        return view;
    }

    private void initViews(View view) {
        recyclerViewConsultas = view.findViewById(R.id.recyclerConsultas);
        emptyState = view.findViewById(R.id.emptyStateConsultas);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);

        recyclerViewConsultas.setLayoutManager(new LinearLayoutManager(getContext()));
        listaConsultas = new ArrayList<>();
        consultasAdapter = new ConsultasAdapter(listaConsultas);
        recyclerViewConsultas.setAdapter(consultasAdapter);

        Log.d(TAG, "Vistas inicializadas correctamente");
    }

    private void cargarConsultas() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ Usuario no autenticado");
            mostrarEmptyState("❌ Usuario no autenticado");
            return;
        }

        Log.d(TAG, "🔍 Cargando consultas para usuario: " + user.getUid());

        db.collection("consultas")
                .whereEqualTo("usuarioId", user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    Log.d(TAG, "📡 Respuesta de Firebase recibida");

                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Consulta exitosa, documentos encontrados: " + task.getResult().size());

                        listaConsultas.clear();
                        int contador = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            contador++;
                            Log.d(TAG, "📄 Documento " + contador + ": " + document.getId());

                            try {
                                Consulta consulta = new Consulta();
                                consulta.setId(document.getId());

                                // Mapear campos con verificaciones de null
                                if (document.contains("nombrePlanta")) {
                                    consulta.setNombrePlanta(document.getString("nombrePlanta"));
                                } else {
                                    consulta.setNombrePlanta("Planta no identificada");
                                }

                                if (document.contains("nombreCientifico")) {
                                    consulta.setNombreCientifico(document.getString("nombreCientifico"));
                                }

                                if (document.contains("descripcion")) {
                                    consulta.setDescripcion(document.getString("descripcion"));
                                }

                                if (document.contains("diagnostico")) {
                                    consulta.setDiagnostico(document.getString("diagnostico"));
                                }

                                if (document.contains("plagas")) {
                                    consulta.setPlagas(document.getString("plagas"));
                                }

                                if (document.contains("recomendaciones")) {
                                    consulta.setRecomendaciones(document.getString("recomendaciones"));
                                }

                                // ✅ CORRECCIÓN CRÍTICA: CARGAR imagenBase64 DESDE FIRESTORE
                                if (document.contains("imagenBase64")) {
                                    String imagenBase64 = document.getString("imagenBase64");
                                    consulta.setImagenBase64(imagenBase64);
                                    Log.d(TAG, "✅ IMAGEN ENCONTRADA en Firestore para: " + consulta.getNombrePlanta());
                                    Log.d(TAG, "📏 Longitud imagenBase64: " + (imagenBase64 != null ? imagenBase64.length() : "NULL"));
                                } else {
                                    Log.e(TAG, "❌ NO EXISTE campo 'imagenBase64' en Firestore para: " + consulta.getNombrePlanta());
                                    // Listar todos los campos disponibles para debug
                                    Log.d(TAG, "📋 Campos disponibles: " + document.getData().keySet());
                                }

                                if (document.contains("probabilidadIdentificacion")) {
                                    Object probObj = document.get("probabilidadIdentificacion");
                                    if (probObj instanceof Double) {
                                        consulta.setProbabilidadIdentificacion(((Double) probObj).floatValue());
                                    } else if (probObj instanceof Long) {
                                        consulta.setProbabilidadIdentificacion(((Long) probObj).floatValue());
                                    } else {
                                        consulta.setProbabilidadIdentificacion(0.0f);
                                    }
                                }

                                if (document.contains("fechaDiagnostico")) {
                                    consulta.setFechaDiagnostico(document.getDate("fechaDiagnostico"));
                                }

                                if (document.contains("usuarioId")) {
                                    consulta.setUsuarioId(document.getString("usuarioId"));
                                }

                                // Debug completo de la consulta
                                debugConsultaCompleta(consulta);

                                listaConsultas.add(consulta);
                                Log.d(TAG, "✅ Consulta agregada: " + consulta.getNombrePlanta());

                            } catch (Exception e) {
                                Log.e(TAG, "❌ Error mapeando documento " + document.getId() + ": " + e.getMessage());
                            }
                        }

                        consultasAdapter.notifyDataSetChanged();

                        if (listaConsultas.isEmpty()) {
                            Log.d(TAG, "📭 No hay consultas para mostrar");
                            mostrarEmptyState("No tienes consultas guardadas\n\nRealiza un diagnóstico con IA para ver tu historial aquí");
                        } else {
                            Log.d(TAG, "📋 Mostrando " + listaConsultas.size() + " consultas");
                            mostrarConsultas();
                        }
                    } else {
                        Exception error = task.getException();
                        Log.e(TAG, "❌ Error cargando consultas: ", error);
                        mostrarEmptyState("❌ Error al cargar consultas: " + (error != null ? error.getMessage() : "Desconocido"));
                    }
                });
    }

    // ✅ NUEVO MÉTODO: Debug completo de consulta
    private void debugConsultaCompleta(Consulta consulta) {
        Log.d(TAG, "=== DEBUG CONSULTA COMPLETA ===");
        Log.d(TAG, "ID: " + consulta.getId());
        Log.d(TAG, "Nombre: " + consulta.getNombrePlanta());
        Log.d(TAG, "Tiene imagenBase64: " + (consulta.getImagenBase64() != null));

        if (consulta.getImagenBase64() != null) {
            Log.d(TAG, "Longitud imagenBase64: " + consulta.getImagenBase64().length());
            Log.d(TAG, "Primeros 50 chars: " + consulta.getImagenBase64().substring(0, Math.min(50, consulta.getImagenBase64().length())));
        } else {
            Log.d(TAG, "❌ imagenBase64 es NULL");
        }
        Log.d(TAG, "=================================");
    }

    private void mostrarEmptyState(String mensaje) {
        Log.d(TAG, "🔄 Mostrando empty state: " + mensaje);
        requireActivity().runOnUiThread(() -> {
            emptyState.setVisibility(View.VISIBLE);
            recyclerViewConsultas.setVisibility(View.GONE);
            if (tvEmptyMessage != null) {
                tvEmptyMessage.setText(mensaje);
            }
        });
    }

    private void mostrarConsultas() {
        Log.d(TAG, "🔄 Mostrando lista de consultas");
        requireActivity().runOnUiThread(() -> {
            emptyState.setVisibility(View.GONE);
            recyclerViewConsultas.setVisibility(View.VISIBLE);
        });
    }

    // Clase Consulta
    public static class Consulta {
        private String id;
        private String nombrePlanta;
        private String nombreCientifico;
        private String descripcion;
        private String diagnostico;
        private String plagas;
        private String recomendaciones;
        private String imagenBase64;
        private String usuarioId;
        private Date fechaDiagnostico;
        private float probabilidadIdentificacion;
        private boolean notificacionesActivadas;
        private String tipo;

        // Getters y setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getNombrePlanta() { return nombrePlanta; }
        public void setNombrePlanta(String nombrePlanta) { this.nombrePlanta = nombrePlanta; }
        public String getNombreCientifico() { return nombreCientifico; }
        public void setNombreCientifico(String nombreCientifico) { this.nombreCientifico = nombreCientifico; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public String getDiagnostico() { return diagnostico; }
        public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }
        public String getPlagas() { return plagas; }
        public void setPlagas(String plagas) { this.plagas = plagas; }
        public String getRecomendaciones() { return recomendaciones; }
        public void setRecomendaciones(String recomendaciones) { this.recomendaciones = recomendaciones; }
        public String getImagenBase64() { return imagenBase64; }
        public void setImagenBase64(String imagenBase64) { this.imagenBase64 = imagenBase64; }
        public String getUsuarioId() { return usuarioId; }
        public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
        public Date getFechaDiagnostico() { return fechaDiagnostico; }
        public void setFechaDiagnostico(Date fechaDiagnostico) { this.fechaDiagnostico = fechaDiagnostico; }
        public float getProbabilidadIdentificacion() { return probabilidadIdentificacion; }
        public void setProbabilidadIdentificacion(float probabilidadIdentificacion) { this.probabilidadIdentificacion = probabilidadIdentificacion; }
        public boolean isNotificacionesActivadas() { return notificacionesActivadas; }
        public void setNotificacionesActivadas(boolean notificacionesActivadas) { this.notificacionesActivadas = notificacionesActivadas; }
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
    }

    // Adapter para el RecyclerView
    private class ConsultasAdapter extends RecyclerView.Adapter<ConsultasAdapter.ConsultaViewHolder> {

        private List<Consulta> consultas;

        public ConsultasAdapter(List<Consulta> consultas) {
            this.consultas = consultas;
        }

        @NonNull
        @Override
        public ConsultaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_consulta, parent, false);
            return new ConsultaViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ConsultaViewHolder holder, int position) {
            Consulta consulta = consultas.get(position);
            holder.bind(consulta);
        }

        @Override
        public int getItemCount() {
            return consultas.size();
        }

        class ConsultaViewHolder extends RecyclerView.ViewHolder {
            private TextView tvNombrePlanta, tvFecha, tvProbabilidad, tvResumen;
            private Button btnVerDetalles, btnGenerarPDF, btnEliminar;
            private ImageView imgConsulta;
            private Consulta currentConsulta;

            public ConsultaViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNombrePlanta = itemView.findViewById(R.id.tvNombrePlanta);
                tvFecha = itemView.findViewById(R.id.tvFecha);
                tvProbabilidad = itemView.findViewById(R.id.tvProbabilidad);
                tvResumen = itemView.findViewById(R.id.tvResumen);
                btnVerDetalles = itemView.findViewById(R.id.btnVerDetalles);
                btnGenerarPDF = itemView.findViewById(R.id.btnGenerarPDF);
                btnEliminar = itemView.findViewById(R.id.btnEliminar);
                imgConsulta = itemView.findViewById(R.id.imgConsulta);
            }

            public void bind(Consulta consulta) {
                Log.d(TAG, "📱 Bind consulta: " + consulta.getNombrePlanta());

                this.currentConsulta = consulta;

                // DEBUG: Verificar si el ImageView existe
                if (imgConsulta == null) {
                    Log.e(TAG, "❌ CRÍTICO: imgConsulta es NULL - Revisa el layout item_consulta.xml");
                } else {
                    Log.d(TAG, "✅ imgConsulta encontrado correctamente");
                }

                tvNombrePlanta.setText(consulta.getNombrePlanta() != null ? consulta.getNombrePlanta() : "Sin nombre");

                // Formatear fecha
                if (consulta.getFechaDiagnostico() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    tvFecha.setText(sdf.format(consulta.getFechaDiagnostico()));
                } else {
                    tvFecha.setText("Fecha no disponible");
                }

                // Probabilidad
                tvProbabilidad.setText(String.format(Locale.getDefault(), "%.1f%%", consulta.getProbabilidadIdentificacion()));

                // Resumen
                String resumen = consulta.getDescripcion() != null ?
                        (consulta.getDescripcion().length() > 100 ?
                                consulta.getDescripcion().substring(0, 100) + "..." :
                                consulta.getDescripcion()) :
                        "Sin descripción disponible";
                tvResumen.setText(resumen);

                // DEBUG: Verificar datos de imagen
                boolean tieneImagen = consulta.getImagenBase64() != null && !consulta.getImagenBase64().isEmpty();
                Log.d(TAG, "📸 Consulta '" + consulta.getNombrePlanta() + "' - Tiene imagen: " + tieneImagen);

                if (tieneImagen) {
                    Log.d(TAG, "📏 Longitud de imagenBase64: " + consulta.getImagenBase64().length());
                }

                // Cargar imagen en la lista si existe
                if (tieneImagen && imgConsulta != null) {
                    try {
                        Log.d(TAG, "🔄 Intentando decodificar imagen...");
                        byte[] decodedString = Base64.decode(consulta.getImagenBase64(), Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                        if (decodedByte != null) {
                            imgConsulta.setImageBitmap(decodedByte);
                            imgConsulta.setVisibility(View.VISIBLE);
                            Log.d(TAG, "✅ Imagen cargada EXITOSAMENTE para: " + consulta.getNombrePlanta());
                        } else {
                            Log.e(TAG, "❌ decodedByte es NULL - la imagen no se pudo decodificar");
                            imgConsulta.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error cargando imagen: " + e.getMessage());
                        imgConsulta.setVisibility(View.GONE);
                    }
                } else {
                    if (imgConsulta != null) {
                        imgConsulta.setVisibility(View.GONE);
                    }
                    if (!tieneImagen) {
                        Log.d(TAG, "ℹ️ No hay imagen para mostrar");
                    } else {
                        Log.e(TAG, "❌ Hay imagen pero imgConsulta es NULL");
                    }
                }

                // Configurar botones
                btnVerDetalles.setOnClickListener(v -> mostrarDetallesConsulta(consulta));
                btnGenerarPDF.setOnClickListener(v -> verificarPermisosYGenerarPDF());
                btnEliminar.setOnClickListener(v -> eliminarConsulta(consulta));
            }

            private void mostrarDetallesConsulta(Consulta consulta) {
                Log.d(TAG, "👁️ Mostrando detalles de: " + consulta.getNombrePlanta());

                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("📋 Diagnóstico - " + consulta.getNombrePlanta());

                ScrollView scrollView = new ScrollView(requireContext());
                LinearLayout layout = new LinearLayout(requireContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(40, 30, 40, 30);

                // ✅ MEJORADO: Mostrar imagen más grande
                if (consulta.getImagenBase64() != null && !consulta.getImagenBase64().isEmpty()) {
                    try {
                        ImageView imageView = new ImageView(requireContext());
                        byte[] decodedString = Base64.decode(consulta.getImagenBase64(), Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        imageView.setImageBitmap(decodedByte);

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                350  // ✅ Más grande
                        );
                        params.setMargins(0, 0, 0, 25);
                        imageView.setLayoutParams(params);
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        imageView.setBackgroundResource(R.drawable.bg_image_placeholder);

                        layout.addView(imageView);
                        Log.d(TAG, "✅ Imagen mostrada en diálogo para: " + consulta.getNombrePlanta());
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error mostrando imagen en detalles: " + e.getMessage());
                    }
                }

                // ✅ MEJORADO: Información organizada en secciones
                agregarSeccionDetalle(layout, "🌱 INFORMACIÓN BÁSICA",
                        "• Planta: " + consulta.getNombrePlanta() + "\n" +
                                "• Nombre científico: " + (consulta.getNombreCientifico() != null ? consulta.getNombreCientifico() : "No disponible") + "\n" +
                                "• Probabilidad: " + String.format("%.1f", consulta.getProbabilidadIdentificacion()) + "%\n" +
                                "• Fecha diagnóstico: " + (consulta.getFechaDiagnostico() != null ?
                                new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(consulta.getFechaDiagnostico()) :
                                "No disponible"));

                // ✅ MEJORADO: Descripción con texto más grande
                if (consulta.getDescripcion() != null && !consulta.getDescripcion().isEmpty()) {
                    agregarSeccionDetalle(layout, "📖 DESCRIPCIÓN", consulta.getDescripcion());
                }

                // ✅ MEJORADO: Diagnóstico con texto más grande
                if (consulta.getDiagnostico() != null && !consulta.getDiagnostico().isEmpty()) {
                    agregarSeccionDetalle(layout, "🔍 DIAGNÓSTICO", consulta.getDiagnostico());
                }

                // ✅ MEJORADO: Plagas con texto más grande
                if (consulta.getPlagas() != null && !consulta.getPlagas().isEmpty()) {
                    agregarSeccionDetalle(layout, "🐛 ANÁLISIS DE PLAGAS", consulta.getPlagas());
                }

                // ✅ MEJORADO: Recomendaciones con texto más grande
                if (consulta.getRecomendaciones() != null && !consulta.getRecomendaciones().isEmpty()) {
                    agregarSeccionDetalle(layout, "💡 RECOMENDACIONES", consulta.getRecomendaciones());
                }

                scrollView.addView(layout);
                builder.setView(scrollView);

                builder.setPositiveButton("✅ Cerrar", null);
                builder.setNeutralButton("📄 Generar PDF", (dialog, which) -> {
                    if (currentConsulta != null) {
                        generarPDFConsulta(currentConsulta);
                    }
                });

                builder.show();
            }

            // ✅ MEJORADO: Método auxiliar mejorado para secciones
            // ✅ MEJORADO: Método auxiliar con texto negro para mejor visibilidad
            private void agregarSeccionDetalle(LinearLayout layout, String titulo, String contenido) {
                // Título de sección - Blanco para máxima visibilidad
                TextView tvTitulo = new TextView(requireContext());
                tvTitulo.setText(titulo);
                tvTitulo.setTextSize(18);
                tvTitulo.setTypeface(null, Typeface.BOLD);
                tvTitulo.setTextColor(Color.parseColor("#FFFFFF")); // 🔥 BLANCO PURO
                tvTitulo.setPadding(0, 25, 0, 10);
                layout.addView(tvTitulo);

                // Contenido de sección - Gris claro muy visible
                TextView tvContenido = new TextView(requireContext());
                tvContenido.setText(contenido);
                tvContenido.setTextSize(16);
                tvContenido.setTextColor(Color.parseColor("#F5F5F5")); // 🔥 GRIS MUY CLARO
                tvContenido.setPadding(0, 0, 0, 20);
                tvContenido.setLineSpacing(0, 1.4f);
                layout.addView(tvContenido);

                // Separador - blanco semitransparente
                View separador = new View(requireContext());
                separador.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 2
                ));
                separador.setBackgroundColor(Color.parseColor("#80FFFFFF")); // 🔥 BLANCO 50%
                separador.setPadding(0, 15, 0, 15);
                layout.addView(separador);
            }

            private void agregarCampoDetalle(LinearLayout layout, String titulo, String contenido) {
                TextView tvTitulo = new TextView(requireContext());
                tvTitulo.setText(titulo);
                tvTitulo.setTextSize(16);
                tvTitulo.setTypeface(null, Typeface.BOLD);
                tvTitulo.setTextColor(Color.parseColor("#2E7D32"));
                tvTitulo.setPadding(0, 20, 0, 5);
                layout.addView(tvTitulo);

                TextView tvContenido = new TextView(requireContext());
                tvContenido.setText(contenido);
                tvContenido.setTextSize(14);
                tvContenido.setTextColor(Color.DKGRAY);
                tvContenido.setPadding(0, 0, 0, 10);
                tvContenido.setLineSpacing(0, 1.2f);
                layout.addView(tvContenido);

                View separador = new View(requireContext());
                separador.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 2
                ));
                separador.setBackgroundColor(Color.LTGRAY);
                separador.setPadding(0, 10, 0, 10);
                layout.addView(separador);
            }

            private void verificarPermisosYGenerarPDF() {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    Log.d(TAG, "📝 Solicitando permisos para generar PDF...");

                    requestPermissions(
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_WRITE_STORAGE
                    );

                    new Handler().postDelayed(() -> {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "✅ Permisos concedidos, generando PDF...");
                            if (currentConsulta != null) {
                                generarPDFConsulta(currentConsulta);
                            } else {
                                Toast.makeText(requireContext(), "❌ Error: No hay consulta seleccionada", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(requireContext(), "❌ Permisos denegados. No se puede generar PDF", Toast.LENGTH_LONG).show();
                        }
                    }, 1000);

                } else {
                    Log.d(TAG, "✅ Permisos ya concedidos, generando PDF...");
                    if (currentConsulta != null) {
                        generarPDFConsulta(currentConsulta);
                    } else {
                        Toast.makeText(requireContext(), "❌ Error: No hay consulta seleccionada", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            private void eliminarConsulta(Consulta consulta) {
                Log.d(TAG, "🗑️ Intentando eliminar: " + consulta.getId());

                new AlertDialog.Builder(requireContext())
                        .setTitle("Eliminar Consulta")
                        .setMessage("¿Estás seguro de que quieres eliminar el diagnóstico de \"" + consulta.getNombrePlanta() + "\"?\n\nEsta acción no se puede deshacer.")
                        .setPositiveButton("✅ Sí, eliminar", (dialog, which) -> {
                            ProgressDialog progressDialog = new ProgressDialog(requireContext());
                            progressDialog.setMessage("Eliminando consulta...");
                            progressDialog.setCancelable(false);
                            progressDialog.show();

                            db.collection("consultas").document(consulta.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        progressDialog.dismiss();
                                        Log.d(TAG, "✅ Consulta eliminada: " + consulta.getId());
                                        Toast.makeText(requireContext(), "✅ Consulta eliminada exitosamente", Toast.LENGTH_SHORT).show();
                                        cargarConsultas();
                                    })
                                    .addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        Log.e(TAG, "❌ Error eliminando consulta: " + e.getMessage());
                                        Toast.makeText(requireContext(), "❌ Error al eliminar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        })
                        .setNegativeButton("❌ Cancelar", null)
                        .show();
            }

            private void generarPDFConsulta(Consulta consulta) {
                Log.d(TAG, "📄 Generando PDF para: " + consulta.getNombrePlanta());

                ProgressDialog progressDialog = new ProgressDialog(requireContext());
                progressDialog.setMessage("Generando PDF...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                new Thread(() -> {
                    try {
                        String fileName = "Diagnostico_" +
                                consulta.getNombrePlanta().replaceAll("[^a-zA-Z0-9]", "_") + "_" +
                                System.currentTimeMillis() + ".pdf";

                        File file;
                        Uri fileUri;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            fileUri = crearArchivoMediaStore(fileName);
                            if (fileUri == null) {
                                throw new Exception("No se pudo crear el archivo en MediaStore");
                            }
                            file = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
                        } else {
                            fileUri = null;
                            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            if (!downloadsDir.exists()) {
                                downloadsDir.mkdirs();
                            }
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
                        canvas.drawText("• Planta: " + consulta.getNombrePlanta(), 50, yPos, paint);
                        yPos += 20;
                        canvas.drawText("• Nombre científico: " +
                                        (consulta.getNombreCientifico() != null ? consulta.getNombreCientifico() : "No disponible"),
                                50, yPos, paint);
                        yPos += 20;
                        canvas.drawText("• Probabilidad: " + String.format("%.1f", consulta.getProbabilidadIdentificacion()) + "%", 50, yPos, paint);
                        yPos += 20;
                        canvas.drawText("• Fecha: " + (consulta.getFechaDiagnostico() != null ?
                                new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(consulta.getFechaDiagnostico()) :
                                "No disponible"), 50, yPos, paint);
                        yPos += 30;

                        if (consulta.getDescripcion() != null && !consulta.getDescripcion().isEmpty()) {
                            paint.setFakeBoldText(true);
                            canvas.drawText("DESCRIPCIÓN", 50, yPos, paint);
                            paint.setFakeBoldText(false);
                            yPos += 20;

                            String descripcion = consulta.getDescripcion();
                            String[] descLines = dividirTexto(descripcion, 70);
                            for (String line : descLines) {
                                if (yPos > 750) break;
                                canvas.drawText(line, 50, yPos, paint);
                                yPos += 15;
                            }
                            yPos += 20;
                        }

                        if (consulta.getDiagnostico() != null && !consulta.getDiagnostico().isEmpty()) {
                            paint.setFakeBoldText(true);
                            canvas.drawText("DIAGNÓSTICO", 50, yPos, paint);
                            paint.setFakeBoldText(false);
                            yPos += 20;

                            String diagnostico = consulta.getDiagnostico();
                            String[] diagnosticoLines = dividirTexto(diagnostico, 70);
                            for (String line : diagnosticoLines) {
                                if (yPos > 750) break;
                                canvas.drawText(line, 50, yPos, paint);
                                yPos += 15;
                            }
                            yPos += 20;
                        }

                        if (consulta.getPlagas() != null && !consulta.getPlagas().isEmpty()) {
                            paint.setFakeBoldText(true);
                            canvas.drawText("ANÁLISIS DE PLAGAS", 50, yPos, paint);
                            paint.setFakeBoldText(false);
                            yPos += 20;

                            String plagas = consulta.getPlagas();
                            String[] plagasLines = dividirTexto(plagas, 70);
                            for (String line : plagasLines) {
                                if (yPos > 750) break;
                                canvas.drawText(line, 50, yPos, paint);
                                yPos += 15;
                            }
                            yPos += 20;
                        }

                        if (consulta.getRecomendaciones() != null && !consulta.getRecomendaciones().isEmpty()) {
                            paint.setFakeBoldText(true);
                            canvas.drawText("RECOMENDACIONES", 50, yPos, paint);
                            paint.setFakeBoldText(false);
                            yPos += 20;

                            String recomendaciones = consulta.getRecomendaciones();
                            String[] recomendacionesLines = dividirTexto(recomendaciones, 70);
                            for (String line : recomendacionesLines) {
                                if (yPos > 750) break;
                                canvas.drawText(line, 50, yPos, paint);
                                yPos += 15;
                            }
                        }

                        document.finishPage(page);

                        FileOutputStream fos;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && fileUri != null) {
                            fos = (FileOutputStream) requireContext().getContentResolver().openOutputStream(fileUri);
                        } else {
                            fos = new FileOutputStream(file);
                        }

                        document.writeTo(fos);
                        document.close();
                        fos.close();

                        requireActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            mostrarDialogoExitoPDF(file, consulta.getNombrePlanta(), fileUri);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error generando PDF: " + e.getMessage());
                        requireActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            mostrarErrorPDF("Error al generar PDF: " + e.getMessage());
                        });
                    }
                }).start();
            }

            @RequiresApi(api = Build.VERSION_CODES.Q)
            private Uri crearArchivoMediaStore(String fileName) {
                try {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                    return requireContext().getContentResolver().insert(
                            MediaStore.Files.getContentUri("external"), values);
                } catch (Exception e) {
                    Log.e(TAG, "Error creando archivo en MediaStore: " + e.getMessage());
                    return null;
                }
            }

            private void mostrarDialogoExitoPDF(File file, String nombrePlanta, Uri fileUri) {
                String ubicacion = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ?
                        "Carpeta de Descargas" : file.getAbsolutePath();

                new AlertDialog.Builder(requireContext())
                        .setTitle("✅ PDF Generado Exitosamente")
                        .setMessage("El diagnóstico de \"" + nombrePlanta + "\" se ha guardado en:\n" +
                                ubicacion + "\n\n¿Qué deseas hacer?")
                        .setPositiveButton("📤 Compartir", (dialog, which) -> {
                            compartirPDF(file, nombrePlanta, fileUri);
                        })
                        .setNeutralButton("📱 Abrir", (dialog, which) -> {
                            abrirPDF(file, fileUri);
                        })
                        .setNegativeButton("✅ Continuar", null)
                        .show();
            }

            private void mostrarErrorPDF(String mensaje) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("❌ Error")
                        .setMessage(mensaje)
                        .setPositiveButton("Reintentar", (dialog, which) -> {
                            if (currentConsulta != null) {
                                generarPDFConsulta(currentConsulta);
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }

            private void compartirPDF(File file, String nombrePlanta, Uri fileUri) {
                try {
                    Uri uriParaCompartir;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && fileUri != null) {
                        uriParaCompartir = fileUri;
                    } else {
                        uriParaCompartir = FileProvider.getUriForFile(requireContext(),
                                requireContext().getPackageName() + ".provider", file);
                    }

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/pdf");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uriParaCompartir);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Diagnóstico: " + nombrePlanta);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "Te comparto el diagnóstico de la planta: " + nombrePlanta);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    Intent chooser = Intent.createChooser(shareIntent, "Compartir PDF de Diagnóstico");

                    if (shareIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                        startActivity(chooser);
                    } else {
                        Toast.makeText(requireContext(), "No hay aplicaciones para compartir PDF", Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error compartiendo PDF: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error al compartir el PDF", Toast.LENGTH_LONG).show();
                }
            }

            private void abrirPDF(File file, Uri fileUri) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && fileUri != null) {
                        intent.setDataAndType(fileUri, "application/pdf");
                    } else {
                        Uri uri = FileProvider.getUriForFile(requireContext(),
                                requireContext().getPackageName() + ".provider", file);
                        intent.setDataAndType(uri, "application/pdf");
                    }

                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                    if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Aplicación Requerida")
                                .setMessage("No hay aplicaciones para abrir PDFs. ¿Quieres instalar una?")
                                .setPositiveButton("Instalar", (dialog, which) -> {
                                    abrirPlayStoreParaPDF();
                                })
                                .setNegativeButton("Cancelar", null)
                                .show();
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "No se pudo abrir el PDF", Toast.LENGTH_SHORT).show();
                }
            }

            private void abrirPlayStoreParaPDF() {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://details?id=com.adobe.reader"));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.adobe.reader"));
                    startActivity(intent);
                }
            }

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
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Permiso concedido, puedes generar PDFs", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Permiso denegado. No se pueden generar PDFs", Toast.LENGTH_LONG).show();
            }
        }
    }
}