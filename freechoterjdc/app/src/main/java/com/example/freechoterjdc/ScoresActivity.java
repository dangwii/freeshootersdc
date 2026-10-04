package com.example.freechoterjdc;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Pantalla de Puntuaciones Acumuladas.
 * Lee desde SharedPreferences los puntos totales que el jugador ha ganado en su historia.
 */
public class ScoresActivity extends AppCompatActivity {

    private TextView tvRegisteredPlayer, tvTotalScore;
    private Button btnBackFromScores;

    private static final String PREFS_NAME = "FreeShooterPrefs";
    private static final String KEY_PLAYER_NAME = "playerName";
    private static final String KEY_TOTAL_SCORE = "totalScore";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scores);

        // Conectamos variables con vistas
        tvRegisteredPlayer = findViewById(R.id.tvRegisteredPlayer);
        tvTotalScore = findViewById(R.id.tvTotalScore);
        btnBackFromScores = findViewById(R.id.btnBackFromScores);

        // Cargamos las preferencias guardadas localmente
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String name = prefs.getString(KEY_PLAYER_NAME, "Sin nombre");
        int totalScore = prefs.getInt(KEY_TOTAL_SCORE, 0);

        // Mostramos la info melita en pantalla
        tvRegisteredPlayer.setText(name);
        tvTotalScore.setText(totalScore + " Puntos");

        // Botón para retroceder tranquilamente
        btnBackFromScores.setOnClickListener(v -> finish());
    }
}