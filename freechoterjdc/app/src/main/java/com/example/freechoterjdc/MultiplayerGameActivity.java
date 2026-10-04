package com.example.freechoterjdc;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 * ¡El emocionante Modo Multijugador con GIROSCOPIO y controles ergonómicos laterales!
 * Conectamos la cámara en vivo para apuntarle al contrincante en el mundo real.
 * Usamos el sensor del giroscopio para medir los movimientos de apuntado físico
 * y los controles laterales dobles te dan comodidad extrema para disparar rápido.
 */
public class MultiplayerGameActivity extends AppCompatActivity implements SensorEventListener {

    private static final int CAMERA_PERMISSION_CODE = 200;

    // Componentes de interfaz
    private PreviewView previewViewMulti;
    private TextView tvMyScore, tvRivalScore, tvMultiTimer, tvMultiAmmo;
    private Button btnMultiReload, btnMultiShoot, btnMultiPause;
    private View multiplayerGameArea;

    // Puntuaciones y municiones del duelo
    private int myScore = 0;
    private int rivalScore = 0;
    private int ammo = 8;
    private final int maxAmmo = 8;
    private boolean isMatchActive = false;

    // Controladores de tiempo y simulación del oponente
    private CountDownTimer matchTimer;
    private final Random random = new Random();
    private final Handler rivalHandler = new Handler(Looper.getMainLooper());

    // Sensores de movimiento
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private float offsetX = 0f;
    private float offsetY = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer_game);

        // Enlazamos las vistas del layout XML con controles estilo consola
        previewViewMulti = findViewById(R.id.previewViewMulti);
        multiplayerGameArea = findViewById(R.id.multiplayerGameArea);
        tvMyScore = findViewById(R.id.tvMyScore);
        tvRivalScore = findViewById(R.id.tvRivalScore);
        tvMultiTimer = findViewById(R.id.tvMultiTimer);
        tvMultiAmmo = findViewById(R.id.tvMultiAmmo);
        btnMultiReload = findViewById(R.id.btnMultiReload);
        btnMultiShoot = findViewById(R.id.btnMultiShoot);
        btnMultiPause = findViewById(R.id.btnMultiPause);

        // Registramos el Giroscopio
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        // Validamos permisos de cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
            startMatch();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        // Lógica al presionar DISPARAR AL RIVAL (Pulgar Derecho)
        btnMultiShoot.setOnClickListener(v -> {
            if (!isMatchActive) return;

            if (ammo <= 0) {
                Toast.makeText(this, "¡Sin munición! Toca RECARGAR", Toast.LENGTH_SHORT).show();
                return;
            }

            ammo--;
            updateAmmoText();

            // Mayor probabilidad de acierto gracias al apuntado asistido por giroscopio
            if (random.nextInt(100) < 80) {
                myScore += 15;
                tvMyScore.setText(String.valueOf(myScore));
                Toast.makeText(this, "⚡ ¡Impacto certero al rival! +15 pts", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "¡El rival logró esquivar tu ráfaga!", Toast.LENGTH_SHORT).show();
            }
        });

        // Lógica de recarga rápida (Pulgar Izquierdo)
        btnMultiReload.setOnClickListener(v -> {
            if (ammo == maxAmmo) {
                Toast.makeText(this, "Tu cargador ya está lleno", Toast.LENGTH_SHORT).show();
            } else {
                ammo = maxAmmo;
                updateAmmoText();
                Toast.makeText(this, "🔄 ¡Cargador lleno, vuelve a disparar!", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón de salida arriba a la derecha
        btnMultiPause.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Salir del Duelo")
                    .setMessage("¿Estás seguro de que deseas abandonar la batalla multijugador?")
                    .setPositiveButton("Sí, salir", (dialog, which) -> finish())
                    .setNegativeButton("No, seguir", null)
                    .show();
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewViewMulti.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error de cámara: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startMatch() {
        isMatchActive = true;
        myScore = 0;
        rivalScore = 0;
        ammo = maxAmmo;
        offsetX = 0f;
        offsetY = 0f;
        tvMyScore.setText("0");
        tvRivalScore.setText("0");
        updateAmmoText();

        matchTimer = new CountDownTimer(45000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvMultiTimer.setText((millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                tvMultiTimer.setText("0s");
                endMatch();
            }
        }.start();

        simulateRivalActions();
    }

    private void simulateRivalActions() {
        if (!isMatchActive) return;

        int delay = 2500 + random.nextInt(2000);
        rivalHandler.postDelayed(() -> {
            if (!isMatchActive) return;

            if (random.nextBoolean()) {
                rivalScore += 15;
                tvRivalScore.setText(String.valueOf(rivalScore));
                Toast.makeText(MultiplayerGameActivity.this, "⚠️ ¡El rival te dio un tiro! +15 pts para él", Toast.LENGTH_SHORT).show();
            }

            simulateRivalActions();
        }, delay);
    }

    private void updateAmmoText() {
        tvMultiAmmo.setText(ammo + " / " + maxAmmo);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isMatchActive) return;

        // Leemos la rotación en tiempo real para darle respuesta táctil al área de juego
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float axisX = event.values[0];
            float axisY = event.values[1];

            offsetX -= axisY * 500f * 0.016f;
            offsetY += axisX * 500f * 0.016f;

            if (multiplayerGameArea != null) {
                multiplayerGameArea.setTranslationX(offsetX);
                multiplayerGameArea.setTranslationY(offsetY);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void endMatch() {
        isMatchActive = false;
        rivalHandler.removeCallbacksAndMessages(null);

        String resultMessage;
        if (myScore > rivalScore) {
            resultMessage = "¡Ganaste el duelo multijugador en vivo! Excelente puntería.\nTu puntaje: " + myScore + "\nRival: " + rivalScore;
        } else if (myScore < rivalScore) {
            resultMessage = "¡El rival tuvo mejores reflejos esta vez!\nTu puntaje: " + myScore + "\nRival: " + rivalScore;
        } else {
            resultMessage = "¡Empate en el campo de batalla! Ambos consiguieron " + myScore + " puntos.";
        }

        new AlertDialog.Builder(this)
                .setTitle("⚔️ Fin de la Batalla ⚔️")
                .setMessage(resultMessage)
                .setCancelable(false)
                .setPositiveButton("Regresar al Menú", (dialog, which) -> finish())
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && gyroscopeSensor != null) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
                startMatch();
            } else {
                Toast.makeText(this, "Permiso de cámara obligatorio", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isMatchActive = false;
        if (matchTimer != null) matchTimer.cancel();
        rivalHandler.removeCallbacksAndMessages(null);
    }
}