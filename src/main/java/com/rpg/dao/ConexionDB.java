package com.rpg.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * JDBC (Java Database Connectivity) es la API estandar de Java (paquete java.sql) para hablar
 * con una base de datos relacional: abrir una conexion, enviar SQL y recorrer los resultados,
 * sin que el codigo dependa de que motor hay detras. Cada motor aporta su propio driver
 * (aqui mysql-connector-j) que traduce esas llamadas al protocolo de MySQL.
 *
 * DriverManager es la clase que administra los drivers cargados. Con getConnection(url, usuario,
 * clave) revisa la URL (jdbc:mysql://...), busca un driver registrado que la acepte y le pide
 * que abra la conexion; si ninguno la acepta, lanza SQLException.
 *
 * Si falta el driver (se quito la dependencia del pom.xml), el proyecto compila igual porque
 * java.sql es parte del JDK, pero al conectar ningun driver reconoce la URL y falla en tiempo
 * de ejecucion. Mensaje exacto obtenido al reproducirlo:
 * java.sql.SQLException: No suitable driver found for jdbc:mysql://localhost:3306/rpg_arena_db
 */
public final class ConexionDB {

    private static final File ARCHIVO_CONFIG = new File("config.txt");
    private static final String SEPARADOR = "=";
    private static final int PARTES_POR_LINEA = 2;
    private static final int POSICION_CLAVE = 0;
    private static final int POSICION_VALOR = 1;
    private static final String CLAVE_URL = "url";
    private static final String CLAVE_USUARIO = "usuario";
    private static final String CLAVE_CLAVE = "clave";
    private static final List<String> CLAVES_OBLIGATORIAS = List.of(CLAVE_URL, CLAVE_USUARIO, CLAVE_CLAVE);

    private ConexionDB() {
    }

    public static Connection obtenerConexion() throws SQLException {
        Map<String, String> config;
        try {
            config = leerConfiguracion();
        } catch (IOException e) {
            throw new SQLException(e.getMessage(), e);
        }
        return DriverManager.getConnection(config.get(CLAVE_URL), config.get(CLAVE_USUARIO), config.get(CLAVE_CLAVE));
    }

    private static Map<String, String> leerConfiguracion() throws IOException {
        if (!ARCHIVO_CONFIG.exists()) {
            throw new FileNotFoundException("No se encontro " + ARCHIVO_CONFIG.getAbsolutePath()
                    + ". Copia config.example.txt como config.txt y completa tus datos.");
        }
        Map<String, String> valores = new HashMap<>();
        try (BufferedReader lector = new BufferedReader(new FileReader(ARCHIVO_CONFIG))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                agregarLinea(valores, linea);
            }
        }
        validarClaves(valores);
        return valores;
    }

    private static void agregarLinea(Map<String, String> valores, String linea) {
        if (linea.isBlank()) {
            return;
        }
        String[] partes = linea.split(SEPARADOR, PARTES_POR_LINEA);
        if (partes.length == PARTES_POR_LINEA) {
            valores.put(partes[POSICION_CLAVE].trim(), partes[POSICION_VALOR].trim());
        }
    }

    private static void validarClaves(Map<String, String> valores) throws IOException {
        for (String clave : CLAVES_OBLIGATORIAS) {
            if (!valores.containsKey(clave)) {
                throw new IOException("Falta la clave '" + clave + "' en " + ARCHIVO_CONFIG.getName() + ".");
            }
        }
    }
}
