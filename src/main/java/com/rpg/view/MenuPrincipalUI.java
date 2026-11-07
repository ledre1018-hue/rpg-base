package com.rpg.view;

import com.rpg.controller.LoginController;
import com.rpg.controller.MonstruoController;
import com.rpg.controller.RankingController;
import com.rpg.controller.Sesion;
import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;

import java.util.NoSuchElementException;
import java.util.Scanner;

public class MenuPrincipalUI {

    private static final int OPCION_SALIR = 0;
    private static final int ULTIMA_OPCION = 10;

    private final Consola consola = new Consola(new Scanner(System.in));
    private final MotorJuego motor = new MotorJuego(consola);
    private final Sesion sesion = new Sesion();
    private final LoginController loginController = new LoginController(sesion);
    private final LoginUI loginUI = new LoginUI(consola, loginController);
    private final MonstruoUI monstruoUI = new MonstruoUI(consola, new MonstruoController(sesion));
    private final RankingUI rankingUI = new RankingUI(consola, new RankingController(sesion));

    public void iniciar() {
        motor.iniciarAutoGuardado();
        motor.setAlVencerMonstruo(rankingUI::registrarVictoria);
        try {
            ejecutarMenu();
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Hasta luego.");
        }
    }

    private void ejecutarMenu() {
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
                case 8 -> loginUI.mostrar();
                case 9 -> monstruoUI.mostrar();
                case 10 -> rankingUI.mostrar();
                default -> System.out.println("Hasta luego.");
            }
        } while (opcion != OPCION_SALIR);
    }

    private void mostrarMenu() {
        System.out.println("\n=== SIMULADOR DE BATALLA RPG ===");
        System.out.println("1. Combate rapido");
        System.out.println("2. Torneo");
        System.out.println("3. Ver bestiario");
        System.out.println("4. Cargar partida");
        System.out.println("5. Ver historial de combates");
        System.out.println("6. Ver archivos de partida");
        System.out.println("7. Borrar partida guardada");
        System.out.println(loginController.haySesion()
                ? "8. Cerrar sesion de " + loginController.usuarioActual()
                : "8. Iniciar sesion");
        System.out.println("9. Monstruos (base de datos)");
        System.out.println("10. Ranking de victorias");
        System.out.println("0. Salir");
    }
}
