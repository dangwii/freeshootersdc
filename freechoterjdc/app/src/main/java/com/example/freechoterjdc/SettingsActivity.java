package com.example.freechoterjdc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Pantalla de Ajustes.
 * Permite resetear las puntuaciones acumuladas o borrar el perfil actual para registrar
 * un nuevo soldado de la patria. ¡Melo, sencillo y optimizado!
 */
public class SettingsActivity extends AppCompatActivity {

    private Button btnResetScores, btnChangeName, btnBackFromSettings;

    private static final String PREFS_NAME = "FreeShooterPrefs";
    private static final String KEY_PLAYER_NAME = "playerName";
    private static final String KEY_TOTAL_SCORE = "totalScore";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        btnResetScores = findViewById(R.id.btnResetScores);
        btnChangeName = findViewById(R.id.btnChangeName);
        btnBackFromSettings = findViewById(R.id.btnBackFromSettings);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Lógica de resetear puntuaciones fijos
        btnResetScores.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_TOTAL_SCORE, 0);
            editor.apply();
            Toast.makeText(this, "¡Puntuaciones reseteadas con éxito a 0!", Toast.LENGTH_SHORT).show();
        });

        // Lógica para cambiar nombre (Borra el nombre guardado y fuerza a re-registrarse)
        btnChangeName.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(KEY_PLAYER_NAME);
            editor.apply();

            Toast.makeText(this, "Nombre de perfil borrado. Redirigiendo al registro...", Toast.LENGTH_LONG).show();

            // Reiniciamos la app abriendo la pantalla de registro inicial y limpiando la pila
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Volver al menú de una
        btnBackFromSettings.setOnClickListener(v -> finish());
    }
}