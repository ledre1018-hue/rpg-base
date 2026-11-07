package com.rpg.core;

import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.concurrency.GestorTurno;
import com.rpg.entities.Enemigo;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.exceptions.TiempoAgotadoException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;
import com.rpg.persistencia.GestorArchivos;
import com.rpg.persistencia.LogCombate;
import com.rpg.persistencia.PartidaGuardada;
import com.rpg.persistencia.RepositorioPartida;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/** Coordina el flujo del juego: crear heroe, combatir, torneo, bestiario y guardado. */
public class MotorJuego {

    private static final String NOMBRE_GREMIO = "Guardianes del Alba";
    private static final int CURACION_POCION_VIDA = 30;
    private static final int SIN_CURACION = 0;
    private static final int REGENERACION_POR_TICK = 6;
    private static final int TICKS_REGENERACION = 3;
    private static final int NUEVO_HEROE = 1;
    private static final int HEROE_DEL_GREMIO = 2;
    private static final int CLASE_GUERRERO = 1;
    private static final int CLASE_MAGO = 2;
    private static final int ACCION_HUIR = 0;
    private static final int ULTIMA_ACCION = 4;
    private static final int CANCELAR = 0;
    private static final int SEGUNDOS_TURNO = 15;
    private static final long INTERVALO_AUTOGUARDADO_MS = 20_000;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Consola consola;
    private final RegistroCombate registro = new RegistroCombate();
    private final Arena arena = new Arena(registro, new Random());
    private final Bestiario bestiario = new Bestiario();
    private final TorneoManager torneo = new TorneoManager(arena, registro);
    private final GestorTurno gestorTurno = new GestorTurno();
    private final LogCombate logCombate = new LogCombate();
    private final LogCombate resumenPartida = new LogCombate(GestorArchivos.RESUMEN);
    private final RepositorioPartida repositorio = new RepositorioPartida();
    private volatile PerfilJugador perfil = new PerfilJugador();
    private volatile Gremio gremio = new Gremio(NOMBRE_GREMIO);
    private volatile String ultimoGuardado = "";
    private volatile boolean partidaActiva;
    private Consumer<String> alVencerMonstruo = nombre -> { };

    public MotorJuego(Consola consola) {
        this.consola = consola;
    }

    // ---- opciones del menu principal

    public void setAlVencerMonstruo(Consumer<String> alVencerMonstruo) {
        this.alVencerMonstruo = alVencerMonstruo;
    }

    public void iniciarAutoGuardado() {
        AutoGuardadoDaemon.iniciar(this::autoGuardar, INTERVALO_AUTOGUARDADO_MS);
    }

    public void combateRapido() {
        registro.limpiar();
        partidaActiva = true;
        Personaje jugador = crearJugador();
        Enemigo enemigo = elegirEnemigo();
        registro.agregar("%n=== %s vs %s ===", jugador.getNombre(), enemigo.getNombre());

        boolean huyo = combatir(jugador, enemigo);
        jugador.detenerEfectos();
        enemigo.detenerEfectos();

        cerrarCombate(jugador, enemigo, huyo);
        registro.volcarEnArchivo(logCombate);
        guardarProgreso();
    }

    public void jugarTorneo() {
        registro.limpiar();
        partidaActiva = true;
        Personaje campeon = torneo.jugarTorneo();
        if (campeon == null) {
            return;
        }
        registro.agregar("%nCAMPEON DEL TORNEO: %s", campeon.getNombre());
        perfil.desbloquearLogro("Campeon del Torneo");
        registro.volcarEnArchivo(logCombate);
        guardarProgreso();
    }

    public void mostrarHistorial() {
        List<String> lineas = logCombate.leer();
        if (lineas.isEmpty()) {
            return;
        }
        System.out.println("\n=== HISTORIAL DE COMBATES (" + logCombate.getArchivo().getPath() + ") ===");
        for (String linea : lineas) {
            System.out.println(linea);
        }
    }

    public void mostrarArchivos() {
        if (GestorArchivos.crearSiNoExiste(GestorArchivos.LOG)) {
            System.out.println("Se creo el archivo vacio " + GestorArchivos.LOG.getPath());
        }
        System.out.println("\n=== ARCHIVOS DE PARTIDA ===");
        File carpeta = GestorArchivos.CARPETA;
        System.out.println("Carpeta: " + carpeta.getPath() + " -> " + carpeta.getAbsolutePath());
        for (File archivo : List.of(GestorArchivos.PARTIDA, GestorArchivos.RESUMEN, GestorArchivos.LOG)) {
            System.out.println(GestorArchivos.describir(archivo));
        }
    }

