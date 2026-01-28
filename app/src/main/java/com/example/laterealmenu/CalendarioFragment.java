package com.example.laterealmenu;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CalendarioFragment extends Fragment {

    private static final String TAG = "CalendarioFragment";


    private static final String GROQ_API_KEY = "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";
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";


    private static final String MODEL = "llama-3.3-70b-versatile";

    private TextView tvMesSeleccionado;
    private GridView gridMeses;
    private LinearLayout layoutResultados;
    private TextView tvResultadoTemporada;
    private AutoCompleteTextView actvBuscarPlanta;
    private Button btnBuscarPlanta;
    private ProgressBar progressBar;

    private String[] meses = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    private Map<String, List<String>> plantasPorTemporada;
    private Map<String, String> temporadaPorPlanta;
    private OkHttpClient client;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendario, container, false);

        // Configurar cliente HTTP
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Log.d(TAG, "onCreateView: Iniciando calendario con Groq API - Modelo: " + MODEL);

        try {
            initViews(view);
            inicializarDatosPlantas();
            setupMesesGrid();
            setupBusqueda();
            Log.d(TAG, "onCreateView: Calendario configurado exitosamente");
        } catch (Exception e) {
            Log.e(TAG, "Error en onCreateView: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error al cargar el calendario", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void initViews(View view) {
        try {
            tvMesSeleccionado = view.findViewById(R.id.tvMesSeleccionado);
            gridMeses = view.findViewById(R.id.gridMeses);
            layoutResultados = view.findViewById(R.id.layoutResultados);
            tvResultadoTemporada = view.findViewById(R.id.tvResultadoTemporada);
            actvBuscarPlanta = view.findViewById(R.id.actvBuscarPlanta);
            btnBuscarPlanta = view.findViewById(R.id.btnBuscarPlanta);
            progressBar = view.findViewById(R.id.progressBar);

            // Verificar que todas las vistas se encontraron
            Log.d(TAG, "Vistas encontradas:");
            Log.d(TAG, " - tvMesSeleccionado: " + (tvMesSeleccionado != null));
            Log.d(TAG, " - gridMeses: " + (gridMeses != null));
            Log.d(TAG, " - layoutResultados: " + (layoutResultados != null));
            Log.d(TAG, " - tvResultadoTemporada: " + (tvResultadoTemporada != null));
            Log.d(TAG, " - actvBuscarPlanta: " + (actvBuscarPlanta != null));
            Log.d(TAG, " - btnBuscarPlanta: " + (btnBuscarPlanta != null));
            Log.d(TAG, " - progressBar: " + (progressBar != null));

            if (progressBar == null) {
                Log.e(TAG, "❌ CRÍTICO: progressBar es NULL - Revisa el layout fragment_calendario.xml");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error en initViews: " + e.getMessage(), e);
        }
    }

    private void inicializarDatosPlantas() {
        plantasPorTemporada = new HashMap<>();
        temporadaPorPlanta = new HashMap<>();

        // Plantas de PRIMAVERA (Marzo, Abril, Mayo)
        List<String> primavera = Arrays.asList(
                "Tomate", "Pimiento", "Berenjena", "Calabacín", "Pepino",
                "Sandía", "Melón", "Maíz", "Girasol", "Albahaca",
                "Perejil", "Cilantro", "Zanahoria", "Rábano", "Lechuga",
                "Espinaca", "Fresa", "Frambuesa", "Lavanda", "Rosa"
        );
        plantasPorTemporada.put("PRIMAVERA", primavera);

        // Plantas de VERANO (Junio, Julio, Agosto)
        List<String> verano = Arrays.asList(
                "Tomate", "Pimiento", "Berenjena", "Calabacín", "Pepino",
                "Sandía", "Melón", "Maíz", "Girasol", "Albahaca",
                "Menta", "Romero", "Tomillo", "Judía", "Calabaza",
                "Cebolla", "Ajo", "Zinnia", "Girasol", "Hibisco"
        );
        plantasPorTemporada.put("VERANO", verano);

        // Plantas de OTOÑO (Septiembre, Octubre, Noviembre)
        List<String> otono = Arrays.asList(
                "Zanahoria", "Rábano", "Espinaca", "Lechuga", "Col",
                "Brócoli", "Coliflor", "Acelga", "Remolacha", "Cebolla",
                "Ajo", "Puerro", "Haba", "Guisante", "Rúcula",
                "Crisantemo", "Pensamiento", "Violeta", "Margarita", "Caléndula"
        );
        plantasPorTemporada.put("OTOÑO", otono);

        // Plantas de INVIERNO (Diciembre, Enero, Febrero)
        List<String> invierno = Arrays.asList(
                "Ajo", "Cebolla", "Puerro", "Espinaca", "Acelga",
                "Col", "Brócoli", "Coliflor", "Haba", "Guisante",
                "Rúcula", "Canónigo", "Escarola", "Clemátide", "Jazmín",
                "Camelia", "Narciso", "Tulipán", "Ciclamen", "Brezo"
        );
        plantasPorTemporada.put("INVIERNO", invierno);

        // Llenar el mapa de búsqueda individual
        for (Map.Entry<String, List<String>> entry : plantasPorTemporada.entrySet()) {
            String temporada = entry.getKey();
            for (String planta : entry.getValue()) {
                temporadaPorPlanta.put(planta.toLowerCase(), temporada);
            }
        }
    }

    private void setupMesesGrid() {
        // Crear un adapter personalizado para los meses
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                meses
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;

                // Aplicar estilos más fuertes a cada item del GridView
                textView.setTextSize(16f);
                textView.setTextColor(Color.WHITE);
                textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
                textView.setGravity(Gravity.CENTER);

                // Fondo con color más fuerte
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    textView.setBackgroundColor(getResources().getColor(R.color.strong_green, null));
                } else {
                    textView.setBackgroundColor(getResources().getColor(R.color.strong_green));
                }

                // Padding y margen
                textView.setPadding(16, 24, 16, 24);

                return view;
            }
        };

        gridMeses.setAdapter(adapter);

        gridMeses.setOnItemClickListener((parent, view, position, id) -> {
            String mesSeleccionado = meses[position];
            tvMesSeleccionado.setText("Mes seleccionado: " + mesSeleccionado);
            mostrarPlantasPorMes(mesSeleccionado);
        });
    }

    private void setupBusqueda() {
        // Crear lista de todas las plantas para el autocomplete
        List<String> todasLasPlantas = new ArrayList<>();
        for (List<String> plantas : plantasPorTemporada.values()) {
            todasLasPlantas.addAll(plantas);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                todasLasPlantas
        );
        actvBuscarPlanta.setAdapter(adapter);
        actvBuscarPlanta.setThreshold(1);

        btnBuscarPlanta.setOnClickListener(v -> {
            try {
                buscarPlantaEspecifica();
            } catch (Exception e) {
                Log.e(TAG, "Error en búsqueda: " + e.getMessage(), e);
                Toast.makeText(requireContext(), "Error en la búsqueda", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarPlantasPorMes(String mes) {
        try {
            String temporada = obtenerTemporadaPorMes(mes);
            List<String> plantas = plantasPorTemporada.get(temporada);

            if (plantas != null && !plantas.isEmpty()) {
                StringBuilder resultado = new StringBuilder();
                resultado.append("🌱 Temporada: ").append(temporada).append("\n\n");
                resultado.append("📋 Plantas recomendadas para ").append(mes).append(":\n\n");

                // Mostrar plantas en columnas de 2
                for (int i = 0; i < plantas.size(); i++) {
                    resultado.append("• ").append(plantas.get(i));
                    if ((i + 1) % 2 == 0) {
                        resultado.append("\n");
                    } else {
                        resultado.append("    ");
                    }
                }

                resultado.append("\n\n💡 Consejos para ").append(temporada.toLowerCase()).append(":\n");
                resultado.append(obtenerConsejosTemporada(temporada));

                if (tvResultadoTemporada != null) {
                    tvResultadoTemporada.setText(resultado.toString());
                }
                if (layoutResultados != null) {
                    layoutResultados.setVisibility(View.VISIBLE);
                }

                Log.d(TAG, "✅ Mostrando " + plantas.size() + " plantas para " + temporada);
            } else {
                if (tvResultadoTemporada != null) {
                    tvResultadoTemporada.setText("No hay plantas registradas para esta temporada.");
                }
                if (layoutResultados != null) {
                    layoutResultados.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error en mostrarPlantasPorMes: " + e.getMessage(), e);
            if (tvResultadoTemporada != null) {
                tvResultadoTemporada.setText("Error al cargar las plantas para este mes.");
            }
            if (layoutResultados != null) {
                layoutResultados.setVisibility(View.VISIBLE);
            }
        }
    }

    private void buscarPlantaEspecifica() {
        String nombrePlanta = actvBuscarPlanta.getText().toString().trim();

        if (nombrePlanta.isEmpty()) {
            Toast.makeText(requireContext(), "⚠️ Escribe el nombre de una planta", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar progreso
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        if (layoutResultados != null) {
            layoutResultados.setVisibility(View.GONE);
        }


        if (!isNetworkAvailable()) {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            mostrarInformacionLocal(nombrePlanta);
            Toast.makeText(requireContext(), "❌ Sin conexión a internet", Toast.LENGTH_SHORT).show();
            return;
        }


        if (GROQ_API_KEY.equals("gsk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx") || GROQ_API_KEY.isEmpty()) {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            mostrarInformacionLocal(nombrePlanta);
            Toast.makeText(requireContext(), "⚠️ Configura tu API Key de Groq", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "🔍 Iniciando búsqueda con Groq para: " + nombrePlanta + " - Modelo: " + MODEL);


        consultarGroq(nombrePlanta);
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "Error verificando conexión: " + e.getMessage());
            return false;
        }
    }

    private void consultarGroq(String nombrePlanta) {
        try {
            // Prompt optimizado para botánica
            String prompt = "Eres un experto en botánica y agricultura. Proporciona información CONCISA sobre la planta: " + nombrePlanta +
                    "\n\nResponde EN ESPAÑOL con este formato EXACTO:\n" +
                    "🌿 " + nombrePlanta.toUpperCase() + "\n\n" +
                    "📖 DESCRIPCIÓN:\n[Descripción breve de 1-2 líneas]\n\n" +
                    "📅 TEMPORADA IDEAL:\n[Indica cuándo sembrar y cultivar, específico para clima templado]\n\n" +
                    "💧 CUIDADOS:\n• Riego: [frecuencia y cantidad]\n• Luz: [horas de sol necesarias]\n• Suelo: [tipo preferido]\n\n" +
                    "🔧 RECOMENDACIONES:\n[2-3 consejos prácticos de cultivo]\n\n" +
                    "Sé preciso, usa emojis y responde SOLO con la información solicitada. No añadas introducción ni conclusión.";

            // JSON para Groq API (formato OpenAI compatible)
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", MODEL);
            requestBody.addProperty("temperature", 0.7);
            requestBody.addProperty("max_tokens", 800);

            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            messages.add(message);

            requestBody.add("messages", messages);

            String requestBodyString = requestBody.toString();
            Log.d(TAG, "📦 Request Body Groq: " + requestBodyString);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestBodyString
            );

            Request request = new Request.Builder()
                    .url(GROQ_API_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + GROQ_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Log.d(TAG, "🚀 Enviando solicitud a Groq con modelo: " + MODEL);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "❌ Error de conexión con Groq: " + e.getMessage());
                    e.printStackTrace();

                    requireActivity().runOnUiThread(() -> {
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        mostrarInformacionLocal(nombrePlanta);
                        Toast.makeText(requireContext(), "❌ Error de conexión", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body().string();
                    int responseCode = response.code();

                    Log.d(TAG, "📥 Respuesta Groq - Código: " + responseCode);

                    requireActivity().runOnUiThread(() -> {
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }

                        if (response.isSuccessful()) {
                            try {
                                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

                                if (jsonResponse.has("choices") && jsonResponse.getAsJsonArray("choices").size() > 0) {
                                    JsonObject choice = jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject();

                                    if (choice.has("message")) {
                                        JsonObject message = choice.getAsJsonObject("message");
                                        String texto = message.get("content").getAsString();

                                        Log.d(TAG, "✅ Respuesta recibida de Groq");

                                        // Mostrar resultado
                                        if (tvResultadoTemporada != null) {
                                            tvResultadoTemporada.setText(texto);
                                        }
                                        if (layoutResultados != null) {
                                            layoutResultados.setVisibility(View.VISIBLE);
                                        }
                                        Toast.makeText(requireContext(), "✅ Información obtenida", Toast.LENGTH_SHORT).show();
                                    } else {
                                        throw new Exception("No hay 'message' en la respuesta");
                                    }
                                } else {
                                    throw new Exception("No hay 'choices' en la respuesta");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "❌ Error procesando respuesta Groq: " + e.getMessage());
                                e.printStackTrace();
                                mostrarInformacionLocal(nombrePlanta);
                                Toast.makeText(requireContext(), "⚠️ Mostrando información local", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "❌ Error HTTP Groq: " + responseCode + " - " + responseBody);
                            mostrarInformacionLocal(nombrePlanta);

                            String errorMsg = "Error del servidor";
                            try {
                                JsonObject errorJson = JsonParser.parseString(responseBody).getAsJsonObject();
                                if (errorJson.has("error") && errorJson.getAsJsonObject("error").has("message")) {
                                    errorMsg = errorJson.getAsJsonObject("error").get("message").getAsString();

                                    // Si es error de modelo descontinuado, sugerir cambio
                                    if (errorMsg.contains("decommissioned") || errorMsg.contains("not found")) {
                                        errorMsg = "Modelo no disponible. Cambia a: llama-3.3-70b-versatile o mixtral-8x7b-32768";
                                    }
                                }
                            } catch (Exception e) {
                                // Ignorar si no se puede parsear
                            }

                            if (responseCode == 429) {
                                errorMsg = "Límite de solicitudes alcanzado. Prueba más tarde.";
                            } else if (responseCode == 401) {
                                errorMsg = "API Key inválida. Configura una clave válida en Groq Console.";
                            } else if (responseCode == 404) {
                                errorMsg = "Modelo no encontrado. Usa: llama-3.3-70b-versatile";
                            }

                            Toast.makeText(requireContext(), "⚠️ " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ Error creando solicitud Groq: " + e.getMessage());
            e.printStackTrace();

            requireActivity().runOnUiThread(() -> {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                mostrarInformacionLocal(nombrePlanta);
                Toast.makeText(requireContext(), "❌ Error en la solicitud", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void mostrarInformacionLocal(String nombrePlanta) {
        String temporada = temporadaPorPlanta.get(nombrePlanta.toLowerCase());

        if (temporada != null) {
            String mesesTemporada = obtenerMesesPorTemporada(temporada);
            StringBuilder resultado = new StringBuilder();

            resultado.append("🌿 ").append(nombrePlanta.toUpperCase()).append("\n\n");
            resultado.append("📅 Temporada ideal: ").append(temporada).append("\n");
            resultado.append("🗓️ Meses recomendados: ").append(mesesTemporada).append("\n\n");
            resultado.append("💡 Consejos de cultivo:\n");
            resultado.append(obtenerConsejosPlantaEspecifica(nombrePlanta));
            resultado.append("\n\n⚠️ Para información más detallada:\n");
            resultado.append("1. Obtén API Key gratis en: console.groq.com\n");
            resultado.append("2. Reemplaza GROQ_API_KEY en el código");

            if (tvResultadoTemporada != null) {
                tvResultadoTemporada.setText(resultado.toString());
            }
            if (layoutResultados != null) {
                layoutResultados.setVisibility(View.VISIBLE);
            }
        } else {
            String mensaje = "❌ No se encontró información local para: " + nombrePlanta +
                    "\n\n💡 Prueba con nombres comunes:\n• Tomate\n• Lechuga\n• Rosa\n• Albahaca\n• Zanahoria" +
                    "\n\n⚠️ Para información detallada:\n1. Ve a: console.groq.com\n2. Regístrate y crea API Key\n3. Reemplaza en el código";

            if (tvResultadoTemporada != null) {
                tvResultadoTemporada.setText(mensaje);
            }
            if (layoutResultados != null) {
                layoutResultados.setVisibility(View.VISIBLE);
            }
        }
    }

    private String obtenerTemporadaPorMes(String mes) {
        switch (mes.toLowerCase()) {
            case "diciembre":
            case "enero":
            case "febrero":
                return "INVIERNO";
            case "marzo":
            case "abril":
            case "mayo":
                return "PRIMAVERA";
            case "junio":
            case "julio":
            case "agosto":
                return "VERANO";
            case "septiembre":
            case "octubre":
            case "noviembre":
                return "OTOÑO";
            default:
                return "GENERAL";
        }
    }

    private String obtenerMesesPorTemporada(String temporada) {
        switch (temporada) {
            case "PRIMAVERA": return "Marzo, Abril, Mayo";
            case "VERANO": return "Junio, Julio, Agosto";
            case "OTOÑO": return "Septiembre, Octubre, Noviembre";
            case "INVIERNO": return "Diciembre, Enero, Febrero";
            default: return "Todo el año";
        }
    }

    private String obtenerConsejosTemporada(String temporada) {
        switch (temporada) {
            case "PRIMAVERA":
                return "• Prepara la tierra con abono orgánico\n• Inicia siembras en semillero\n• Controla plagas tempranas\n• Riega regularmente según necesidades";
            case "VERANO":
                return "• Riega temprano por la mañana\n• Protege del sol intenso\n• Controla malas hierbas\n• Cosecha regularmente";
            case "OTOÑO":
                return "• Prepara para el invierno\n• Siembra cultivos de raíz\n• Reduce riegos progresivamente\n• Protege de primeras heladas";
            case "INVIERNO":
                return "• Protege del frío intenso\n• Reduce riegos al mínimo\n• Planifica la próxima temporada\n• Mantén invernaderos ventilados";
            default:
                return "• Adapta cuidados según clima local\n• Observa necesidades específicas\n• Mantén registro de crecimiento";
        }
    }

    private String obtenerConsejosPlantaEspecifica(String planta) {
        String plantaLower = planta.toLowerCase();

        if (plantaLower.contains("tomate") || plantaLower.contains("pimiento") || plantaLower.contains("berenjena")) {
            return "• Necesita 6-8 horas de sol directo\n• Riego regular sin encharcar\n• Tutorar para mejor crecimiento\n• Fertilizar cada 15 días";
        } else if (plantaLower.contains("lechuga") || plantaLower.contains("espinaca") || plantaLower.contains("acelga")) {
            return "• Sol parcial o sombra ligera\n• Riego constante pero moderado\n• Cosecha hojas externas primero\n• Evita el calor extremo";
        } else if (plantaLower.contains("zanahoria") || plantaLower.contains("rábano") || plantaLower.contains("remolacha")) {
            return "• Suelo suelto y sin piedras\n• Riego profundo pero infrecuente\n• Aclareo de plántulas necesario\n• Cosecha cuando raíz esté formada";
        } else if (plantaLower.contains("rosa") || plantaLower.contains("lavanda") || plantaLower.contains("girasol")) {
            return "• Sol directo preferido\n• Riego profundo ocasional\n• Poda de formación importante\n• Control de plagas regular";
        } else if (plantaLower.contains("albahaca") || plantaLower.contains("menta") || plantaLower.contains("romero")) {
            return "• Sol moderado a pleno\n• Riego cuando suelo se seque\n• Poda frecuente para espesar\n• Cosecha antes de floración";
        } else {
            return "• Observa necesidades específicas\n• Adapta riego al clima local\n• Controla plagas regularmente\n• Fertiliza según crecimiento";
        }
    }
}