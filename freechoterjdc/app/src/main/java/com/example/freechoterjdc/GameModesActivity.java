package com.example.freechoterjdc;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Pantalla para elegir el Modo de Juego.
 * Ofrecemos el Modo Individual (1 Jugador) para luchar contra soldados españoles,
 * el Modo Multijugador para retar a un amigo en tiempo real, o el botón Retroceder.
 */
public class GameModesActivity extends AppCompatActivity {

    private Button btnSinglePlayer, btnMultiplayer, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_modes);

        // Vinculamos los botones visuales
        btnSinglePlayer = findViewById(R.id.btnSinglePlayer);
        btnMultiplayer = findViewById(R.id.btnMultiplayer);
        btnBack = findViewById(R.id.btnBack);

        // Al presionar 1 Jugador vamos a la simulación AR con la cámara
        btnSinglePlayer.setOnClickListener(v -> {
            Intent intent = new Intent(GameModesActivity.this, SinglePlayerGameActivity.class);
            startActivity(intent);
        });

        // Al presionar Multijugador vamos a la simulación de duelo
        btnMultiplayer.setOnClickListener(v -> {
            Intent intent = new Intent(GameModesActivity.this, MultiplayerGameActivity.class);
            startActivity(intent);
        });

        // Al presionar Retroceder volvemos al menú principal de una
        btnBack.setOnClickListener(v -> {
            finish(); // Cierra esta actividad y regresa a la anterior (MainMenuActivity)
        });
    }
}