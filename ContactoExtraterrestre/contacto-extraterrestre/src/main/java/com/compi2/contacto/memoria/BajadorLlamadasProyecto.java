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
    private String funcionActual;
    private final List<ParametroPendiente> parametrosPendientes;
    private final List<FuncionDisponible> funcionesDisponibles;
    private int siguienteTemporal;
    private int siguienteTemporalObjeto;

    public BajadorLlamadasProyecto() {
        parametrosPendientes = new ArrayList<>();
        funcionesDisponibles = new ArrayList<>();
    }

    public ProgramaIntermedio bajar(
            ProgramaIntermedio origen,
            PlanMemoriaProyecto plan
    ) {
        Objects.requireNonNull(
                origen,
                "El programa intermedio es obligatorio"
        );

        this.plan = Objects.requireNonNull(
                plan,
                "El plan de memoria es obligatorio"
        );

        destino = new ProgramaIntermedio();
        marcoActual = null;
        funcionActual = null;
        parametrosPendientes.clear();
        funcionesDisponibles.clear();
        siguienteTemporal = 0;
        siguienteTemporalObjeto = 0;

        registrarFunciones(origen);

        for (Cuarteta cuarteta : origen.cuartetas()) {
            procesar(cuarteta);
        }

        vaciarParametrosRestantes();

        return destino;
    }

    private void registrarFunciones(
            ProgramaIntermedio programa
    ) {
        for (Cuarteta cuarteta : programa.cuartetas()) {
            if (cuarteta.operador()
                    != OperadorCuarteta.INICIO_FUNCION) {
                continue;
            }

            int cantidadParametros =
                    entero(
                            cuarteta.resultado(),
                            -1
                    );

            if (cantidadParametros < 0
                    || esVacio(cuarteta.argumento1())) {
                continue;
            }

            funcionesDisponibles.add(
                    new FuncionDisponible(
                            cuarteta.argumento1(),
                            cantidadParametros
                    )
            );
        }
    }

    private void procesar(
            Cuarteta cuarteta
    ) {
        switch (cuarteta.operador()) {
            case INICIO_FUNCION ->
                    iniciarFuncion(cuarteta);

            case FIN_FUNCION ->
                    finalizarFuncion(cuarteta);

            case PARAMETRO ->
                    registrarParametro(cuarteta);

            case LLAMAR ->
                    bajarLlamada(cuarteta);

            case NUEVO_OBJETO ->
                    bajarNuevoObjeto(cuarteta);

            default ->
                    copiar(cuarteta);
        }
    }

    private void iniciarFuncion(
            Cuarteta cuarteta
    ) {
        vaciarParametrosRestantes();

        funcionActual =
                cuarteta.argumento1();

        marcoActual =
                plan.marco(
                        funcionActual
                ).orElse(null);

        copiar(cuarteta);
    }

    private void finalizarFuncion(
            Cuarteta cuarteta
    ) {
        vaciarParametrosRestantes();

        copiar(cuarteta);

        marcoActual = null;
        funcionActual = null;
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
        int cantidadExplicita =
                entero(
                        cuarteta.argumento2(),
                        0
                );

        List<ParametroPendiente> parametros =
                consumirUltimos(
                        cantidadExplicita
                );

        String destinoLlamada =
                cuarteta.argumento1();

        Optional<MarcoStackProyecto> marcoDestino =
                plan.marco(
                        destinoLlamada
                );

        boolean llamadaInternaZ = false;

        if (marcoDestino.isEmpty()) {
            Optional<String> resuelto =
                    resolverMetodoInternoZ(
                            destinoLlamada,
                            cantidadExplicita
                    );

            if (resuelto.isPresent()) {
                destinoLlamada =
                        resuelto.orElseThrow();

                marcoDestino =
                        plan.marco(
                                destinoLlamada
                        );

                llamadaInternaZ =
                        marcoDestino.isPresent();
            }
        }

        if (marcoActual == null
                || marcoDestino.isEmpty()) {

            copiarParametrosSimbolicos(
                    parametros
            );

            copiar(cuarteta);

            return;
        }

        int cantidadTotal =
                cantidadExplicita;

        if (llamadaInternaZ) {
            Optional<SlotStackProyecto> thisSlot =
                    marcoActual.thisSlot();

            if (thisSlot.isEmpty()) {
                copiarParametrosSimbolicos(
                        parametros
                );

                copiar(cuarteta);

                return;
            }

            String referenciaThis =
                    nuevoTemporal();

            agregar(
                    OperadorCuarteta.LEER_STACK,
                    thisSlot.orElseThrow()
                            .direccion(),
                    null,
                    referenciaThis
            );

            List<ParametroPendiente> completos =
                    new ArrayList<>();

            completos.add(
                    new ParametroPendiente(
                            referenciaThis,
                            "0"
                    )
            );

            for (int indice = 0;
                 indice < parametros.size();
                 indice++) {

                ParametroPendiente parametro =
                        parametros.get(indice);

                int indiceOriginal =
                        entero(
                                parametro.indice(),
                                indice
                        );

                completos.add(
                        new ParametroPendiente(
                                parametro.valor(),
                                String.valueOf(
                                        indiceOriginal + 1
                                )
                        )
                );
            }

            parametros =
                    completos;

            cantidadTotal =
                    cantidadExplicita + 1;
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
                destinoLlamada,
                String.valueOf(
                        cantidadTotal
                ),
                null
        );

        MarcoStackProyecto destino =
                marcoDestino.orElseThrow();

        boolean retornaValor =
                destino.retorno()
                        .map(
                                slot ->
                                        !slot.tipo()
                                                .equalsIgnoreCase(
                                                        "void"
                                                )
                        )
                        .orElse(false);

        if (retornaValor
                && !esVacio(
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

    private Optional<String> resolverMetodoInternoZ(
            String destino,
            int cantidadExplicita
    ) {
        String clase =
                claseFuncionActual();

        if (clase == null) {
            return Optional.empty();
        }

        String metodo =
                nombreMetodo(
                        destino
                );

        if (metodo == null
                || metodo.equals("<init>")) {

            return Optional.empty();
        }

        int cantidadEsperada =
                cantidadExplicita + 1;

        List<FuncionDisponible> candidatos =
                funcionesDisponibles.stream()
                        .filter(
                                funcion ->
                                        funcion.nombre()
                                                .startsWith(
                                                        clase + "."
                                                )
                        )
                        .filter(
                                funcion ->
                                        metodo.equals(
                                                nombreMetodo(
                                                        funcion.nombre()
                                                )
                                        )
                        )
                        .filter(
                                funcion ->
                                        funcion.cantidadParametros()
                                                == cantidadEsperada
                        )
                        .toList();

        if (candidatos.size() == 1) {
            return Optional.of(
                    candidatos.get(0)
                            .nombre()
            );
        }

        if (candidatos.size() > 1
                && destino.contains("(")) {

            String firma =
                    destino;

            int punto =
                    firma.lastIndexOf('.');

            if (punto >= 0) {
                firma =
                        firma.substring(
                                punto + 1
                        );
            }

            String firmaFinal =
                    firma;

            List<FuncionDisponible> exactos =
                    candidatos.stream()
                            .filter(
                                    funcion ->
                                            funcion.nombre()
                                                    .endsWith(
                                                            "."
                                                                    + firmaFinal
                                                    )
                            )
                            .toList();

            if (exactos.size() == 1) {
                return Optional.of(
                        exactos.get(0)
                                .nombre()
                );
            }
        }

        return Optional.empty();
    }

    private String claseFuncionActual() {
        if (funcionActual == null
                || funcionActual.equals("MAIOR")) {
            return null;
        }

        int punto =
                funcionActual.indexOf('.');

        if (punto <= 0) {
            return null;
        }

        String clase =
                funcionActual.substring(
                        0,
                        punto
                );

        return plan.layout(
                "Z::" + clase
        ).isPresent()
                ? clase
                : null;
    }

    private String nombreMetodo(
            String nombre
    ) {
        if (nombre == null
                || nombre.isBlank()) {
            return null;
        }

        String resultado =
                nombre;

        int punto =
                resultado.lastIndexOf('.');

        if (punto >= 0
                && punto < resultado.length() - 1) {

            resultado =
                    resultado.substring(
                            punto + 1
                    );
        }

        int parentesis =
                resultado.indexOf('(');

        if (parentesis >= 0) {
            resultado =
                    resultado.substring(
                            0,
                            parentesis
                    );
        }

        return resultado;
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
                || layout.isEmpty()) {

            copiarParametrosSimbolicos(
                    parametros
            );

            copiar(cuarteta);

            return;
        }

        if (marcoConstructor.isEmpty()) {
            if (cantidad == 0) {
                bajarConstructorImplicito(
                        cuarteta,
                        layout.orElseThrow()
                );

                return;
            }

            copiarParametrosSimbolicos(
                    parametros
            );

            copiar(cuarteta);

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
                        Math.max(
                                1,
                                layoutObjeto.tamano()
                        )
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
                        tamanoLlamador + 1
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

    private void bajarConstructorImplicito(
            Cuarteta cuarteta,
            LayoutHeapProyecto layout
    ) {
        String referenciaObjeto =
                nuevoTemporalObjeto();

        agregar(
                OperadorCuarteta.ASIGNAR,
                "H",
                null,
                referenciaObjeto
        );

        int tamano =
                Math.max(
                        1,
                        layout.tamano()
                );

        String nuevoHeap =
                nuevoTemporalObjeto();

        agregar(
                OperadorCuarteta.SUMAR,
                "H",
                String.valueOf(
                        tamano
                ),
                nuevoHeap
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                nuevoHeap,
                null,
                "H"
        );

        for (LayoutHeapProyecto.Campo campo
                : layout.campos()) {

            agregar(
                    OperadorCuarteta.ESCRIBIR_HEAP,
                    referenciaObjeto
                            + "+"
                            + campo.desplazamiento(),
                    valorInicialCampo(
                            campo.tipo()
                    ),
                    null
            );
        }

        if (!esVacio(
                cuarteta.resultado()
        )) {

            agregar(
                    OperadorCuarteta.ASIGNAR,
                    referenciaObjeto,
                    null,
                    cuarteta.resultado()
            );
        }
    }

    private String valorInicialCampo(
            String tipo
    ) {
        if (tipo == null
                || tipo.isBlank()) {
            return "0";
        }

        return switch (tipo) {
            case "int",
                 "double",
                 "char",
                 "boolean" ->
                    "0";

            default ->
                    "-1";
        };
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
                        disponibles - cantidad
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

    private record FuncionDisponible(
            String nombre,
            int cantidadParametros
    ) {
    }
}