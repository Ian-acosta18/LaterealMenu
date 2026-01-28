package com.example.laterealmenu;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditarPlantaFragment extends Fragment {

    private static final int PICK_IMAGE = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;

    // Views
    private ImageView imgPlanta;
    private Button btnTomarFoto, btnGaleria, btnGuardarCambios;
    private TextInputEditText etTituloRegistro, etNombrePlanta, etDescripcionBreve;
    private AutoCompleteTextView actvCategoria, actvPrioridad;
    private TextView tvFechaCreacion, tvDiasRiego, tvDiasFertilizante;
    private SeekBar seekBarRiego, seekBarFertilizante;
    private Switch switchNotificaciones;

    // Datos
    private Planta planta;
    private Bitmap imagenBitmap;
    private Uri imagenUri;
    private FirebaseFirestore db;

    // Arrays para los dropdowns
    private String[] categorias = {"Interior", "Exterior", "Suculentas", "Cactus", "Hierbas", "Flores", "Vegetales", "Árboles", "Arbustos", "Orquídeas"};
    private String[] prioridades = {"Baja", "Media", "Alta"};

    public static EditarPlantaFragment newInstance(Planta planta) {
        EditarPlantaFragment fragment = new EditarPlantaFragment();
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
        View view = inflater.inflate(R.layout.fragment_editar_planta, container, false);
        initViews(view);
        cargarDatosPlanta();
        setupClickListeners();
        return view;
    }

    private void initViews(View view) {
        // Vistas de imagen
        imgPlanta = view.findViewById(R.id.imgPlantaEditar);
        btnTomarFoto = view.findViewById(R.id.btnTomarFotoEditar);
        btnGaleria = view.findViewById(R.id.btnGaleriaEditar);


        // Campos de texto
        etTituloRegistro = view.findViewById(R.id.etTituloRegistroEditar);
        etNombrePlanta = view.findViewById(R.id.etNombrePlantaEditar);
        etDescripcionBreve = view.findViewById(R.id.etDescripcionBreveEditar);

        // Dropdowns
        actvCategoria = view.findViewById(R.id.actvCategoriaEditar);
        actvPrioridad = view.findViewById(R.id.actvPrioridadEditar);

        // Fecha y controles
        tvFechaCreacion = view.findViewById(R.id.tvFechaCreacionEditar);
        tvDiasRiego = view.findViewById(R.id.tvDiasRiegoEditar);
        tvDiasFertilizante = view.findViewById(R.id.tvDiasFertilizanteEditar);
        seekBarRiego = view.findViewById(R.id.seekBarRiegoEditar);
        seekBarFertilizante = view.findViewById(R.id.seekBarFertilizanteEditar);
        switchNotificaciones = view.findViewById(R.id.switchNotificacionesEditar);
        btnGuardarCambios = view.findViewById(R.id.btnGuardarCambios);
        // Configurar dropdowns
        configurarDropdowns();
        configurarSeekBars();
    }

    private void configurarDropdowns() {
        ArrayAdapter<String> categoriaAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categorias);
        actvCategoria.setAdapter(categoriaAdapter);

        ArrayAdapter<String> prioridadAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, prioridades);
        actvPrioridad.setAdapter(prioridadAdapter);
    }

    private void configurarSeekBars() {
        seekBarRiego.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int dias = progress + 1;
                tvDiasRiego.setText(dias + " días");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarFertilizante.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int dias = progress + 1;
                tvDiasFertilizante.setText(dias + " días");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void cargarDatosPlanta() {
        if (planta != null) {
            Log.d("EditarPlanta", "Cargando datos de la planta: " + planta.getNombreComun());

            // 1. CAMPOS DE TEXTO - Usar setText() correctamente
            etTituloRegistro.setText(planta.getTituloRegistro() != null ? planta.getTituloRegistro() : planta.getNombreComun());
            etNombrePlanta.setText(planta.getNombreComun());
            etDescripcionBreve.setText(planta.getDescripcion() != null ? planta.getDescripcion() : "");

            // 2. DROPDOWNS - Usar setText() con el valor exacto
            if (planta.getCategoria() != null && !planta.getCategoria().isEmpty()) {
                actvCategoria.setText(planta.getCategoria(), false);
                Log.d("EditarPlanta", "Categoría cargada: " + planta.getCategoria());
            } else {
                actvCategoria.setText("Interior", false); // Valor por defecto
            }

            if (planta.getPrioridad() != null && !planta.getPrioridad().isEmpty()) {
                actvPrioridad.setText(planta.getPrioridad(), false);
                Log.d("EditarPlanta", "Prioridad cargada: " + planta.getPrioridad());
            } else {
                actvPrioridad.setText("Media", false); // Valor por defecto
            }

            // 3. FECHA
            if (planta.getFechaCreacion() != null && !planta.getFechaCreacion().isEmpty()) {
                tvFechaCreacion.setText(planta.getFechaCreacion());
                Log.d("EditarPlanta", "Fecha cargada: " + planta.getFechaCreacion());
            } else {
                // Si no hay fecha, usar la actual
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String fechaActual = sdf.format(new java.util.Date());
                tvFechaCreacion.setText(fechaActual);
                Log.d("EditarPlanta", "Fecha establecida: " + fechaActual);
            }

            // 4. SEEKBARS - Verificar valores antes de establecer
            if (seekBarRiego != null) {
                int diasRiego = planta.getDiasRiego() > 0 ? planta.getDiasRiego() : 7;
                seekBarRiego.setProgress(diasRiego - 1);
                tvDiasRiego.setText(diasRiego + " días");
                Log.d("EditarPlanta", "Días riego: " + diasRiego);
            }

            if (seekBarFertilizante != null) {
                int diasFertilizante = planta.getDiasFertilizante() > 0 ? planta.getDiasFertilizante() : 30;
                seekBarFertilizante.setProgress(diasFertilizante - 1);
                tvDiasFertilizante.setText(diasFertilizante + " días");
                Log.d("EditarPlanta", "Días fertilizante: " + diasFertilizante);
            }

            // 5. SWITCH DE NOTIFICACIONES
            boolean notificaciones = planta.isNotificacionesActivadas();
            switchNotificaciones.setChecked(notificaciones);
            Log.d("EditarPlanta", "Notificaciones: " + notificaciones);

            // 6. IMAGEN
            if (planta.getImagenBase64() != null && !planta.getImagenBase64().isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(planta.getImagenBase64(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    if (decodedByte != null) {
                        imgPlanta.setImageBitmap(decodedByte);
                        Log.d("EditarPlanta", "Imagen cargada correctamente");
                    } else {
                        Log.w("EditarPlanta", "Bitmap decodificado es null");
                    }
                } catch (Exception e) {
                    Log.e("EditarPlanta", "Error cargando imagen: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                Log.w("EditarPlanta", "No hay imagen base64 para cargar");
            }

            // 7. Hacer la fecha clickeable
            tvFechaCreacion.setOnClickListener(v -> mostrarDatePicker());

            // DEBUG: Verificar todos los valores cargados
            Log.d("EditarPlanta", "=== RESUMEN DATOS CARGADOS ===");
            Log.d("EditarPlanta", "Título: " + etTituloRegistro.getText().toString());
            Log.d("EditarPlanta", "Nombre: " + etNombrePlanta.getText().toString());
            Log.d("EditarPlanta", "Descripción: " + etDescripcionBreve.getText().toString());
            Log.d("EditarPlanta", "Categoría: " + actvCategoria.getText().toString());
            Log.d("EditarPlanta", "Prioridad: " + actvPrioridad.getText().toString());
            Log.d("EditarPlanta", "Fecha: " + tvFechaCreacion.getText().toString());

        } else {
            Log.e("EditarPlanta", "La planta es null, no se pueden cargar datos");
            Toast.makeText(requireContext(), "❌ Error: No se encontraron datos de la planta", Toast.LENGTH_LONG).show();
        }
    }



    private void setupClickListeners() {
        btnTomarFoto.setOnClickListener(v -> tomarFoto());
        btnGaleria.setOnClickListener(v -> abrirGaleria());
        btnGuardarCambios.setOnClickListener(v -> guardarCambios());
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

    private void tomarFoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == PICK_IMAGE && data != null) {
                imagenUri = data.getData();
                try {
                    imagenBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imagenUri);
                    imgPlanta.setImageBitmap(imagenBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                imagenBitmap = (Bitmap) extras.get("data");
                imgPlanta.setImageBitmap(imagenBitmap);
            }
        }
    }

    private void guardarCambios() {
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
        int diasRiego = seekBarRiego.getProgress() + 1;
        int diasFertilizante = seekBarFertilizante.getProgress() + 1;
        boolean notificaciones = switchNotificaciones.isChecked();

        // Preparar datos para actualizar
        Map<String, Object> updates = new HashMap<>();
        updates.put("tituloRegistro", titulo);
        updates.put("nombreComun", nombre);
        updates.put("descripcion", descripcion);
        updates.put("categoria", categoria.isEmpty() ? "General" : categoria);
        updates.put("prioridad", prioridad);
        updates.put("fechaCreacion", fechaCreacion);
        updates.put("diasRiego", diasRiego);
        updates.put("diasFertilizante", diasFertilizante);
        updates.put("notificacionesActivadas", notificaciones);
        updates.put("recomendaciones", "Riego cada " + diasRiego + " días, fertilización cada " + diasFertilizante + " días");

        // Si hay nueva imagen, actualizarla
        if (imagenBitmap != null) {
            String imagenBase64 = bitmapToBase64(imagenBitmap);
            updates.put("imagenBase64", imagenBase64);
        }

        // Actualizar en Firebase
        DocumentReference docRef = db.collection("plantas").document(planta.getId());
        docRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "✅ Cambios guardados exitosamente", Toast.LENGTH_SHORT).show();

                    // Regresar a Mis Plantas
                    if (getActivity() instanceof MainActivity) {
                        MainActivity activity = (MainActivity) getActivity();
                        activity.loadFragment(new MisPlantasFragment(), R.id.nav_mis_plantas);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "❌ Error al guardar cambios: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
}