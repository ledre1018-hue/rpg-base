package com.rpg.entities;

import com.rpg.concurrency.EfectoContinuoTask;
import com.rpg.entities.enums.EstadoPersonaje;
import com.rpg.entities.enums.TipoPersonaje;
import com.rpg.exceptions.SinManaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public abstract class Personaje implements ICombatiente, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int VIDA_BASE = 100;
    private static final int ATAQUE_BASE = 10;
    private static final int DEFENSA_BASE = 5;
    private static final int CAPACIDAD_INVENTARIO = 3;
    private static final int DANIO_MINIMO = 1;
    private static final double PORCENTAJE_TOTAL = 100.0;
    private static final long INTERVALO_EFECTO_MS = 2000;
    private static final long ESPERA_FIN_EFECTO_MS = 500;

    private String nombre;
    private final TipoPersonaje tipo;
    private int vidaMaxima;
    private int vida;
    private final int ataqueBase;
    private final int defensaBase;
    private EstadoPersonaje estado = EstadoPersonaje.VIVO;

    private Arma arma;
    private Gremio gremio;
    private final Inventario<Pocion> inventario = new Inventario<>(CAPACIDAD_INVENTARIO);
    private final Set<String> habilidades = new HashSet<>();
    private final Map<String, EstadoEfecto> efectos = new LinkedHashMap<>();

    private transient Map<String, Thread> hilosEfectos;
    private transient Consumer<String> notificador;

    protected Personaje(String nombre, TipoPersonaje tipo) {
        this(nombre, tipo, VIDA_BASE, ATAQUE_BASE, DEFENSA_BASE);
    }

    protected Personaje(String nombre, TipoPersonaje tipo, int vidaMaxima, int ataqueBase, int defensaBase) {
        validarNombre(nombre);
        if (vidaMaxima <= 0 || ataqueBase < 0 || defensaBase < 0) {
            throw new IllegalArgumentException("Estadisticas invalidas para " + nombre);
        }
        this.nombre = nombre;
        this.tipo = tipo;
        this.vidaMaxima = vidaMaxima;
        this.vida = vidaMaxima;
        this.ataqueBase = ataqueBase;
        this.defensaBase = defensaBase;
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
    }

    // ---- combate

    /**
     * Ataque especial propio de cada clase.
     * @return descripcion de lo ocurrido, para el registro de combate
     */
    public abstract String realizarAtaqueEspecial(Personaje objetivo) throws SinManaException;

    /** Hook que se ejecuta al comenzar el turno (por ejemplo, el mago regenera mana). */
    public void alIniciarTurno() {
    }

    public int calcularDanio() {
        double bruto = ataqueBase + tipo.getBonificadorAtaque();
        if (arma != null) {
            bruto += arma.getDanioEfectivo();
        }
        return (int) Math.round(bruto);
    }

    @Override
    public void defender(int danoRecibido) {
        // Con enteros, 100 / 123 daria 0: por eso se divide con un double
        double factorMitigacion = PORCENTAJE_TOTAL / (PORCENTAJE_TOTAL + getDefensa());
        int danioFinal = (int) Math.round(danoRecibido * factorMitigacion);
        recibirDanio(Math.max(DANIO_MINIMO, danioFinal));
    }

    // ---- vida

    public synchronized void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo");
        }
        setVida(vida - cantidad);
    }

    /** @return vida realmente recuperada */
    public synchronized int curar(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La curacion no puede ser negativa");
        }
        if (!estaVivo()) {
            return 0;
        }
        int vidaAntes = vida;
        setVida(vida + cantidad);
        return vida - vidaAntes;
    }

    public void restaurarVida() {
        detenerEfectos();
        synchronized (this) {
            estado = EstadoPersonaje.VIVO;
            vida = vidaMaxima;
        }
    }

    @Override
    public synchronized boolean estaVivo() {
        return estado != EstadoPersonaje.MUERTO;
    }

    // ---- efectos DoT / HoT

    public void aplicarEfecto(EstadoEfecto efecto) {
        EstadoEfecto copia = efecto.copia();
        Thread hilo = new Thread(new EfectoContinuoTask(this, copia, INTERVALO_EFECTO_MS),
                "efecto-" + nombre + "-" + copia.getNombre());
        hilo.setDaemon(true);
        Thread anterior;
        synchronized (this) {
            if (!estaVivo()) {
                return;
            }
            efectos.put(copia.getNombre(), copia);
            anterior = hilos().put(copia.getNombre(), hilo);
            if (copia.esDanio() && estado == EstadoPersonaje.VIVO) {
                estado = EstadoPersonaje.ENVENENADO;
            }
        }
        if (anterior != null) {
            anterior.interrupt();
        }
        hilo.start();
    }

    public boolean quitarEfecto(String nombreEfecto) {
        Thread hilo;
        synchronized (this) {
            if (efectos.remove(nombreEfecto) == null) {
                return false;
            }
            hilo = hilos().remove(nombreEfecto);
            normalizarEstado();
        }
        if (hilo != null) {
            hilo.interrupt();
        }
        return true;
    }

    public void quitarEfectosDanio() {
        List<String> nombresDanio = new ArrayList<>();
        synchronized (this) {
            for (EstadoEfecto efecto : efectos.values()) {
                if (efecto.esDanio()) {
                    nombresDanio.add(efecto.getNombre());
                }
            }
        }
        for (String nombreEfecto : nombresDanio) {
            quitarEfecto(nombreEfecto);
        }
    }

    public void detenerEfectos() {
        List<Thread> activos;
        synchronized (this) {
            activos = new ArrayList<>(hilos().values());
            hilos().clear();
            efectos.clear();
            normalizarEstado();
        }
        for (Thread hilo : activos) {
            hilo.interrupt();
        }
        try {
            for (Thread hilo : activos) {
                hilo.join(ESPERA_FIN_EFECTO_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** @return mensaje del tick, o null si el efecto ya no esta activo */
    public synchronized String aplicarTickEfecto(EstadoEfecto efecto) {
        if (efectos.get(efecto.getNombre()) != efecto || !estaVivo()) {
            return null;
        }
        String mensaje;
        if (efecto.esDanio()) {
            recibirDanio(efecto.getValorPorTick());
            mensaje = String.format("%s sufre %d de dano por %s", nombre, efecto.getValorPorTick(), efecto.getNombre());
        } else {
            int curado = curar(efecto.getValorPorTick());
            mensaje = String.format("%s recupera %d de vida por %s", nombre, curado, efecto.getNombre());
        }
        efecto.consumirTurno();
        return mensaje;
    }

    public synchronized void finalizarEfecto(EstadoEfecto efecto) {
        efectos.remove(efecto.getNombre(), efecto);
        hilos().remove(efecto.getNombre(), Thread.currentThread());
        normalizarEstado();
    }

    public void setNotificador(Consumer<String> notificador) {
        this.notificador = notificador;
    }

    public boolean tieneNotificador() {
        return notificador != null;
    }

    public void notificar(String mensaje) {
        Consumer<String> destino = notificador;
        if (destino != null && mensaje != null) {
            destino.accept(mensaje);
        }
    }

    private Map<String, Thread> hilos() {
        if (hilosEfectos == null) {
            hilosEfectos = new HashMap<>();
        }
        return hilosEfectos;
    }

    private boolean tieneEfectoDanio() {
        for (EstadoEfecto efecto : efectos.values()) {
            if (efecto.esDanio()) {
                return true;
            }
        }
        return false;
    }

    /** Si ya no queda ningun DoT, un personaje ENVENENADO vuelve a estar VIVO. */
    private void normalizarEstado() {
        if (estado == EstadoPersonaje.ENVENENADO && !tieneEfectoDanio()) {
            estado = EstadoPersonaje.VIVO;
        }
    }

    // ---- aturdimiento

    public synchronized void aturdir() {
        if (estaVivo()) {
            estado = EstadoPersonaje.ATURDIDO;
        }
    }

    /** @return true si estaba aturdido (y por tanto pierde este turno) */
    public synchronized boolean consumirAturdimiento() {
        boolean estabaAturdido = estado == EstadoPersonaje.ATURDIDO;
        if (estabaAturdido) {
            estado = tieneEfectoDanio() ? EstadoPersonaje.ENVENENADO : EstadoPersonaje.VIVO;
        }
        return estabaAturdido;
    }

    // ---- inventario, arma, gremio, habilidades

    public String usarPocion(Pocion pocion) {
        int curado = curar(pocion.getCuracionReal());
        if (pocion.getEfecto() != null) {
            aplicarEfecto(pocion.getEfecto());
        }
        if (pocion.isPurifica()) {
            quitarEfectosDanio();
        }
        return String.format("%s usa %s (+%d de vida)", nombre, pocion.getNombre(), curado);
    }

    public void equiparArma(Arma nuevaArma) {
        this.arma = nuevaArma;
    }

    public void desequiparArma() {
        this.arma = null;
    }

    public void unirseAGremio(Gremio nuevoGremio) {
        if (nuevoGremio == null || nuevoGremio == gremio) {
            return;
        }
        if (gremio != null) {
            gremio.removerMiembro(this);
        }
        gremio = nuevoGremio;
        nuevoGremio.agregarMiembro(this);
    }

    public void abandonarGremio() {
        Gremio anterior = gremio;
        if (anterior == null) {
            return;
        }
        gremio = null;
        anterior.removerMiembro(this);
    }

    /** @return false si la habilidad ya estaba aprendida (el Set rechaza duplicados) */
    public boolean aprenderHabilidad(String habilidad) {
        return habilidades.add(habilidad);
    }

    // ---- getters y setters validados

    public synchronized void setVida(int nuevaVida) {
        vida = Math.max(0, Math.min(nuevaVida, vidaMaxima));
        if (vida == 0) {
            estado = EstadoPersonaje.MUERTO;
        }
    }

    public void setNombre(String nombre) {
        validarNombre(nombre);
        this.nombre = nombre;
    }

    public synchronized void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser positiva");
        }
        this.vidaMaxima = vidaMaxima;
        this.vida = Math.min(vida, vidaMaxima);
    }

    public int getDefensa() {
        return defensaBase + tipo.getBonificadorDefensa();
    }

    public String getNombre() {
        return nombre;
    }

    public TipoPersonaje getTipo() {
        return tipo;
    }

    public synchronized int getVida() {
        return vida;
    }

    public synchronized int getVidaMaxima() {
        return vidaMaxima;
    }

    public synchronized EstadoPersonaje getEstado() {
        return estado;
    }

    public Arma getArma() {
        return arma;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Inventario<Pocion> getInventario() {
        return inventario;
    }

    public Set<String> getHabilidades() {
        return Collections.unmodifiableSet(habilidades);
    }

    public synchronized Collection<EstadoEfecto> getEfectos() {
        return List.copyOf(efectos.values());
    }

    @Serial
    private synchronized void writeObject(ObjectOutputStream salida) throws IOException {
        salida.defaultWriteObject();
    }

    @Serial
    private void readObject(ObjectInputStream entrada) throws IOException, ClassNotFoundException {
        entrada.defaultReadObject();
        efectos.clear();
        if (estado == EstadoPersonaje.ENVENENADO) {
            estado = EstadoPersonaje.VIVO;
        }
    }
}
