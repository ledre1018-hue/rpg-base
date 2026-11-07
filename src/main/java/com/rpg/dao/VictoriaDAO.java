package com.rpg.dao;

import com.rpg.exceptions.AccesoDatosException;
import com.rpg.model.PuestoRanking;
import com.rpg.model.Victoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VictoriaDAO {

    private static final String SQL_REGISTRAR =
            "INSERT INTO victorias (jugador_id, monstruo_id, fecha) VALUES (?, ?, NOW())";
    private static final String SQL_RANKING =
            "SELECT j.usuario, COUNT(v.id) AS total FROM victorias v "
                    + "JOIN jugadores j ON v.jugador_id = j.id "
                    + "GROUP BY j.id, j.usuario ORDER BY total DESC";
    private static final String SQL_VICTORIAS_DE_JUGADOR =
            "SELECT m.nombre, v.fecha FROM victorias v "
                    + "JOIN jugadores j ON v.jugador_id = j.id "
                    + "JOIN monstruos m ON v.monstruo_id = m.id "
                    + "WHERE j.usuario = ? ORDER BY v.fecha DESC";
    private static final String SQL_CONTAR_POR_MONSTRUO =
            "SELECT COUNT(*) AS total FROM victorias WHERE monstruo_id = ?";

    public boolean registrar(int jugadorId, int monstruoId) {
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_REGISTRAR)) {
            sentencia.setInt(Parametro.PRIMERO, jugadorId);
            sentencia.setInt(Parametro.SEGUNDO, monstruoId);
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo registrar la victoria", e);
        }
    }

    public List<PuestoRanking> rankingPorJugador() {
        List<PuestoRanking> ranking = new ArrayList<>();
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_RANKING);
             ResultSet filas = sentencia.executeQuery()) {
            while (filas.next()) {
                ranking.add(new PuestoRanking(filas.getString("usuario"), filas.getInt("total")));
            }
            return ranking;
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo obtener el ranking", e);
        }
    }

    public List<Victoria> victoriasDeJugador(String usuario) {
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_VICTORIAS_DE_JUGADOR)) {
            sentencia.setString(Parametro.PRIMERO, usuario);
            return leerVictorias(sentencia);
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo obtener las victorias", e);
        }
    }

    public int contarPorMonstruo(int monstruoId) {
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_CONTAR_POR_MONSTRUO)) {
            sentencia.setInt(Parametro.PRIMERO, monstruoId);
            try (ResultSet filas = sentencia.executeQuery()) {
                filas.next();
                return filas.getInt("total");
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo contar las victorias", e);
        }
    }

    private List<Victoria> leerVictorias(PreparedStatement sentencia) throws SQLException {
        List<Victoria> victorias = new ArrayList<>();
        try (ResultSet filas = sentencia.executeQuery()) {
            while (filas.next()) {
                victorias.add(new Victoria(filas.getString("nombre"), filas.getObject("fecha", LocalDateTime.class)));
            }
        }
        return victorias;
    }
}
