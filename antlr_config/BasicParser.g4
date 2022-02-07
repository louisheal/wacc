parser grammar BasicParser;

options {
  tokenVocab=BasicLexer;
}

// EOF indicates that the program must consume to the end of the input.
prog: BEGIN func* stat END EOF ;

func: type IDENT P_OPEN paramList? P_CLOSE IS stat END ;

paramList: param (COMMA param)* ;

param: type IDENT ;

stat: S_SKIP
    | type IDENT ASSIGN assignRHS
    | assignLHS ASSIGN assignRHS
    | READ assignLHS
    | FREE expr
    | RETURN expr
    | EXIT expr
    | PRINT expr
    | PRINTLN expr
    | IF expr THEN stat ELSE stat FI
    | WHILE expr DO stat DONE
    | BEGIN stat END
    | stat SEMI_COLON stat ;

assignLHS: IDENT
          | arrayElem
          | pairElem ;

assignRHS: expr
          | arrayLiter
          | NEW_PAIR P_OPEN expr COMMA expr P_CLOSE
          | pairElem
          | CALL IDENT P_OPEN ;

argList: expr (COMMA expr)* ;

pairElem: FST expr
        | SND expr ;

type: baseType
    | type SB_OPEN SB_CLOSE
    | pairType ;

baseType: INT
        | BOOL
        | CHAR
        | STRING ;

arrayType: type SB_OPEN SB_CLOSE ;

pairType: PAIR P_OPEN pairElemType COMMA pairElemType P_CLOSE ;

pairElemType: baseType
            | arrayType
            | PAIR ;

expr: INT_LITER
    | BOOL_LITER
    | CHAR_LITER
    | STR_LITER
    | pairLiter
    | IDENT
    | arrayElem
    | unaryOper expr
    | expr binaryOper expr
    | P_OPEN expr P_CLOSE
    ;

unaryOper: NOT
         | MINUS
         | LEN
         | ORD
         | CHR ;

binaryOper: PLUS
          | MINUS
          | MULTIPLY
          | DIVIDE
          | MODULO
          | GREATER_THAN
          | GREATER_THAN_OR_EQUAL
          | LESS_THAN
          | LESS_THAN_OR_EQUAL
          | EQUAL
          | NOT_EQUAL
          | AND
          | OR ;

arrayElem: IDENT (SB_OPEN expr SB_CLOSE)+ ;

arrayLiter: SB_OPEN (expr (COMMA expr)*)? SB_CLOSE ;

pairLiter: NULL ;

comment: COMMENT ;
