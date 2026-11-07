package com.rpg.controller;

import com.rpg.model.Monstruo;

/** Regla de validacion de un monstruo: devuelve el mensaje de error, o null si la regla se cumple. */
@FunctionalInterface
public interface ReglaMonstruo {

    String validar(Monstruo candidato);
}
