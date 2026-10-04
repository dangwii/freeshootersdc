package com.example.freechoterjdc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

/**
 * ¡Qué más, parcero! Esta es la MainActivity, el punto de partida de nuestro juego.
 * Aquí manejamos el registro inicial del jugador. Como somos optimizados, si el jugador
 * ya se registró antes, lo mandamos directo al Menú Principal sin aburrirlo.
 */
public class MainActivity extends AppCompatActivity {

    // Declaramos los componentes de la interfaz de registro
    private TextInputEditText etPlayerName;
    private Button btnAccept, btnCancel;

    // Nombre de nuestro archivo de SharedPreferences para persistir datos locales
    private static final String PREFS_NAME = "FreeShooterPrefs";
    private static final String KEY_PLAYER_NAME = "playerName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verificamos si el jugador ya se registró anteriormente
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedName = prefs.getString(KEY_PLAYER_NAME, null);

        if (savedName != null && !savedName.trim().isEmpty()) {
            // ¡Ya está registrado! Vamos de una para el menú principal sin rodeos
            goToMainMenu();
            return;
        }

        // Si no está registrado, le cargamos la hermosa pantalla de registro
        setContentView(R.layout.activity_main);

        // Vinculamos las variables con los componentes visuales del XML
        etPlayerName = findViewById(R.id.etPlayerName);
        btnAccept = findViewById(R.id.btnAccept);
        btnCancel = findViewById(R.id.btnCancel);

        // Configuración de la lógica para el botón "Aceptar"
        btnAccept.setOnClickListener(v -> {
            String name = etPlayerName.getText() != null ? etPlayerName.getText().toString().trim() : "";

            if (name.isEmpty()) {
                // Si el usuario deja el campo vacío, le avisamos con estilo
                Toast.makeText(MainActivity.this, "¡Oye! Escribe tu nombre para iniciar la batalla", Toast.LENGTH_SHORT).show();
            } else {
                // Guardamos el nombre en SharedPreferences para que quede grabado permanentemente
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_PLAYER_NAME, name);
                editor.apply();

                // Le damos una bienvenida melita
                Toast.makeText(MainActivity.this, "¡Registro Exitoso, Soldado " + name + "!", Toast.LENGTH_SHORT).show();

                // Avanzamos al Menú Principal
                goToMainMenu();
            }
        });

        // Configuración para el botón "Cancelar"
        btnCancel.setOnClickListener(v -> {
            // Si le da cancelar, limpiamos el campo o salimos de la app de forma segura
            etPlayerName.setText("");
            Toast.makeText(MainActivity.this, "Operación cancelada. ¡Vuelve pronto!", Toast.LENGTH_SHORT).show();
            finish(); // Cierra la app de una
        });
    }

    /**
     * Método auxiliar para navegar al menú principal de forma limpia.
     */
    private void goToMainMenu() {
        Intent intent = new Intent(MainActivity.this, MainMenuActivity.class);
        startActivity(intent);
        finish(); // Finalizamos esta actividad para que el jugador no pueda regresar al registro dándole al botón "atrás"
    }
}