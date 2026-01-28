package com.example.laterealmenu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class Login extends Fragment {

    private EditText editTextEmail, editTextPassword;
    private Button buttonlogin;
    private FirebaseAuth mAuth;

    public Login() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflar la vista del fragmento
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Referencias a los elementos
        editTextEmail = view.findViewById(R.id.editTextTextEmailAddress);
        editTextPassword = view.findViewById(R.id.editTextTextPassword);
        buttonlogin = view.findViewById(R.id.buttonLogin);
        mAuth = FirebaseAuth.getInstance();

        // Listener del botón
        buttonlogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String pass = editTextPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(getContext(), "Campos vacíos", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();

                            // 🔹 Crear instancia del fragment destino
                            Bienvenida fragmentBienvenida = new Bienvenida();

                            // 🔹 Reemplazar el fragment actual con el de inicio
                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.contenedor,fragmentBienvenida)
                                    .addToBackStack(null) // permite volver atrás si se desea
                                    .commit();

                        } else {
                            Toast.makeText(getContext(), "Error al iniciar sesión", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        return view;
    }
}
