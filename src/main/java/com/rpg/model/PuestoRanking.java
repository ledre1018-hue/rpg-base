package com.rpg.model;

public class PuestoRanking {

    private final String usuario;
    private final int totalVictorias;

    public PuestoRanking(String usuario, int totalVictorias) {
        this.usuario = usuario;
        this.totalVictorias = totalVictorias;
    }

    public String getUsuario() {
        return usuario;
    }

    public int getTotalVictorias() {
        return totalVictorias;
    }

    @Override
    public String toString() {
        return usuario + ": " + totalVictorias;
    }
}
