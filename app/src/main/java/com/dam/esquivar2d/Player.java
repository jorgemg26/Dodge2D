package com.dam.esquivar2d;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

/*
 * Player - el jugador controlado por el usuario.
 * Se dibuja como una flecha blanca apuntando hacia arriba.
 * La hitbox es más pequeña que la forma visual para que se sienta justo.
 */
public class Player {

    private int x, y;
    private int anchoPantalla;

    // Flecha más pequeña que antes (era 80×100)
    private static final int ANCHO_FLECHA = 58;
    private static final int ALTO_FLECHA  = 72;

    // Hitbox muy precisa — solo el eje central de la flecha
    private static final int HITBOX_ANCHO = 18;
    private static final int HITBOX_ALTO  = 28;

    public Player(int ancho, int zonaControles) {
        anchoPantalla = ancho;
        // Centrar la flecha horizontalmente en pantalla
        x = ancho / 2 - ANCHO_FLECHA / 2;
        // Posición vertical original — NO SE CAMBIA
        y = zonaControles - 180;
    }

    // La velocidad la calcula GameView con aceleración; aquí solo aplicamos el delta
    public void moverConVelocidad(int delta) {
        x = Math.max(0, Math.min(anchoPantalla - ANCHO_FLECHA, x + delta));
    }

    /*
     * Dibuja una flecha apuntando hacia ARRIBA con un Path.
     * Estructura: punta en la cima → alas a media altura → eje fino hasta abajo.
     */
    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        float cx        = x + ANCHO_FLECHA / 2f;
        float top       = y;
        float mid       = y + ALTO_FLECHA * 0.45f;  // donde arrancan las alas
        float bottom    = y + ALTO_FLECHA;
        float halfAncho = ANCHO_FLECHA / 2f;         // mitad del ancho (alcance de alas)
        float shaftHalf = ANCHO_FLECHA * 0.18f;      // mitad del eje fino

        Path flecha = new Path();
        flecha.moveTo(cx, top);                  // punta (apex)
        flecha.lineTo(cx + halfAncho, mid);       // ala derecha exterior
        flecha.lineTo(cx + shaftHalf, mid);       // eje derecho, unión ala
        flecha.lineTo(cx + shaftHalf, bottom);    // eje derecho, base
        flecha.lineTo(cx - shaftHalf, bottom);    // eje izquierdo, base
        flecha.lineTo(cx - shaftHalf, mid);       // eje izquierdo, unión ala
        flecha.lineTo(cx - halfAncho, mid);       // ala izquierda exterior
        flecha.close();                            // cierra de vuelta al apex

        canvas.drawPath(flecha, paint);
    }

    // Hitbox pequeña centrada en el eje de la flecha (para colisiones)
    public Rect getHitbox() {
        int cx = x + ANCHO_FLECHA / 2;
        int cy = y + ALTO_FLECHA / 2;
        return new Rect(
            cx - HITBOX_ANCHO / 2,
            cy - HITBOX_ALTO / 2,
            cx + HITBOX_ANCHO / 2,
            cy + HITBOX_ALTO / 2
        );
    }

    // Centro horizontal del jugador
    public int getCentroX() {
        return x + ANCHO_FLECHA / 2;
    }
}
