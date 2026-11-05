package com.rpg.persistencia;

import com.rpg.entities.Escuadron;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;

public final class LaboratorioArchivos {

    private static final File CARPETA_LAB = new File(GestorArchivos.CARPETA, "laboratorio");

    private LaboratorioArchivos() {
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        probarFile();
        probarAppendVsSobrescribir();
        probarBorrado();
        probarNotSerializable();
        probarTransientYCiclo();
        Files.deleteIfExists(CARPETA_LAB.toPath());
    }

    private static void probarFile() {
        System.out.println("=== File: rutas y creacion ===");
        System.out.println("mkdirs: " + (CARPETA_LAB.mkdirs() ? "carpeta creada" : "ya existia o no se pudo crear"));
        File relativo = new File(CARPETA_LAB, "prueba.txt");
        File absoluto = new File(relativo.getAbsolutePath());
        System.out.println("Relativa (depende del directorio de trabajo): " + relativo.getPath());
        System.out.println("Absoluta (desde la raiz del disco):          " + absoluto.getPath());
        try {
            System.out.println("createNewFile: " + relativo.createNewFile() + " | otra vez: " + relativo.createNewFile());
        } catch (IOException e) {
            System.out.println("No se pudo crear el archivo: " + e.getMessage());
        }
        System.out.printf("exists=%s length=%d name=%s%n", absoluto.exists(), absoluto.length(), absoluto.getName());
    }

    private static void probarAppendVsSobrescribir() throws IOException {
        System.out.println("\n=== FileWriter: append vs sobrescribir ===");
        LogCombate log = new LogCombate(new File(CARPETA_LAB, "log.txt"));
        log.escribir("Combate 1", false);
        log.escribir("Combate 2", true);
        System.out.println("Con append: " + log.leer());
        log.escribir("Combate 3", false);
        System.out.println("Sin append: " + log.leer());
        File nio = new File(CARPETA_LAB, "nio.txt");
        Files.writeString(nio.toPath(), "Escrito de una vez con Files.writeString" + System.lineSeparator());
        System.out.println("Files.writeString: " + Files.readAllLines(nio.toPath()));
        new LogCombate(new File(CARPETA_LAB, "no-existe.txt")).leer();
    }

    private static void probarBorrado() throws IOException {
        System.out.println("\n=== Borrado ===");
        for (File archivo : List.of(new File(CARPETA_LAB, "prueba.txt"), new File(CARPETA_LAB, "log.txt"))) {
            System.out.println("File.delete(" + archivo.getName() + "): " + archivo.delete()
                    + " | segunda vez: " + archivo.delete());
        }
        File nio = new File(CARPETA_LAB, "nio.txt");
        System.out.println("Files.deleteIfExists: " + Files.deleteIfExists(nio.toPath())
                + " | segunda vez: " + Files.deleteIfExists(nio.toPath()));
        try {
            Files.delete(nio.toPath());
        } catch (NoSuchFileException e) {
            System.out.println("Files.delete sobre un archivo inexistente lanza NoSuchFileException");
        }
    }

    private static void probarNotSerializable() throws IOException {
        System.out.println("\n=== NotSerializableException ===");
        Escuadron escuadron = new Escuadron("Vanguardia");
        escuadron.agregar(new Guerrero("Leonidas"));
        List<Object> partida = new ArrayList<>(List.of(escuadron));
        try {
            serializar(partida);
        } catch (NotSerializableException e) {
            System.out.println("Fallo al guardar: " + e.getMessage() + " no es Serializable");
        }
        List<Personaje> solucion = new ArrayList<>(escuadron.getMiembros());
        System.out.println("Solucion (guardar sus miembros): " + serializar(solucion).length + " bytes");
    }

    private static void probarTransientYCiclo() throws IOException, ClassNotFoundException {
        System.out.println("\n=== transient y ciclo Personaje - Gremio ===");
        Gremio gremio = new Gremio("Orden del Fenix");
        Guerrero guerrero = new Guerrero("Arturo");
        Mago mago = new Mago("Morgana");
        guerrero.unirseAGremio(gremio);
        mago.unirseAGremio(gremio);
        guerrero.setNotificador(System.out::println);
        System.out.println("Antes de guardar, notificador asignado: " + guerrero.tieneNotificador());

        Gremio cargado;
        try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(serializar(gremio)))) {
            cargado = (Gremio) entrada.readObject();
        }
        Personaje arturo = cargado.getMiembros().get(0);
        System.out.println("Despues de cargar, notificador (transient) asignado: " + arturo.tieneNotificador());
        for (Personaje miembro : cargado.getMiembros()) {
            System.out.println(miembro.getNombre() + " apunta al gremio cargado: " + (miembro.getGremio() == cargado));
        }
    }

    private static byte[] serializar(Object objeto) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream salida = new ObjectOutputStream(bytes)) {
            salida.writeObject(objeto);
        }
        return bytes.toByteArray();
    }
}
