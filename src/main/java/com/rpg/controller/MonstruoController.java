package com.rpg.controller;

import com.rpg.dao.MonstruoDAO;
import com.rpg.dao.VictoriaDAO;
import com.rpg.model.Monstruo;

import java.util.List;

public class MonstruoController {

    public static final String MENSAJE_SOLO_ADMIN = "Solo el rol admin puede crear, editar o eliminar monstruos.";
    private static final int LARGO_MAXIMO_NOMBRE = 50;
    private static final int VALOR_MINIMO = 1;
    private static final int SIN_VICTORIAS = 0;
    private static final String SIN_ERRORES = null;

    private final MonstruoDAO monstruoDAO = new MonstruoDAO();
    private final VictoriaDAO victoriaDAO = new VictoriaDAO();
    private final Sesion sesion;

    public MonstruoController(Sesion sesion) {
        this.sesion = sesion;
    }

    public boolean puedeAdministrar() {
        return sesion.esAdmin();
    }

    public List<Monstruo> listar() {
        return monstruoDAO.listar();
    }

    public Monstruo buscarPorNombre(String nombre) {
        return monstruoDAO.buscarPorNombre(nombre.trim());
    }

    public boolean existe(int id) {
        return monstruoDAO.buscarPorId(id) != null;
    }

    public List<Monstruo> listarPorVidaMinima(int vidaMinima) {
        return monstruoDAO.listarPorVidaMinima(vidaMinima);
    }

    public String crear(String nombre, int vida, int ataque) {
        if (!puedeAdministrar()) {
            return MENSAJE_SOLO_ADMIN;
        }
        String error = validar(nombre, vida, ataque, Monstruo.SIN_ID);
        if (error != null) {
            return error;
        }
        monstruoDAO.crear(new Monstruo(nombre.trim(), vida, ataque));
        return "Monstruo " + nombre.trim() + " creado.";
    }

    public String editar(int id, String nombre, int vida, int ataque) {
        if (!puedeAdministrar()) {
            return MENSAJE_SOLO_ADMIN;
        }
        if (!existe(id)) {
            return noExiste(id);
        }
        String error = validar(nombre, vida, ataque, id);
        if (error != null) {
            return error;
        }
        monstruoDAO.actualizar(new Monstruo(id, nombre.trim(), vida, ataque));
        return "Monstruo #" + id + " actualizado.";
    }

    public String eliminar(int id) {
        if (!puedeAdministrar()) {
            return MENSAJE_SOLO_ADMIN;
        }
        Monstruo monstruo = monstruoDAO.buscarPorId(id);
        if (monstruo == null) {
            return noExiste(id);
        }
        int victorias = victoriaDAO.contarPorMonstruo(id);
        if (victorias > SIN_VICTORIAS) {
            return "No se puede eliminar " + monstruo.getNombre() + ": tiene " + victorias + " victorias registradas.";
        }
        monstruoDAO.eliminar(id);
        return "Monstruo " + monstruo.getNombre() + " eliminado.";
    }

    private String validar(String nombre, int vida, int ataque, int idActual) {
        if (nombre.isBlank()) {
            return "El nombre no puede estar vacio.";
        }
        if (nombre.trim().length() > LARGO_MAXIMO_NOMBRE) {
            return "El nombre admite maximo " + LARGO_MAXIMO_NOMBRE + " caracteres.";
        }
        if (vida < VALOR_MINIMO || ataque < VALOR_MINIMO) {
            return "La vida y el ataque deben ser mayores que 0.";
        }
        Monstruo existente = monstruoDAO.buscarPorNombre(nombre.trim());
        if (existente != null && existente.getId() != idActual) {
            return "Ya existe un monstruo llamado " + existente.getNombre() + ".";
        }
        return SIN_ERRORES;
    }

    private String noExiste(int id) {
        return "No existe un monstruo con id " + id + ".";
    }
}
