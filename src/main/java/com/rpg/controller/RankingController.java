package com.rpg.controller;

import com.rpg.dao.MonstruoDAO;
import com.rpg.dao.VictoriaDAO;
import com.rpg.model.Jugador;
import com.rpg.model.Monstruo;
import com.rpg.model.PuestoRanking;
import com.rpg.model.Victoria;

import java.util.List;

public class RankingController {

    private final VictoriaDAO victoriaDAO = new VictoriaDAO();
    private final MonstruoDAO monstruoDAO = new MonstruoDAO();
    private final Sesion sesion;

    public RankingController(Sesion sesion) {
        this.sesion = sesion;
    }

    public List<PuestoRanking> ranking() {
        return victoriaDAO.rankingPorJugador();
    }

    public List<Victoria> victoriasDe(String usuario) {
        return victoriaDAO.victoriasDeJugador(usuario.trim());
    }

    public String registrarVictoria(String nombreMonstruo) {
        if (!sesion.activa()) {
            return "Victoria no registrada en el ranking: no hay sesion iniciada.";
        }
        Monstruo monstruo = monstruoDAO.buscarPorNombre(nombreMonstruo);
        if (monstruo == null) {
            return "Victoria no registrada en el ranking: " + nombreMonstruo + " no esta en la tabla monstruos.";
        }
        Jugador jugador = sesion.getJugador();
        victoriaDAO.registrar(jugador.getId(), monstruo.getId());
        return "Victoria contra " + monstruo.getNombre() + " registrada para " + jugador.getUsuario() + ".";
    }
}
