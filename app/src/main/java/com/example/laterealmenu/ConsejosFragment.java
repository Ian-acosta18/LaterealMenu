package com.example.laterealmenu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import java.util.Random;

public class ConsejosFragment extends Fragment {

    private TextView consejoText;
    private Button btnNuevoConsejo;
    private Random random = new Random();

    private String[] consejos = {
            "💧 Riega temprano en la mañana para evitar hongos y permitir que las plantas aprovechen el agua durante el día.",

            "🌱 Realiza rotación de cultivos para evitar el agotamiento del suelo y reducir plagas específicas.",

            "🐞 Atrae insectos benéficos como mariquitas y crisopas plantando flores como caléndulas y cilantro.",

            "✂️ Poda las hojas amarillas o enfermas regularmente para mantener la planta saludable y prevenir propagación.",

            "🌞 Conoce las necesidades de sol de cada planta: algunas prefieren sol directo, otras sombra parcial.",

            "🧪 Realiza análisis de suelo cada temporada para ajustar los nutrientes según las necesidades específicas.",

            "🛡️ Usa barreras físicas como mallas anti-insectos para proteger cultivos sin usar químicos.",

            "💚 Aplica acolchado (mulch) para conservar humedad, controlar malezas y mejorar el suelo.",

            "🔍 Revisa el envés de las hojas regularmente, donde muchas plagas ponen sus huevos.",

            "🌧️ Colecta agua de lluvia para riego - es gratis y mejor para las plantas que el agua clorada.",

            "🕒 Establece una rutina de cuidado: las plantas prosperan con atención consistente.",

            "🌼 Planta flores entre tus hortalizas para aumentar la biodiversidad y polinización.",

            "📝 Lleva un diario de jardinería para registrar siembras, problemas y soluciones que funcionan.",

            "🌡️ Protege las plantas sensibles durante olas de calor con mallas de sombreo.",

            "🔄 Limpia tus herramientas después de cada uso para prevenir propagación de enfermedades."
    };

    public ConsejosFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_consejos, container, false);

        consejoText = view.findViewById(R.id.consejoText);
        btnNuevoConsejo = view.findViewById(R.id.btnNuevoConsejo);

        mostrarConsejoAleatorio();

        btnNuevoConsejo.setOnClickListener(v -> mostrarConsejoAleatorio());

        return view;
    }

    private void mostrarConsejoAleatorio() {
        int indice = random.nextInt(consejos.length);
        consejoText.setText(consejos[indice]);
    }
}