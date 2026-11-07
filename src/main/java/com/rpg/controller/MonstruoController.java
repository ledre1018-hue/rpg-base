package com.rpg.controller;

import com.rpg.dao.MonstruoDAO;
import com.rpg.dao.VictoriaDAO;
import com.rpg.model.Monstruo;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class MonstruoController {

    public static final String MENSAJE_SOLO_ADMIN = "Solo el rol admin puede crear, editar o eliminar monstruos.";
    private static final int LARGO_MAXIMO_NOMBRE = 50;
    private static final int VALOR_MINIMO = 1;
    private static final int SIN_VICTORIAS = 0;
    private static final String SIN_ERRORES = null;
    private static final Predicate<String> NOMBRE_VACIO = String::isBlank;
    private static final Predicate<String> NOMBRE_LARGO = nombre -> nombre.length() > LARGO_MAXIMO_NOMBRE;
    private static final Predicate<Integer> NO_POSITIVO = valor -> valor < VALOR_MINIMO;

    private final MonstruoDAO monstruoDAO = new MonstruoDAO();
    private final List<ReglaMonstruo> reglas = List.of(
            candidato -> NOMBRE_VACIO.test(candidato.getNombre())
                    ? "El nombre no puede estar vacio." : SIN_ERRORES,
            candidato -> NOMBRE_LARGO.test(candidato.getNombre())
                    ? "El nombre admite maximo " + LARGO_MAXIMO_NOMBRE + " caracteres." : SIN_ERRORES,
            candidato -> NO_POSITIVO.test(candidato.getVida()) || NO_POSITIVO.test(candidato.getAtaque())
                    ? "La vida y el ataque deben ser mayores que 0." : SIN_ERRORES,
            this::nombreRepetido);
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
        Monstruo nuevo = new Monstruo(nombre.trim(), vida, ataque);
        String error = validar(nuevo);
        if (error != null) {
            return error;
        }
        monstruoDAO.crear(nuevo);
        return "Monstruo " + nuevo.getNombre() + " creado.";
    }

    public String editar(int id, String nombre, int vida, int ataque) {
        if (!puedeAdministrar()) {
            return MENSAJE_SOLO_ADMIN;
        }
        if (!existe(id)) {
            return noExiste(id);
        }
        Monstruo editado = new Monstruo(id, nombre.trim(), vida, ataque);
        String error = validar(editado);
        if (error != null) {
            return error;
        }
        monstruoDAO.actualizar(editado);
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

    /** Aplica las reglas en orden y devuelve el primer error, o null si todas se cumplen. */
    private String validar(Monstruo candidato) {
        return reglas.stream()
                .map(regla -> regla.validar(candidato))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(SIN_ERRORES);
    }

    private String nombreRepetido(Monstruo candidato) {
        Monstruo existente = monstruoDAO.buscarPorNombre(candidato.getNombre());
        if (existente != null && existente.getId() != candidato.getId()) {
            return "Ya existe un monstruo llamado " + existente.getNombre() + ".";
        }
        return SIN_ERRORES;
    }

    private String noExiste(int id) {
        return "No existe un monstruo con id " + id + ".";
    }
}
