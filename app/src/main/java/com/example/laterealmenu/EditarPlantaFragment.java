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

    private ImageView imgPlanta;
    private Button btnTomarFoto, btnGaleria, btnGuardarCambios;
    private TextInputEditText etTituloRegistro, etNombrePlanta, etDescripcionBreve;
    private AutoCompleteTextView actvCategoria, actvPrioridad;
    private TextView tvFechaCreacion, tvDiasRiego, tvDiasFertilizante;
    private SeekBar seekBarRiego, seekBarFertilizante;
    private Switch switchNotificaciones;

    private Planta planta;
    private Bitmap imagenBitmap;
    private Uri imagenUri;
    private FirebaseFirestore db;

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
        imgPlanta = view.findViewById(R.id.imgPlantaEditar);
        btnTomarFoto = view.findViewById(R.id.btnTomarFotoEditar);
        btnGaleria = view.findViewById(R.id.btnGaleriaEditar);
        etTituloRegistro = view.findViewById(R.id.etTituloRegistroEditar);
        etNombrePlanta = view.findViewById(R.id.etNombrePlantaEditar);
        etDescripcionBreve = view.findViewById(R.id.etDescripcionBreveEditar);
        actvCategoria = view.findViewById(R.id.actvCategoriaEditar);
        actvPrioridad = view.findViewById(R.id.actvPrioridadEditar);
        tvFechaCreacion = view.findViewById(R.id.tvFechaCreacionEditar);
        tvDiasRiego = view.findViewById(R.id.tvDiasRiegoEditar);
        tvDiasFertilizante = view.findViewById(R.id.tvDiasFertilizanteEditar);
        seekBarRiego = view.findViewById(R.id.seekBarRiegoEditar);
        seekBarFertilizante = view.findViewById(R.id.seekBarFertilizanteEditar);
        switchNotificaciones = view.findViewById(R.id.switchNotificacionesEditar);
        btnGuardarCambios = view.findViewById(R.id.btnGuardarCambios);
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
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { tvDiasRiego.setText((progress + 1) + " días"); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarFertilizante.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { tvDiasFertilizante.setText((progress + 1) + " días"); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void cargarDatosPlanta() {
        if (planta != null) {
            etTituloRegistro.setText(planta.getTituloRegistro() != null ? planta.getTituloRegistro() : planta.getNombreComun());
            etNombrePlanta.setText(planta.getNombreComun());
            etDescripcionBreve.setText(planta.getDescripcion() != null ? planta.getDescripcion() : "");

            actvCategoria.setText(planta.getCategoria() != null ? planta.getCategoria() : "Interior", false);
            actvPrioridad.setText(planta.getPrioridad() != null ? planta.getPrioridad() : "Media", false);

            String fecha = planta.getFechaCreacion() != null ? planta.getFechaCreacion() : new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new java.util.Date());
            tvFechaCreacion.setText(fecha);

            int diasRiego = planta.getDiasRiego() > 0 ? planta.getDiasRiego() : 7;
            if (seekBarRiego != null) {
                seekBarRiego.setProgress(diasRiego - 1);
                tvDiasRiego.setText(diasRiego + " días");
            }

            int diasFertilizante = planta.getDiasFertilizante() > 0 ? planta.getDiasFertilizante() : 30;
            if (seekBarFertilizante != null) {
                seekBarFertilizante.setProgress(diasFertilizante - 1);
                tvDiasFertilizante.setText(diasFertilizante + " días");
            }

            switchNotificaciones.setChecked(planta.isNotificacionesActivadas());

            if (planta.getImagenBase64() != null && !planta.getImagenBase64().isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(planta.getImagenBase64(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    if (decodedByte != null) imgPlanta.setImageBitmap(decodedByte);
                } catch (Exception e) { e.printStackTrace(); }
            }

            tvFechaCreacion.setOnClickListener(v -> mostrarDatePicker());
        }
    }

    private void setupClickListeners() {
        btnTomarFoto.setOnClickListener(v -> tomarFoto());
        btnGaleria.setOnClickListener(v -> abrirGaleria());
        btnGuardarCambios.setOnClickListener(v -> guardarCambios());
    }

    private void mostrarDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) ->
                tvFechaCreacion.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year)),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
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
                } catch (IOException e) { e.printStackTrace(); }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                imagenBitmap = (Bitmap) extras.get("data");
                imgPlanta.setImageBitmap(imagenBitmap);
            }
        }
    }

    // ✅ MÉTODO ACTUALIZADO: Refresca el widget al guardar cambios
    private void guardarCambios() {
        String titulo = etTituloRegistro.getText().toString().trim();
        String nombre = etNombrePlanta.getText().toString().trim();
        String descripcion = etDescripcionBreve.getText().toString().trim();
        String categoria = actvCategoria.getText().toString().trim();
        String prioridad = actvPrioridad.getText().toString().trim();
        String fechaCreacion = tvFechaCreacion.getText().toString().trim();

        if (titulo.isEmpty() || nombre.isEmpty()) {
            Toast.makeText(requireContext(), "⚠️ Completa título y nombre", Toast.LENGTH_SHORT).show();
            return;
        }

        int diasRiego = seekBarRiego.getProgress() + 1;
        int diasFertilizante = seekBarFertilizante.getProgress() + 1;
        boolean notificaciones = switchNotificaciones.isChecked();

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

        if (imagenBitmap != null) {
            updates.put("imagenBase64", bitmapToBase64(imagenBitmap));
        }

        DocumentReference docRef = db.collection("plantas").document(planta.getId());
        docRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "✅ Cambios guardados", Toast.LENGTH_SHORT).show();

                    // 🚀 ACTUALIZAR WIDGET
                    try {
                        RecordatorioWidget.forzarActualizacion(requireContext());
                    } catch (Exception e) { Log.e("Widget", "Error update: " + e); }

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).loadFragment(new MisPlantasFragment(), R.id.nav_mis_plantas);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
    }
}