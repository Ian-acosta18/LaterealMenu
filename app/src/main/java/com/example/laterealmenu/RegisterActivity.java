package com.example.laterealmenu;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etApellido, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etNombre = findViewById(R.id.etNombre);
        etApellido = findViewById(R.id.etApellido);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> goToLogin());
    }

    // En RegisterActivity.java - actualizar el método registerUser
    private void registerUser() {
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validaciones mejoradas
        if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar que nombre y apellido solo contengan letras
        if (!nombre.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+")) {
            Toast.makeText(this, "El nombre solo puede contener letras", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!apellido.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+")) {
            Toast.makeText(this, "El apellido solo puede contener letras", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar email
        if (!isValidEmail(email)) {
            Toast.makeText(this, "Ingresa un email válido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar contraseña con requisitos
        if (!isValidPassword(password)) {
            Toast.makeText(this, "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número", Toast.LENGTH_LONG).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar progreso
        btnRegister.setEnabled(false);
        btnRegister.setText("Creando cuenta...");

        // Crear usuario en Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        guardarUsuarioEnFirestore(user, nombre, apellido, email);
                    } else {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Registrarse");
                        Toast.makeText(RegisterActivity.this,
                                "Error al registrar: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Método para validar contraseña
    private boolean isValidPassword(String password) {
        // Al menos 8 caracteres, una mayúscula, una minúscula y un número
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        return password.matches(passwordPattern);
    }
    private void guardarUsuarioEnFirestore(FirebaseUser user, String nombre, String apellido, String email) {
        if (user == null) {
            btnRegister.setEnabled(true);
            btnRegister.setText("Registrarse");
            Toast.makeText(this, "Error: Usuario no creado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear objeto usuario para Firestore
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("uid", user.getUid());
        usuario.put("nombre", nombre);
        usuario.put("apellido", apellido);
        usuario.put("email", email);
        usuario.put("fechaRegistro", com.google.firebase.Timestamp.now());
        usuario.put("tipo", "usuario");
        usuario.put("plantasCount", 0);
        usuario.put("nivel", "principiante");

        // Guardar en Firestore
        db.collection("usuarios")
                .document(user.getUid()) // Usar el UID como ID del documento
                .set(usuario)
                .addOnSuccessListener(aVoid -> {
                    // Éxito: usuario guardado en Firestore
                    Toast.makeText(RegisterActivity.this, "¡Cuenta creada exitosamente!", Toast.LENGTH_SHORT).show();

                    // Opcional: enviar verificación de email
                    user.sendEmailVerification()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(RegisterActivity.this,
                                            "Se envió un email de verificación",
                                            Toast.LENGTH_LONG).show();
                                }
                            });

                    // Ir a MainActivity
                    startMainActivity(user, nombre);
                })
                .addOnFailureListener(e -> {
                    // Error en Firestore
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Registrarse");
                    Toast.makeText(RegisterActivity.this,
                            "Error guardando datos: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void startMainActivity(FirebaseUser user, String nombre) {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.putExtra("user_email", user.getEmail());
        intent.putExtra("user_name", nombre);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}