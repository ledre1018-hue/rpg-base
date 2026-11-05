package com.rpg.core;

import com.rpg.exceptions.TiempoAgotadoException;

import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** Entrada por teclado con validacion y reintento. */
public class Consola {

    private static final int LARGO_MAXIMO_NOMBRE = 15;
    private static final String PATRON_NOMBRE = "[\\p{L}0-9 ]+";

    private static final String FIN_ENTRADA = "\u0000FIN_ENTRADA";
    private static final String TIEMPO_AGOTADO = "\u0000TIEMPO_AGOTADO";

    private final BlockingQueue<String> lineas = new LinkedBlockingQueue<>();

    public Consola(Scanner scanner) {
        Thread lector = new Thread(() -> leerEntrada(scanner), "lector-consola");
        lector.setDaemon(true);
        lector.start();
    }

    private void leerEntrada(Scanner scanner) {
        try {
            while (scanner.hasNextLine()) {
                lineas.put(scanner.nextLine());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IllegalStateException e) {
            System.out.println("La entrada se cerro: " + e.getMessage());
        } finally {
            lineas.offer(FIN_ENTRADA);
        }
    }

    public String leerLinea(String mensaje) {
        System.out.print(mensaje);
        String linea;
        try {
            linea = lineas.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NoSuchElementException("Lectura interrumpida");
        }
        if (FIN_ENTRADA.equals(linea)) {
            lineas.offer(FIN_ENTRADA);
            throw new NoSuchElementException("Fin de la entrada");
        }
        if (TIEMPO_AGOTADO.equals(linea)) {
            throw new TiempoAgotadoException();
        }
        return linea.trim();
    }

    public void avisarTiempoAgotado() {
        lineas.offer(TIEMPO_AGOTADO);
    }

    public void descartarAvisosDeTiempo() {
        lineas.removeIf(TIEMPO_AGOTADO::equals);
    }

    public boolean confirmar(String mensaje) {
        while (true) {
            String respuesta = leerLinea(mensaje + " (s/n): ");
            if (respuesta.equalsIgnoreCase("s")) {
                return true;
            }
            if (respuesta.equalsIgnoreCase("n")) {
                return false;
            }
            System.out.println("Responde s o n.");
        }
    }

    public int leerEntero(String mensaje, int minimo, int maximo) {
        while (true) {
            String texto = leerLinea(mensaje);
            if (texto.isEmpty()) {
                System.out.println("No escribiste nada.");
                continue;
            }
            try {
                int valor = Integer.parseInt(texto);
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
                System.out.printf("Numero fuera de rango (%d a %d).%n", minimo, maximo);
            } catch (NumberFormatException e) {
                System.out.println("\"" + texto + "\" no es un numero.");
            }
        }
    }

    public String leerNombre(String mensaje) {
        while (true) {
            String nombre = leerLinea(mensaje);
            if (nombre.isEmpty()) {
                System.out.println("El nombre no puede estar vacio.");
            } else if (nombre.length() > LARGO_MAXIMO_NOMBRE) {
                System.out.println("Maximo " + LARGO_MAXIMO_NOMBRE + " caracteres.");
            } else if (!nombre.matches(PATRON_NOMBRE)) {
                System.out.println("Usa solo letras, numeros y espacios.");
            } else {
                return nombre;
            }
        }
    }
}
