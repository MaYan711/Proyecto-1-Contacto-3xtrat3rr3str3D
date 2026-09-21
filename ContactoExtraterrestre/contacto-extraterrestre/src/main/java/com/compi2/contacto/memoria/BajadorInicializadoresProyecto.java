package com.compi2.contacto.memoria;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BajadorInicializadoresProyecto {

    private ProgramaIntermedio destino;
    private int siguienteTemporalHeap;

    public ProgramaIntermedio bajar(
            ProgramaIntermedio origen
    ) {
        Objects.requireNonNull(
                origen,
                "El programa intermedio es obligatorio"
        );

        destino =
                new ProgramaIntermedio();
        siguienteTemporalHeap = 0;

        for (Cuarteta cuarteta
                : origen.cuartetas()) {

            if (cuarteta.operador()
                    == OperadorCuarteta.INICIALIZAR_COMPUESTO) {

                bajarInicializador(
                        cuarteta
                );

            } else {

                copiar(
                        cuarteta
                );
            }
        }

        return destino;
    }

    private void bajarInicializador(
            Cuarteta cuarteta
    ) {
        if (esVacio(
                cuarteta.argumento1()
        )
                || esVacio(
                cuarteta.resultado()
        )) {

            copiar(
                    cuarteta
            );

            return;
        }

        Optional<Lista> raiz =
                new ParserLista(
                        cuarteta.argumento1()
                ).parsear();

        if (raiz.isEmpty()) {

            copiar(
                    cuarteta
            );

            return;
        }

        Optional<List<Integer>> dimensiones =
                calcularDimensiones(
                        raiz.orElseThrow()
                );

        if (dimensiones.isEmpty()) {

            copiar(
                    cuarteta
            );

            return;
        }

        List<String> valores =
                new ArrayList<>();

        aplanar(
                raiz.orElseThrow(),
                valores
        );

        String referencia =
                cuarteta.resultado();

        List<Integer> dimensionesLista =
                dimensiones.orElseThrow();

        int inicioDatos =
                1
                        + dimensionesLista.size();

        int totalCeldas =
                inicioDatos
                        + valores.size();

        agregar(
                OperadorCuarteta.ASIGNAR,
                "H",
                null,
                referencia
        );

        String siguienteHeap =
                "init_h"
                        + siguienteTemporalHeap++;

        agregar(
                OperadorCuarteta.SUMAR,
                "H",
                String.valueOf(
                        totalCeldas
                ),
                siguienteHeap
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                siguienteHeap,
                null,
                "H"
        );

        agregar(
                OperadorCuarteta.ESCRIBIR_HEAP,
                referencia + "+0",
                String.valueOf(
                        dimensionesLista.size()
                ),
                null
        );

        for (int indice = 0;
             indice < dimensionesLista.size();
             indice++) {

            agregar(
                    OperadorCuarteta.ESCRIBIR_HEAP,
                    referencia
                            + "+"
                            + (
                            indice + 1
                    ),
                    String.valueOf(
                            dimensionesLista.get(
                                    indice
                            )
                    ),
                    null
            );
        }

        for (int indice = 0;
             indice < valores.size();
             indice++) {

            agregar(
                    OperadorCuarteta.ESCRIBIR_HEAP,
                    referencia
                            + "+"
                            + (
                            inicioDatos + indice
                    ),
                    valores.get(
                            indice
                    ),
                    null
            );
        }
    }

    private Optional<List<Integer>> calcularDimensiones(
            Lista lista
    ) {
        List<Integer> resultado =
                new ArrayList<>();

        if (!calcularDimensiones(
                lista,
                resultado
        )) {

            return Optional.empty();
        }

        return Optional.of(
                resultado
        );
    }

    private boolean calcularDimensiones(
            Lista lista,
            List<Integer> resultado
    ) {
        resultado.add(
                lista.elementos()
                        .size()
        );

        if (lista.elementos()
                .isEmpty()) {

            return true;
        }

        Elemento primero =
                lista.elementos()
                        .get(0);

        if (primero instanceof Valor) {

            for (Elemento elemento
                    : lista.elementos()) {

                if (!(elemento
                        instanceof Valor)) {

                    return false;
                }
            }

            return true;
        }

        if (!(primero
                instanceof Lista primeraLista)) {

            return false;
        }

        List<Integer> dimensionesHijo =
                new ArrayList<>();

        if (!calcularDimensiones(
                primeraLista,
                dimensionesHijo
        )) {

            return false;
        }

        for (int indice = 1;
             indice < lista.elementos()
                     .size();
             indice++) {

            Elemento elemento =
                    lista.elementos()
                            .get(indice);

            if (!(elemento
                    instanceof Lista listaHija)) {

                return false;
            }

            List<Integer> dimensionesActuales =
                    new ArrayList<>();

            if (!calcularDimensiones(
                    listaHija,
                    dimensionesActuales
            )) {

                return false;
            }

            if (!dimensionesHijo.equals(
                    dimensionesActuales
            )) {

                return false;
            }
        }

        resultado.addAll(
                dimensionesHijo
        );

        return true;
    }

    private void aplanar(
            Lista lista,
            List<String> valores
    ) {
        for (Elemento elemento
                : lista.elementos()) {

            if (elemento
                    instanceof Valor valor) {

                valores.add(
                        valor.texto()
                );

            } else if (elemento
                    instanceof Lista interna) {

                aplanar(
                        interna,
                        valores
                );
            }
        }
    }

    private boolean esVacio(
            String valor
    ) {
        return valor == null
                || valor.isBlank()
                || valor.equals("-");
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

    private sealed interface Elemento
            permits Valor, Lista {
    }

    private record Valor(
            String texto
    ) implements Elemento {

        private Valor {
            Objects.requireNonNull(
                    texto
            );
        }
    }

    private record Lista(
            List<Elemento> elementos
    ) implements Elemento {

        private Lista {
            elementos =
                    List.copyOf(
                            elementos
                    );
        }
    }

    private static final class ParserLista {

        private final String texto;
        private int posicion;

        private ParserLista(
                String texto
        ) {
            this.texto =
                    Objects.requireNonNull(
                            texto
                    );

            posicion = 0;
        }

        private Optional<Lista> parsear() {
            omitirEspacios();

            Optional<Lista> resultado =
                    parsearLista();

            if (resultado.isEmpty()) {
                return Optional.empty();
            }

            omitirEspacios();

            if (posicion
                    != texto.length()) {

                return Optional.empty();
            }

            return resultado;
        }

        private Optional<Lista> parsearLista() {
            omitirEspacios();

            if (!consumir(
                    '{'
            )) {

                return Optional.empty();
            }

            List<Elemento> elementos =
                    new ArrayList<>();

            omitirEspacios();

            if (consumir(
                    '}'
            )) {

                return Optional.of(
                        new Lista(
                                elementos
                        )
                );
            }

            while (true) {
                omitirEspacios();

                if (actual()
                        == '{') {

                    Optional<Lista> interna =
                            parsearLista();

                    if (interna.isEmpty()) {
                        return Optional.empty();
                    }

                    elementos.add(
                            interna.orElseThrow()
                    );

                } else {

                    Optional<Valor> valor =
                            parsearValor();

                    if (valor.isEmpty()) {
                        return Optional.empty();
                    }

                    elementos.add(
                            valor.orElseThrow()
                    );
                }

                omitirEspacios();

                if (consumir(
                        '}'
                )) {

                    break;
                }

                if (!consumir(
                        ','
                )) {

                    return Optional.empty();
                }
            }

            return Optional.of(
                    new Lista(
                            elementos
                    )
            );
        }

        private Optional<Valor> parsearValor() {
            omitirEspacios();

            int inicio =
                    posicion;

            boolean cadena =
                    false;

            boolean caracter =
                    false;

            boolean escape =
                    false;

            int parentesis =
                    0;

            while (posicion
                    < texto.length()) {

                char actual =
                        texto.charAt(
                                posicion
                        );

                if (escape) {
                    escape = false;
                    posicion++;
                    continue;
                }

                if ((cadena
                        || caracter)
                        && actual == '\\') {

                    escape = true;
                    posicion++;
                    continue;
                }

                if (!caracter
                        && actual == '"') {

                    cadena =
                            !cadena;

                    posicion++;
                    continue;
                }

                if (!cadena
                        && actual == '\'') {

                    caracter =
                            !caracter;

                    posicion++;
                    continue;
                }

                if (!cadena
                        && !caracter) {

                    if (actual == '(') {
                        parentesis++;
                        posicion++;
                        continue;
                    }

                    if (actual == ')'
                            && parentesis > 0) {

                        parentesis--;
                        posicion++;
                        continue;
                    }

                    if (parentesis == 0
                            && (
                            actual == ','
                                    || actual == '}'
                    )) {

                        break;
                    }
                }

                posicion++;
            }

            if (cadena
                    || caracter
                    || parentesis != 0) {

                return Optional.empty();
            }

            String valor =
                    texto.substring(
                            inicio,
                            posicion
                    ).trim();

            if (valor.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(
                    new Valor(
                            valor
                    )
            );
        }

        private void omitirEspacios() {
            while (posicion
                    < texto.length()
                    && Character.isWhitespace(
                    texto.charAt(
                            posicion
                    )
            )) {

                posicion++;
            }
        }

        private boolean consumir(
                char esperado
        ) {
            if (posicion
                    >= texto.length()
                    || texto.charAt(
                    posicion
            ) != esperado) {

                return false;
            }

            posicion++;
            return true;
        }

        private char actual() {
            if (posicion
                    >= texto.length()) {

                return '\0';
            }

            return texto.charAt(
                    posicion
            );
        }
    }
}