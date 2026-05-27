package com.dam.esquivar2d;

import android.graphics.Rect;
import java.util.Random;

/*
 * PowerUp - objetos circulares que benefician al jugador.
 * Tipo 0 = Escudo (azul), Tipo 1 = Slow motion (rojo).
 * GameView usa getTamaño() para dibujarlos como círculos.
 */
public class PowerUp {

    private int x, y;
    private static final int TAMAÑO   = 70;   // radio efectivo: 35px
    private static final int VELOCIDAD = 7;
    private int tipo;

    private int anchoPantalla;
    private int altoPantalla;
    private Random random;

    public PowerUp(int anchoPantalla, int altoPantalla) {
        this.anchoPantalla = anchoPantalla;
        this.altoPantalla  = altoPantalla;
        this.random        = new Random();
        generarNuevo();
    }

    private void generarNuevo() {
        x    = random.nextInt(anchoPantalla - TAMAÑO);
        y    = -TAMAÑO;
        tipo = random.nextInt(2);
    }

    public void update() {
        y += VELOCIDAD;
        if (y > altoPantalla) generarNuevo();
    }

    public void reset() { generarNuevo(); }

    // Rect cuadrado usado para la detección de colisión (igual que antes)
    public Rect getRect() {
        return new Rect(x, y, x + TAMAÑO, y + TAMAÑO);
    }

    public int getType()   { return tipo; }
    public int getX()      { return x; }
    public int getY()      { return y; }
    public int getTamaño() { return TAMAÑO; }   // necesario para dibujar el círculo
}
