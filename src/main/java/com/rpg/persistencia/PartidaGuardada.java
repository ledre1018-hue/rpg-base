package com.rpg.persistencia;

import com.rpg.entities.Gremio;
import com.rpg.entities.PerfilJugador;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class PartidaGuardada implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final PerfilJugador perfil;
    private final Gremio gremio;
    private final LinkedHashMap<String, Integer> enemigosDerrotados;
    private final String resumen;
    private final LocalDateTime fecha = LocalDateTime.now();

    public PartidaGuardada(PerfilJugador perfil, Gremio gremio, Map<String, Integer> enemigosDerrotados,
                           String resumen) {
        this.perfil = perfil;
        this.gremio = gremio;
        this.enemigosDerrotados = new LinkedHashMap<>(enemigosDerrotados);
        this.resumen = resumen;
    }

    public PerfilJugador getPerfil() {
        return perfil;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Map<String, Integer> getEnemigosDerrotados() {
        return enemigosDerrotados;
    }

    public String getResumen() {
        return resumen;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
