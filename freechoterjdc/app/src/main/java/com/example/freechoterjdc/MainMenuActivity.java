package com.example.freechoterjdc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * ¡Bienvenido al Menú Principal, parcero!
 * Aquí el jugador decide qué camino tomar. Mostramos su nombre con orgullo
 * y le damos acceso a Jugar, Puntuación, Ajustes y Salir. Todo bien fluido.
 */
public class MainMenuActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnPlay, btnScores, btnSettings, btnExit;

    private static final String PREFS_NAME = "FreeShooterPrefs";
    private static final String KEY_PLAYER_NAME = "playerName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // Vinculamos vistas con código Java
        tvWelcome = findViewById(R.id.tvWelcome);
        btnPlay = findViewById(R.id.btnPlay);
        btnScores = findViewById(R.id.btnScores);
        btnSettings = findViewById(R.id.btnSettings);
        btnExit = findViewById(R.id.btnExit);

        // Leemos el nombre guardado para personalizar el saludo melo
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String playerName = prefs.getString(KEY_PLAYER_NAME, "Soldado");
        tvWelcome.setText("¡Bienvenido, " + playerName + "!");

        // Acción al presionar JUGAR: Nos manda a la pantalla de selección de modos
        btnPlay.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, GameModesActivity.class);
            startActivity(intent);
        });

        // Acción al presionar PUNTUACIÓN: Muestra el récord acumulado
        btnScores.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, ScoresActivity.class);
            startActivity(intent);
        });

        // Acción al presionar AJUSTES: Permite gestionar el perfil o reiniciar puntos
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Acción al presionar SALIR: Cierra la aplicación elegantemente
        btnExit.setOnClickListener(v -> {
            Toast.makeText(MainMenuActivity.this, "¡Nos vemos en el campo de batalla, recluta!", Toast.LENGTH_SHORT).show();
            finishAffinity(); // Cierra todas las actividades y sale por completo
        });
    }
}