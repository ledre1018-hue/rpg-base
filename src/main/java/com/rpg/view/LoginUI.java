package com.rpg.view;

import com.rpg.controller.LoginController;
import com.rpg.core.Consola;
import com.rpg.exceptions.AccesoDatosException;

public class LoginUI {

    private final Consola consola;
    private final LoginController controller;

    public LoginUI(Consola consola, LoginController controller) {
        this.consola = consola;
        this.controller = controller;
    }

    public void mostrar() {
        if (controller.haySesion()) {
            System.out.println(controller.cerrarSesion());
            return;
        }
        if (controller.estaBloqueada()) {
            System.out.println(LoginController.MENSAJE_BLOQUEO);
            return;
        }
        System.out.println("\n=== LOGIN ===");
        try {
            pedirCredenciales();
        } catch (AccesoDatosException e) {
            System.out.println(e.getMessage());
        }
    }

    private void pedirCredenciales() {
        while (!controller.haySesion() && !controller.estaBloqueada()) {
            String usuario = consola.leerLinea("Usuario: ");
            String clave = consola.leerLinea("Clave: ");
            System.out.println(controller.iniciarSesion(usuario, clave));
        }
    }
}
