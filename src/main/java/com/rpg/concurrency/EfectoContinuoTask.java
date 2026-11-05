package com.rpg.concurrency;

import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Personaje;

public class EfectoContinuoTask implements Runnable {

    private final Personaje personaje;
    private final EstadoEfecto efecto;
    private final long intervaloMs;

    public EfectoContinuoTask(Personaje personaje, EstadoEfecto efecto, long intervaloMs) {
        this.personaje = personaje;
        this.efecto = efecto;
        this.intervaloMs = intervaloMs;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(intervaloMs);
                } catch (InterruptedException e) {
                    personaje.finalizarEfecto(efecto);
                    Thread.currentThread().interrupt();
                    break;
                }
                String mensaje = personaje.aplicarTickEfecto(efecto);
                if (mensaje == null) {
                    break;
                }
                personaje.notificar(mensaje);
                if (efecto.haTerminado()) {
                    personaje.notificar(personaje.getNombre() + ": termina " + efecto.getNombre());
                    break;
                }
                if (!personaje.estaVivo()) {
                    break;
                }
            }
        } finally {
            personaje.finalizarEfecto(efecto);
        }
    }

    public String getNombreEfecto() {
        return efecto.getNombre();
    }

    public long getIntervaloMs() {
        return intervaloMs;
    }
}
