package com.rpg.controller;

import com.rpg.model.Jugador;

public class Sesion {

    private Jugador jugador;

    public void iniciar(Jugador jugador) {
        this.jugador = jugador;
    }

    public void cerrar() {
        jugador = null;
    }

    public boolean activa() {
        return jugador != null;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public boolean esAdmin() {
        return activa() && jugador.esAdmin();
    }
}
