package com.rpg.view;

import com.rpg.controller.MonstruoController;
import com.rpg.core.Consola;
import com.rpg.exceptions.AccesoDatosException;
import com.rpg.model.Monstruo;

import java.util.List;

public class MonstruoUI {

    private static final int VOLVER = 0;
    private static final int ULTIMA_OPCION = 6;
    private static final int ID_MINIMO = 1;

    private final Consola consola;
    private final MonstruoController controller;

    public MonstruoUI(Consola consola, MonstruoController controller) {
        this.consola = consola;
        this.controller = controller;
    }

    public void mostrar() {
        int opcion;
        do {
            mostrarMenu();
            opcion = consola.leerEntero("Opcion: ", VOLVER, ULTIMA_OPCION);
            ejecutar(opcion);
        } while (opcion != VOLVER);
    }

    private void mostrarMenu() {
        System.out.println("\n=== MONSTRUOS ===");
        System.out.println("1. Listar");
        System.out.println("2. Buscar por nombre");
        System.out.println("3. Listar por vida minima");
        System.out.println("4. Crear (admin)");
        System.out.println("5. Editar (admin)");
        System.out.println("6. Eliminar (admin)");
        System.out.println("0. Volver");
    }

    private void ejecutar(int opcion) {
        try {
            switch (opcion) {
                case 1 -> imprimir(controller.listar());
                case 2 -> buscarPorNombre();
                case 3 -> imprimir(controller.listarPorVidaMinima(leerNumero("Vida minima: ")));
                case 4 -> crear();
                case 5 -> editar();
                case 6 -> eliminar();
                default -> System.out.println("Volviendo al menu principal.");
            }
        } catch (AccesoDatosException e) {
            System.out.println(e.getMessage());
        }
    }

    private void buscarPorNombre() {
        Monstruo monstruo = controller.buscarPorNombre(consola.leerLinea("Nombre: "));
        System.out.println(monstruo != null ? monstruo : "No existe un monstruo con ese nombre.");
    }

    private void crear() {
        if (sinPermiso()) {
            return;
        }
        String nombre = consola.leerLinea("Nombre: ");
        System.out.println(controller.crear(nombre, leerNumero("Vida: "), leerNumero("Ataque: ")));
    }

    private void editar() {
        if (sinPermiso()) {
            return;
        }
        int id = leerId();
        if (!controller.existe(id)) {
            System.out.println("No existe un monstruo con id " + id + ".");
            return;
        }
        String nombre = consola.leerLinea("Nuevo nombre: ");
        System.out.println(controller.editar(id, nombre, leerNumero("Nueva vida: "), leerNumero("Nuevo ataque: ")));
    }

    private void eliminar() {
        if (sinPermiso()) {
            return;
        }
        System.out.println(controller.eliminar(leerId()));
    }

    private boolean sinPermiso() {
        if (controller.puedeAdministrar()) {
            return false;
        }
        System.out.println(MonstruoController.MENSAJE_SOLO_ADMIN);
        return true;
    }

    private int leerId() {
        return consola.leerEntero("Id del monstruo: ", ID_MINIMO, Integer.MAX_VALUE);
    }

    private int leerNumero(String mensaje) {
        return consola.leerEntero(mensaje, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private void imprimir(List<Monstruo> monstruos) {
        if (monstruos.isEmpty()) {
            System.out.println("No hay monstruos que mostrar.");
        }
        monstruos.forEach(System.out::println);
    }
}
