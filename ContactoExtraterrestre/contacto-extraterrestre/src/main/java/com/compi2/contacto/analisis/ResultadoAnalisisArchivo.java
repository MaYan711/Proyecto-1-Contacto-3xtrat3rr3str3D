package com.compi2.contacto.analisis;

import com.compi2.contacto.errores.Diagnostico;
import com.compi2.contacto.proyecto.ArchivoFuente;

import java.util.List;
import java.util.Objects;

public record ResultadoAnalisisArchivo(
        ArchivoFuente archivo,
        List<Diagnostico> diagnosticos
) {
    public ResultadoAnalisisArchivo {
        Objects.requireNonNull(archivo, "El archivo es obligatorio");
        diagnosticos = List.copyOf(
                Objects.requireNonNull(diagnosticos, "Los diagnosticos son obligatorios")
        );
    }

    public boolean esValido() {
        return diagnosticos.stream().noneMatch(Diagnostico::esError);
    }
}

