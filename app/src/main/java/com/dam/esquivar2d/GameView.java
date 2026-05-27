package com.dam.esquivar2d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;

/*
 * GameView - bucle principal del juego.
 * Gestiona actualización de lógica, dibujado y entrada táctil.
 */
public class GameView extends SurfaceView implements Runnable {

    private Thread thread;
    private volatile boolean jugando = false;   // volatile: visible desde el hilo del juego

    private final SurfaceHolder holder;
    private final Paint paint;
    private final Random random;

    private Player jugador;
    private final ArrayList<Obstacle> obstaculos;
    private final ArrayList<PowerUp>  powerUps;

    private int anchoPantalla;
    private int altoPantalla;
    private int puntuacion = 0;

    // Estados del juego
    private static final int JUGANDO   = 0;
    private static final int GAME_OVER = 1;
    private int estado = JUGANDO;

    // Power-ups activos
    private boolean escudo = false;
    private boolean slow   = false;
    private long    tiempoSlow = 0;

    // Controles — misma Y de inicio, botones más altos y anchos para tocar mejor
    private int zonaControles;
    private static final int ALTO_BOTON = 210;
    private int anchoBoton;

    private boolean moverIzquierda = false;
    private boolean moverDerecha   = false;

    // Velocidad actual del jugador — se acumula con aceleración
    private float velJugador = 0f;
    private static final float VEL_MAX = 22f;
    private static final float ACEL    = 4f;   // px/frame de aceleración

    // Botones de game over
    private RectF botonReintentar;
    private RectF botonMenu;

    // Referencia a la actividad para volver al menú
    private final MainActivity actividad;

    public GameView(Context context) {
        super(context);
        actividad  = (MainActivity) context;
        holder     = getHolder();
        paint      = new Paint();
        paint.setAntiAlias(true);
        random     = new Random();
        obstaculos = new ArrayList<>();
        powerUps   = new ArrayList<>();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        anchoPantalla = metrics.widthPixels;
        altoPantalla  = metrics.heightPixels;

        anchoBoton    = anchoPantalla / 3;   // más ancho → más fácil de tocar
        zonaControles = altoPantalla - 380;  // botones un poco más arriba

        jugador = new Player(anchoPantalla, zonaControles);

        // Calcular posición de botones de game over una sola vez
        float btnAncho = anchoPantalla * 0.65f;
        float btnAlto  = 120f;
        float btnX0    = (anchoPantalla - btnAncho) / 2f;
        float centroY  = altoPantalla / 2f;

        botonReintentar = new RectF(btnX0, centroY + 50,  btnX0 + btnAncho, centroY + 50  + btnAlto);
        botonMenu       = new RectF(btnX0, centroY + 210, btnX0 + btnAncho, centroY + 210 + btnAlto);
    }

    // ─── Bucle principal ────────────────────────────────────────────────────────

    @Override
    public void run() {
        while (jugando) {
            actualizar();
            dibujar();
            dormir();
        }
    }

    // ─── Dificultad progresiva ───────────────────────────────────────────────────

    // Velocidad: empieza suave, sube despacio, techo razonable
    private int getVelocidad() {
        return Math.min(7 + puntuacion / 500, 16);
    }

    // Spawn: empieza con poca frecuencia, techo bajo para que nunca sea imposible
    private int getSpawnRate() {
        return Math.min(2 + puntuacion / 700, 6);
    }

    // Color de obstáculos según fase — fases más largas para transiciones suaves
    private int getColorObstaculo() {
        if (puntuacion < 1000) return Color.GREEN;
        if (puntuacion < 3000) return Color.YELLOW;
        if (puntuacion < 6000) return Color.rgb(255, 140, 0);  // naranja
        return Color.RED;
    }

    // ─── Lógica ─────────────────────────────────────────────────────────────────

