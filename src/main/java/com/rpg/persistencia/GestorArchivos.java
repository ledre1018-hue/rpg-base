package com.rpg.persistencia;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class GestorArchivos {

    public static final Object CANDADO = new Object();
    public static final File CARPETA = new File("partidas");
    public static final File PARTIDA = new File(CARPETA, "partida.dat");
    public static final File RESUMEN = new File(CARPETA, "partida.txt");
    public static final File LOG = new File(CARPETA, "combates.txt");

    private GestorArchivos() {
    }

    public static boolean prepararCarpeta() {
        if (CARPETA.isDirectory()) {
            return true;
        }
        if (CARPETA.mkdirs() || CARPETA.isDirectory()) {
            return true;
        }
        System.out.println("No se pudo crear la carpeta " + CARPETA.getAbsolutePath());
        return false;
    }

    public static boolean crearSiNoExiste(File archivo) {
        if (!prepararCarpeta()) {
            return false;
        }
        try {
            return archivo.createNewFile();
        } catch (IOException e) {
            System.out.println("No se pudo crear " + archivo.getPath() + ": " + e.getMessage());
            return false;
        }
    }

    public static String describir(File archivo) {
        File absoluto = new File(archivo.getAbsolutePath());
        return String.format("%s%n  ruta relativa: %s%n  ruta absoluta: %s%n  existe: %s | tamano: %d bytes",
                archivo.getName(), archivo.getPath(), absoluto.getPath(),
                archivo.exists() ? "si" : "no", archivo.length());
    }

    public static boolean borrarConFile(File archivo) {
        synchronized (CANDADO) {
            return archivo.delete();
        }
    }

    public static boolean borrarSiExiste(File archivo) {
        synchronized (CANDADO) {
            try {
                return Files.deleteIfExists(archivo.toPath());
            } catch (IOException e) {
                System.out.println("No se pudo borrar " + archivo.getPath() + ": " + e.getMessage());
                return false;
            }
        }
    }
}
