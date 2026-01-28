package com.example.laterealmenu;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link spinner#newInstance} factory method to
 * create an instance of this fragment.
 */
public class spinner extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public spinner() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment spinner.
     */
    // TODO: Rename and change types and number of parameters
    public static spinner newInstance(String param1, String param2) {
        spinner fragment = new spinner();
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

        View view = inflater.inflate(R.layout.fragment_spinner, container, false);

        Spinner spinnerOperaciones = view.findViewById(R.id.spinnerOperaciones);
        Button verificar = view.findViewById(R.id.btnverificar);
        TextView resultado = view.findViewById(R.id.txtresultado);
        EditText edtnum1 = view.findViewById(R.id.edtnumero1);
        EditText edtnum2 = view.findViewById(R.id.edtnumero2);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.operaciones_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOperaciones.setAdapter(adapter);

        final String[] operacionSeleccionada = { "Suma" }; // valor por defecto

        spinnerOperaciones.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                operacionSeleccionada[0] = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        verificar.setOnClickListener(v -> {
            double num1 = Double.parseDouble(edtnum1.getText().toString());
            double num2 = Double.parseDouble(edtnum2.getText().toString());
            double total = 0;

            switch (operacionSeleccionada[0]) {
                case "Suma":
                    total = num1 + num2;
                    break;
                case "Resta":
                    total = num1 - num2;
                    break;
                case "Multiplicación":
                    total = num1 * num2;
                    break;
                case "División":
                    total = num1 / num2;
                    break;
            }

            resultado.setText("Resultado: " + total);
        });

        return view;
}
}