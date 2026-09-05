package com.compi2.contacto.ir;

import java.util.ArrayList;
import java.util.List;

public final class ProgramaIntermedio {

    private final List<Cuarteta> cuartetas;
    private int siguienteTemporal;
    private int siguienteEtiqueta;

    public ProgramaIntermedio() {
        this.cuartetas = new ArrayList<>();
    }

    public void agregar(Cuarteta cuarteta) {
        if (cuarteta == null) {
            throw new IllegalArgumentException("La cuarteta es obligatoria");
        }
        cuartetas.add(cuarteta);
    }

    public List<Cuarteta> cuartetas() {
        return List.copyOf(cuartetas);
    }

    public String nuevoTemporal() {
        return "t" + siguienteTemporal++;
    }

    public String nuevaEtiqueta() {
        return "L" + siguienteEtiqueta++;
    }
}

