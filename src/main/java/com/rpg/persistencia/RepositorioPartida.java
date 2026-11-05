package com.rpg.persistencia;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class RepositorioPartida {

    private static final String CLASES_PERMITIDAS =
            "maxdepth=50;com.rpg.**;java.lang.*;java.util.*;java.time.*;!*";

    private final File archivo;

    public RepositorioPartida() {
        this(GestorArchivos.PARTIDA);
    }

    public RepositorioPartida(File archivo) {
        this.archivo = archivo;
    }

    public boolean guardar(PartidaGuardada partida) {
        synchronized (GestorArchivos.CANDADO) {
            if (!GestorArchivos.prepararCarpeta()) {
                return false;
            }
            File temporal = new File(archivo.getPath() + ".tmp");
            try (ObjectOutputStream salida = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(temporal)))) {
                salida.writeObject(partida);
            } catch (NotSerializableException e) {
                System.out.println("No se puede guardar: la clase " + e.getMessage() + " no es Serializable");
                return false;
            } catch (IOException e) {
                System.out.println("No se pudo escribir la partida: " + e.getMessage());
                return false;
            }
            return reemplazar(temporal.toPath(), archivo.toPath());
        }
    }

    private boolean reemplazar(Path origen, Path destino) {
        try {
            try {
                Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            System.out.println("No se pudo reemplazar la partida guardada: " + e.getMessage());
            return false;
        }
    }

    public PartidaGuardada cargar() {
        synchronized (GestorArchivos.CANDADO) {
            try (ObjectInputStream entrada = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(archivo)))) {
                entrada.setObjectInputFilter(ObjectInputFilter.Config.createFilter(CLASES_PERMITIDAS));
                return (PartidaGuardada) entrada.readObject();
            } catch (FileNotFoundException e) {
                System.out.println("No hay partida guardada en " + archivo.getPath());
            } catch (InvalidClassException e) {
                System.out.println("La partida guardada no es compatible con esta version: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                System.out.println("La partida usa una clase desconocida: " + e.getMessage());
            } catch (ClassCastException e) {
                System.out.println("El archivo no contiene una partida valida");
            } catch (IOException e) {
                System.out.println("El archivo de partida esta danado o no se puede leer: " + e.getMessage());
            }
            return null;
        }
    }

    public File getArchivo() {
        return archivo;
    }
}