    private void actualizar() {
        if (estado == GAME_OVER) return;

        puntuacion++;

        // Aceleración suave: un toquecito mueve poco, mantener da velocidad completa
        if (moverIzquierda) {
            velJugador = Math.max(-VEL_MAX, velJugador - ACEL);
        } else if (moverDerecha) {
            velJugador = Math.min(VEL_MAX, velJugador + ACEL);
        } else {
            velJugador = 0f;  // para en seco al soltar
        }
        jugador.moverConVelocidad(Math.round(velJugador));

        // Spawn de obstáculos
        if (random.nextInt(100) < getSpawnRate()) {
            obstaculos.add(new Obstacle(anchoPantalla, altoPantalla, getVelocidad(), getColorObstaculo()));
        }

        // Spawn de power-ups — muy poco frecuente
        if (random.nextInt(1000) < 2) {
            powerUps.add(new PowerUp(anchoPantalla, altoPantalla));
        }

        Rect hitbox = jugador.getHitbox();

        // Actualizar obstáculos — sincronizar color con la fase actual
        int colorFase = getColorObstaculo();
        for (Obstacle o : obstaculos) {
            o.setColor(colorFase);  // corrige obstáculos de fases anteriores
            o.update(slow);
            if (Rect.intersects(o.getRect(), hitbox)) {
                if (escudo) {
                    escudo = false;
                    o.reset();   // el escudo absorbe el golpe
                } else {
                    estado = GAME_OVER;
                    return;
                }
            }
        }

        // Actualizar power-ups y detectar recogida
        for (PowerUp p : powerUps) {
            p.update();
            if (Rect.intersects(p.getRect(), hitbox)) {
                if (p.getType() == 0) {
                    escudo = true;
                } else {
                    slow       = true;
                    tiempoSlow = System.currentTimeMillis() + 3000;
                }
                p.reset();
            }
        }

        // Desactivar slow cuando expire
        if (slow && System.currentTimeMillis() > tiempoSlow) {
            slow = false;
        }
    }

    // ─── Dibujado ────────────────────────────────────────────────────────────────

