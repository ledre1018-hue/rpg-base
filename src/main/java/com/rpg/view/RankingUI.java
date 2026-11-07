package com.rpg.view;

import com.rpg.controller.RankingController;
import com.rpg.core.Consola;
import com.rpg.exceptions.AccesoDatosException;
import com.rpg.model.PuestoRanking;
import com.rpg.model.Victoria;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class RankingUI {

    private static final int VOLVER = 0;
    private static final int ULTIMA_OPCION = 2;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Consola consola;
    private final RankingController controller;

    public RankingUI(Consola consola, RankingController controller) {
        this.consola = consola;
        this.controller = controller;
    }

    public void mostrar() {
        int opcion;
        do {
            System.out.println("\n=== RANKING ===");
            System.out.println("1. Ranking por jugador");
            System.out.println("2. Victorias de un jugador");
            System.out.println("0. Volver");
            opcion = consola.leerEntero("Opcion: ", VOLVER, ULTIMA_OPCION);
            ejecutar(opcion);
        } while (opcion != VOLVER);
    }

    public void registrarVictoria(String nombreMonstruo) {
        try {
            System.out.println(controller.registrarVictoria(nombreMonstruo));
        } catch (AccesoDatosException e) {
            System.out.println(e.getMessage());
        }
    }

    private void ejecutar(int opcion) {
        try {
            switch (opcion) {
                case 1 -> imprimirRanking(controller.ranking());
                case 2 -> imprimirVictorias(controller.victoriasDe(consola.leerLinea("Usuario: ")));
                default -> System.out.println("Volviendo al menu principal.");
            }
        } catch (AccesoDatosException e) {
            System.out.println(e.getMessage());
        }
    }

    private void imprimirRanking(List<PuestoRanking> ranking) {
        if (ranking.isEmpty()) {
            System.out.println("Todavia no hay victorias registradas.");
        }
        for (int i = 0; i < ranking.size(); i++) {
            PuestoRanking puesto = ranking.get(i);
            System.out.printf("%d. %-15s %d victorias%n", i + 1, puesto.getUsuario(), puesto.getTotalVictorias());
        }
    }

    private void imprimirVictorias(List<Victoria> victorias) {
        if (victorias.isEmpty()) {
            System.out.println("Ese jugador no tiene victorias registradas.");
        }
        victorias.forEach(victoria ->
                System.out.printf("%-15s %s%n", victoria.getMonstruo(), victoria.getFecha().format(FORMATO_FECHA)));
    }
}
