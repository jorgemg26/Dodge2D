package com.dam.esquivar2d;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/*
 * MainActivity - punto de entrada de la app.
 * Gestiona la navegación entre el menú principal y el juego.
 */
public class MainActivity extends AppCompatActivity {

    private GameView gameView;  // null cuando estamos en el menú

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarMenu();  // arrancamos siempre en el menú
    }

    // Muestra la pantalla del menú principal
    public void mostrarMenu() {
        if (gameView != null) {
            gameView.pause();   // detiene el hilo del juego
            gameView = null;
        }
        setContentView(new MenuView(this));
    }

    // Inicia el juego en modo infinito
    public void iniciarJuego() {
        gameView = new GameView(this);
        setContentView(gameView);
        gameView.resume();  // arranca el hilo inmediatamente
    }

    @Override
    protected void onResume() {
        super.onResume();
        // resume() tiene guardia interna contra doble inicio
        if (gameView != null) gameView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) gameView.pause();
    }
}