    public void borrarPartida() {
        if (!GestorArchivos.PARTIDA.exists() && !GestorArchivos.RESUMEN.exists()) {
            System.out.println("No hay partida guardada que borrar.");
            return;
        }
        if (!consola.confirmar("Seguro que quieres borrar la partida guardada?")) {
            System.out.println("Borrado cancelado.");
            return;
        }
        boolean datBorrado = GestorArchivos.borrarSiExiste(GestorArchivos.PARTIDA);
        boolean txtBorrado = GestorArchivos.borrarConFile(GestorArchivos.RESUMEN);
        System.out.printf("%s: %s | %s: %s%n",
                GestorArchivos.PARTIDA.getName(), datBorrado ? "borrado" : "no existia",
                GestorArchivos.RESUMEN.getName(), txtBorrado ? "borrado" : "no se pudo borrar o no existia");
        partidaActiva = false;
    }

    public synchronized void cargarProgreso() {
        PartidaGuardada partida = repositorio.cargar();
        if (partida == null) {
            return;
        }
        perfil = partida.getPerfil();
        gremio = partida.getGremio();
        bestiario.cargarDerrotados(partida.getEnemigosDerrotados());
        ultimoGuardado = partida.getResumen();
        partidaActiva = true;
        System.out.printf("Partida del %s cargada. Victorias: %d | Derrotas: %d | Logros: %s%n",
                partida.getFecha().format(FORMATO_FECHA), perfil.getVictorias(), perfil.getDerrotas(),
                perfil.getLogros());
        for (Personaje miembro : gremio.getMiembros()) {
            System.out.printf("  %s (%s) sigue en el gremio %s: %s%n", miembro.getNombre(), miembro.getTipo(),
                    gremio.getNombre(), miembro.getGremio() == gremio ? "si" : "no");
        }
    }

    public void mostrarBestiario() {
        System.out.println("\n=== BESTIARIO ===");
        System.out.print(bestiario.describir());
        System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
    }

    // ---- creacion de personajes

    private Personaje crearJugador() {
        List<Personaje> miembros = gremio.getMiembros();
        if (!miembros.isEmpty() && consola.leerEntero("1. Nuevo heroe  2. Heroe del gremio: ",
                NUEVO_HEROE, HEROE_DEL_GREMIO) == HEROE_DEL_GREMIO) {
            return elegirMiembro(miembros);
        }
        int clase = consola.leerEntero("Clase (1. Guerrero, 2. Mago): ", CLASE_GUERRERO, CLASE_MAGO);
        String nombre = consola.leerNombre("Nombre del heroe: ");
        Personaje jugador = switch (clase) {
            case CLASE_GUERRERO -> new Guerrero(nombre);
            default -> new Mago(nombre);
        };
        entregarKitInicial(jugador);
        jugador.unirseAGremio(gremio);
        System.out.printf("%s se une al gremio %s (%d miembros).%n",
                jugador.getNombre(), gremio.getNombre(), gremio.getMiembros().size());
        return jugador;
    }

    private Personaje elegirMiembro(List<Personaje> miembros) {
        for (int i = 0; i < miembros.size(); i++) {
            Personaje miembro = miembros.get(i);
            System.out.printf("  %d. %s (%s) - %d pociones%n", i + 1, miembro.getNombre(), miembro.getTipo(),
                    miembro.getInventario().tamanio());
        }
        Personaje elegido = miembros.get(consola.leerEntero("Heroe: ", 1, miembros.size()) - 1);
        elegido.restaurarVida();
        return elegido;
    }

