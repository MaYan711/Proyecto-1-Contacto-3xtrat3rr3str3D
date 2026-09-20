package com.compi2.contacto.semantica;

import com.compi2.contacto.ast.piglatin.PAst;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class EnlacesPig {

    private final Map<PAst.Llamada, EnlaceLlamada> llamadas;
    private final Map<PAst.NuevoObjeto, EnlaceConstructor> constructores;

    public EnlacesPig() {
        llamadas = new IdentityHashMap<>();
        constructores = new IdentityHashMap<>();
    }

    public void registrarLlamada(
            PAst.Llamada llamada,
            String destino,
            Optional<PAst.Expresion> receptor
    ) {
        Objects.requireNonNull(llamada);
        Objects.requireNonNull(destino);
        Objects.requireNonNull(receptor);

        llamadas.put(
                llamada,
                new EnlaceLlamada(
                        destino,
                        receptor
                )
        );
    }

    public void registrarConstructor(
            PAst.NuevoObjeto nuevo,
            String destino,
            String clase
    ) {
        Objects.requireNonNull(nuevo);
        Objects.requireNonNull(destino);
        Objects.requireNonNull(clase);

        constructores.put(
                nuevo,
                new EnlaceConstructor(
                        destino,
                        clase
                )
        );
    }

    public Optional<EnlaceLlamada> buscarLlamada(
            PAst.Llamada llamada
    ) {
        return Optional.ofNullable(
                llamadas.get(
                        llamada
                )
        );
    }

    public Optional<EnlaceConstructor> buscarConstructor(
            PAst.NuevoObjeto nuevo
    ) {
        return Optional.ofNullable(
                constructores.get(
                        nuevo
                )
        );
    }

    public record EnlaceLlamada(
            String destino,
            Optional<PAst.Expresion> receptor
    ) {

        public EnlaceLlamada {
            Objects.requireNonNull(destino);
            Objects.requireNonNull(receptor);
        }

        public boolean tieneReceptor() {
            return receptor.isPresent();
        }
    }

    public record EnlaceConstructor(
            String destino,
            String clase
    ) {

        public EnlaceConstructor {
            Objects.requireNonNull(destino);
            Objects.requireNonNull(clase);
        }
    }
}