package com.compi2.contacto.memoria;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record PlanMemoriaProyecto(
        Map<String, MarcoStackProyecto> marcos,
        Map<String, LayoutHeapProyecto> layoutsHeap
) {

    public PlanMemoriaProyecto {
        marcos = Map.copyOf(
                Objects.requireNonNull(marcos)
        );

        layoutsHeap = Map.copyOf(
                Objects.requireNonNull(layoutsHeap)
        );
    }

    public Optional<MarcoStackProyecto> marco(
            String nombre
    ) {
        return Optional.ofNullable(
                marcos.get(nombre)
        );
    }

    public Optional<LayoutHeapProyecto> layout(
            String nombre
    ) {
        return Optional.ofNullable(
                layoutsHeap.get(nombre)
        );
    }
}