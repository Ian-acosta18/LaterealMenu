package com.example.laterealmenu;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RaddioButton#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RaddioButton extends Fragment {


    private TextView tvPregunta;
    private RadioGroup radioGroup;
    private Button btnVerificar, btnSiguiente;

    private int indicePregunta = 0;

    private String[] preguntas = {
            "¿Qué lenguaje usa Android?",
            "¿Cuál es la extensión de un archivo Java?",
            "¿Qué IDE se usa para Android?"
    };

    private String[][] opciones = {
            {"Java", "Python", "C++", "Ruby"},
            {".java", ".py", ".cpp", ".rb"},
            {"Android Studio", "Visual Studio", "Eclipse", "NetBeans"}
    };

    private String[] respuestasCorrectas = {"Java", ".java", "Android Studio"};



    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public RaddioButton() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RaddioButton.
     */
    // TODO: Rename and change types and number of parameters
    public static RaddioButton newInstance(String param1, String param2) {
        RaddioButton fragment = new RaddioButton();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_raddio_button, container, false);

        tvPregunta = view.findViewById(R.id.tvPregunta);
        radioGroup = view.findViewById(R.id.radioGroup);
        btnVerificar = view.findViewById(R.id.btnVerificar);
        btnSiguiente = view.findViewById(R.id.btnSiguiente);

        mostrarPregunta();

        btnVerificar.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();

            if (selectedId == -1) {
                Toast.makeText(requireContext(), "Selecciona una opción", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton seleccion = view.findViewById(selectedId);
            String respuesta = seleccion.getText().toString();

            if (respuesta.equals(respuestasCorrectas[indicePregunta])) {
                Toast.makeText(requireContext(), "Correcto", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), " Incorrecto. La correcta es: " + respuestasCorrectas[indicePregunta], Toast.LENGTH_SHORT).show();
            }

            // Activar botón siguiente después de verificar
            btnSiguiente.setEnabled(true);
        });

        btnSiguiente.setOnClickListener(v -> {
            indicePregunta++;
            if (indicePregunta < preguntas.length) {
                mostrarPregunta();
                btnSiguiente.setEnabled(false); // deshabilitar hasta verificar
            } else {
                Toast.makeText(requireContext(), "¡Has terminado el cuestionario!", Toast.LENGTH_LONG).show();
                btnSiguiente.setEnabled(false);
                btnVerificar.setEnabled(false);
            }
        });

        return view;
    }

    private void mostrarPregunta() {
        tvPregunta.setText(preguntas[indicePregunta]);
        radioGroup.removeAllViews();

        for (String opcion : opciones[indicePregunta]) {
            RadioButton rb = new RadioButton(requireContext());
            rb.setText(opcion);
            radioGroup.addView(rb);
        }
    }
}