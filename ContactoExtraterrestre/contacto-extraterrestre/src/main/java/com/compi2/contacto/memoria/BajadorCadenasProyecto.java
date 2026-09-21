package com.compi2.contacto.memoria;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class BajadorCadenasProyecto {

    private ProgramaIntermedio destino;
    private int siguienteCadena;
    private int siguienteHeap;

    public ProgramaIntermedio bajar(
            ProgramaIntermedio origen
    ) {
        Objects.requireNonNull(
                origen,
                "El programa intermedio es obligatorio"
        );

        destino =
                new ProgramaIntermedio();

        siguienteCadena = 0;
        siguienteHeap = 0;

        for (Cuarteta cuarteta
                : origen.cuartetas()) {

            procesar(
                    cuarteta
            );
        }

        return destino;
    }

    private void procesar(
            Cuarteta cuarteta
    ) {
        switch (cuarteta.operador()) {

            case ASIGNAR ->
                    agregar(
                            cuarteta.operador(),
                            materializar(
                                    cuarteta.argumento1()
                            ),
                            cuarteta.argumento2(),
                            cuarteta.resultado()
                    );

            case PARAMETRO ->
                    agregar(
                            cuarteta.operador(),
                            materializar(
                                    cuarteta.argumento1()
                            ),
                            cuarteta.argumento2(),
                            cuarteta.resultado()
                    );

            case RETORNAR ->
                    agregar(
                            cuarteta.operador(),
                            materializar(
                                    cuarteta.argumento1()
                            ),
                            cuarteta.argumento2(),
                            cuarteta.resultado()
                    );

            case ESCRIBIR_STACK,
                 ESCRIBIR_HEAP ->
                    agregar(
                            cuarteta.operador(),
                            cuarteta.argumento1(),
                            materializar(
                                    cuarteta.argumento2()
                            ),
                            cuarteta.resultado()
                    );

            case IMPRIMIR ->
                    agregar(
                            cuarteta.operador(),
                            materializar(
                                    cuarteta.argumento1()
                            ),
                            cuarteta.argumento2(),
                            cuarteta.resultado()
                    );

            default ->
                    copiar(
                            cuarteta
                    );
        }
    }

    private String materializar(
            String valor
    ) {
        if (!esLiteralCadena(
                valor
        )) {
            return valor;
        }

        String contenido =
                decodificar(
                        valor
                );

        byte[] bytes =
                contenido.getBytes(
                        StandardCharsets.UTF_8
                );

        String referencia =
                "str_t"
                        + siguienteCadena++;

        agregar(
                OperadorCuarteta.ASIGNAR,
                "H",
                null,
                referencia
        );

        String nuevoHeap =
                "str_h"
                        + siguienteHeap++;

        agregar(
                OperadorCuarteta.SUMAR,
                "H",
                String.valueOf(
                        bytes.length + 1
                ),
                nuevoHeap
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                nuevoHeap,
                null,
                "H"
        );

        for (int indice = 0;
             indice < bytes.length;
             indice++) {

            agregar(
                    OperadorCuarteta.ESCRIBIR_HEAP,
                    referencia
                            + "+"
                            + indice,
                    String.valueOf(
                            Byte.toUnsignedInt(
                                    bytes[indice]
                            )
                    ),
                    null
            );
        }

        agregar(
                OperadorCuarteta.ESCRIBIR_HEAP,
                referencia
                        + "+"
                        + bytes.length,
                "-1",
                null
        );

        return referencia;
    }

    private boolean esLiteralCadena(
            String valor
    ) {
        if (valor == null) {
            return false;
        }

        String texto =
                valor.trim();

        return texto.length() >= 2
                && texto.startsWith("\"")
                && texto.endsWith("\"");
    }

    private String decodificar(
            String literal
    ) {
        String contenido =
                literal.substring(
                        1,
                        literal.length() - 1
                );

        StringBuilder resultado =
                new StringBuilder();

        for (int indice = 0;
             indice < contenido.length();
             indice++) {

            char actual =
                    contenido.charAt(
                            indice
                    );

            if (actual != '\\') {
                resultado.append(
                        actual
                );
                continue;
            }

            if (indice + 1
                    >= contenido.length()) {

                resultado.append(
                        '\\'
                );
                continue;
            }

            char siguiente =
                    contenido.charAt(
                            ++indice
                    );

            if (siguiente == 'u'
                    && indice + 4
                    < contenido.length()) {

                String hexadecimal =
                        contenido.substring(
                                indice + 1,
                                indice + 5
                        );

                try {
                    resultado.append(
                            (char) Integer.parseInt(
                                    hexadecimal,
                                    16
                            )
                    );

                    indice += 4;
                    continue;

                } catch (NumberFormatException ignorada) {
                    resultado.append(
                            'u'
                    );
                    continue;
                }
            }

            switch (siguiente) {

                case 'n' ->
                        resultado.append(
                                '\n'
                        );

                case 'r' ->
                        resultado.append(
                                '\r'
                        );

                case 't' ->
                        resultado.append(
                                '\t'
                        );

                case 'b' ->
                        resultado.append(
                                '\b'
                        );

                case 'f' ->
                        resultado.append(
                                '\f'
                        );

                case '0' ->
                        resultado.append(
                                '\0'
                        );

                case '"' ->
                        resultado.append(
                                '"'
                        );

                case '\'' ->
                        resultado.append(
                                '\''
                        );

                case '\\' ->
                        resultado.append(
                                '\\'
                        );

                default ->
                        resultado.append(
                                siguiente
                        );
            }
        }

        return resultado.toString();
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
}