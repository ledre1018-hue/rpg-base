package com.rpg.model;

public class Monstruo {

    public static final int SIN_ID = 0;

    private final int id;
    private final String nombre;
    private final int vida;
    private final int ataque;

    public Monstruo(int id, String nombre, int vida, int ataque) {
        this.id = id;
        this.nombre = nombre;
        this.vida = vida;
        this.ataque = ataque;
    }

    public Monstruo(String nombre, int vida, int ataque) {
        this(SIN_ID, nombre, vida, ataque);
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getVida() {
        return vida;
    }

    public int getAtaque() {
        return ataque;
    }

    @Override
    public String toString() {
        return String.format("#%d %s (vida %d, ataque %d)", id, nombre, vida, ataque);
    }
}
