package com.example.laterealmenu;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CheckButton#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CheckButton extends Fragment {

    private TextView tvPregunta;
    private LinearLayout checkboxContainer;
    private Button btnVerificar, btnSiguiente;

    private int indicePregunta = 0;

    private String[] preguntas = {
            "Selecciona los lenguajes usados en Android:",
            "Selecciona los sistemas operativos móviles:",
            "Selecciona los IDE para desarrollo Android:"
    };

    private String[][] opciones = {
            {"Java", "Python", "Kotlin", "C++"},
            {"Android", "iOS", "Windows", "Linux"},
            {"Android Studio", "Visual Studio", "Eclipse", "NetBeans"}
    };

    private List<List<String>> respuestasCorrectas = Arrays.asList(
            Arrays.asList("Java", "Kotlin"),
            Arrays.asList("Android", "iOS"),
            Arrays.asList("Android Studio", "Eclipse")
    );

    private List<CheckBox> checkBoxes = new ArrayList<>();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public CheckButton() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CheckButton.
     */
    // TODO: Rename and change types and number of parameters
    public static CheckButton newInstance(String param1, String param2) {
        CheckButton fragment = new CheckButton();
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
        View view = inflater.inflate(R.layout.fragment_check_button, container, false);

        tvPregunta = view.findViewById(R.id.tvPregunta);
        checkboxContainer = view.findViewById(R.id.checkboxContainer);
        btnVerificar = view.findViewById(R.id.btnVerificar);
        btnSiguiente = view.findViewById(R.id.btnSiguiente);

        mostrarPregunta();

        btnVerificar.setOnClickListener(v -> {
            List<String> seleccionadas = new ArrayList<>();
            for (CheckBox cb : checkBoxes) {
                if (cb.isChecked()) seleccionadas.add(cb.getText().toString());
            }

            if (seleccionadas.isEmpty()) {
                Toast.makeText(requireContext(), "Selecciona al menos una opción", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> correctas = respuestasCorrectas.get(indicePregunta);

            if (seleccionadas.containsAll(correctas) && correctas.containsAll(seleccionadas)) {
                Toast.makeText(requireContext(), "✅ Correcto", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), " Incorrecto. Correctas: " + correctas, Toast.LENGTH_LONG).show();
            }

            btnSiguiente.setEnabled(true); // habilitar siguiente
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
        checkboxContainer.removeAllViews();
        checkBoxes.clear();

        for (String opcion : opciones[indicePregunta]) {
            CheckBox cb = new CheckBox(requireContext());
            cb.setText(opcion);
            checkboxContainer.addView(cb);
            checkBoxes.add(cb);
        }
    }
}