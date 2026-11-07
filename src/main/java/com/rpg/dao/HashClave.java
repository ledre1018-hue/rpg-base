package com.rpg.dao;

import com.rpg.exceptions.AccesoDatosException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Convierte una clave en su hash SHA-256 (64 caracteres hexadecimales en minuscula). */
final class HashClave {

    private static final String ALGORITMO = "SHA-256";

    private HashClave() {
    }

    static String sha256(String clave) {
        try {
            MessageDigest resumen = MessageDigest.getInstance(ALGORITMO);
            byte[] bytes = resumen.digest(clave.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new AccesoDatosException("Esta JVM no soporta " + ALGORITMO, e);
        }
    }
}
