package com.compi2.contacto.ir;

import com.compi2.contacto.ast.ProgramaAst;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.compi2.contacto.semantica.EnlacesPig;

public final class GeneradorIntermedioProyecto {

    private static final Pattern TEMPORAL =
            Pattern.compile("\\bt(\\d+)\\b");

    private static final Pattern ETIQUETA =
            Pattern.compile("\\bL(\\d+)\\b");

    public ProgramaIntermedio generar(
            List<ProgramaAst> programasY,
            List<ProgramaAst> programasZ,
            List<ProgramaAst> programasPig
    ) {
        return generar(
                programasY,
                programasZ,
                programasPig,
                new EnlacesPig()
        );
    }

    public ProgramaIntermedio generar(
            List<ProgramaAst> programasY,
            List<ProgramaAst> programasZ,
            List<ProgramaAst> programasPig,
            EnlacesPig enlaces
    ) {
        Objects.requireNonNull(
                programasY,
                "Los programas Y son obligatorios"
        );

        Objects.requireNonNull(
                programasZ,
                "Los programas Z son obligatorios"
        );

        Objects.requireNonNull(
                programasPig,
                "Los programas Pig son obligatorios"
        );

        Objects.requireNonNull(
                enlaces,
                "Los enlaces son obligatorios"
        );

        ProgramaIntermedio y =
                new GeneradorCuartetasY()
                        .generar(
                                programasY
                        );

        ProgramaIntermedio z =
                new GeneradorCuartetasZ()
                        .generar(
                                programasZ
                        );

        ProgramaIntermedio pig =
                new GeneradorCuartetasPig()
                        .generar(
                                programasPig,
                                enlaces
                        );

        ProgramaIntermedio resultado =
                new ProgramaIntermedio();

        copiar(
                y,
                resultado,
                "y"
        );

        copiar(
                z,
                resultado,
                "z"
        );

        copiar(
                pig,
                resultado,
                "pig"
        );

        return resultado;
    }

    private void copiar(
            ProgramaIntermedio origen,
            ProgramaIntermedio destino,
            String prefijo
    ) {
        for (Cuarteta cuarteta
                : origen.cuartetas()) {

            destino.agregar(
                    new Cuarteta(
                            cuarteta.operador(),
                            renombrar(
                                    cuarteta.argumento1(),
                                    prefijo
                            ),
                            renombrar(
                                    cuarteta.argumento2(),
                                    prefijo
                            ),
                            renombrar(
                                    cuarteta.resultado(),
                                    prefijo
                            )
                    )
            );
        }
    }

    private String renombrar(
            String valor,
            String prefijo
    ) {
        if (valor == null) {
            return null;
        }

        String resultado =
                reemplazar(
                        valor,
                        TEMPORAL,
                        prefijo + "_t"
                );

        return reemplazar(
                resultado,
                ETIQUETA,
                prefijo + "_L"
        );
    }

    private String reemplazar(
            String valor,
            Pattern patron,
            String prefijo
    ) {
        Matcher matcher =
                patron.matcher(
                        valor
                );

        StringBuffer resultado =
                new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(
                    resultado,
                    Matcher.quoteReplacement(
                            prefijo
                                    + matcher.group(1)
                    )
            );
        }

        matcher.appendTail(
                resultado
        );

        return resultado.toString();
    }
}