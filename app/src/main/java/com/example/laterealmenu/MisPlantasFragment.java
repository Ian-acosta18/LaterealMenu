package com.example.laterealmenu;

import android.Manifest;
import android.app.AlertDialog;
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
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MisPlantasFragment extends Fragment {

    private static final String TAG = "MisPlantasFragment";

    private RecyclerView recyclerView;
    private PlantasAdapter plantasAdapter;
    private List<Planta> listaPlantas;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LinearLayout emptyState;
    private Button btnGenerarReporte;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mis_plantas, container, false);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews(view);
        cargarPlantas();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerPlantas);
        emptyState = view.findViewById(R.id.emptyState);
        btnGenerarReporte = view.findViewById(R.id.btnGenerarReporte);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        listaPlantas = new ArrayList<>();
        plantasAdapter = new PlantasAdapter(listaPlantas);
        recyclerView.setAdapter(plantasAdapter);

        btnGenerarReporte.setOnClickListener(v -> generarReporteCompleto());
    }

    private void cargarPlantas() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("plantas")
                .whereEqualTo("usuarioId", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listaPlantas.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Planta planta = document.toObject(Planta.class);
                            planta.setId(document.getId());

                            listaPlantas.add(planta);
                        }
                        plantasAdapter.notifyDataSetChanged();

                        if (listaPlantas.isEmpty()) {
                            emptyState.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            emptyState.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }

                    } else {
                        Toast.makeText(getContext(), "Error al cargar plantas", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void generarReporteCompleto() {
        if (listaPlantas.isEmpty()) {
            Toast.makeText(getContext(), "No hay plantas para generar reporte", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar permisos de almacenamiento
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
            return;
        }

        generarPDFParaTodasLasPlantas();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generarPDFParaTodasLasPlantas();
            } else {
                Toast.makeText(getContext(), "Se necesitan permisos de almacenamiento para generar el PDF", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void generarPDFParaTodasLasPlantas() {
        try {
            Toast.makeText(getContext(), "Generando PDF...", Toast.LENGTH_SHORT).show();

            // Crear documento PDF
            PdfDocument document = new PdfDocument();

            // Crear página A4 (595 x 842 puntos a 72 dpi)
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Configurar paints
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);

            TextPaint titlePaint = new TextPaint();
            titlePaint.setColor(Color.BLACK);
            titlePaint.setTextSize(22);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setAntiAlias(true);

            TextPaint headerPaint = new TextPaint();
            headerPaint.setColor(Color.BLACK);
            headerPaint.setTextSize(16);
            headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            headerPaint.setAntiAlias(true);

            TextPaint normalPaint = new TextPaint();
            normalPaint.setColor(Color.BLACK);
            normalPaint.setTextSize(12);
            normalPaint.setAntiAlias(true);

            int yPosition = 80;
            int margin = 50;
            int pageWidth = pageInfo.getPageWidth() - (2 * margin);

            // Fondo del título
            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.parseColor("#E8F5E9"));
            canvas.drawRect(margin - 10, yPosition - 40, pageInfo.getPageWidth() - margin + 10, yPosition + 10, bgPaint);

            // Título del reporte
            String title = "🌿 REPORTE DE MIS PLANTAS";
            canvas.drawText(title, margin, yPosition, titlePaint);
            yPosition += 60;

            // Información del reporte
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale.getDefault());
            String fechaGeneracion = "Fecha de generación: " + sdf.format(new Date());
            canvas.drawText(fechaGeneracion, margin, yPosition, normalPaint);
            yPosition += 25;

            String totalPlantas = "Total de plantas registradas: " + listaPlantas.size();
            canvas.drawText(totalPlantas, margin, yPosition, headerPaint);
            yPosition += 40;

            // Línea separadora
            Paint linePaint = new Paint();
            linePaint.setColor(Color.GRAY);
            linePaint.setStrokeWidth(1);
            canvas.drawLine(margin, yPosition, pageInfo.getPageWidth() - margin, yPosition, linePaint);
            yPosition += 30;

            // Información de cada planta
            int pageNumber = 1;
            for (int i = 0; i < listaPlantas.size(); i++) {
                Planta planta = listaPlantas.get(i);

                // Verificar si necesitamos una nueva página
                if (yPosition > 700) {
                    // Agregar número de página
                    String paginaActual = "Página " + pageNumber;
                    canvas.drawText(paginaActual, pageInfo.getPageWidth() - margin - 80, pageInfo.getPageHeight() - 30, normalPaint);

                    document.finishPage(page);
                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = 50;

                    // Título en nueva página
                    canvas.drawText("🌿 REPORTE DE MIS PLANTAS (Cont.)", margin, yPosition, titlePaint);
                    yPosition += 40;
                    canvas.drawLine(margin, yPosition, pageInfo.getPageWidth() - margin, yPosition, linePaint);
                    yPosition += 30;
                }

                // Encabezado de planta con fondo
                bgPaint.setColor(Color.parseColor("#F5F5F5"));
                canvas.drawRect(margin - 5, yPosition - 5, pageInfo.getPageWidth() - margin + 5, yPosition + 25, bgPaint);

                String plantaHeader = (i + 1) + ". " + planta.getNombreComun().toUpperCase();
                canvas.drawText(plantaHeader, margin, yPosition + 15, headerPaint);
                yPosition += 35;

                // Información detallada en dos columnas
                int column1 = margin;
                int column2 = margin + 280;

                // Columna 1
                drawTextWithLabel(canvas, "Título:", getSafeText(planta.getTituloRegistro()), column1, yPosition, normalPaint);
                yPosition += 18;

                drawTextWithLabel(canvas, "Nombre científico:", getSafeText(planta.getNombreCientifico()), column1, yPosition, normalPaint);
                yPosition += 18;

                drawTextWithLabel(canvas, "Categoría:", getSafeText(planta.getCategoria()), column1, yPosition, normalPaint);
                yPosition += 18;

                drawTextWithLabel(canvas, "Estado:", getSafeText(planta.getEstadoSeguimiento()), column1, yPosition, normalPaint);
                yPosition += 18;

                // Columna 2
                drawTextWithLabel(canvas, "Prioridad:", getSafeText(planta.getPrioridad()), column2, yPosition - 54, normalPaint);
                drawTextWithLabel(canvas, "Días riego:", String.valueOf(planta.getDiasRiego()), column2, yPosition - 36, normalPaint);
                drawTextWithLabel(canvas, "Días fertilización:", String.valueOf(planta.getDiasFertilizante()), column2, yPosition - 18, normalPaint);

                drawTextWithLabel(canvas, "Regado hoy:", planta.isRegadoHoy() ? "SÍ" : "NO", column2, yPosition, normalPaint);
                yPosition += 18;

                drawTextWithLabel(canvas, "Fertilizado hoy:", planta.isFertilizadoHoy() ? "SÍ" : "NO", column2, yPosition, normalPaint);
                yPosition += 18;

                if (planta.getProximoRiego() != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    drawTextWithLabel(canvas, "Próximo riego:", dateFormat.format(planta.getProximoRiego()), column1, yPosition, normalPaint);
                    yPosition += 18;
                }

                // Descripción si existe
                if (planta.getDescripcion() != null && !planta.getDescripcion().isEmpty()) {
                    yPosition += 5;
                    canvas.drawText("Descripción:", margin, yPosition, headerPaint);
                    yPosition += 20;

                    String descripcion = planta.getDescripcion();
                    StaticLayout staticLayout = new StaticLayout(
                            descripcion, normalPaint, pageWidth,
                            Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false
                    );
                    canvas.save();
                    canvas.translate(margin, yPosition);
                    staticLayout.draw(canvas);
                    canvas.restore();
                    yPosition += staticLayout.getHeight() + 15;
                }

                // Línea separadora entre plantas
                if (i < listaPlantas.size() - 1) {
                    canvas.drawLine(margin, yPosition, pageInfo.getPageWidth() - margin, yPosition, linePaint);
                    yPosition += 20;
                }
            }

            // Agregar número de página final
            String paginaFinal = "Página " + pageNumber;
            canvas.drawText(paginaFinal, pageInfo.getPageWidth() - margin - 80, pageInfo.getPageHeight() - 30, normalPaint);

            document.finishPage(page);

            // Guardar el PDF
            guardarPDF(document);

        } catch (Exception e) {
            Log.e(TAG, "Error generando PDF: " + e.getMessage(), e);
            Toast.makeText(getContext(), "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getSafeText(String text) {
        return text != null && !text.equals("null") ? text : "N/A";
    }

    private void drawTextWithLabel(Canvas canvas, String label, String value, int x, int y, TextPaint paint) {
        TextPaint labelPaint = new TextPaint(paint);
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText(label, x, y, labelPaint);
        canvas.drawText(value, x + 120, y, paint);
    }

    private void guardarPDF(PdfDocument document) {
        try {
            // Crear nombre del archivo
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Reporte_Plantas_" + timeStamp + ".pdf";

            File file;
            String folderPath;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - Usar Directorio Documents
                File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                File appDir = new File(documentsDir, "MiJardinerApp");
                if (!appDir.exists()) {
                    appDir.mkdirs();
                }
                file = new File(appDir, fileName);
                folderPath = appDir.getAbsolutePath();
            } else {
                // Android 9 y anteriores - Usar Downloads
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File appDir = new File(downloadsDir, "MiJardinerApp");
                if (!appDir.exists()) {
                    appDir.mkdirs();
                }
                file = new File(appDir, fileName);
                folderPath = appDir.getAbsolutePath();
            }

            // Crear el archivo
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            // Mensaje de éxito con ubicación
            String message = "✅ PDF guardado exitosamente!\n\n";
            message += "Archivo: " + fileName + "\n";
            message += "Carpeta: MiJardinerApp\n";
            message += "Ruta: " + folderPath;

            // Mostrar diálogo con opciones
            new AlertDialog.Builder(getContext())
                    .setTitle("✅ PDF Generado")
                    .setMessage(message)
                    .setPositiveButton("Abrir Archivo", (dialog, which) -> {
                        abrirArchivoPDF(file);
                    })
                    .setNeutralButton("Abrir Carpeta", (dialog, which) -> {
                        abrirCarpetaContenedora(file);
                    })
                    .setNegativeButton("OK", null)
                    .show();

            Log.d(TAG, "PDF guardado en: " + file.getAbsolutePath());

        } catch (IOException e) {
            Log.e(TAG, "Error guardando PDF: " + e.getMessage(), e);
            Toast.makeText(getContext(), "❌ Error guardando archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void abrirArchivoPDF(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getApplicationContext().getPackageName() + ".provider",
                        file);
            } else {
                uri = Uri.fromFile(file);
            }
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            // Verificar si hay una app para abrir PDFs
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "No hay aplicación para abrir PDFs", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error abriendo archivo: " + e.getMessage());
            Toast.makeText(getContext(), "Error al abrir el archivo", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirCarpetaContenedora(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            File parentDir = file.getParentFile();
            Uri uri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getApplicationContext().getPackageName() + ".provider",
                        parentDir);
            } else {
                uri = Uri.fromFile(parentDir);
            }

            intent.setDataAndType(uri, "resource/folder");

            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Fallback: mostrar la ruta
                Toast.makeText(getContext(), "Carpeta: " + parentDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error abriendo carpeta: " + e.getMessage());
            // Mostrar ruta completa
            Toast.makeText(getContext(), "Ruta completa:\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }


    // ----------------------------- ADAPTER ---------------------------------------------------

    private class PlantasAdapter extends RecyclerView.Adapter<PlantasAdapter.PlantaViewHolder> {

        private List<Planta> plantas;

        public PlantasAdapter(List<Planta> plantas) {
            this.plantas = plantas;
        }

        @NonNull
        @Override
        public PlantaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_planta, parent, false);
            return new PlantaViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PlantaViewHolder holder, int position) {
            holder.bind(plantas.get(position));
        }

        @Override
        public int getItemCount() {
            return plantas.size();
        }

        class PlantaViewHolder extends RecyclerView.ViewHolder {

            private TextView tvTitulo, tvEstado, tvNombre, tvNombreCientifico, tvPrioridad, tvProximoRiego;
            private TextView tvRiegoHoy, tvFertilizacionHoy;
            private ImageButton btnEliminar, btnVerDetalles, btnEditar, btnProgreso;
            private ImageView imgPlanta;

            public PlantaViewHolder(@NonNull View itemView) {
                super(itemView);

                tvTitulo = itemView.findViewById(R.id.tvTitulo);
                tvEstado = itemView.findViewById(R.id.tvEstado);
                tvNombre = itemView.findViewById(R.id.tvNombre);
                tvNombreCientifico = itemView.findViewById(R.id.tvNombreCientifico);
                tvPrioridad = itemView.findViewById(R.id.tvPrioridad);
                tvRiegoHoy = itemView.findViewById(R.id.tvRiegoHoy);
                tvFertilizacionHoy = itemView.findViewById(R.id.tvFertilizacionHoy);
                tvProximoRiego = itemView.findViewById(R.id.tvProximoRiego);

                imgPlanta = itemView.findViewById(R.id.imgPlanta);

                btnEliminar = itemView.findViewById(R.id.btnEliminar);
                btnVerDetalles = itemView.findViewById(R.id.btnVerDetalles);
                btnEditar = itemView.findViewById(R.id.btnEditar);
                btnProgreso = itemView.findViewById(R.id.btnProgreso);

                // ✅ CONFIGURAR LISTENERS DE BOTONES
                configurarListenersDeBotones();
            }

            // ✅ MÉTODO PARA CONFIGURAR LISTENERS
            private void configurarListenersDeBotones() {
                // Botón Eliminar
                btnEliminar.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Planta planta = plantas.get(position);
                        mostrarDialogoEliminar(planta);
                    }
                });

                // Botón Ver Detalles
                btnVerDetalles.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Planta planta = plantas.get(position);
                        verDetallesPlanta(planta);
                    }
                });

                // Botón Editar
                btnEditar.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Planta planta = plantas.get(position);
                        editarPlanta(planta);
                    }
                });

                // Botón Progreso
                btnProgreso.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Planta planta = plantas.get(position);
                        abrirProgresoPlanta(planta);
                    }
                });

                // ✅ Click en toda la tarjeta para ver detalles
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Planta planta = plantas.get(position);
                        verDetallesPlanta(planta);
                    }
                });
            }

            public void bind(Planta planta) {
                try {
                    tvTitulo.setText(planta.getTituloRegistro() != null ?
                            planta.getTituloRegistro() : planta.getNombreComun());

                    tvNombre.setText(planta.getNombreComun());
                    tvNombreCientifico.setText(planta.getNombreCientifico());

                    String estado = planta.getEstadoSeguimiento() != null ? planta.getEstadoSeguimiento() : "Activo";
                    tvEstado.setText(estado);
                    configurarColorEstado(estado, tvEstado);

                    String prioridad = planta.getPrioridad() != null ? planta.getPrioridad() : "Media";
                    tvPrioridad.setText(prioridad);
                    configurarColorPrioridad(prioridad, tvPrioridad);

                    // Riego hoy con manejo seguro
                    if (planta.isRegadoHoy()) {
                        tvRiegoHoy.setText("💧 Regado");
                        tvRiegoHoy.setTextColor(Color.parseColor("#4CAF50"));
                    } else {
                        tvRiegoHoy.setText("💧 No regado");
                        tvRiegoHoy.setTextColor(Color.parseColor("#F44336"));
                    }

                    // Fertilización hoy con manejo seguro
                    if (planta.isFertilizadoHoy()) {
                        tvFertilizacionHoy.setText("🌱 Fertilizado");
                        tvFertilizacionHoy.setTextColor(Color.parseColor("#4CAF50"));
                    } else {
                        tvFertilizacionHoy.setText("🌱 No fertilizado");
                        tvFertilizacionHoy.setTextColor(Color.parseColor("#F44336"));
                    }

                    // Próximo riego con manejo seguro de fechas
                    try {
                        Date proximoRiegoDate = planta.getProximoRiegoAsDate();
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        String proximoRiego = sdf.format(proximoRiegoDate);
                        tvProximoRiego.setText("Próximo riego: " + proximoRiego);
                    } catch (Exception e) {
                        Log.e("MisPlantas", "Error formateando fecha: " + e.getMessage());
                        tvProximoRiego.setText("Próximo riego: Hoy");
                    }

                    // Imagen con manejo seguro
                    if (planta.getImagenBase64() != null && !planta.getImagenBase64().isEmpty()) {
                        try {
                            byte[] decodedString = Base64.decode(planta.getImagenBase64(), Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            imgPlanta.setImageBitmap(bitmap);
                            imgPlanta.setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            imgPlanta.setVisibility(View.GONE);
                            Log.e("MisPlantas", "Error cargando imagen: " + e.getMessage());
                        }
                    } else {
                        imgPlanta.setVisibility(View.GONE);
                    }

                } catch (Exception e) {
                    Log.e("MisPlantas", "Error en bind: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            private void configurarColorEstado(String estado, TextView textView) {
                switch (estado) {
                    case "Activo":
                        textView.setBackgroundColor(Color.parseColor("#E8F5E9"));
                        textView.setTextColor(Color.parseColor("#2E7D32"));
                        break;
                    case "En proceso":
                        textView.setBackgroundColor(Color.parseColor("#FFF3E0"));
                        textView.setTextColor(Color.parseColor("#FF9800"));
                        break;
                    case "En revisión":
                        textView.setBackgroundColor(Color.parseColor("#E3F2FD"));
                        textView.setTextColor(Color.parseColor("#2196F3"));
                        break;
                    case "Completado":
                        textView.setBackgroundColor(Color.parseColor("#E8F5E9"));
                        textView.setTextColor(Color.parseColor("#4CAF50"));
                        break;
                    case "Cancelado":
                        textView.setBackgroundColor(Color.parseColor("#FFEBEE"));
                        textView.setTextColor(Color.parseColor("#F44336"));
                        break;
                }
            }

            private void configurarColorPrioridad(String prioridad, TextView textView) {
                switch (prioridad) {
                    case "Alta":
                        textView.setBackgroundColor(Color.parseColor("#FFEBEE"));
                        textView.setTextColor(Color.parseColor("#F44336"));
                        break;
                    case "Media":
                        textView.setBackgroundColor(Color.parseColor("#FFF3E0"));
                        textView.setTextColor(Color.parseColor("#FF9800"));
                        break;
                    case "Baja":
                        textView.setBackgroundColor(Color.parseColor("#E8F5E9"));
                        textView.setTextColor(Color.parseColor("#4CAF50"));
                        break;
                }
            }

            // ✅ MÉTODOS DE ACCIÓN PARA LOS BOTONES

            private void mostrarDialogoEliminar(Planta planta) {
                new AlertDialog.Builder(itemView.getContext())
                        .setTitle("Eliminar Planta")
                        .setMessage("¿Estás seguro de eliminar \"" + planta.getNombreComun() + "\"?")
                        .setPositiveButton("Eliminar", (dialog, which) -> eliminarPlanta(planta))
                        .setNegativeButton("Cancelar", null)
                        .show();
            }

            private void eliminarPlanta(Planta planta) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("plantas").document(planta.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(itemView.getContext(), "✅ Planta eliminada", Toast.LENGTH_SHORT).show();
                            // Actualizar la lista localmente
                            int position = getAdapterPosition();
                            if (position != RecyclerView.NO_POSITION) {
                                plantas.remove(position);
                                notifyItemRemoved(position);

                                // Mostrar empty state si no hay plantas
                                if (plantas.isEmpty()) {
                                    emptyState.setVisibility(View.VISIBLE);
                                    recyclerView.setVisibility(View.GONE);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("MisPlantas", "Error al eliminar planta: " + e.getMessage());
                            Toast.makeText(itemView.getContext(), "❌ Error al eliminar", Toast.LENGTH_SHORT).show();
                        });
            }

            // EN MisPlantasFragment.java - método editarPlanta()
            private void editarPlanta(Planta planta) {
                try {
                    
                    // EditarPlantaFragment fragment = EditarPlantaFragment.newInstance(planta);
                    EditarPlantaFragment fragment = new EditarPlantaFragment();
                    Bundle args = new Bundle();
                    args.putSerializable("planta", planta); // Asegúrate de que la clave sea "planta"
                    fragment.setArguments(args);
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.contenedor, fragment) // Asegúrate que R.id.contenedor es correcto
                            .addToBackStack("editar_planta") // Para poder volver atrás
                            .commit();

                    Toast.makeText(itemView.getContext(), "✏️ Editando: " + planta.getNombreComun(), Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Log.e("MisPlantas", "Error al editar planta: " + e.getMessage(), e);
                    Toast.makeText(itemView.getContext(), "❌ Error al abrir editor", Toast.LENGTH_SHORT).show();
                }
            }

            private void abrirProgresoPlanta(Planta planta) {
                try {
                    // ✅ REDIRIGIR AL FRAGMENT DE PROGRESO
                    ProgresoPlantaFragment fragment = ProgresoPlantaFragment.newInstance(planta);
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.contenedor, fragment)
                            .addToBackStack("progreso_planta")
                            .commit();

                    Toast.makeText(itemView.getContext(), "📈 Abriendo progreso de " + planta.getNombreComun(), Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Log.e("MisPlantas", "Error al abrir progreso: " + e.getMessage());
                    Toast.makeText(itemView.getContext(), "❌ Error al abrir progreso", Toast.LENGTH_SHORT).show();
                }
            }

            private void verDetallesPlanta(Planta planta) {
                AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext())
                        .setTitle("🌿 " + planta.getNombreComun())
                        .setMessage(
                                "📌 Título: " + (planta.getTituloRegistro() != null ? planta.getTituloRegistro() : "N/A") + "\n" +
                                        "🔬 Nombre científico: " + (planta.getNombreCientifico() != null ? planta.getNombreCientifico() : "N/A") + "\n" +
                                        "📊 Estado: " + (planta.getEstadoSeguimiento() != null ? planta.getEstadoSeguimiento() : "Activo") + "\n" +
                                        "⚡ Prioridad: " + (planta.getPrioridad() != null ? planta.getPrioridad() : "Media") + "\n" +
                                        "📂 Categoría: " + (planta.getCategoria() != null ? planta.getCategoria() : "General") + "\n" +
                                        "💧 Riego hoy: " + (planta.isRegadoHoy() ? "✅ Regado" : "❌ No regado") + "\n" +
                                        "🌱 Fertilización hoy: " + (planta.isFertilizadoHoy() ? "✅ Fertilizado" : "❌ No fertilizado") + "\n" +
                                        "📅 Próximo riego: " + (planta.getProximoRiego() != null ?
                                        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(planta.getProximoRiegoAsDate()) : "Hoy") + "\n" +
                                        "📅 Días entre riegos: " + planta.getDiasRiego() + " días\n" +
                                        "🌿 Días entre fertilizaciones: " + planta.getDiasFertilizante() + " días"
                        )
                        .setPositiveButton("Cerrar", null)
                        .setNeutralButton("✏️ Editar", (d, w) -> editarPlanta(planta))
                        .setNegativeButton("📈 Progreso", (d, w) -> abrirProgresoPlanta(planta));

                builder.show();
            }

            // ✅ MÉTODOS AUXILIARES PARA PROGRESO

            private void marcarComoRegadoHoy(Planta planta) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                Map<String, Object> updates = new HashMap<>();
                updates.put("regadoHoy", true);
                updates.put("ultimoRiego", System.currentTimeMillis() / 1000);
                updates.put("totalRiegos", planta.getTotalRiegos() + 1);

                // Calcular próximo riego
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, planta.getDiasRiego());
                updates.put("proximoRiego", calendar.getTimeInMillis() / 1000);

                db.collection("plantas").document(planta.getId())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(itemView.getContext(), "✅ Marcado como regado hoy", Toast.LENGTH_SHORT).show();
                            planta.setRegadoHoy(true);
                            planta.setTotalRiegos(planta.getTotalRiegos() + 1);
                            notifyItemChanged(getAdapterPosition());
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(itemView.getContext(), "❌ Error al actualizar", Toast.LENGTH_SHORT).show();
                        });
            }

            private void marcarComoFertilizadoHoy(Planta planta) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                Map<String, Object> updates = new HashMap<>();
                updates.put("fertilizadoHoy", true);
                updates.put("ultimaFertilizacion", System.currentTimeMillis() / 1000);
                updates.put("totalFertilizaciones", planta.getTotalFertilizaciones() + 1);

                // Calcular próxima fertilización
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, planta.getDiasFertilizante());
                updates.put("proximaFertilizacion", calendar.getTimeInMillis() / 1000);

                db.collection("plantas").document(planta.getId())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(itemView.getContext(), "✅ Marcado como fertilizado hoy", Toast.LENGTH_SHORT).show();
                            planta.setFertilizadoHoy(true);
                            planta.setTotalFertilizaciones(planta.getTotalFertilizaciones() + 1);
                            notifyItemChanged(getAdapterPosition());
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(itemView.getContext(), "❌ Error al actualizar", Toast.LENGTH_SHORT).show();
                        });
            }

            // ✅ MÉTODO PARA GENERAR PDF INDIVIDUAL
            private void generarPDFIndividual(Planta planta) {
                try {
                    Toast.makeText(itemView.getContext(), "Generando ficha de planta...", Toast.LENGTH_SHORT).show();

                    PdfDocument document = new PdfDocument();
                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                    PdfDocument.Page page = document.startPage(pageInfo);
                    Canvas canvas = page.getCanvas();

                    // Configurar paints
                    TextPaint titlePaint = new TextPaint();
                    titlePaint.setColor(Color.BLACK);
                    titlePaint.setTextSize(24);
                    titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    titlePaint.setAntiAlias(true);

                    TextPaint headerPaint = new TextPaint();
                    headerPaint.setColor(Color.BLACK);
                    headerPaint.setTextSize(16);
                    headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    headerPaint.setAntiAlias(true);

                    TextPaint normalPaint = new TextPaint();
                    normalPaint.setColor(Color.BLACK);
                    normalPaint.setTextSize(12);
                    normalPaint.setAntiAlias(true);

                    int yPosition = 80;
                    int margin = 50;

                    // Fondo del título
                    Paint bgPaint = new Paint();
                    bgPaint.setColor(Color.parseColor("#E8F5E9"));
                    canvas.drawRect(margin - 10, yPosition - 40, pageInfo.getPageWidth() - margin + 10, yPosition + 10, bgPaint);

                    // Título
                    String title = "🌿 FICHA DE PLANTA INDIVIDUAL";
                    canvas.drawText(title, margin, yPosition, titlePaint);
                    yPosition += 60;

                    // Información de la planta
                    canvas.drawText("Nombre común: " + planta.getNombreComun(), margin, yPosition, headerPaint);
                    yPosition += 25;

                    if (planta.getTituloRegistro() != null) {
                        canvas.drawText("Título: " + planta.getTituloRegistro(), margin, yPosition, normalPaint);
                        yPosition += 18;
                    }

                    if (planta.getNombreCientifico() != null) {
                        canvas.drawText("Nombre científico: " + planta.getNombreCientifico(), margin, yPosition, normalPaint);
                        yPosition += 18;
                    }

                    if (planta.getCategoria() != null) {
                        canvas.drawText("Categoría: " + planta.getCategoria(), margin, yPosition, normalPaint);
                        yPosition += 18;
                    }

                    canvas.drawText("Estado: " + (planta.getEstadoSeguimiento() != null ? planta.getEstadoSeguimiento() : "Activo"), margin, yPosition, normalPaint);
                    yPosition += 18;

                    canvas.drawText("Prioridad: " + (planta.getPrioridad() != null ? planta.getPrioridad() : "Media"), margin, yPosition, normalPaint);
                    yPosition += 18;

                    canvas.drawText("Días entre riegos: " + planta.getDiasRiego(), margin, yPosition, normalPaint);
                    yPosition += 18;

                    canvas.drawText("Días entre fertilizaciones: " + planta.getDiasFertilizante(), margin, yPosition, normalPaint);
                    yPosition += 18;

                    canvas.drawText("Regado hoy: " + (planta.isRegadoHoy() ? "Sí" : "No"), margin, yPosition, normalPaint);
                    yPosition += 18;

                    canvas.drawText("Fertilizado hoy: " + (planta.isFertilizadoHoy() ? "Sí" : "No"), margin, yPosition, normalPaint);
                    yPosition += 18;

                    canvas.drawText("Total de riegos: " + planta.getTotalRiegos(), margin, yPosition, normalPaint);
                    yPosition += 18;

                    canvas.drawText("Total de fertilizaciones: " + planta.getTotalFertilizaciones(), margin, yPosition, normalPaint);
                    yPosition += 18;

                    if (planta.getProximoRiego() != null) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        canvas.drawText("Próximo riego: " + dateFormat.format(planta.getProximoRiego()), margin, yPosition, normalPaint);
                        yPosition += 18;
                    }

                    if (planta.getProximaFertilizacion() != null) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        canvas.drawText("Próxima fertilización: " + dateFormat.format(planta.getProximaFertilizacion()), margin, yPosition, normalPaint);
                        yPosition += 18;
                    }

                    if (planta.getDescripcion() != null && !planta.getDescripcion().isEmpty()) {
                        yPosition += 10;
                        canvas.drawText("Descripción:", margin, yPosition, headerPaint);
                        yPosition += 25;

                        // Manejar texto largo
                        StaticLayout staticLayout = new StaticLayout(
                                planta.getDescripcion(), normalPaint, pageInfo.getPageWidth() - (2 * margin),
                                Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false
                        );
                        canvas.save();
                        canvas.translate(margin, yPosition);
                        staticLayout.draw(canvas);
                        canvas.restore();
                    }

                    // Fecha de generación
                    yPosition += 50;
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale.getDefault());
                    canvas.drawText("Generado el: " + sdf.format(new Date()), margin, yPosition, normalPaint);

                    document.finishPage(page);

                    // Guardar PDF individual
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    String fileName = "Ficha_" + planta.getNombreComun().replace(" ", "_") + "_" + timeStamp + ".pdf";

                    File file;
                    String folderPath;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                        File appDir = new File(documentsDir, "MiJardinerApp");
                        if (!appDir.exists()) {
                            appDir.mkdirs();
                        }
                        file = new File(appDir, fileName);
                        folderPath = appDir.getAbsolutePath();
                    } else {
                        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        File appDir = new File(downloadsDir, "MiJardinerApp");
                        if (!appDir.exists()) {
                            appDir.mkdirs();
                        }
                        file = new File(appDir, fileName);
                        folderPath = appDir.getAbsolutePath();
                    }

                    FileOutputStream fos = new FileOutputStream(file);
                    document.writeTo(fos);
                    document.close();
                    fos.close();

                    String message = "✅ Ficha individual guardada!\n\n";
                    message += "Archivo: " + fileName + "\n";
                    message += "Carpeta: MiJardinerApp\n";
                    message += "Ruta: " + folderPath;

                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle("✅ Ficha Generada")
                            .setMessage(message)
                            .setPositiveButton("Abrir", (dialog, which) -> abrirArchivoPDF(file))
                            .setNegativeButton("OK", null)
                            .show();

                } catch (Exception e) {
                    Log.e(TAG, "Error PDF individual: " + e.getMessage(), e);
                    Toast.makeText(itemView.getContext(), "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}