    private void entregarKitInicial(Personaje jugador) {
        List<Pocion> kit = List.of(
                new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN),
                new Pocion("Elixir de Regeneracion", SIN_CURACION, RaridadItem.RARO,
                        new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA,
                                REGENERACION_POR_TICK, TICKS_REGENERACION), false),
                new Pocion("Antidoto", SIN_CURACION, RaridadItem.COMUN, null, true));
        try {
            for (Pocion pocion : kit) {
                jugador.getInventario().agregar(pocion);
            }
        } catch (CapacidadExcedidaException e) {
            System.out.println("No se pudo entregar todo el kit: " + e.getMessage());
        }
    }

    private Enemigo elegirEnemigo() {
        List<String> nombres = bestiario.nombres();
        System.out.println("Rivales disponibles:");
        for (int i = 0; i < nombres.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, nombres.get(i));
        }
        while (true) {
            String texto = consola.leerLinea("Rival (numero o nombre): ");
            Enemigo enemigo = resolverRival(texto, nombres);
            if (enemigo != null) {
                return enemigo;
            }
            System.out.println("No existe ese rival.");
        }
    }

    private Enemigo resolverRival(String texto, List<String> nombres) {
        try {
            int numero = Integer.parseInt(texto);
            boolean valido = numero >= 1 && numero <= nombres.size();
            return valido ? bestiario.crearEnemigo(nombres.get(numero - 1)) : null;
        } catch (NumberFormatException e) {
            return bestiario.crearEnemigo(texto);
        }
    }

    // ---- bucle de combate

    /** @return true si el jugador huyo */
    private boolean combatir(Personaje jugador, Enemigo enemigo) {
        arena.prepararCombatiente(jugador);
        arena.prepararCombatiente(enemigo);
        int ronda = 1;
        while (jugador.estaVivo() && enemigo.estaVivo()) {
            registro.agregar("%n-- Ronda %d --", ronda);
            if (arena.iniciarTurno(jugador) && turnoJugador(jugador, enemigo)) {
                return true;
            }
            arena.turnoAutomatico(enemigo, jugador);
            ronda++;
        }
        return false;
    }

    /**
     * Turno del jugador. Las acciones que no gastan turno (ver estado, cancelar) vuelven a pedir.
     * @return true si el jugador decidio huir
     */
    private boolean turnoJugador(Personaje jugador, Enemigo enemigo) {
        boolean turnoGastado;
        do {
            registro.agregar(FormatoCombate.resumen(jugador));
            registro.agregar(FormatoCombate.resumen(enemigo));
            Accion accion = pedirAccion();
            if (!jugador.estaVivo() || !enemigo.estaVivo()) {
                return false;
            }
            if (accion == Accion.HUIR) {
                return true;
            }
            turnoGastado = switch (accion) {
                case TURNO_PERDIDO -> {
                    registro.agregar("%s tarda demasiado y pierde el turno", jugador.getNombre());
                    yield true;
                }
                case ATACAR -> {
                    arena.atacar(jugador, enemigo);
                    yield true;
                }
                case ESPECIAL -> arena.intentarEspecial(jugador, enemigo);
                case POCION -> usarPocion(jugador);
                default -> {
                    mostrarEstado(jugador);
                    yield false;
                }
            };
        } while (!turnoGastado && enemigo.estaVivo());
        return false;
    }

    /**
     * Pide la accion al jugador. Si no responde en {@code SEGUNDOS_TURNO}, el turno se pierde.
     */
    private Accion pedirAccion() {
        System.out.printf("1. Atacar  2. Habilidad especial  3. Usar pocion  4. Ver estado  0. Huir  (%d s)%n",
                SEGUNDOS_TURNO);
        TurnoCallback alAgotarse = () -> {
            System.out.println("\nTiempo agotado!");
            consola.avisarTiempoAgotado();
        };
        gestorTurno.iniciar(SEGUNDOS_TURNO, alAgotarse);
        try {
            int opcion = consola.leerEntero("Accion: ", ACCION_HUIR, ULTIMA_ACCION);
            return switch (opcion) {
                case 1 -> Accion.ATACAR;
                case 2 -> Accion.ESPECIAL;
                case 3 -> Accion.POCION;
                case 4 -> Accion.ESTADO;
                default -> Accion.HUIR;
            };
        } catch (TiempoAgotadoException e) {
            return Accion.TURNO_PERDIDO;
        } finally {
            gestorTurno.cancelar();
            consola.descartarAvisosDeTiempo();
        }
    }

    private boolean usarPocion(Personaje jugador) {
        Inventario<Pocion> inventario = jugador.getInventario();
        if (inventario.estaVacio()) {
            System.out.println("No tienes pociones.");
            return false;
        }
        int numero = 1;
        for (Pocion pocion : inventario) {
            System.out.printf("  %d. %s%n", numero++, pocion.getNombre());
        }
        int eleccion = consola.leerEntero("Pocion (0 = cancelar): ", CANCELAR, inventario.tamanio());
        if (eleccion == CANCELAR) {
            return false;
        }
        registro.agregar(jugador.usarPocion(inventario.quitar(eleccion - 1)));
        return true;
    }

    private void mostrarEstado(Personaje jugador) {
        System.out.printf("Habilidades: %s | Arma: %s | Defensa: %d | Pociones: %d%n",
                jugador.getHabilidades(),
                jugador.getArma() != null ? jugador.getArma().getNombre() : "ninguna",
                jugador.getDefensa(), jugador.getInventario().tamanio());
        for (EstadoEfecto efecto : jugador.getEfectos()) {
            System.out.printf("Efecto activo: %s (%d ticks)%n", efecto.getNombre(), efecto.getTurnosRestantes());
        }
    }

    // ---- resultado del combate

    private void cerrarCombate(Personaje jugador, Enemigo enemigo, boolean huyo) {
        if (huyo) {
            registro.agregar("%s huye del combate.", jugador.getNombre());
        } else if (jugador.estaVivo()) {
            registro.agregar("%nVictoria de %s!", jugador.getNombre());
            perfil.registrarVictoria();
            bestiario.registrarEnemigoDerrotado(enemigo.getNombre());
            alVencerMonstruo.accept(enemigo.getNombre());
            desbloquearLogros(jugador, enemigo);
            repartirBotin(jugador);
        } else {
            registro.agregar("%n%s ha caido ante %s.", jugador.getNombre(), enemigo.getNombre());
            perfil.registrarDerrota();
        }
    }

    private void desbloquearLogros(Personaje jugador, Enemigo enemigo) {
        boolean intacto = jugador.getVida() == jugador.getVidaMaxima();
        List<String> candidatos = List.of("Primera Victoria", "Cazador de " + enemigo.getNombre(),
                intacto ? "Intocable" : "");
        for (String logro : candidatos) {
            if (!logro.isEmpty() && perfil.desbloquearLogro(logro)) {
                registro.agregar("Logro desbloqueado: %s", logro);
            }
        }
    }

    private void repartirBotin(Personaje jugador) {
        try {
            jugador.getInventario().agregar(new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN));
            registro.agregar("Botin: Pocion de Vida.");
        } catch (CapacidadExcedidaException e) {
            registro.agregar("Botin perdido: %s", e.getMessage());
        }
    }

    // ---- guardado

    private void autoGuardar() {
        if (partidaActiva) {
            guardarProgreso(false);
        }
    }

    public void guardarProgreso() {
        guardarProgreso(true);
    }

    /**
     * Guarda el estado en memoria ({@code ultimoGuardado}), un resumen en texto y la partida serializada.
     */
    public synchronized void guardarProgreso(boolean avisar) {
        ultimoGuardado = construirResumen();
        boolean guardadoEnDisco = guardarEnDisco();
        if (avisar) {
            System.out.println(guardadoEnDisco
                    ? "[Progreso guardado en " + GestorArchivos.CARPETA.getPath() + "/]"
                    : "[Progreso guardado solo en memoria]");
        }
    }

    private String construirResumen() {
        String salto = System.lineSeparator();
        StringBuilder contenido = new StringBuilder();
        contenido.append("=== PARTIDA RPG ===").append(salto);
        agregarPerfil(contenido, salto);
        agregarGremio(contenido, salto);
        contenido.append("Enemigos derrotados: ").append(bestiario.resumenDerrotados()).append(salto);
        contenido.append(salto).append("--- Ultimo combate ---").append(salto);
        contenido.append(registro.getTexto());
        return contenido.toString();
    }

    private void agregarPerfil(StringBuilder contenido, String salto) {
        contenido.append("Victorias: ").append(perfil.getVictorias()).append(salto);
        contenido.append("Derrotas: ").append(perfil.getDerrotas()).append(salto);
        contenido.append("Logros: ").append(String.join(", ", perfil.getLogros())).append(salto);
    }

    private void agregarGremio(StringBuilder contenido, String salto) {
        List<Personaje> miembros = gremio.getMiembros();
        contenido.append("Gremio ").append(gremio.getNombre()).append(": ")
                .append(miembros.size()).append(" miembros").append(salto);
        for (Personaje miembro : miembros) {
            contenido.append("  ").append(FormatoCombate.resumen(miembro)).append(salto);
        }
    }

    private boolean guardarEnDisco() {
        boolean resumenOk = resumenPartida.escribir(ultimoGuardado, false);
        boolean partidaOk = repositorio.guardar(
                new PartidaGuardada(perfil, gremio, bestiario.copiaDerrotados(), ultimoGuardado));
        return resumenOk && partidaOk;
    }

    public String getUltimoGuardado() {
        return ultimoGuardado;
    }
}
