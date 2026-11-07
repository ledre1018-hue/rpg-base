package com.rpg.core;

import com.rpg.entities.Escuadron;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;

import java.util.List;
import java.util.function.Supplier;

/** Torneo de 4 gladiadores: dos semifinales y una final. */
public class TorneoManager {

    private static final long OBSERVACION_MS = 50;

    private final Arena arena;
    private final RegistroCombate registro;

    public TorneoManager(Arena arena, RegistroCombate registro) {
        this.arena = arena;
        this.registro = registro;
    }

    /** @return el campeon del torneo */
    public Personaje jugarTorneo() {
        List<Personaje> gladiadores = crearGladiadores().getMiembros();
        Personaje[] finalistas = new Personaje[2];

        Thread semifinalA = new Thread(
                () -> finalistas[0] = simularSemifinal(gladiadores.get(0), gladiadores.get(1)), "semifinal-A");
        Thread semifinalB = new Thread(
                () -> finalistas[1] = simularSemifinal(gladiadores.get(2), gladiadores.get(3)), "semifinal-B");

        mostrarEstados("antes de start()", semifinalA, semifinalB);
        semifinalA.start();
        semifinalB.start();
        try {
            Thread.sleep(OBSERVACION_MS);
            mostrarEstados("durante la ejecucion", semifinalA, semifinalB);
            semifinalA.join();
            semifinalB.join();
        } catch (InterruptedException e) {
            semifinalA.interrupt();
            semifinalB.interrupt();
            Thread.currentThread().interrupt();
            registro.agregar("El torneo se interrumpio antes de la final.");
            return null;
        }
        mostrarEstados("despues de join()", semifinalA, semifinalB);

        return simularFinal(finalistas[0], finalistas[1]);
    }

    private void mostrarEstados(String momento, Thread semifinalA, Thread semifinalB) {
        registro.agregar("[Hilos %s] %s: %s | %s: %s", momento, semifinalA.getName(), semifinalA.getState(),
                semifinalB.getName(), semifinalB.getState());
    }

    public Personaje simularSemifinal(Personaje a, Personaje b) {
        registro.agregar("=== SEMIFINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        Personaje ganador = arena.duelo(a, b);
        registro.agregar("Clasifica a la final: %s", ganador.getNombre());
        return ganador;
    }

    private Personaje simularFinal(Personaje a, Personaje b) {
        a.restaurarVida();
        b.restaurarVida();
        registro.agregar("=== FINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        return arena.duelo(a, b);
    }

    private Escuadron crearGladiadores() {
        Escuadron escuadron = new Escuadron("Gladiadores");
        List<Supplier<Personaje>> fabricasDeGladiadores = List.of(
                () -> new Guerrero("Maximo"),
                () -> new Mago("Circe"),
                () -> new Guerrero("Aquiles"),
                () -> new Mago("Merlina"));
        fabricasDeGladiadores.stream()
                .map(Supplier::get)
                .forEach(escuadron::agregar);
        return escuadron;
    }
}
