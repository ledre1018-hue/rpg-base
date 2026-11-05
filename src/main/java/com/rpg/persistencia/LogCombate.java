package com.rpg.persistencia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LogCombate {

    private final File archivo;

    public LogCombate() {
        this(GestorArchivos.LOG);
    }

    public LogCombate(File archivo) {
        this.archivo = archivo;
    }

    public boolean escribir(String texto, boolean agregar) {
        synchronized (GestorArchivos.CANDADO) {
            if (!GestorArchivos.prepararCarpeta()) {
                return false;
            }
            try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivo, agregar))) {
                for (String linea : texto.split("\\R")) {
                    escritor.write(linea);
                    escritor.newLine();
                }
                escritor.flush();
                return true;
            } catch (IOException e) {
                System.out.println("No se pudo escribir " + archivo.getPath() + ": " + e.getMessage());
                return false;
            }
        }
    }

    public List<String> leer() {
        List<String> lineas = new ArrayList<>();
        synchronized (GestorArchivos.CANDADO) {
            try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    lineas.add(linea);
                }
            } catch (FileNotFoundException e) {
                System.out.println("Aun no existe el archivo " + archivo.getPath() + ". Juega un combate primero.");
            } catch (IOException e) {
                System.out.println("No se pudo leer " + archivo.getPath() + ": " + e.getMessage());
            }
        }
        return lineas;
    }

    public File getArchivo() {
        return archivo;
    }
}
