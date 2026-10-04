package com.example.freechoterjdc;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
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
 * ¡El campo de batalla individual con GIROSCOPIO y controles ergonómicos!
 * Aquí es donde ocurre la magia, mi hermano. Conectamos CameraX de fondo y
 * le metemos el giroscopio del celular para mover los soldados y próceres en pantalla
 * según te muevas físicamente en tu sala. ¡Melo melo!
 */
public class SinglePlayerGameActivity extends AppCompatActivity implements SensorEventListener {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final String PREFS_NAME = "FreeShooterPrefs";
    private static final String KEY_TOTAL_SCORE = "totalScore";

    // Componentes de la interfaz
    private PreviewView previewView;
    private FrameLayout gameArea;
    private TextView tvScore, tvTimer, tvLevel, tvAmmo;
    private Button btnReload, btnShoot, btnPause;

    // Variables de estado del juego
    private int score = 0;
    private int level = 1;
    private int ammo = 10;
    private final int maxAmmo = 10;
    private boolean isGameActive = false;
    private boolean isPaused = false;

    // Temporizadores y generadores de números aleatorios
    private CountDownTimer gameTimer;
    private long timeLeftInMillis = 60000; // 60 segundos iniciales
    private final Random random = new Random();
    private final Handler targetHandler = new Handler(Looper.getMainLooper());
    private View currentTargetView = null;
    private boolean isTargetProcer = false;

    // Sensores para el Apuntado por Giroscopio
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    
    // Desplazamiento acumulado por el giroscopio para simular la vista AR panorámica
    private float offsetX = 0f;
    private float offsetY = 0f;
    private static final float GYRO_SENSITIVITY = 650f; // Multiplicador de velocidad de paneo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_player_game);

        // Inicializamos los componentes del layout lateral
        previewView = findViewById(R.id.previewView);
        gameArea = findViewById(R.id.gameArea);
        tvScore = findViewById(R.id.tvScore);
        tvTimer = findViewById(R.id.tvTimer);
        tvLevel = findViewById(R.id.tvLevel);
        tvAmmo = findViewById(R.id.tvAmmo);
        btnReload = findViewById(R.id.btnReload);
        btnShoot = findViewById(R.id.btnShoot);
        btnPause = findViewById(R.id.btnPause);

        // Configuramos el gestor de sensores de Android
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        if (gyroscopeSensor == null) {
            Toast.makeText(this, "⚠️ Giroscopio no detectado. Se usará modo estático.", Toast.LENGTH_LONG).show();
        }

        // Actualizamos textos iniciales en pantalla
        updateAmmoText();

        // Verificamos permisos para encender la cámara antes de empezar
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
            startGame();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        // Lógica del botón de disparo lateral derecho (Ergonómico para el pulgar)
        btnShoot.setOnClickListener(v -> shootCentralCrosshair());

        // Lógica del botón de Recarga rápida lateral izquierdo
        btnReload.setOnClickListener(v -> {
            if (isPaused) return;
            if (ammo == maxAmmo) {
                Toast.makeText(this, "¡Tu cargador ya está lleno!", Toast.LENGTH_SHORT).show();
            } else {
                ammo = maxAmmo;
                updateAmmoText();
                Toast.makeText(this, "¡Cargador listo! ¡Fuego al objetivo!", Toast.LENGTH_SHORT).show();
            }
        });

