package com.compi2.contacto.memoria;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BajadorLlamadasProyecto {

    private PlanMemoriaProyecto plan;
    private ProgramaIntermedio destino;
    private MarcoStackProyecto marcoActual;
    private final List<ParametroPendiente> parametrosPendientes;
    private int siguienteTemporal;
    private int siguienteTemporalObjeto;

    public BajadorLlamadasProyecto() {
        parametrosPendientes =
                new ArrayList<>();
    }

    public ProgramaIntermedio bajar(
            ProgramaIntermedio origen,
            PlanMemoriaProyecto plan
    ) {
        Objects.requireNonNull(
                origen,
                "El programa intermedio es obligatorio"
        );

        this.plan =
                Objects.requireNonNull(
                        plan,
                        "El plan de memoria es obligatorio"
                );

        destino =
                new ProgramaIntermedio();

        marcoActual = null;
        parametrosPendientes.clear();
        siguienteTemporal = 0;
        siguienteTemporalObjeto = 0;

        for (Cuarteta cuarteta
                : origen.cuartetas()) {

            procesar(
                    cuarteta
            );
        }

        vaciarParametrosRestantes();

        return destino;
    }

    private void procesar(
            Cuarteta cuarteta
    ) {
        switch (cuarteta.operador()) {

            case INICIO_FUNCION ->
                    iniciarFuncion(
                            cuarteta
                    );

            case FIN_FUNCION ->
                    finalizarFuncion(
                            cuarteta
                    );

            case PARAMETRO ->
                    registrarParametro(
                            cuarteta
                    );

            case LLAMAR ->
                    bajarLlamada(
                            cuarteta
                    );

            case NUEVO_OBJETO ->
                    bajarNuevoObjeto(
                            cuarteta
                    );

            default ->
                    copiar(
                            cuarteta
                    );
        }
    }

    private void iniciarFuncion(
            Cuarteta cuarteta
    ) {
        vaciarParametrosRestantes();

        marcoActual =
                plan.marco(
                        cuarteta.argumento1()
                ).orElse(
                        null
                );

        copiar(
                cuarteta
        );
    }

    private void finalizarFuncion(
            Cuarteta cuarteta
    ) {
        vaciarParametrosRestantes();

        copiar(
                cuarteta
        );

        marcoActual = null;
    }

    private void registrarParametro(
            Cuarteta cuarteta
    ) {
        parametrosPendientes.add(
                new ParametroPendiente(
                        cuarteta.argumento1(),
                        cuarteta.argumento2()
                )
        );
    }

    private void bajarLlamada(
            Cuarteta cuarteta
    ) {
        int cantidad =
                entero(
                        cuarteta.argumento2(),
                        0
                );

        List<ParametroPendiente> parametros =
                consumirUltimos(
                        cantidad
                );

        Optional<MarcoStackProyecto> marcoDestino =
                plan.marco(
                        cuarteta.argumento1()
                );

        if (marcoActual == null
                || marcoDestino.isEmpty()) {

            copiarParametrosSimbolicos(
                    parametros
            );

            copiar(
                    cuarteta
            );

            return;
        }

        int tamanoLlamador =
                marcoActual.tamano();

        for (int indiceLista = 0;
             indiceLista < parametros.size();
             indiceLista++) {

            ParametroPendiente parametro =
                    parametros.get(
                            indiceLista
                    );

            int indiceParametro =
                    entero(
                            parametro.indice(),
                            indiceLista
                    );

            int desplazamiento =
                    tamanoLlamador
                            + 1
                            + indiceParametro;

            agregar(
                    OperadorCuarteta.ESCRIBIR_STACK,
                    "P+" + desplazamiento,
                    parametro.valor(),
                    null
            );
        }

        moverPAdelante(
                tamanoLlamador
        );

        agregar(
                OperadorCuarteta.LLAMAR,
                cuarteta.argumento1(),
                cuarteta.argumento2(),
                null
        );

        MarcoStackProyecto destinoLlamada =
                marcoDestino.orElseThrow();

        boolean retornaValor =
                destinoLlamada.retorno()
                        .map(
                                slot ->
                                        !slot.tipo()
                                                .equalsIgnoreCase(
                                                        "void"
                                                )
                        )
                        .orElse(
                                false
                        );

        String resultado =
                cuarteta.resultado();

        if (retornaValor
                && !esVacio(
                resultado
        )) {

            agregar(
                    OperadorCuarteta.LEER_STACK,
                    "P+0",
                    null,
                    resultado
            );
        }

        moverPAtras(
                tamanoLlamador
        );
    }

    private void bajarNuevoObjeto(
            Cuarteta cuarteta
    ) {
        int cantidad =
                entero(
                        cuarteta.argumento2(),
                        0
                );

        List<ParametroPendiente> parametros =
                consumirUltimos(
                        cantidad
                );

        String constructor =
                cuarteta.argumento1();

        String clase =
                extraerClaseConstructor(
                        constructor
                );

        Optional<MarcoStackProyecto> marcoConstructor =
                plan.marco(
                        constructor
                );

        Optional<LayoutHeapProyecto> layout =
                clase == null
                        ? Optional.empty()
                        : plan.layout(
                        "Z::" + clase
                );

        if (marcoActual == null
                || marcoConstructor.isEmpty()
                || layout.isEmpty()) {

            copiarParametrosSimbolicos(
                    parametros
            );

            copiar(
                    cuarteta
            );

            return;
        }

        LayoutHeapProyecto layoutObjeto =
                layout.orElseThrow();

        String referenciaObjeto =
                nuevoTemporalObjeto();

        agregar(
                OperadorCuarteta.ASIGNAR,
                "H",
                null,
                referenciaObjeto
        );

        String nuevoHeap =
                nuevoTemporalObjeto();

        agregar(
                OperadorCuarteta.SUMAR,
                "H",
                String.valueOf(
                        layoutObjeto.tamano()
                ),
                nuevoHeap
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                nuevoHeap,
                null,
                "H"
        );

        int tamanoLlamador =
                marcoActual.tamano();

        agregar(
                OperadorCuarteta.ESCRIBIR_STACK,
                "P+"
                        + (
                        tamanoLlamador
                                + 1
                ),
                referenciaObjeto,
                null
        );

        for (int indiceLista = 0;
             indiceLista < parametros.size();
             indiceLista++) {

            ParametroPendiente parametro =
                    parametros.get(
                            indiceLista
                    );

            int indiceParametro =
                    entero(
                            parametro.indice(),
                            indiceLista
                    );

            int desplazamiento =
                    tamanoLlamador
                            + 2
                            + indiceParametro;

            agregar(
                    OperadorCuarteta.ESCRIBIR_STACK,
                    "P+" + desplazamiento,
                    parametro.valor(),
                    null
            );
        }

        moverPAdelante(
                tamanoLlamador
        );

        agregar(
                OperadorCuarteta.LLAMAR,
                constructor,
                String.valueOf(
                        cantidad + 1
                ),
                null
        );

        if (!esVacio(
                cuarteta.resultado()
        )) {

            agregar(
                    OperadorCuarteta.LEER_STACK,
                    "P+0",
                    null,
                    cuarteta.resultado()
            );
        }

        moverPAtras(
                tamanoLlamador
        );
    }

    private String extraerClaseConstructor(
            String constructor
    ) {
        if (constructor == null
                || constructor.isBlank()) {

            return null;
        }

        int indice =
                constructor.indexOf(
                        ".<init>("
                );

        if (indice <= 0) {
            return null;
        }

        return constructor.substring(
                0,
                indice
        );
    }

    private void moverPAdelante(
            int cantidad
    ) {
        if (cantidad <= 0) {
            return;
        }

        String temporal =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.SUMAR,
                "P",
                String.valueOf(
                        cantidad
                ),
                temporal
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                temporal,
                null,
                "P"
        );
    }

    private void moverPAtras(
            int cantidad
    ) {
        if (cantidad <= 0) {
            return;
        }

        String temporal =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.RESTAR,
                "P",
                String.valueOf(
                        cantidad
                ),
                temporal
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                temporal,
                null,
                "P"
        );
    }

    private List<ParametroPendiente> consumirUltimos(
            int cantidad
    ) {
        if (cantidad <= 0) {
            return List.of();
        }

        int disponibles =
                parametrosPendientes.size();

        int inicio =
                Math.max(
                        0,
                        disponibles
                                - cantidad
                );

        List<ParametroPendiente> resultado =
                new ArrayList<>(
                        parametrosPendientes.subList(
                                inicio,
                                disponibles
                        )
                );

        parametrosPendientes.subList(
                inicio,
                disponibles
        ).clear();

        return resultado;
    }

    private void copiarParametrosSimbolicos(
            List<ParametroPendiente> parametros
    ) {
        for (ParametroPendiente parametro
                : parametros) {

            agregar(
                    OperadorCuarteta.PARAMETRO,
                    parametro.valor(),
                    parametro.indice(),
                    null
            );
        }
    }

    private void vaciarParametrosRestantes() {
        copiarParametrosSimbolicos(
                new ArrayList<>(
                        parametrosPendientes
                )
        );

        parametrosPendientes.clear();
    }

    private int entero(
            String valor,
            int predeterminado
    ) {
        if (valor == null
                || valor.isBlank()
                || valor.equals("-")) {

            return predeterminado;
        }

        try {
            return Integer.parseInt(
                    valor
            );

        } catch (NumberFormatException excepcion) {

            return predeterminado;
        }
    }

    private boolean esVacio(
            String valor
    ) {
        return valor == null
                || valor.isBlank()
                || valor.equals("-");
    }

    private String nuevoTemporal() {
        return "call_t"
                + siguienteTemporal++;
    }

    private String nuevoTemporalObjeto() {
        return "obj_t"
                + siguienteTemporalObjeto++;
    }

    private void copiar(
            Cuarteta cuarteta
    ) {
        agregar(
                cuarteta.operador(),
                cuarteta.argumento1(),
                cuarteta.argumento2(),
                cuarteta.resultado()
        );
    }

    private void agregar(
            OperadorCuarteta operador,
            String argumento1,
            String argumento2,
            String resultado
    ) {
        destino.agregar(
                new Cuarteta(
                        operador,
                        argumento1,
                        argumento2,
                        resultado
                )
        );
    }

    private record ParametroPendiente(
            String valor,
            String indice
    ) {
    }
}