    private void dibujar() {
        if (!holder.getSurface().isValid()) return;

        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;

        canvas.drawColor(Color.BLACK);

        // Jugador — flecha blanca
        jugador.draw(canvas, paint);

        // Obstáculos — rectángulos de color según fase
        paint.setStyle(Paint.Style.FILL);
        for (Obstacle o : obstaculos) {
            paint.setColor(o.getColor());
            canvas.drawRect(o.getRect(), paint);
        }

        // Power-ups — círculos con etiqueta centrada
        for (PowerUp p : powerUps) {
            float cx    = p.getX() + p.getTamaño() / 2f;
            float cy    = p.getY() + p.getTamaño() / 2f;
            float radio = p.getTamaño() / 2f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(p.getType() == 0 ? Color.BLUE : Color.RED);
            canvas.drawCircle(cx, cy, radio, paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(32);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(p.getType() == 0 ? "S" : "L", cx, cy + 11, paint);
        }

        // Puntuación — más abajo para que no quede tapada por la barra del sistema
        paint.setColor(Color.WHITE);
        paint.setTextSize(55);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Puntos: " + puntuacion, 50, 170, paint);

        // Power-ups activos (arriba derecha, uno debajo de otro)
        dibujarPowerUpsActivos(canvas);

        // Controles — MISMA posición y tamaño que el original
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(180, 80, 80, 80));
        canvas.drawRect(0, zonaControles, anchoBoton, zonaControles + ALTO_BOTON, paint);
        canvas.drawRect(anchoPantalla - anchoBoton, zonaControles, anchoPantalla, zonaControles + ALTO_BOTON, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(110);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("<", anchoBoton / 2f,                 zonaControles + 120, paint);
        canvas.drawText(">", anchoPantalla - anchoBoton / 2f, zonaControles + 120, paint);

        // Pantalla de game over
        if (estado == GAME_OVER) {
            dibujarGameOver(canvas);
        }

        holder.unlockCanvasAndPost(canvas);
    }

    // Muestra los power-ups activos arriba a la derecha, en vertical
    private void dibujarPowerUpsActivos(Canvas canvas) {
        int iconX = anchoPantalla - 55;
        int iconY = 160;  // alineado con la puntuación
        int radio = 22;

        if (escudo) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLUE);
            canvas.drawCircle(iconX, iconY, radio, paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(26);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("S", iconX, iconY + 9, paint);

            paint.setTextSize(30);
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setColor(Color.rgb(100, 180, 255));
            canvas.drawText("Escudo", iconX - radio - 8, iconY + 10, paint);

            iconY += 55;
        }

        if (slow) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.RED);
            canvas.drawCircle(iconX, iconY, radio, paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(26);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("L", iconX, iconY + 9, paint);

            paint.setTextSize(30);
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setColor(Color.rgb(255, 120, 120));
            canvas.drawText("Slow", iconX - radio - 8, iconY + 10, paint);
        }
    }

    // Dibuja el overlay de game over con los dos botones
    private void dibujarGameOver(Canvas canvas) {
        // Fondo semitransparente sobre el juego
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(170, 0, 0, 0));
        canvas.drawRect(0, 0, anchoPantalla, altoPantalla, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(110);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("GAME OVER", anchoPantalla / 2f, altoPantalla / 2f - 60, paint);

        paint.setTextSize(60);
        canvas.drawText("Puntos: " + puntuacion, anchoPantalla / 2f, altoPantalla / 2f + 20, paint);

        // Botón Reintentar
        paint.setColor(Color.rgb(30, 160, 30));
        canvas.drawRoundRect(botonReintentar, 20, 20, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(55);
        canvas.drawText("Reintentar", anchoPantalla / 2f, botonReintentar.centerY() + 18, paint);

        // Botón Volver al menú
        paint.setColor(Color.rgb(40, 90, 190));
        canvas.drawRoundRect(botonMenu, 20, 20, paint);
        paint.setColor(Color.WHITE);
        canvas.drawText("Volver al menú", anchoPantalla / 2f, botonMenu.centerY() + 18, paint);
    }

    // ─── Entrada táctil ──────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // En game over solo respondemos al levantar el dedo
        if (estado == GAME_OVER) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                float tx = event.getX();
                float ty = event.getY();
                if (botonReintentar.contains(tx, ty)) {
                    reiniciar();
                } else if (botonMenu.contains(tx, ty)) {
                    // Parar el hilo y volver al menú en el hilo de UI
                    jugando = false;
                    actividad.runOnUiThread(() -> actividad.mostrarMenu());
                }
            }
            return true;
        }

        // Multi-touch: comprobamos todos los punteros activos cada frame
        int accion = event.getActionMasked();
        moverIzquierda = false;
        moverDerecha   = false;

        if (accion != MotionEvent.ACTION_UP && accion != MotionEvent.ACTION_CANCEL) {
            for (int i = 0; i < event.getPointerCount(); i++) {
                if (event.getX(i) < anchoPantalla / 2f) {
                    moverIzquierda = true;
                } else {
                    moverDerecha = true;
                }
            }
        }

        return true;
    }

    // ─── Gestión del ciclo de vida ────────────────────────────────────────────────

    private void reiniciar() {
        puntuacion     = 0;
        estado         = JUGANDO;
        escudo         = false;
        slow           = false;
        moverIzquierda = false;
        moverDerecha   = false;
        velJugador     = 0f;
        obstaculos.clear();
        powerUps.clear();
        jugador = new Player(anchoPantalla, zonaControles);
    }

    // Guardia para evitar doble inicio si se llama resume() dos veces
    public void resume() {
        if (jugando) return;
        jugando = true;
        thread  = new Thread(this);
        thread.start();
    }

    public void pause() {
        try {
            jugando = false;
            if (thread != null) thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void dormir() {
        try { Thread.sleep(16); } catch (InterruptedException e) { e.printStackTrace(); }
    }
}
