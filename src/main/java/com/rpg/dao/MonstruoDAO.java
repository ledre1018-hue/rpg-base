package com.rpg.dao;

import com.rpg.exceptions.AccesoDatosException;
import com.rpg.model.Monstruo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MonstruoDAO {

    private static final String COLUMNAS = "SELECT id, nombre, vida, ataque FROM monstruos";
    private static final String SQL_CREAR = "INSERT INTO monstruos (nombre, vida, ataque) VALUES (?, ?, ?)";
    private static final String SQL_BUSCAR_POR_ID = COLUMNAS + " WHERE id = ?";
    private static final String SQL_BUSCAR_POR_NOMBRE = COLUMNAS + " WHERE nombre = ?";
    private static final String SQL_LISTAR = COLUMNAS + " ORDER BY id";
    private static final String SQL_LISTAR_POR_VIDA_MINIMA = COLUMNAS + " WHERE vida >= ? ORDER BY vida";
    private static final String SQL_ACTUALIZAR = "UPDATE monstruos SET nombre = ?, vida = ?, ataque = ? WHERE id = ?";
    private static final String SQL_ELIMINAR = "DELETE FROM monstruos WHERE id = ?";

    public boolean crear(Monstruo monstruo) {
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_CREAR)) {
            asignarDatos(sentencia, monstruo);
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo crear el monstruo", e);
        }
    }

    public Monstruo buscarPorId(int id) {
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_POR_ID)) {
            sentencia.setInt(Parametro.PRIMERO, id);
            return buscarUno(sentencia);
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo buscar el monstruo", e);
        }
    }

    public Monstruo buscarPorNombre(String nombre) {
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_POR_NOMBRE)) {
            sentencia.setString(Parametro.PRIMERO, nombre);
            return buscarUno(sentencia);
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo buscar el monstruo", e);
        }
    }

    public List<Monstruo> listar() {
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR)) {
            return buscarVarios(sentencia);
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo listar los monstruos", e);
        }
    }

    public List<Monstruo> listarPorVidaMinima(int vidaMinima) {
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR_POR_VIDA_MINIMA)) {
            sentencia.setInt(Parametro.PRIMERO, vidaMinima);
            return buscarVarios(sentencia);
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo listar los monstruos", e);
        }
    }

    public boolean actualizar(Monstruo monstruo) {
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR)) {
            asignarDatos(sentencia, monstruo);
            sentencia.setInt(Parametro.CUARTO, monstruo.getId());
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo actualizar el monstruo", e);
        }
    }

    public boolean eliminar(int id) {
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ELIMINAR)) {
            sentencia.setInt(Parametro.PRIMERO, id);
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo eliminar el monstruo", e);
        }
    }

    private void asignarDatos(PreparedStatement sentencia, Monstruo monstruo) throws SQLException {
        sentencia.setString(Parametro.PRIMERO, monstruo.getNombre());
        sentencia.setInt(Parametro.SEGUNDO, monstruo.getVida());
        sentencia.setInt(Parametro.TERCERO, monstruo.getAtaque());
    }

    private Monstruo buscarUno(PreparedStatement sentencia) throws SQLException {
        try (ResultSet filas = sentencia.executeQuery()) {
            return filas.next() ? mapear(filas) : null;
        }
    }

    private List<Monstruo> buscarVarios(PreparedStatement sentencia) throws SQLException {
        List<Monstruo> monstruos = new ArrayList<>();
        try (ResultSet filas = sentencia.executeQuery()) {
            while (filas.next()) {
                monstruos.add(mapear(filas));
            }
        }
        return monstruos;
    }

    private Monstruo mapear(ResultSet filas) throws SQLException {
        return new Monstruo(filas.getInt("id"), filas.getString("nombre"), filas.getInt("vida"), filas.getInt("ataque"));
    }
}
