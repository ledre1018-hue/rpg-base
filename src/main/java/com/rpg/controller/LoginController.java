package com.rpg.controller;

import com.rpg.dao.JugadorDAO;
import com.rpg.model.Jugador;

public class LoginController {

    public static final int MAXIMO_INTENTOS = 3;
    public static final String MENSAJE_BLOQUEO =
            "Sesion bloqueada: superaste los " + MAXIMO_INTENTOS + " intentos de login.";
    private static final int SIN_INTENTOS = 0;

    private final JugadorDAO jugadorDAO = new JugadorDAO();
    private final Sesion sesion;
    private int intentosFallidos = SIN_INTENTOS;

    public LoginController(Sesion sesion) {
        this.sesion = sesion;
    }

    public boolean estaBloqueada() {
        return intentosFallidos >= MAXIMO_INTENTOS;
    }

    public boolean haySesion() {
        return sesion.activa();
    }

    public String usuarioActual() {
        return sesion.getJugador().toString();
    }

    public String iniciarSesion(String usuario, String clave) {
        if (estaBloqueada()) {
            return MENSAJE_BLOQUEO;
        }
        if (usuario.isBlank() || clave.isBlank()) {
            return "Usuario y clave son obligatorios.";
        }
        Jugador jugador = jugadorDAO.autenticar(usuario, clave);
        if (jugador == null) {
            return registrarFallo();
        }
        intentosFallidos = SIN_INTENTOS;
        sesion.iniciar(jugador);
        return "Bienvenido, " + jugador + ".";
    }

    public String cerrarSesion() {
        String mensaje = "Sesion de " + usuarioActual() + " cerrada.";
        sesion.cerrar();
        return mensaje;
    }

    private String registrarFallo() {
        intentosFallidos++;
        if (estaBloqueada()) {
            return MENSAJE_BLOQUEO;
        }
        return "Usuario o clave incorrectos. Intentos restantes: " + (MAXIMO_INTENTOS - intentosFallidos) + ".";
    }
}
