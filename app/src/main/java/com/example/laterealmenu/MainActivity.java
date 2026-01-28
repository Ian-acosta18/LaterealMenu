package com.example.laterealmenu;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    // Constante de clave VIP
    private static final String CLAVE_ESPECIAL_VIP = "AGRO-VIP";

    // Instancias de Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inicializar vistas con validación
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        if (drawerLayout == null || navigationView == null || toolbar == null) {
            Toast.makeText(this, "Error: No se pudieron cargar los componentes", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        toolbar.setNavigationOnClickListener(view -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment = null;

            Log.d("MainActivity", "Item clickeado: " + id);

            if (id == R.id.nav_inicio) {
                fragment = new InicioFragment();
            } else if (id == R.id.nav_mis_plantas) {
                fragment = new MisPlantasFragment();
            } else if (id == R.id.nav_agregar_planta) {
                fragment = new AgregarPlantaFragment();
            } else if (id == R.id.nav_diagnostico_ia) {
                fragment = new DiagnosticoPlantaFragment();
            } else if (id == R.id.nav_mis_consultas) {
                fragment = new MisConsultasFragment();
            } else if (id == R.id.nav_calendario) {
                fragment = new CalendarioFragment();
            } else if (id == R.id.nav_consejos) {
                fragment = new ConsejosFragment();
            } else if (id == R.id.nav_vip_access) {
                // ✅ NUEVO: Verificar estado antes de mostrar diálogo
                verificarEstadoVip();
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_logout) {
                logoutUser();
                return true;
            }

            if (fragment != null) {
                loadFragment(fragment, id);
            }

            if (drawerLayout != null) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });

        startPlantNotificationSystem();

        if (savedInstanceState == null) {
            loadFragment(new InicioFragment(), R.id.nav_inicio);
        }
    }

    // ✅ NUEVO: Método inteligente para decidir qué diálogo mostrar
    private void verificarEstadoVip() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Debes iniciar sesión primero", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar un pequeño toast de carga
        Toast.makeText(this, "Verificando estado...", Toast.LENGTH_SHORT).show();

        db.collection("usuarios").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nivel = documentSnapshot.getString("nivel");

                        // Si ya es premium, ofrecemos salir
                        if ("premium".equalsIgnoreCase(nivel)) {
                            mostrarDialogoDesactivarVip();
                        } else {
                            // Si no es premium, ofrecemos entrar (pedir clave)
                            showVipAccessDialog();
                        }
                    } else {
                        showVipAccessDialog(); // Fallback por defecto
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MainActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show()
                );
    }

    // ✅ Diálogo para ingresar la clave (Entrar a VIP)
    private void showVipAccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Acceso VIP 🌟");
        builder.setMessage("Ingresa tu código especial para desbloquear funciones Premium ilimitadas.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        // Sin hint, como pediste
        builder.setView(input);

        builder.setPositiveButton("Canjear", (dialog, which) -> {
            String codigo = input.getText().toString().trim();
            if (codigo.equals(CLAVE_ESPECIAL_VIP)) {
                actualizarNivelUsuario("premium", "¡Felicidades! 🌟", "Has activado el modo Premium. Disfruta de escaneos ilimitados.");
            } else {
                Toast.makeText(MainActivity.this, "❌ Código incorrecto", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // ✅ NUEVO: Diálogo para salir de VIP
    private void mostrarDialogoDesactivarVip() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modo VIP Activo ⭐");
        builder.setMessage("Actualmente tienes acceso ilimitado a todas las funciones.\n\n¿Deseas desactivar el modo VIP y volver al plan gratuito (limitado)?");

        builder.setIcon(android.R.drawable.ic_dialog_info);

        builder.setPositiveButton("Desactivar VIP", (dialog, which) -> {
            actualizarNivelUsuario("principiante", "Modo VIP Desactivado", "Has vuelto al plan gratuito (3 escaneos máximos).");
        });

        builder.setNegativeButton("Mantener VIP", null);
        builder.show();
    }

    // ✅ Método genérico para cambiar el nivel en Firebase
    private void actualizarNivelUsuario(String nuevoNivel, String tituloAlerta, String mensajeAlerta) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            ProgressDialog pd = new ProgressDialog(this);
            pd.setMessage("Actualizando perfil...");
            pd.show();

            db.collection("usuarios").document(user.getUid())
                    .update("nivel", nuevoNivel)
                    .addOnSuccessListener(aVoid -> {
                        pd.dismiss();
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(tituloAlerta)
                                .setMessage(mensajeAlerta)
                                .setPositiveButton("Entendido", null)
                                .show();
                    })
                    .addOnFailureListener(e -> {
                        pd.dismiss();
                        Toast.makeText(MainActivity.this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    public void loadFragment(Fragment fragment, int menuItemId) {
        try {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.contenedor, fragment)
                    .addToBackStack(null)
                    .commit();

            if (navigationView != null) {
                navigationView.setCheckedItem(menuItemId);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar fragmento: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void logoutUser() {
        PlantReminderReceiver.cancelDailyCheck(this);
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
    }

    private void startPlantNotificationSystem() {
        Log.d("MainActivity", "🌱 Iniciando sistema de notificaciones...");
        schedulePlantReminders();
    }

    void schedulePlantReminders() {
        try {
            PlantReminderReceiver.scheduleDailyCheck(this);
            new android.os.Handler().postDelayed(this::checkPlantsNow, 3000);
        } catch (Exception e) {
            Log.e("MainActivity", "❌ Error notificaciones: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                schedulePlantReminders();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleIntentExtras();
    }

    private void handleIntentExtras() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("fragment")) {
            String fragmentToLoad = intent.getStringExtra("fragment");
            String plantId = intent.getStringExtra("plant_id");
            Fragment fragment = null;
            int menuItemId = R.id.nav_inicio;

            switch (fragmentToLoad) {
                case "mis_plantas": fragment = new MisPlantasFragment(); menuItemId = R.id.nav_mis_plantas; break;
                case "mis_consultas": fragment = new MisConsultasFragment(); menuItemId = R.id.nav_mis_consultas; break;
                case "diagnostico": fragment = new DiagnosticoPlantaFragment(); menuItemId = R.id.nav_diagnostico_ia; break;
                case "agregar_planta": fragment = new AgregarPlantaFragment(); menuItemId = R.id.nav_agregar_planta; break;
            }

            if (fragment != null) {
                if (plantId != null) {
                    Bundle args = new Bundle();
                    args.putString("plant_id", plantId);
                    fragment.setArguments(args);
                }
                loadFragment(fragment, menuItemId);
                intent.removeExtra("fragment");
                intent.removeExtra("plant_id");
            }
        }
    }

    public void checkPlantsNow() {
        try {
            Intent intent = new Intent(this, PlantReminderReceiver.class);
            intent.setAction("ACTION_CHECK_PLANT_REMINDERS");
            sendBroadcast(intent);
        } catch (Exception e) {
            Log.e("MainActivity", "Error check manual: " + e.getMessage());
        }
    }
}