        // Lógica del botón de Pausa arriba a la derecha
        btnPause.setOnClickListener(v -> togglePauseGame());
    }

    /**
     * Enciende e inicializa CameraX para usar la cámara trasera como fondo de Realidad Aumentada.
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error al iniciar la cámara: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Comienza el conteo de la partida y el bucle de apariciones de objetivos.
     */
    private void startGame() {
        isGameActive = true;
        isPaused = false;
        score = 0;
        level = 1;
        ammo = maxAmmo;
        offsetX = 0f;
        offsetY = 0f;
        tvScore.setText("0");
        tvLevel.setText("1");
        updateAmmoText();

        startTimer(timeLeftInMillis);
        spawnTargetLoop();
    }

    /**
     * Inicializa o reanuda el temporizador de la partida.
     */
    private void startTimer(long timeDuration) {
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        gameTimer = new CountDownTimer(timeDuration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                tvTimer.setText((millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                tvTimer.setText("0s");
                endGame();
            }
        }.start();
    }

    /**
     * Activa o desactiva la pausa del juego congelando contadores e hilos.
     */
    private void togglePauseGame() {
        if (!isGameActive) return;

        if (!isPaused) {
            // Pausar
            isPaused = true;
            if (gameTimer != null) gameTimer.cancel();
            targetHandler.removeCallbacksAndMessages(null);
            
            new AlertDialog.Builder(this)
                    .setTitle("⏸️ Juego en Pausa")
                    .setMessage("¿Qué deseas hacer, soldado de la patria?")
                    .setCancelable(false)
                    .setPositiveButton("Continuar", (dialog, which) -> {
                        isPaused = false;
                        startTimer(timeLeftInMillis);
                        spawnTargetLoop();
                    })
                    .setNegativeButton("Salir al Menú", (dialog, which) -> finish())
                    .show();
        }
    }

    /**
     * Bucle continuo para aparecer y desaparecer objetivos en la pantalla.
     */
    private void spawnTargetLoop() {
        if (!isGameActive || isPaused) return;

        if (currentTargetView != null) {
            gameArea.removeView(currentTargetView);
        }

        // Para hacerlo bien interactivo, si sale enemigo usamos un Layout con la imagen que mandaste
        // Si sale prócer, mantenemos el letrero patriótico dorado.
        View target;
        FrameLayout.LayoutParams params;

        // Decidimos si sale un soldado español (70%) o un prócer de Colombia (30%)
        if (random.nextInt(10) < 7) {
            isTargetProcer = false;

            // Creamos un contenedor vertical para meter la imagen del español sin fondo blanco y su etiqueta debajo
            android.widget.LinearLayout llEnemy = new android.widget.LinearLayout(this);
            llEnemy.setOrientation(android.widget.LinearLayout.VERTICAL);
            llEnemy.setGravity(Gravity.CENTER);

            // Creamos el ImageView para renderizar la foto del soldado español
            android.widget.ImageView ivEnemy = new android.widget.ImageView(this);
            ivEnemy.setImageResource(R.drawable.soldado_espanol);
            
            // ¡Excelente! Como la nueva imagen ya viene nativamente sin fondo (PNG transparente),
            // removemos el filtro de mezcla antiguo para que mantenga sus colores e iluminación originales intactos.
            // Queda nítido y perfecto sobre la cámara AR.

            // Le damos un tamaño un pelin más grande y elegante a la foto flotante del enemigo
            android.widget.LinearLayout.LayoutParams imgParams = new android.widget.LinearLayout.LayoutParams(380, 380);
            ivEnemy.setLayoutParams(imgParams);

            // Añadimos una etiqueta de texto pequeña abajo para guiar el disparo táctico
            TextView tvLabel = new TextView(this);
            tvLabel.setText("🇪🇸 ¡INVASOR!");
            tvLabel.setTextColor(Color.RED);
            tvLabel.setTextSize(13f);
            tvLabel.setGravity(Gravity.CENTER);
            tvLabel.setBackgroundColor(Color.parseColor("#80000000")); // Fondo negro suave para legibilidad

            llEnemy.addView(ivEnemy);
            llEnemy.addView(tvLabel);

            target = llEnemy;
            params = new FrameLayout.LayoutParams(440, 440);
        } else {
            isTargetProcer = true;

            // Creamos un contenedor vertical ergonómico e interactivo para el patriota colombiano
            android.widget.LinearLayout llAlly = new android.widget.LinearLayout(this);
            llAlly.setOrientation(android.widget.LinearLayout.VERTICAL);
            llAlly.setGravity(Gravity.CENTER);

            // Cargamos la imagen nativa sin fondo (PNG transparente) del aliado colombiano
            android.widget.ImageView ivAlly = new android.widget.ImageView(this);
            ivAlly.setImageResource(R.drawable.aliado_colombiano);

            // Le damos un tamaño un pelin más grande y vistoso al aliado colombiano para equilibrar el HUD
            android.widget.LinearLayout.LayoutParams imgParams = new android.widget.LinearLayout.LayoutParams(380, 380);
            ivAlly.setLayoutParams(imgParams);

            // Etiqueta patriótica inferior para saber a quién rescatar
            TextView tvLabel = new TextView(this);
            String[] proceres = {"Simón Bolívar", "Francisco de Paula Santander", "Policarpa Salavarrieta", "Antonio Nariño"};
            String procerElegido = proceres[random.nextInt(proceres.length)];
            tvLabel.setText("🇨🇴 " + procerElegido);
            tvLabel.setTextColor(Color.parseColor("#FFD700")); // Dorado brillante
            tvLabel.setTextSize(13f);
            tvLabel.setGravity(Gravity.CENTER);
            tvLabel.setBackgroundColor(Color.parseColor("#80000000")); // Fondo negro suave para máxima legibilidad

            llAlly.addView(ivAlly);
            llAlly.addView(tvLabel);

            target = llAlly;
            params = new FrameLayout.LayoutParams(440, 440);
        }

        int width = gameArea.getWidth();
        int height = gameArea.getHeight();

        if (width == 0) width = 1080;
        if (height == 0) height = 1920;

        // Ajustamos los márgenes aleatorios adaptados al nuevo tamaño de los personajes para que no se corten en los bordes
        int randomX = random.nextInt(Math.max(width - 500, 50));
        int randomY = random.nextInt(Math.max(height - 700, 150)) + 150;

        params.leftMargin = randomX;
        params.topMargin = randomY;
        target.setLayoutParams(params);

        // Toque táctil directo en la figura flotante
        target.setOnClickListener(v -> handleHit(true));

        currentTargetView = target;
        gameArea.addView(target);

        // Velocidad adaptativa según el nivel alcanzado
        long delay = Math.max(3000 - (level * 250L), 1200);
        targetHandler.postDelayed(this::spawnTargetLoop, delay);
    }

    private void handleHit(boolean directTouch) {
        if (!isGameActive || isPaused) return;

        if (ammo <= 0) {
            Toast.makeText(this, "¡Sin munición! Toca RECARGAR", Toast.LENGTH_SHORT).show();
            return;
        }

        if (directTouch) {
            ammo--;
            updateAmmoText();
        }

        if (isTargetProcer) {
            if (directTouch) {
                score += 25; // ¡Rescatado con éxito al tocarlo!
                Toast.makeText(this, "¡Prócer rescatado con éxito! 🇨🇴 +25 pts", Toast.LENGTH_SHORT).show();
            } else {
                score = Math.max(score - 15, 0); // Lo hirió con la mira central por accidente
                Toast.makeText(this, "⚠️ ¡Cuidado! Disparaste a un aliado -15 pts", Toast.LENGTH_SHORT).show();
            }
        } else {
            score += 10;
            Toast.makeText(this, "💥 ¡Soldado español abatido! +10 pts", Toast.LENGTH_SHORT).show();
        }

        tvScore.setText(String.valueOf(score));

        int calculatedLevel = (score / 50) + 1;
        if (calculatedLevel > level) {
            level = calculatedLevel;
            tvLevel.setText(String.valueOf(level));
            Toast.makeText(this, "⭐ ¡NIVEL UP! Nivel " + level + ". Enemigos más veloces", Toast.LENGTH_SHORT).show();
        }

        if (currentTargetView != null) {
            gameArea.removeView(currentTargetView);
            currentTargetView = null;
        }
        targetHandler.removeCallbacksAndMessages(null);
        spawnTargetLoop();
    }

    /**
     * Acción de disparar con el botón físico/táctil lateral derecho usando la mira fija central.
     */
    private void shootCentralCrosshair() {
        if (!isGameActive || isPaused) return;

        if (ammo <= 0) {
            Toast.makeText(this, "¡Sin balas! Recarga de inmediato", Toast.LENGTH_SHORT).show();
            return;
        }

        ammo--;
        updateAmmoText();

        if (currentTargetView != null) {
            int[] location = new int[2];
            currentTargetView.getLocationOnScreen(location);
            int targetX = location[0] + (currentTargetView.getWidth() / 2);
            int targetY = location[1] + (currentTargetView.getHeight() / 2);

            int screenWidth = gameArea.getWidth();
            int screenHeight = gameArea.getHeight();
            if (screenWidth == 0) screenWidth = 1080;
            if (screenHeight == 0) screenHeight = 1920;

            int centerX = screenWidth / 2;
            int centerY = screenHeight / 2;

            // Radio de colisión para validar el acierto con la mira
            int radius = 260;

            if (Math.abs(targetX - centerX) < radius && Math.abs(targetY - centerY) < radius) {
                handleHit(false);
            } else {
                Toast.makeText(this, "💨 ¡Disparo fallido! Mueve el celular para apuntar mejor", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "¡Disparo al aire!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAmmoText() {
        tvAmmo.setText(ammo + " / " + maxAmmo);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isGameActive || isPaused) return;

        // Capturamos la velocidad angular detectada por el giroscopio físico
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float axisX = event.values[0]; // Rotación vertical (inclinación arriba/abajo)
            float axisY = event.values[1]; // Rotación horizontal (giro izquierda/derecha)

            // Acumulamos el movimiento aplicando la sensibilidad
            offsetX -= axisY * GYRO_SENSITIVITY * 0.016f;
            offsetY += axisX * GYRO_SENSITIVITY * 0.016f;

            // Movemos el área contenedora de forma inversa para simular que el jugador está paneando el espacio 3D real
            gameArea.setTranslationX(offsetX);
            gameArea.setTranslationY(offsetY);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No se requiere para esta lógica melita
    }

    private void endGame() {
        isGameActive = false;
        targetHandler.removeCallbacksAndMessages(null);
        if (currentTargetView != null) {
            gameArea.removeView(currentTargetView);
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int currentTotalScore = prefs.getInt(KEY_TOTAL_SCORE, 0);
        int newTotalScore = currentTotalScore + score;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_TOTAL_SCORE, newTotalScore);
        editor.apply();

        new AlertDialog.Builder(this)
                .setTitle("¡Partida Terminada, Héroe!")
                .setMessage("Lograste hacer " + score + " puntos.\nTu puntuación total acumulada es: " + newTotalScore + " pts.")
                .setCancelable(false)
                .setPositiveButton("Volver al Menú", (dialog, which) -> finish())
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && gyroscopeSensor != null) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if (isPaused) {
            togglePauseGame();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (isGameActive && !isPaused) {
            togglePauseGame();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isGameActive = false;
        if (gameTimer != null) gameTimer.cancel();
        targetHandler.removeCallbacksAndMessages(null);
    }
}