package com.rpg.exceptions;

import java.io.Serial;

public class TiempoAgotadoException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TiempoAgotadoException() {
        super("Se agoto el tiempo del turno");
    }
}
