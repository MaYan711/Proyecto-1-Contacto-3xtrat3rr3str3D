package com.compi2.contacto.compilacion;

import com.compi2.contacto.analisis.AnalizadorArchivo;
import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.errores.Diagnostico;
import com.compi2.contacto.errores.Severidad;
import com.compi2.contacto.errores.TipoDiagnostico;
import com.compi2.contacto.ir.ProgramaIntermedio;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;
import com.compi2.contacto.semantica.TablaSimbolos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CompiladorProyecto {

    private final AnalizadorArchivo analizadorArchivo;

    public CompiladorProyecto() {
        this.analizadorArchivo = new AnalizadorArchivo();
    }

    public ResultadoCompilacion compilar(ProyectoCompilacion proyecto) {
        Objects.requireNonNull(proyecto, "El proyecto es obligatorio");

        List<ResultadoAnalisisArchivo> resultados = proyecto.archivos()
                .stream()
                .map(analizadorArchivo::analizar)
                .toList();

        List<Diagnostico> diagnosticosProyecto = new ArrayList<>();

        if (proyecto.archivos().isEmpty()) {
            diagnosticosProyecto.add(new Diagnostico(
                    TipoDiagnostico.PROYECTO,
                    Severidad.ERROR,
                    proyecto.raiz(),
                    1,
                    0,
                    "El proyecto no contiene archivos .y, .z o .pig"
            ));
        } else if (!proyecto.contieneLenguaje(LenguajeFuente.PIG_LATIN)) {
            diagnosticosProyecto.add(new Diagnostico(
                    TipoDiagnostico.PROYECTO,
                    Severidad.ERROR,
                    proyecto.raiz(),
                    1,
                    0,
                    "El proyecto necesita al menos un archivo principal .pig"
            ));
        }

        return new ResultadoCompilacion(
                proyecto,
                resultados,
                diagnosticosProyecto,
                new TablaSimbolos(),
                new ProgramaIntermedio()
        );
    }
}

