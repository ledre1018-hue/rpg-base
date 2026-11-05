package com.rpg.concurrency;

import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.items.Pocion;

import java.io.Serial;
import java.util.concurrent.locks.ReentrantLock;

public final class LaboratorioConcurrencia {

    private static final int VIDA_INICIAL = 1_000_000;
    private static final int GOLPES_POR_HILO = 100_000;

    private LaboratorioConcurrencia() {
    }

    public static void main(String[] args) throws InterruptedException {
        probarCondicionDeCarrera();
        probarEfectosContinuos();
        probarDaemon();
    }

    static class GuerreroSinProteccion extends Guerrero {

        @Serial
        private static final long serialVersionUID = 1L;

        GuerreroSinProteccion(String nombre) {
            super(nombre);
        }

        @Override
        public void recibirDanio(int cantidad) {
            int vidaLeida = getVida();
            Thread.yield();
            setVida(vidaLeida - cantidad);
        }
    }

    static class GuerreroConLock extends Guerrero {

        @Serial
        private static final long serialVersionUID = 1L;

        private final transient ReentrantLock candado = new ReentrantLock();

        GuerreroConLock(String nombre) {
            super(nombre);
        }

        @Override
        public void recibirDanio(int cantidad) {
            if (cantidad < 0) {
                throw new IllegalArgumentException("El danio no puede ser negativo");
            }
            candado.lock();
            try {
                setVida(getVida() - cantidad);
            } finally {
                candado.unlock();
            }
        }
    }

    private static void probarCondicionDeCarrera() throws InterruptedException {
        System.out.println("=== Condicion de carrera en la vida ===");
        golpearDesdeDosHilos(new GuerreroSinProteccion("SinProteccion"));
        golpearDesdeDosHilos(new Guerrero("Synchronized"));
        golpearDesdeDosHilos(new GuerreroConLock("ReentrantLock"));
    }

    private static void golpearDesdeDosHilos(Personaje personaje) throws InterruptedException {
        personaje.setVidaMaxima(VIDA_INICIAL);
        personaje.setVida(VIDA_INICIAL);
        Runnable golpes = () -> {
            for (int i = 0; i < GOLPES_POR_HILO; i++) {
                personaje.recibirDanio(1);
            }
        };
        Thread veneno = new Thread(golpes, "veneno");
        Thread ataque = new Thread(golpes, "ataque");
        veneno.start();
        ataque.start();
        veneno.join();
        ataque.join();
        int esperada = VIDA_INICIAL - 2 * GOLPES_POR_HILO;
        System.out.printf("%-14s vida esperada %d | obtenida %d | actualizaciones perdidas: %d%n",
                personaje.getNombre(), esperada, personaje.getVida(), personaje.getVida() - esperada);
    }

    private static void probarEfectosContinuos() throws InterruptedException {
        System.out.println("\n=== Efectos DoT / HoT en hilos propios ===");
        Guerrero heroe = new Guerrero("Heroe");
        heroe.setNotificador(mensaje -> System.out.println("  [" + Thread.currentThread().getName() + "] " + mensaje));
        heroe.aplicarEfecto(new EstadoEfecto("Veneno", TipoEfecto.DANIO_CONTINUO, 4, 10));
        heroe.aplicarEfecto(new EstadoEfecto("Quemadura", TipoEfecto.DANIO_CONTINUO, 6, 10));
        System.out.println("Estado con dos DoT: " + heroe.getEstado());
        Thread.sleep(4500);
        System.out.println(heroe.usarPocion(new Pocion("Antidoto", 0, RaridadItem.COMUN, null, true)));
        Thread.sleep(300);
        System.out.println("Tras el antidoto: " + heroe.getEstado() + " | efectos activos: " + heroe.getEfectos().size());
        heroe.aplicarEfecto(new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 5, 10));
        Thread.sleep(2500);
        heroe.detenerEfectos();
        System.out.println("Fin del combate: efectos activos " + heroe.getEfectos().size() + " | vida " + heroe.getVida());
    }

    private static void probarDaemon() throws InterruptedException {
        System.out.println("\n=== Hilos daemon ===");
        Thread tarde = new Thread(() -> { }, "tarde");
        tarde.start();
        try {
            tarde.setDaemon(true);
        } catch (IllegalThreadStateException e) {
            System.out.println("setDaemon(true) despues de start() lanza " + e.getClass().getSimpleName());
        }
        tarde.join();

        Thread vigia = new Thread(() -> {
            while (true) {
                System.out.println("  [daemon] sigo vivo...");
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "vigia");
        vigia.setDaemon(true);
        vigia.start();
        Thread.sleep(1000);
        System.out.println("main termina con el daemon vivo (" + vigia.isAlive() + "): la JVM cierra igual.");
    }
}
