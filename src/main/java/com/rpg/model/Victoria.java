package com.rpg.model;

import java.time.LocalDateTime;

public class Victoria {

    private final String monstruo;
    private final LocalDateTime fecha;

    public Victoria(String monstruo, LocalDateTime fecha) {
        this.monstruo = monstruo;
        this.fecha = fecha;
    }

    public String getMonstruo() {
        return monstruo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    @Override
    public String toString() {
        return monstruo + " - " + fecha;
    }
}
