package com.rpg.dao;

import com.rpg.exceptions.AccesoDatosException;
import com.rpg.model.Jugador;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JugadorDAO {

    private static final String SQL_REGISTRAR = "INSERT INTO jugadores (usuario, clave, rol) VALUES (?, ?, ?)";
    private static final String SQL_AUTENTICAR =
            "SELECT id, usuario, rol FROM jugadores WHERE usuario = ? AND clave = ?";
    private static final String SQL_CAMBIAR_ROL = "UPDATE jugadores SET rol = ? WHERE id = ?";
    private static final String SQL_ELIMINAR = "DELETE FROM jugadores WHERE id = ?";

    public boolean registrar(String usuario, String clave, String rol) {
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_REGISTRAR)) {
            sentencia.setString(Parametro.PRIMERO, usuario);
            sentencia.setString(Parametro.SEGUNDO, HashClave.sha256(clave));
            sentencia.setString(Parametro.TERCERO, rol);
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo registrar el jugador", e);
        }
    }

    public Jugador autenticar(String usuario, String clave) {
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_AUTENTICAR)) {
            sentencia.setString(Parametro.PRIMERO, usuario);
            sentencia.setString(Parametro.SEGUNDO, HashClave.sha256(clave));
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? mapear(filas) : null;
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo validar el login", e);
        }
    }

    public boolean cambiarRol(int id, String rol) {
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_CAMBIAR_ROL)) {
            sentencia.setString(Parametro.PRIMERO, rol);
            sentencia.setInt(Parametro.SEGUNDO, id);
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo cambiar el rol", e);
        }
    }

    public boolean eliminar(int id) {
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ELIMINAR)) {
            sentencia.setInt(Parametro.PRIMERO, id);
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo eliminar el jugador", e);
        }
    }

    private Jugador mapear(ResultSet filas) throws SQLException {
        return new Jugador(filas.getInt("id"), filas.getString("usuario"), filas.getString("rol"));
    }
}
