parser grammar PigLatinParser;

options {
    tokenVocab = PigLatinLexer;
}


archivo
    : elemento* EOF
    ;

elemento
    : .
    ;

