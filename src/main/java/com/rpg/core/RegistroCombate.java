package com.rpg.core;

import com.rpg.persistencia.LogCombate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Imprime cada evento del combate y lo guarda para incluirlo en el guardado de la partida. */
public class RegistroCombate {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringBuilder texto = new StringBuilder();

    public synchronized void agregar(String linea) {
        System.out.println(linea);
        texto.append(linea).append(System.lineSeparator());
    }

    public void agregar(String formato, Object... argumentos) {
        agregar(String.format(formato, argumentos));
    }

    public synchronized void limpiar() {
        texto.setLength(0);
    }

    public boolean volcarEnArchivo(LogCombate log) {
        String encabezado = "##### Combate del " + LocalDateTime.now().format(FORMATO_FECHA) + " #####";
        return log.escribir(encabezado + System.lineSeparator() + getTexto(), true);
    }

    public synchronized String getTexto() {
        return texto.toString();
    }
}
