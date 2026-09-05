parser grammar ZetarianoParser;

options {
    tokenVocab = ZetarianoLexer;
}

archivo
    : elemento* EOF
    ;

elemento
    : .
    ;

