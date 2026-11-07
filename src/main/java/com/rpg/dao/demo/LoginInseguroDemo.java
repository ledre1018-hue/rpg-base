package com.rpg.dao.demo;

import com.rpg.dao.ConexionDB;
import com.rpg.dao.JugadorDAO;
import com.rpg.exceptions.AccesoDatosException;
import com.rpg.model.Jugador;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Solo con fines didacticos: login vulnerable a inyeccion SQL por concatenar texto. */
public class LoginInseguroDemo {

    private static final String USUARIO_ATAQUE = "admin' OR 1=1 -- ";
    private static final String CLAVE_CUALQUIERA = "no_la_se";

    public boolean autenticar(String usuario, String clave) throws SQLException {
        String sql = "SELECT id FROM jugadores WHERE usuario = '" + usuario + "' AND clave = '" + clave + "'";
        System.out.println("SQL enviado: " + sql);
        try (Connection conexion = ConexionDB.obtenerConexion();
             Statement sentencia = conexion.createStatement();
             ResultSet filas = sentencia.executeQuery(sql)) {
            return filas.next();
        }
    }

    public static void main(String[] args) {
        System.out.println("Usuario: [" + USUARIO_ATAQUE + "]  Clave: [" + CLAVE_CUALQUIERA + "]");
        probarInseguro();
        probarSeguro();
    }

    private static void probarInseguro() {
        System.out.println("\n--- Login inseguro (Statement + concatenacion) ---");
        try {
            boolean entro = new LoginInseguroDemo().autenticar(USUARIO_ATAQUE, CLAVE_CUALQUIERA);
            System.out.println(entro ? "ACCESO CONCEDIDO sin conocer la clave" : "Acceso denegado");
        } catch (SQLException e) {
            System.out.println("Error en la base de datos: " + e.getMessage());
        }
    }

    private static void probarSeguro() {
        System.out.println("\n--- Login seguro (JugadorDAO.autenticar con PreparedStatement) ---");
        try {
            Jugador jugador = new JugadorDAO().autenticar(USUARIO_ATAQUE, CLAVE_CUALQUIERA);
            System.out.println(jugador != null ? "ACCESO CONCEDIDO a " + jugador : "Acceso denegado");
        } catch (AccesoDatosException e) {
            System.out.println(e.getMessage());
        }
    }
}
