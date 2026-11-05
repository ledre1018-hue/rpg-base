package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

public class GestorTurno {

    private static final long ESPERA_CANCELACION_MS = 200;

    private Thread temporizador;

    public void iniciar(int segundos, TurnoCallback callback) {
        cancelar();
        temporizador = new Thread(new TemporizadorTurnoTask(segundos, callback), "temporizador-turno");
        temporizador.setDaemon(true);
        temporizador.start();
    }

    public void cancelar() {
        Thread actual = temporizador;
        temporizador = null;
        if (actual == null || !actual.isAlive()) {
            return;
        }
        actual.interrupt();
        try {
            actual.join(ESPERA_CANCELACION_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
