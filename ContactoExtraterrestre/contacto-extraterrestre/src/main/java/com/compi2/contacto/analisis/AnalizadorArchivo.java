package com.compi2.contacto.analisis;

import com.compi2.contacto.antlr.piglatin.PigLatinLexer;
import com.compi2.contacto.antlr.piglatin.PigLatinParser;
import com.compi2.contacto.antlr.y.YLexer;
import com.compi2.contacto.antlr.y.YParser;
import com.compi2.contacto.antlr.zetariano.ZetarianoLexer;
import com.compi2.contacto.antlr.zetariano.ZetarianoParser;
import com.compi2.contacto.errores.Diagnostico;
import com.compi2.contacto.errores.Severidad;
import com.compi2.contacto.errores.TipoDiagnostico;
import com.compi2.contacto.proyecto.ArchivoFuente;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AnalizadorArchivo {

    private static final Pattern CLASE_PUBLICA = Pattern.compile(
            "\\bpublic\\s+class\\s+([A-Za-z_][A-Za-z0-9_]*)"
    );

    public ResultadoAnalisisArchivo analizar(ArchivoFuente archivo) {
        Objects.requireNonNull(archivo, "El archivo es obligatorio");

        List<Diagnostico> diagnosticos = new ArrayList<>();

        switch (archivo.lenguaje()) {
            case Y -> analizarY(archivo, diagnosticos);
            case ZETARIANO -> analizarZetariano(archivo, diagnosticos);
            case PIG_LATIN -> analizarPigLatin(archivo, diagnosticos);
        }

        if (archivo.contenido().isBlank()) {
            diagnosticos.add(new Diagnostico(
                    TipoDiagnostico.PROYECTO,
                    Severidad.ADVERTENCIA,
                    archivo.ruta(),
                    1,
                    0,
                    "El archivo esta vacio"
            ));
        }

        return new ResultadoAnalisisArchivo(archivo, diagnosticos);
    }

    private void analizarY(
            ArchivoFuente archivo,
            List<Diagnostico> diagnosticos
    ) {
        YLexer lexer = new YLexer(CharStreams.fromString(archivo.contenido()));
        configurarLexer(archivo, lexer, diagnosticos);
        TokenizadorIndentacionY indentacion = new TokenizadorIndentacionY(lexer);
        registrarErroresIndentacion(archivo, indentacion, diagnosticos);
        YParser parser = new YParser(new CommonTokenStream(indentacion));
        configurarParser(archivo, parser, diagnosticos);
        parser.archivo();
    }

    private void registrarErroresIndentacion(
            ArchivoFuente archivo,
            TokenizadorIndentacionY indentacion,
            List<Diagnostico> diagnosticos
    ) {
        indentacion.erroresIndentacion().forEach(token ->
                diagnosticos.add(new Diagnostico(
                        TipoDiagnostico.SINTACTICO,
                        Severidad.ERROR,
                        archivo.ruta(),
                        Math.max(token.getLine(), 1),
                        Math.max(token.getCharPositionInLine(), 0),
                        token.getText()
                ))
        );
    }

    private void analizarZetariano(
            ArchivoFuente archivo,
            List<Diagnostico> diagnosticos
    ) {
        ZetarianoLexer lexer = new ZetarianoLexer(
                CharStreams.fromString(archivo.contenido())
        );
        ZetarianoParser parser = new ZetarianoParser(
                new CommonTokenStream(lexer)
        );
        configurar(archivo, lexer, parser, diagnosticos);
        parser.archivo();
        validarNombreClase(archivo, diagnosticos);
    }

    private void analizarPigLatin(
            ArchivoFuente archivo,
            List<Diagnostico> diagnosticos
    ) {
        PigLatinLexer lexer = new PigLatinLexer(
                CharStreams.fromString(archivo.contenido())
        );
        PigLatinParser parser = new PigLatinParser(
                new CommonTokenStream(lexer)
        );
        configurar(archivo, lexer, parser, diagnosticos);
        parser.archivo();
    }

    private void configurar(
            ArchivoFuente archivo,
            Lexer lexer,
            Parser parser,
            List<Diagnostico> diagnosticos
    ) {
        configurarLexer(archivo, lexer, diagnosticos);
        configurarParser(archivo, parser, diagnosticos);
    }

    private void configurarLexer(
            ArchivoFuente archivo,
            Lexer lexer,
            List<Diagnostico> diagnosticos
    ) {
        lexer.removeErrorListeners();
        lexer.addErrorListener(new RecolectorErroresAntlr(
                archivo.ruta(),
                TipoDiagnostico.LEXICO,
                diagnosticos
        ));
    }

    private void configurarParser(
            ArchivoFuente archivo,
            Parser parser,
            List<Diagnostico> diagnosticos
    ) {
        parser.removeErrorListeners();
        parser.addErrorListener(new RecolectorErroresAntlr(
                archivo.ruta(),
                TipoDiagnostico.SINTACTICO,
                diagnosticos
        ));
    }

    private void validarNombreClase(
            ArchivoFuente archivo,
            List<Diagnostico> diagnosticos
    ) {
        Matcher matcher = CLASE_PUBLICA.matcher(archivo.contenido());
        if (!matcher.find()) {
            return;
        }

        String nombreClase = matcher.group(1);
        String nombreArchivo = archivo.nombre();
        String esperado = nombreClase + ".z";

        if (!nombreArchivo.equals(esperado)) {
            diagnosticos.add(new Diagnostico(
                    TipoDiagnostico.SEMANTICO,
                    Severidad.ERROR,
                    archivo.ruta(),
                    1,
                    0,
                    "El archivo debe llamarse " + esperado
            ));
        }
    }
}
