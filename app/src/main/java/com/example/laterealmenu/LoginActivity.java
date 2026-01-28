package com.example.laterealmenu;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate iniciado");

        try {
            setContentView(R.layout.activity_login);
            Log.d(TAG, "setContentView exitoso");

            // Inicializar Firebase Auth
            mAuth = FirebaseAuth.getInstance();

            // Verificar si ya está autenticado
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                Log.d(TAG, "Usuario ya autenticado, redirigiendo a MainActivity");
                startMainActivity(currentUser);
                return;
            }

            initViews();
            setupClickListeners();

        } catch (Exception e) {
            Log.e(TAG, "ERROR en LoginActivity: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        tvRegister.setOnClickListener(v -> goToRegister());
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar progreso
        btnLogin.setEnabled(false);
        btnLogin.setText("Iniciando sesión...");

        Log.d(TAG, "Intentando login con Firebase");

        // Autenticación con Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Iniciar Sesión");

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "Login exitoso, usuario: " + user.getEmail());
                        Toast.makeText(LoginActivity.this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                        startMainActivity(user);
                    } else {
                        Log.e(TAG, "Error en login: " + task.getException().getMessage());
                        Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToRegister() {
        Log.d(TAG, "Redirigiendo a RegisterActivity");
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
        // No hacer finish() para que pueda volver atrás
    }

    private void startMainActivity(FirebaseUser user) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("user_email", user.getEmail());
        intent.putExtra("user_name", user.getDisplayName());
        startActivity(intent);
        finish();
    }

}