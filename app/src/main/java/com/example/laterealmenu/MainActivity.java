package com.example.laterealmenu;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

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

        // Inicializar vistas con validación
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        // Validar que las vistas existen
        if (drawerLayout == null || navigationView == null || toolbar == null) {
            Toast.makeText(this, "Error: No se pudieron cargar los componentes", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Configurar toolbar
        //setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Configurar listener del navigation
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment = null;

            Log.d("MainActivity", "Item clickeado: " + id);

            if (id == R.id.nav_inicio) {
                fragment = new InicioFragment();
                Log.d("MainActivity", "Navegando a InicioFragment");
            } else if (id == R.id.nav_mis_plantas) {
                fragment = new MisPlantasFragment();
                Log.d("MainActivity", "Navegando a MisPlantasFragment");
            } else if (id == R.id.nav_agregar_planta) {
                fragment = new AgregarPlantaFragment();
                Log.d("MainActivity", "Navegando a AgregarPlantaFragment");
            } else if (id == R.id.nav_diagnostico_ia) {
                fragment = new DiagnosticoPlantaFragment();
                Log.d("MainActivity", "Navegando a DiagnosticoPlantaFragment");
            } else if (id == R.id.nav_mis_consultas) {
                fragment = new MisConsultasFragment();
                Log.d("MainActivity", "Navegando a MisConsultasFragment");
            } else if (id == R.id.nav_calendario) {
                fragment = new CalendarioFragment();
                Log.d("MainActivity", "Navegando a CalendarioFragment");
            } else if (id == R.id.nav_consejos) {
                fragment = new ConsejosFragment();
                Log.d("MainActivity", "Navegando a ConsejosFragment");
            } else if (id == R.id.nav_logout) {
                logoutUser();
                return true;
            }

            if (fragment != null) {
                loadFragment(fragment, id);
                Log.d("MainActivity", "Fragment cargado: " + fragment.getClass().getSimpleName());
            } else {
                Toast.makeText(this, "Funcionalidad en desarrollo", Toast.LENGTH_SHORT).show();
            }

            if (drawerLayout != null) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });

        // Iniciar sistema de notificaciones para plantas
        startPlantNotificationSystem();

        // Cargar fragment inicial
        if (savedInstanceState == null) {
            loadFragment(new InicioFragment(), R.id.nav_inicio);
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
        // Cancelar notificaciones al cerrar sesión
        PlantReminderReceiver.cancelDailyCheck(this);

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
    }

    private void startPlantNotificationSystem() {
        Log.d("MainActivity", "🌱 Iniciando sistema de notificaciones para plantas...");
/*
        // Verificar permisos (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                schedulePlantReminders();
            }
        } else {
            schedulePlantReminders();
        }*/
    }

    void schedulePlantReminders() {
        try {
            PlantReminderReceiver.scheduleDailyCheck(this);
            Log.d("MainActivity", "✅ Notificaciones programadas para plantas (9:00 AM diariamente)");

            // Verificar inmediatamente si hay plantas que necesitan atención
            new android.os.Handler().postDelayed(() -> {
                checkPlantsNow();
            }, 3000); // Esperar 3 segundos para que la app se inicialice completamente

        } catch (Exception e) {
            Log.e("MainActivity", "❌ Error programando notificaciones: " + e.getMessage());
            Toast.makeText(this, "Error al programar notificaciones", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                schedulePlantReminders();
                Toast.makeText(this, "🔔 Permisos de notificación concedidos", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "⚠️ Las notificaciones no funcionarán sin permisos",
                        Toast.LENGTH_LONG).show();
                Log.w("MainActivity", "Permisos de notificación denegados");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "🔄 MainActivity en primer plano");

        // Verificar si hay intent extras para redirigir a un fragment específico
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
                case "mis_plantas":
                    fragment = new MisPlantasFragment();
                    menuItemId = R.id.nav_mis_plantas;
                    break;
                case "mis_consultas":
                    fragment = new MisConsultasFragment();
                    menuItemId = R.id.nav_mis_consultas;
                    break;
                case "diagnostico":
                    fragment = new DiagnosticoPlantaFragment();
                    menuItemId = R.id.nav_diagnostico_ia;
                    break;
                case "agregar_planta":
                    fragment = new AgregarPlantaFragment();
                    menuItemId = R.id.nav_agregar_planta;
                    break;
            }

            if (fragment != null) {
                // Pasar el plant_id si existe
                if (plantId != null) {
                    Bundle args = new Bundle();
                    args.putString("plant_id", plantId);
                    fragment.setArguments(args);
                }

                loadFragment(fragment, menuItemId);
                // Limpiar los extras para no redirigir nuevamente
                intent.removeExtra("fragment");
                intent.removeExtra("plant_id");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity", "🔚 MainActivity destruida");
    }

    // ✅ CORREGIDO: Método público para que los fragments puedan probar notificaciones
    public void testPlantNotification(String plantName, String type) {
        NotificationHelper notificationHelper = new NotificationHelper(this);
        String title, message;

        if ("water".equals(type)) {
            title = "💧 Recordatorio de Riego";
            message = "Es hora de regar tu planta \"" + plantName + "\". No te olvides de darle agua hoy!";
            notificationHelper.sendPlantNotification(title, message, 9999, "test", "water");
            Toast.makeText(this, "💧 Probando notificación de riego", Toast.LENGTH_SHORT).show();
        } else if ("fert".equals(type)) {
            title = "🌱 Recordatorio de Fertilización";
            message = "Es hora de fertilizar tu planta \"" + plantName + "\". Tu planta te lo agradecerá!";
            notificationHelper.sendPlantNotification(title, message, 9998, "test", "fertilizer");
            Toast.makeText(this, "🌱 Probando notificación de fertilización", Toast.LENGTH_SHORT).show();
        }

        Log.d("MainActivity", "🔔 Notificación de prueba enviada para: " + plantName);
    }

    // ✅ CORREGIDO: Método público para que los fragments puedan verificar plantas inmediatamente
    public void checkPlantsNow() {
        try {
            Intent intent = new Intent(this, PlantReminderReceiver.class);
            intent.setAction("ACTION_CHECK_PLANT_REMINDERS");
            sendBroadcast(intent);
            Toast.makeText(this, "🔍 Verificando plantas...", Toast.LENGTH_SHORT).show();
            Log.d("MainActivity", "🔍 Verificación manual de plantas iniciada");
        } catch (Exception e) {
            Log.e("MainActivity", "❌ Error en verificación manual: " + e.getMessage());
            Toast.makeText(this, "Error al verificar plantas", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ NUEVO: Método para activar/desactivar notificaciones
    public void togglePlantNotifications(boolean enable) {
        if (enable) {
            schedulePlantReminders();
            Toast.makeText(this, "🔔 Notificaciones activadas", Toast.LENGTH_SHORT).show();
        } else {
            PlantReminderReceiver.cancelDailyCheck(this);
            Toast.makeText(this, "🔕 Notificaciones desactivadas", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ NUEVO: Método para verificar el estado del sistema de notificaciones
    public void checkNotificationStatus() {
        boolean hasPermission = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }

        String status = "Estado notificaciones: " +
                (hasPermission ? "✅ Permisos OK" : "❌ Sin permisos") +
                " | Sistema: " +
                (isNotificationScheduled() ? "✅ Programado" : "❌ No programado");

        Log.d("MainActivity", status);
        Toast.makeText(this, status, Toast.LENGTH_LONG).show();
    }

    // ✅ NUEVO: Verificar si las notificaciones están programadas
    private boolean isNotificationScheduled() {
        try {
            // Verificar si existe el PendingIntent
            Intent intent = new Intent(this, PlantReminderReceiver.class);
            intent.setAction("ACTION_CHECK_PLANT_REMINDERS");

            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                    this,
                    1001,
                    intent,
                    android.app.PendingIntent.FLAG_NO_CREATE | android.app.PendingIntent.FLAG_IMMUTABLE
            );

            return pendingIntent != null;
        } catch (Exception e) {
            return false;
        }
    }

    // ✅ NUEVO: Método para forzar una verificación inmediata (útil para debugging)
    public void forceImmediateCheck() {
        Log.d("MainActivity", "🚀 Forzando verificación inmediata de plantas...");

        // Ejecutar en un hilo separado para no bloquear la UI
        new Thread(() -> {
            try {
                PlantReminderReceiver receiver = new PlantReminderReceiver();
                receiver.onReceive(this, new Intent("ACTION_CHECK_PLANT_REMINDERS"));

                runOnUiThread(() -> {
                    Toast.makeText(this, "✅ Verificación forzada completada", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "❌ Error en verificación forzada", Toast.LENGTH_SHORT).show();
                });
                Log.e("MainActivity", "Error en verificación forzada: " + e.getMessage());
            }
        }).start();
    }
}