package com.example.laterealmenu;

import android.os.Bundle;
import android.view.MenuItem;



import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;


public class MainActivity extends AppCompatActivity {
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    MaterialToolbar toolbar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(view -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

       navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
           @Override
           public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

               Fragment fragmentSeleccionado = null;
               int id = menuItem.getItemId();

               if (id == R.id.nav_inicio) {
                   fragmentSeleccionado = new Inicio();
               } else if (id == R.id.nav_calculadora) {
                   fragmentSeleccionado = new Calculadora();
               }else if (id == R.id.nav_spinner) {
                   fragmentSeleccionado = new spinner();
               }else if (id == R.id.nav_view) {
                   fragmentSeleccionado = new List_view();
               }else if (id == R.id.nav_) {
                   fragmentSeleccionado = new RaddioButton();
               }else if (id == R.id.checkbox) {
                   fragmentSeleccionado = new CheckButton();
               }else if (id == R.id.buttonLogin) {
                   fragmentSeleccionado = new Login();
               }

               if (fragmentSeleccionado != null) {
                   getSupportFragmentManager()
                           .beginTransaction()
                           .replace(R.id.contenedor, fragmentSeleccionado)
                           .commit();
               }

               drawerLayout.closeDrawers();
               return true;
           }
       });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}