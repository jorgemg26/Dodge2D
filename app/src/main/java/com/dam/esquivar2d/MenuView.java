package com.dam.esquivar2d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

/*
 * MenuView - pantalla de inicio del juego.
 * Muestra tres botones: Modo Infinito (activo), Modo Niveles (deshabilitado)
 * y Estadísticas (placeholder).
 */
public class MenuView extends View {

    private final Paint paint;
    private final int anchoPantalla;
    private final int altoPantalla;
    private final MainActivity actividad;

    // Áreas táctiles de los tres botones
    private final RectF botonInfinito;
    private final RectF botonNiveles;
    private final RectF botonEstadisticas;

    public MenuView(Context context) {
        super(context);
        actividad = (MainActivity) context;

        paint = new Paint();
        paint.setAntiAlias(true);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        anchoPantalla = metrics.widthPixels;
        altoPantalla  = metrics.heightPixels;

        // Botones centrados horizontalmente, a partir del 50% de la pantalla
        float btnAncho = anchoPantalla * 0.72f;
        float btnAlto  = 140f;
        float btnX0    = (anchoPantalla - btnAncho) / 2f;
        float startY   = altoPantalla * 0.50f;

        botonInfinito     = new RectF(btnX0, startY,           btnX0 + btnAncho, startY + btnAlto);
        botonNiveles      = new RectF(btnX0, startY + 185,     btnX0 + btnAncho, startY + 185 + btnAlto);
        botonEstadisticas = new RectF(btnX0, startY + 370,     btnX0 + btnAncho, startY + 370 + btnAlto);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Fondo negro
        canvas.drawColor(Color.BLACK);

        // ── Título ──────────────────────────────────────────────────────────────
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(95);
        canvas.drawText("ESQUIVAR 2D", anchoPantalla / 2f, altoPantalla * 0.27f, paint);

        paint.setTextSize(38);
        paint.setColor(Color.LTGRAY);
        canvas.drawText("¿Hasta dónde llegas?", anchoPantalla / 2f, altoPantalla * 0.35f, paint);

        // ── Botón MODO INFINITO (activo — verde) ────────────────────────────────
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(30, 170, 50));
        canvas.drawRoundRect(botonInfinito, 22, 22, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(58);
        canvas.drawText("Modo Infinito", anchoPantalla / 2f, botonInfinito.centerY() + 20, paint);

        // ── Botón MODO NIVELES (deshabilitado — gris oscuro) ────────────────────
        paint.setColor(Color.rgb(55, 55, 55));
        canvas.drawRoundRect(botonNiveles, 22, 22, paint);

        paint.setColor(Color.rgb(120, 120, 120));
        paint.setTextSize(58);
        canvas.drawText("Modo Niveles", anchoPantalla / 2f, botonNiveles.centerY() + 5, paint);

        paint.setTextSize(28);
        canvas.drawText("(Próximamente)", anchoPantalla / 2f, botonNiveles.bottom - 14, paint);

        // ── Botón ESTADÍSTICAS (placeholder — azul oscuro) ──────────────────────
        paint.setColor(Color.rgb(35, 85, 180));
        canvas.drawRoundRect(botonEstadisticas, 22, 22, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(58);
        canvas.drawText("Estadísticas", anchoPantalla / 2f, botonEstadisticas.centerY() + 20, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Solo reaccionar al levantar el dedo para evitar dobles pulsaciones
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float tx = event.getX();
            float ty = event.getY();

            if (botonInfinito.contains(tx, ty)) {
                actividad.iniciarJuego();
            }
            // botonNiveles: deshabilitado → no hace nada
            // botonEstadisticas: placeholder → no hace nada
        }
        return true;
    }
}
