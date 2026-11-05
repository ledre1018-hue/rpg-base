package com.rpg;

import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;

import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {

    private static final int OPCION_SALIR = 0;
    private static final int ULTIMA_OPCION = 7;

    public static void main(String[] args) {
        Consola consola = new Consola(new Scanner(System.in));
        MotorJuego motor = new MotorJuego(consola);
        motor.iniciarAutoGuardado();
        try {
            ejecutarMenu(consola, motor);
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Hasta luego.");
        }
    }

    private static void ejecutarMenu(Consola consola, MotorJuego motor) {
        int opcion;
        do {
            mostrarMenu();
            opcion = consola.leerEntero("Opcion: ", OPCION_SALIR, ULTIMA_OPCION);
            switch (opcion) {
                case 1 -> motor.combateRapido();
                case 2 -> motor.jugarTorneo();
                case 3 -> motor.mostrarBestiario();
                case 4 -> motor.cargarProgreso();
                case 5 -> motor.mostrarHistorial();
                case 6 -> motor.mostrarArchivos();
                case 7 -> motor.borrarPartida();
                default -> System.out.println("Hasta luego.");
            }
        } while (opcion != OPCION_SALIR);
    }

    private static void mostrarMenu() {
        System.out.println("\n=== SIMULADOR DE BATALLA RPG ===");
        System.out.println("1. Combate rapido");
        System.out.println("2. Torneo");
        System.out.println("3. Ver bestiario");
        System.out.println("4. Cargar partida");
        System.out.println("5. Ver historial de combates");
        System.out.println("6. Ver archivos de partida");
        System.out.println("7. Borrar partida guardada");
        System.out.println("0. Salir");
    }
}
