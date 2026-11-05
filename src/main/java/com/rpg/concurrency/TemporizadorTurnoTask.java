package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

public class TemporizadorTurnoTask implements Runnable {

    private static final long UN_SEGUNDO_MS = 1000;
    private static final int SEGUNDOS_AVISO = 3;

    private final int duracionSegundos;
    private final TurnoCallback callback;

    public TemporizadorTurnoTask(int duracionSegundos, TurnoCallback callback) {
        if (duracionSegundos <= 0) {
            throw new IllegalArgumentException("La duracion del turno debe ser positiva");
        }
        this.duracionSegundos = duracionSegundos;
        this.callback = callback;
    }

    @Override
    public void run() {
        for (int restantes = duracionSegundos; restantes > 0; restantes--) {
            if (restantes <= SEGUNDOS_AVISO) {
                System.out.printf("%n[Quedan %d s] ", restantes);
            }
            try {
                Thread.sleep(UN_SEGUNDO_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        callback.onTiempoAgotado();
    }
}
