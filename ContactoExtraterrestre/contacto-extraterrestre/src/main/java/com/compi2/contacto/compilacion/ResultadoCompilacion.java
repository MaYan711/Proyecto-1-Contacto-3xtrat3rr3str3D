package com.compi2.contacto.compilacion;

import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.errores.Diagnostico;
import com.compi2.contacto.ir.ProgramaIntermedio;
import com.compi2.contacto.proyecto.ProyectoCompilacion;
import com.compi2.contacto.semantica.TablaSimbolos;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record ResultadoCompilacion(
        ProyectoCompilacion proyecto,
        List<ResultadoAnalisisArchivo> analisisArchivos,
        List<Diagnostico> diagnosticosProyecto,
        TablaSimbolos tablaSimbolos,
        ProgramaIntermedio programaIntermedio
) {
    public ResultadoCompilacion {
        Objects.requireNonNull(proyecto, "El proyecto es obligatorio");
        analisisArchivos = List.copyOf(analisisArchivos);
        diagnosticosProyecto = List.copyOf(diagnosticosProyecto);
        Objects.requireNonNull(tablaSimbolos, "La tabla es obligatoria");
        Objects.requireNonNull(programaIntermedio, "El programa es obligatorio");
    }

    public List<Diagnostico> diagnosticos() {
        return Stream.concat(
                        diagnosticosProyecto.stream(),
                        analisisArchivos.stream()
                                .flatMap(resultado -> resultado.diagnosticos().stream())
                )
                .toList();
    }

    public boolean esValido() {
        return diagnosticos().stream().noneMatch(Diagnostico::esError);
    }
}

