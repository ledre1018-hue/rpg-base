package com.rpg.exceptions;

import java.io.Serial;

public class AccesoDatosException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AccesoDatosException(String mensaje, Throwable causa) {
        super(mensaje + ": " + causa.getMessage(), causa);
    }
}
