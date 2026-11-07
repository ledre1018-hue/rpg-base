package com.rpg.model;

public class Jugador {

    public static final String ROL_JUGADOR = "jugador";
    public static final String ROL_ADMIN = "admin";

    private final int id;
    private final String usuario;
    private final String rol;

    public Jugador(int id, String usuario, String rol) {
        this.id = id;
        this.usuario = usuario;
        this.rol = rol;
    }

    public int getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getRol() {
        return rol;
    }

    public boolean esAdmin() {
        return ROL_ADMIN.equals(rol);
    }

    @Override
    public String toString() {
        return usuario + " (" + rol + ")";
    }
}
