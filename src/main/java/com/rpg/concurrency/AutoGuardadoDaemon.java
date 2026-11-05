package com.rpg.concurrency;

/**
 * Un hilo de usuario mantiene viva la JVM hasta que termina; un hilo daemon no: cuando solo quedan
 * daemons, la JVM cierra sin esperarlos. Por eso el auto-guardado es daemon y no retrasa la salida.
 */
public class AutoGuardadoDaemon implements Runnable {

    private final Runnable guardado;
    private final long intervaloMs;

    public AutoGuardadoDaemon(Runnable guardado, long intervaloMs) {
        if (intervaloMs <= 0) {
            throw new IllegalArgumentException("El intervalo debe ser positivo");
        }
        this.guardado = guardado;
        this.intervaloMs = intervaloMs;
    }

    public static Thread iniciar(Runnable guardado, long intervaloMs) {
        Thread hilo = new Thread(new AutoGuardadoDaemon(guardado, intervaloMs), "auto-guardado");
        hilo.setDaemon(true);
        hilo.start();
        return hilo;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(intervaloMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            try {
                guardado.run();
            } catch (RuntimeException e) {
                System.out.println("[Auto-guardado fallido: " + e.getMessage() + "]");
            }
        }
    }
}
