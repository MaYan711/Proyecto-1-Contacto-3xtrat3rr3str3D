package com.compi2.contacto.memoria;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class BajadorMemoriaProyecto {

    private static final Pattern TEMPORAL =
            Pattern.compile("(?:(?:y|z|pig|mem)_)?t\\d+");

    private static final Pattern NUMERO =
            Pattern.compile("-?\\d+(?:\\.\\d+)?");

    private PlanMemoriaProyecto plan;
    private ProgramaIntermedio destino;
    private MarcoStackProyecto marcoActual;
    private String funcionActual;
    private String claseZActual;
    private int siguienteTemporal;

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
        funcionActual = null;
        claseZActual = null;
        siguienteTemporal = 0;

        for (Cuarteta cuarteta
                : origen.cuartetas()) {

            bajarCuarteta(
                    cuarteta
            );
        }

        return destino;
    }

    private void bajarCuarteta(
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

            case ASIGNAR ->
                    bajarAsignacion(
                            cuarteta
                    );

            case RETORNAR ->
                    bajarRetorno(
                            cuarteta
                    );

            case IMPRIMIR,
                 PARAMETRO ->
                    bajarUsoUnario(
                            cuarteta
                    );

            case SALTAR_SI_FALSO,
                 SALTAR_SI_VERDADERO ->
                    bajarSaltoCondicional(
                            cuarteta
                    );

            case SUMAR,
                 RESTAR,
                 MULTIPLICAR,
                 DIVIDIR,
                 MODULO,
                 IGUALDAD,
                 DIFERENTE,
                 MENOR,
                 MAYOR,
                 MENOR_IGUAL,
                 MAYOR_IGUAL,
                 AND,
                 OR,
                 COMPARAR ->
                    bajarOperacionBinaria(
                            cuarteta
                    );

            case NEGATIVO,
                 NEGAR ->
                    bajarOperacionUnaria(
                            cuarteta
                    );

            case LEER,
                 LLAMAR,
                 NUEVO_OBJETO,
                 NUEVO_ARREGLO,
                 INICIALIZAR_COMPUESTO ->
                    bajarProductorValor(
                            cuarteta
                    );

            case DECLARAR ->
                    bajarDeclaracion(
                            cuarteta
                    );

            case DECLARAR_PARAMETRO,
                 ETIQUETA,
                 SALTAR,
                 LEER_STACK,
                 ESCRIBIR_STACK,
                 LEER_HEAP,
                 ESCRIBIR_HEAP ->
                    copiar(
                            cuarteta
                    );
        }
    }

    private void iniciarFuncion(
            Cuarteta cuarteta
    ) {
        funcionActual =
                cuarteta.argumento1();

        marcoActual =
                plan.marco(
                        funcionActual
                ).orElse(
                        null
                );

        claseZActual =
                extraerClaseZ(
                        funcionActual
                );

        copiar(
                cuarteta
        );
    }

    private void finalizarFuncion(
            Cuarteta cuarteta
    ) {
        copiar(
                cuarteta
        );

        funcionActual = null;
        marcoActual = null;
        claseZActual = null;
    }

    private void bajarDeclaracion(
            Cuarteta cuarteta
    ) {
        copiar(
                cuarteta
        );

        if (marcoActual == null
                || esVacio(
                cuarteta.resultado()
        )) {

            return;
        }

        Optional<SlotStackProyecto> slot =
                marcoActual.buscar(
                        cuarteta.resultado()
                );

        if (slot.isEmpty()) {
            return;
        }

        SlotStackProyecto destino =
                slot.orElseThrow();

        agregar(
                OperadorCuarteta.ESCRIBIR_STACK,
                destino.direccion(),
                valorInicialTipo(
                        destino.tipo()
                ),
                null
        );
    }

    private String valorInicialTipo(
            String tipo
    ) {
        if (tipo == null
                || tipo.isBlank()) {

            return "0";
        }

        String normalizado =
                tipo.trim();

        if (normalizado.endsWith("[]")) {
            return "-1";
        }

        return switch (normalizado) {
            case "entero",
                 "flotante",
                 "caracter",
                 "bool",
                 "int",
                 "double",
                 "char",
                 "boolean",
                 "numerus",
                 "decimalis",
                 "littera",
                 "void" ->
                    "0";

            default ->
                    "-1";
        };
    }

    private void bajarAsignacion(
            Cuarteta cuarteta
    ) {
        String valor =
                leerValor(
                        cuarteta.argumento1()
                );

        escribirDestino(
                cuarteta.resultado(),
                valor
        );
    }

    private void bajarRetorno(
            Cuarteta cuarteta
    ) {
        if (!esVacio(
                cuarteta.argumento1()
        )) {
            String valor =
                    leerValor(
                            cuarteta.argumento1()
                    );

            if (marcoActual != null
                    && marcoActual.retorno()
                    .isPresent()) {

                SlotStackProyecto retorno =
                        marcoActual.retorno()
                                .orElseThrow();

                agregar(
                        OperadorCuarteta.ESCRIBIR_STACK,
                        retorno.direccion(),
                        valor,
                        null
                );
            }
        }

        agregar(
                OperadorCuarteta.RETORNAR,
                null,
                null,
                null
        );
    }

    private void bajarUsoUnario(
            Cuarteta cuarteta
    ) {
        String argumento =
                leerValor(
                        cuarteta.argumento1()
                );

        agregar(
                cuarteta.operador(),
                argumento,
                cuarteta.argumento2(),
                cuarteta.resultado()
        );
    }

    private void bajarSaltoCondicional(
            Cuarteta cuarteta
    ) {
        String condicion =
                leerValor(
                        cuarteta.argumento1()
                );

        agregar(
                cuarteta.operador(),
                condicion,
                cuarteta.argumento2(),
                cuarteta.resultado()
        );
    }

    private void bajarOperacionBinaria(
            Cuarteta cuarteta
    ) {
        String izquierda =
                leerValor(
                        cuarteta.argumento1()
                );

        String derecha =
                leerValor(
                        cuarteta.argumento2()
                );

        emitirConDestino(
                cuarteta.operador(),
                izquierda,
                derecha,
                cuarteta.resultado()
        );
    }

    private void bajarOperacionUnaria(
            Cuarteta cuarteta
    ) {
        String valor =
                leerValor(
                        cuarteta.argumento1()
                );

        emitirConDestino(
                cuarteta.operador(),
                valor,
                cuarteta.argumento2(),
                cuarteta.resultado()
        );
    }

    private void bajarProductorValor(
            Cuarteta cuarteta
    ) {
        emitirConDestino(
                cuarteta.operador(),
                cuarteta.argumento1(),
                cuarteta.argumento2(),
                cuarteta.resultado()
        );
    }

    private void emitirConDestino(
            OperadorCuarteta operador,
            String argumento1,
            String argumento2,
            String resultado
    ) {
        if (esVacio(
                resultado
        )) {
            agregar(
                    operador,
                    argumento1,
                    argumento2,
                    resultado
            );

            return;
        }

        if (esDestinoMemoria(
                resultado
        )) {
            String temporal =
                    nuevoTemporal();

            agregar(
                    operador,
                    argumento1,
                    argumento2,
                    temporal
            );

            escribirDestino(
                    resultado,
                    temporal
            );

            return;
        }

        agregar(
                operador,
                argumento1,
                argumento2,
                resultado
        );
    }

    private String leerValor(
            String valor
    ) {
        if (esVacio(
                valor
        )
                || esLiteral(
                valor
        )
                || esDireccion(
                valor
        )) {

            return valor;
        }

        Optional<SlotStackProyecto> slot =
                buscarSlot(
                        valor
                );

        if (slot.isPresent()) {
            return leerStack(
                    slot.orElseThrow()
            );
        }

        Optional<String> campoActual =
                direccionCampoActual(
                        valor
                );

        if (campoActual.isPresent()) {
            return leerHeap(
                    campoActual.orElseThrow(),
                    tipoCampoActual(
                            valor
                    ).orElse(null)
            );
        }

        Optional<String> miembro =
                direccionMiembro(
                        valor
                );

        if (miembro.isPresent()) {
            return leerHeap(
                    miembro.orElseThrow(),
                    tipoMiembro(
                            valor
                    ).orElse(null)
            );
        }

        if (esTemporal(
                valor
        )) {
            return valor;
        }

        return valor;
    }

    private void escribirDestino(
            String destinoSimbolico,
            String valor
    ) {
        Optional<SlotStackProyecto> slot =
                buscarSlot(
                        destinoSimbolico
                );

        if (slot.isPresent()) {
            agregar(
                    OperadorCuarteta.ESCRIBIR_STACK,
                    slot.orElseThrow()
                            .direccion(),
                    valor,
                    null
            );

            return;
        }

        Optional<String> campoActual =
                direccionCampoActual(
                        destinoSimbolico
                );

        if (campoActual.isPresent()) {
            agregar(
                    OperadorCuarteta.ESCRIBIR_HEAP,
                    campoActual.orElseThrow(),
                    valor,
                    null
            );

            return;
        }

        Optional<String> miembro =
                direccionMiembro(
                        destinoSimbolico
                );

        if (miembro.isPresent()) {
            agregar(
                    OperadorCuarteta.ESCRIBIR_HEAP,
                    miembro.orElseThrow(),
                    valor,
                    null
            );

            return;
        }

        agregar(
                OperadorCuarteta.ASIGNAR,
                valor,
                null,
                destinoSimbolico
        );
    }

    private boolean esDestinoMemoria(
            String valor
    ) {
        return buscarSlot(
                valor
        ).isPresent()
                || direccionCampoActual(
                valor
        ).isPresent()
                || direccionMiembroSinEmitir(
                valor
        ).isPresent();
    }

    private Optional<SlotStackProyecto> buscarSlot(
            String nombre
    ) {
        if (marcoActual == null
                || esVacio(
                nombre
        )) {

            return Optional.empty();
        }

        return marcoActual.buscar(
                nombre
        );
    }

    private Optional<String> direccionCampoActual(
            String nombre
    ) {
        if (claseZActual == null
                || marcoActual == null
                || nombre.contains(".")
                || nombre.contains("[")
                || esVacio(
                nombre
        )) {

            return Optional.empty();
        }

        Optional<LayoutHeapProyecto> layout =
                plan.layout(
                        "Z::"
                                + claseZActual
                );

        if (layout.isEmpty()) {
            return Optional.empty();
        }

        Optional<LayoutHeapProyecto.Campo> campo =
                layout.orElseThrow()
                        .buscarCampo(
                                nombre
                        );

        if (campo.isEmpty()) {
            return Optional.empty();
        }

        Optional<SlotStackProyecto> thisSlot =
                marcoActual.thisSlot();

        if (thisSlot.isEmpty()) {
            return Optional.empty();
        }

        String referencia =
                leerStack(
                        thisSlot.orElseThrow()
                );

        return Optional.of(
                referencia
                        + "+"
                        + campo.orElseThrow()
                        .desplazamiento()
        );
    }

    private Optional<String> direccionMiembro(
            String expresion
    ) {
        Optional<DireccionMiembro> direccion =
                analizarMiembro(
                        expresion
                );

        if (direccion.isEmpty()) {
            return Optional.empty();
        }

        DireccionMiembro miembro =
                direccion.orElseThrow();

        String referencia =
                leerStack(
                        miembro.slot()
                );

        return Optional.of(
                referencia
                        + "+"
                        + miembro.campo()
                        .desplazamiento()
        );
    }

    private Optional<DireccionMiembro> direccionMiembroSinEmitir(
            String expresion
    ) {
        return analizarMiembro(
                expresion
        );
    }

    private Optional<DireccionMiembro> analizarMiembro(
            String expresion
    ) {
        if (marcoActual == null
                || esVacio(
                expresion
        )
                || expresion.contains("[")
                || expresion.contains("(")) {

            return Optional.empty();
        }

        int punto =
                expresion.indexOf('.');

        if (punto <= 0
                || punto != expresion.lastIndexOf('.')
                || punto >= expresion.length() - 1) {

            return Optional.empty();
        }

        String base =
                expresion.substring(
                        0,
                        punto
                );

        String campoNombre =
                expresion.substring(
                        punto + 1
                );

        Optional<SlotStackProyecto> slot =
                marcoActual.buscar(
                        base
                );

        if (slot.isEmpty()) {
            return Optional.empty();
        }

        Optional<LayoutHeapProyecto> layout =
                buscarLayoutTipo(
                        slot.orElseThrow()
                                .tipo()
                );

        if (layout.isEmpty()) {
            return Optional.empty();
        }

        Optional<LayoutHeapProyecto.Campo> campo =
                layout.orElseThrow()
                        .buscarCampo(
                                campoNombre
                        );

        if (campo.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                new DireccionMiembro(
                        slot.orElseThrow(),
                        campo.orElseThrow()
                )
        );
    }

    private Optional<String> tipoCampoActual(
            String nombre
    ) {
        if (claseZActual == null
                || nombre == null
                || nombre.isBlank()
                || nombre.contains(".")
                || nombre.contains("[")) {

            return Optional.empty();
        }

        Optional<LayoutHeapProyecto> layout =
                plan.layout(
                        "Z::"
                                + claseZActual
                );

        if (layout.isEmpty()) {
            return Optional.empty();
        }

        return layout.orElseThrow()
                .buscarCampo(
                        nombre
                )
                .map(
                        LayoutHeapProyecto.Campo::tipo
                );
    }

    private Optional<String> tipoMiembro(
            String expresion
    ) {
        return analizarMiembro(
                expresion
        ).map(
                direccion ->
                        direccion.campo()
                                .tipo()
        );
    }

    private Optional<LayoutHeapProyecto> buscarLayoutTipo(
            String tipo
    ) {
        if (tipo == null
                || tipo.isBlank()
                || tipo.endsWith("[]")) {

            return Optional.empty();
        }

        Optional<LayoutHeapProyecto> z =
                plan.layout(
                        "Z::"
                                + tipo
                );

        if (z.isPresent()) {
            return z;
        }

        Optional<LayoutHeapProyecto> yGlobal =
                plan.layout(
                        "Y::"
                                + tipo
                );

        if (yGlobal.isPresent()) {
            return yGlobal;
        }

        if (funcionActual != null) {
            return plan.layout(
                    "Y::"
                            + funcionActual
                            + "::"
                            + tipo
            );
        }

        return Optional.empty();
    }

    private String leerStack(
            SlotStackProyecto slot
    ) {
        String temporal =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.LEER_STACK,
                slot.direccion(),
                slot.tipo(),
                temporal
        );

        return temporal;
    }

    private String leerHeap(
            String direccion,
            String tipo
    ) {
        String temporal =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.LEER_HEAP,
                direccion,
                tipo,
                temporal
        );

        return temporal;
    }

    private String extraerClaseZ(
            String funcion
    ) {
        if (funcion == null
                || funcion.equals("MAIOR")
                || !funcion.contains(".")) {

            return null;
        }

        int punto =
                funcion.indexOf('.');

        if (punto <= 0) {
            return null;
        }

        String clase =
                funcion.substring(
                        0,
                        punto
                );

        return plan.layout(
                "Z::"
                        + clase
        ).isPresent()
                ? clase
                : null;
    }

    private boolean esLiteral(
            String valor
    ) {
        if (valor == null) {
            return false;
        }

        String texto =
                valor.trim();

        if (texto.startsWith("\"")
                && texto.endsWith("\"")) {

            return true;
        }

        if (texto.startsWith("'")
                && texto.endsWith("'")) {

            return true;
        }

        if (NUMERO.matcher(
                texto
        ).matches()) {

            return true;
        }

        return texto.equals("true")
                || texto.equals("false")
                || texto.equals("verdadero")
                || texto.equals("falso")
                || texto.equals("verum")
                || texto.equals("falsus")
                || texto.equals("null");
    }

    private boolean esTemporal(
            String valor
    ) {
        return valor != null
                && TEMPORAL.matcher(
                valor
        ).matches();
    }

    private boolean esDireccion(
            String valor
    ) {
        if (valor == null) {
            return false;
        }

        return valor.startsWith("P+")
                || valor.equals("P")
                || valor.startsWith("H+")
                || valor.equals("H");
    }

    private boolean esVacio(
            String valor
    ) {
        return valor == null
                || valor.isBlank()
                || valor.equals("-");
    }

    private String nuevoTemporal() {
        return "mem_t"
                + siguienteTemporal++;
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

    private record DireccionMiembro(
            SlotStackProyecto slot,
            LayoutHeapProyecto.Campo campo
    ) {
    }
}