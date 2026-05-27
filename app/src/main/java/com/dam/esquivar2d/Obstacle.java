package com.dam.esquivar2d;

import android.graphics.Rect;
import java.util.Random;

/*
 * Obstacle - obstáculos que caen desde arriba.
 * El color refleja la fase de dificultad actual (verde → amarillo → naranja → rojo).
 */
public class Obstacle {

    private int x, y;
    private static final int TAMAÑO = 80;   // ligeramente más pequeño que el original (100)
    private int velocidad;
    private int color;                       // color de fase pasado desde GameView

    private int anchoPantalla;
    private int altoPantalla;
    private Random random;

    public Obstacle(int anchoPantalla, int altoPantalla, int velocidadBase, int color) {
        this.anchoPantalla = anchoPantalla;
        this.altoPantalla  = altoPantalla;
        this.random        = new Random();
        this.color         = color;

        x = random.nextInt(anchoPantalla - TAMAÑO);
        y = -TAMAÑO;

        // Variación aleatoria de velocidad para que no todos caigan igual
        velocidad = velocidadBase + random.nextInt(4);
    }

    public void update(boolean slow) {
        y += slow ? velocidad / 2 : velocidad;
        if (y > altoPantalla) reset();
    }

    public void reset() {
        x = random.nextInt(anchoPantalla - TAMAÑO);
        y = -TAMAÑO;
    }

    public void setColor(int color) { this.color = color; }
    public int  getColor()          { return color; }
    public int  getX()              { return x; }

    public Rect getRect() {
        return new Rect(x, y, x + TAMAÑO, y + TAMAÑO);
    }
}
