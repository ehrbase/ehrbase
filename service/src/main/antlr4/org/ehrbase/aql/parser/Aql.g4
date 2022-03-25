//Modified version from:
// Author: Bostjan Lah
// (c) Copyright, Marand, http://www.marand.com
// Licensed under LGPL: http://www.gnu.org/copyleft/lesser.html
// Based on AQL grammar by Ocean Informatics: http://www.openehr.org/wiki/download/attachments/2949295/EQL_v0.6.grm?version=1&modificationDate=1259650833000
//
// Author: Christian Chevalley - July 2016:
// - modified to support ANTLR4
// - removed dependencies on specific packages
// - clean-up lexer conflicts and ambiguities
// - simplified grammar
//-------------------------------------------
grammar Aql;

//@header {
//package com.ethercis.aql.parser;
//}

query   :   queryExpr ;

queryExpr : select from (where)? (orderBy (limit offset?)? | (limit offset?) orderBy?)? EOF ;

select
        : SELECT selectExpr
        | SELECT topExpr selectExpr ;

topExpr
        : TOP INTEGER (BACKWARD|FORWARD)?;
//        | TOP INTEGER BACKWARD
//        | TOP INTEGER FORWARD ;

function
        : FUNCTION_IDENTIFIER OPEN_PAR (IDENTIFIER|identifiedPath|operand|) (COMMA (IDENTIFIER|identifiedPath|operand))* CLOSE_PAR;

castFunction :
        CAST_FUNCTION_IDENTIFIER OPEN_PAR (IDENTIFIER|identifiedPath|operand) AS STRING CLOSE_PAR;

extension
        : EXTENSION_IDENTIFIER OPEN_PAR STRING COMMA STRING CLOSE_PAR;

where
        : WHERE identifiedExpr ;

orderBy
        : ORDERBY orderBySeq ;

limit
        : LIMIT INTEGER;

offset
        : OFFSET INTEGER;

orderBySeq
        : orderByExpr (COMMA orderBySeq)?;
//      | orderByExpr COMMA orderBySeq ;

orderByExpr : identifiedPath (DESCENDING|ASCENDING|DESC|ASC)?;
//      | identifiedPath DESCENDING
//      | identifiedPath ASCENDING
//      | identifiedPath DESC
//      | identifiedPath ASC ;

//selectExpr
//        : variableSeq;

selectExpr
        : (DISTINCT)? identifiedPath (AS IDENTIFIER)? (COMMA selectExpr)?
        | (DISTINCT)? stdExpression (AS IDENTIFIER)? (COMMA selectExpr)?
        ;

stdExpression
        : function
        | castFunction
        | extension
        | INTEGER
        | STRING
        | FLOAT
        | REAL
        | DATE
        | PARAMETER
        | BOOLEAN
        | TRUE
        | FALSE
        | NULL
        | UNKNOWN
        ;

//variableSeq_
//        : identifiedPath (AS IDENTIFIER)? (COMMA variableSeq)?
//        | function (AS IDENTIFIER)? (COMMA variableSeq)?
//        ;

from
        : FROM fromExpr     // stop or/and without root class
        | FROM fromEHR (CONTAINS containsExpression)? (COMMA fromForeignData)?
        | FROM fromForeignData (COMMA fromForeignData)?;
//        | FROM ContainsOr;

fromEHR
        : EHR standardPredicate
        | EHR IDENTIFIER standardPredicate
        | EHR IDENTIFIER ;

//foreign data
fromForeignData
        : (AGENT | GROUP | ORGANISATION | PERSON) IDENTIFIER joinPredicate
        | (AGENT | GROUP | ORGANISATION | PERSON) IDENTIFIER;

//====== CONTAINMENT
fromExpr
        : containsExpression;


containsExpression
        : containExpressionBool ((AND|OR|XOR) containsExpression)?;

containExpressionBool
        : contains
        | OPEN_PAR containsExpression CLOSE_PAR;

contains
        : simpleClassExpr (CONTAINS containsExpression)?;
//======= END CONTAINMENT

identifiedExpr
        : identifiedEquality ((OR|XOR|AND) identifiedEquality)*
        | OPEN_PAR identifiedEquality ((OR|XOR|AND)identifiedEquality)* CLOSE_PAR
        ;

//identifiedExprAnd
//      : identifiedEquality (AND identifiedEquality)*;
//      : identifiedEquality (AND identifiedExpr)*;

identifiedEquality
        : OPEN_PAR* NOT? identifiedOperand COMPARABLEOPERATOR identifiedOperand CLOSE_PAR*
        | OPEN_PAR* NOT? identifiedOperand MATCHES OPEN_CURLY matchesOperand CLOSE_CURLY CLOSE_PAR*
        | OPEN_PAR* NOT? identifiedOperand MATCHES REGEXPATTERN CLOSE_PAR*
        | OPEN_PAR* NOT? identifiedOperand LIKE STRING CLOSE_PAR*
        | OPEN_PAR* NOT? identifiedOperand ILIKE STRING CLOSE_PAR*
        | OPEN_PAR* NOT? identifiedOperand SIMILARTO STRING CLOSE_PAR*
        | OPEN_PAR* identifiedOperand NOT? IN OPEN_PAR  (identifiedOperand|matchesOperand) CLOSE_PAR CLOSE_PAR*
        | OPEN_PAR* NOT? identifiedOperand (COMPARABLEOPERATOR|LIKE|ILIKE) (ANY|ALL|SOME) OPEN_PAR (identifiedOperand|matchesOperand) CLOSE_PAR CLOSE_PAR*
        | OPEN_PAR* identifiedOperand NOT? BETWEEN identifiedOperand AND identifiedOperand CLOSE_PAR*
        | OPEN_PAR* identifiedOperand IS NOT? NULL CLOSE_PAR*
        | OPEN_PAR* identifiedOperand IS NOT? UNKNOWN CLOSE_PAR*
        | OPEN_PAR* identifiedOperand IS NOT? (TRUE|FALSE) CLOSE_PAR*
        | OPEN_PAR* identifiedOperand IS NOT? DISTINCT FROM identifiedOperand CLOSE_PAR*
        | OPEN_PAR* NOT? OPEN_PAR identifiedExpr CLOSE_PAR CLOSE_PAR*
        | OPEN_PAR* NOT? EXISTS identifiedPath CLOSE_PAR*
        | OPEN_PAR* NOT? EXISTS identifiedExpr CLOSE_PAR*;

identifiedOperand
        : operand
        | identifiedPath
        | stdExpression;

identifiedPath
        : IDENTIFIER (SLASH objectPath)?
        | IDENTIFIER predicate (SLASH objectPath)?;
//        | IDENTIFIER SLASH objectPath
//        | IDENTIFIER predicate SLASH objectPath ;


predicate : OPEN_BRACKET nodePredicateOr CLOSE_BRACKET;

//nodePredicate_
//        : OPEN_BRACKET nodePredicateOr CLOSE_BRACKET;

nodePredicateOr
        : nodePredicateAnd (OR nodePredicateAnd)*;
//        | nodePredicateOr (OR nodePredicateAnd)* ;

nodePredicateAnd
        : nodePredicateComparable (AND nodePredicateComparable)*;
//        | nodePredicateAnd AND nodePredicateComparable ;

nodePredicateComparable
    : NODEID (COMMA (STRING|PARAMETER))?
    | ARCHETYPEID (COMMA (STRING|PARAMETER))?
    | predicateOperand ((COMPARABLEOPERATOR predicateOperand)|(MATCHES REGEXPATTERN))
    | REGEXPATTERN     //! /items[{/at0001.*/}], /items[at0001 and name/value matches {//}]
    | PARAMETER (COMMA (STRING|PARAMETER))?
    ;

nodePredicateRegEx
        : REGEXPATTERN
        | predicateOperand MATCHES REGEXPATTERN ;


matchesOperand
        : valueListItems
        | identifiedPath
        | URIVALUE ;

valueListItems
        : operand (COMMA valueListItems)?;
//        | operand COMMA valueListItems ;

versionpredicate
        : OPEN_BRACKET versionpredicateOptions CLOSE_BRACKET;

versionpredicateOptions
        : LATEST_VERSION
        | ALL_VERSIONS;

standardPredicate
        : OPEN_BRACKET predicateExpr CLOSE_BRACKET;

joinPredicate
        : OPEN_BRACKET JOINON predicateEquality CLOSE_BRACKET;

predicateExpr
        : predicateAnd (OR predicateAnd)*;

//predicateOr_
//        : predicateAnd (OR predicateAnd)*;

predicateAnd
        : predicateEquality (AND predicateEquality)*;

predicateEquality
        : predicateOperand COMPARABLEOPERATOR predicateOperand;

predicateOperand
        : objectPath | operand;



operand
                : STRING
                | INTEGER
                | FLOAT
                | REAL
                | DATE
                | PARAMETER
                | BOOLEAN
                | TRUE
                | FALSE
                | NULL
                | UNKNOWN
        	    | invokeOperand
        	    | function
        	    | castFunction;


invokeOperand
        	: invokeExpr;

invokeExpr
                : TERMINOLOGY OPEN_PAR STRING COMMA STRING COMMA STRING CLOSE_PAR ;

objectPath
        : pathPart (SLASH pathPart)*;


pathPart
        : IDENTIFIER predicate?;

classExpr
        : OPEN_PAR simpleClassExpr CLOSE_PAR
        | simpleClassExpr
        ;

simpleClassExpr
        : IDENTIFIER IDENTIFIER?                    //! RM_TYPE_NAME .. RM_TYPE_NAME identifier
        | archetypedClassExpr
        | versionedClassExpr
        | versionClassExpr;

archetypedClassExpr
        : IDENTIFIER (IDENTIFIER)? (OPEN_BRACKET ARCHETYPEID CLOSE_BRACKET)?;   //! RM_TYPE_NAME identifier? [archetype_id]

versionedClassExpr
        : VERSIONED_OBJECT (IDENTIFIER)? (standardPredicate)?;

versionClassExpr
        : VERSION (IDENTIFIER)? (standardPredicate|versionpredicate)?;

//
// LEXER PATTERNS
//

EHR : E H R;
AND :  A N D ;
OR : O R ;
XOR : X O R ;
NOT : N O T ;
IN : I N ;
MATCHES : M A T C H E S ;
TERMINOLOGY : T E R M I N O L O G Y ;
LIKE : L I K E ;
ILIKE : I L I K E ;
SIMILARTO: S I M I L A R ' ' T O;
SELECT : S E L E C T ;
TOP : T O P ;
FORWARD : F O R W A R D ;
BACKWARD : B A C K W A R D ;
AS : A S ;
CONTAINS : C O N T A I N S ;
WHERE : W H E R E ;
ORDERBY : O R D E R ' ' B Y ;
OFFSET: O F F S E T ;
LIMIT: L I M I T ;
FROM : F R O M ;
DESCENDING : D E S C E N D I N G ;
ASCENDING : A S C E N D I N G ;
DESC : D E S C ;
ASC : A S C ;
EXISTS: E X I S T S ;
VERSION :   V E R S I O N ;
VERSIONED_OBJECT    :   V E R S I O N E D '_' O B J E C T;
ALL_VERSIONS :  A L L '_' V E R S I O N S;
LATEST_VERSION : L A T E S T '_' V E R S I O N ;
DISTINCT : D I S T I N C T ;
JOINON: J O I N ' ' O N;
ANY: A N Y;
ALL: A L L;
SOME: S O M E;
BETWEEN: B E T W E E N;
IS: I S;
NULL: N U L L;
UNKNOWN: U N K N O W N;
TRUE: T R U E;
FALSE: F A L S E;


//demographic binding
PERSON: P E R S O N ;
AGENT: A G E N T ;
ORGANISATION: O R G A N I S A T I O N ;
GROUP: G R O U P ;

FUNCTION_IDENTIFIER : COUNT | AVG | BOOL_AND | BOOL_OR | EVERY | MAX | MIN | SUM |
//statistics
                      CORR | COVAR_POP | COVAR_SAMP | REGR_AVGX | REGR_AVGY | REGR_COUNT | REGR_INTERCEPT | REGR_R2 | REGR_SLOPE | REGR_SXX |
                      REGR_SXY | REGR_SYY | STDDEV | STDDEV_POP | STDDEV_SAMP | VARIANCE | VAR_POP | VAR_SAMP |
//string functions
                      SUBSTR | STRPOS | SPLIT_PART | BTRIM | CONCAT | CONCAT_WS | DECODE | ENCODE | FORMAT | INITCAP | LEFT | LENGTH | LPAD | LTRIM |
                       REGEXP_MATCH | REGEXP_REPLACE | REGEXP_SPLIT_TO_ARRAY | REGEXP_SPLIT_TO_TABLE | REPEAT | REPLACE | REVERSE | RIGHT | RPAD |
                       RTRIM | TRANSLATE |
//encoding functions
                       RAW_COMPOSITION_ENCODE |
//basic date functions
                       NOW | AGE | CURRENT_TIME | CURRENT_DATE

                       ;
CAST_FUNCTION_IDENTIFIER: C A S T;


EXTENSION_IDENTIFIER: '_' E X T;

// Terminal Definitions
BOOLEAN :   (T R U E)|(F A L S E) ;
NODEID  :   'at' DIGIT+ ('.' DIGIT+)*;
ARCHETYPEID :   LETTER+ '-' LETTER+ '-' (LETTER|'_')+ '.' (IDCHAR|'-')+ '.v' DIGIT+ ('.' DIGIT+)?;

IDENTIFIER
    :   A (ALPHANUM|'_')*
    |   LETTERMINUSA IDCHAR*
    ;

DEMOGRAPHIC
        : (AGENT | GROUP | ORGANISATION | PERSON) ;

INTEGER :   '-'? DIGIT+;
FLOAT   :   '-'? DIGIT+ '.' DIGIT+;
REAL    :   '-'? DIGIT+ ('.' DIGIT+)? (E (|'+'|'-') DIGIT+)?;
DATE    :   '\'' DIGIT DIGIT DIGIT DIGIT DIGIT DIGIT DIGIT DIGIT 'T' DIGIT DIGIT DIGIT DIGIT DIGIT DIGIT '.' DIGIT DIGIT DIGIT '+' DIGIT DIGIT DIGIT DIGIT '\'';
PARAMETER : '$' LETTER IDCHAR*;

UNIQUEID:   DIGIT+ ('.' DIGIT+)+ '.' DIGIT+  // OID
            | HEXCHAR+ ('-' HEXCHAR+)+       // UUID
    ;

COMPARABLEOPERATOR
    :   '=' | '!=' | '>' | '>=' | '<' | '<='
    ;

URIVALUE: LETTER+ '://' (URISTRING|OPEN_BRACKET|CLOSE_BRACKET|', \''|'\'')*;

REGEXPATTERN : '{/' REGEXCHAR+ '/}';

STRING
        :  '\'' ( ESC_SEQ | ~('\\'|'\'') )* '\''
        |  '"' ( ESC_SEQ | ~('\\'|'"') )* '"'
        ;

EXP_STRING
    : ([uUbB]? [rR]? | [rR]? [uUbB]?)
    ( '\''     ('\\' (([ \t]+ ('\r'? '\n')?)|.) | ~[\\\r\n'])*  '\''
    | '"'      ('\\' (([ \t]+ ('\r'? '\n')?)|.) | ~[\\\r\n"])*  '"'
    | '"""'    ('\\' .                          | ~'\\'     )*? '"""'
    | '\'\'\'' ('\\' .                          | ~'\\'     )*? '\'\'\''
    )
;

SLASH   :   '/';
COMMA   :   ',';
SEMICOLON : ';';
OPEN_BRACKET :  '[';
CLOSE_BRACKET : ']';
OPEN_PAR    :   '(';
CLOSE_PAR   :   ')';
OPEN_CURLY :    '{';
CLOSE_CURLY :   '}';

ARITHMETIC_BINOP:
      '/'
    | '*'
    | '+'
    | '-'
    ;

COUNT: C O U N T;
AVG: A V G;
BOOL_AND: B O O L '_' A N D;
BOOL_OR: B O O L '_' O R;
EVERY: E V E R Y;
MAX: M A X;
MIN: M I N;
SUM: S U M;
SUBSTR: S U B S T R;
STRPOS: S T R P O S;
SPLIT_PART: S P L I T '_' P A R T;
BTRIM : B T R I M;
CONCAT: C O N C A T;
CONCAT_WS : C O N C A T '_' W S;
DECODE : D E C O D E;
ENCODE : E N C O D E;
FORMAT : F O R M A T;
INITCAP : I N I T C A P;
LEFT : L E F T;
LENGTH : L E N G T H;
LPAD : L P A D;
LTRIM : L T R I M;
REGEXP_MATCH : R E G E X P '_' M A T C H;
REGEXP_REPLACE : R E G E X P '_' R E P L A C E;
REGEXP_SPLIT_TO_ARRAY : R E G E X P '_' S P L I T '_' T O '_' A R R A Y;
REGEXP_SPLIT_TO_TABLE : R E G E X P '_' S P L I T '_' T O '_' T A B L E;
REPEAT : R E P E A T;
REPLACE : R E P L A C E;
REVERSE : R E V E R S E;
RIGHT : R I G H T;
RPAD : R P A D;
RTRIM : R T R I M;
TRANSLATE : T R A N S L A T E;
CORR : C O R R;
COVAR_POP : C O V A R '_' P O P;
COVAR_SAMP : C O V A R '_' S A M P;
REGR_AVGX : R E G R '_' A V G X;
REGR_AVGY  : R E G R '_' A V G Y;
REGR_COUNT  : R E G R '_' C O U N T;
REGR_INTERCEPT : R E G R '_' I N T E R C E P T;
REGR_R2  : R E G R '_' R '2';
REGR_SLOPE  : R E G R '_' S L O P E;
REGR_SXX  : R E G R '_' S X X;
REGR_SXY  : R E G R '_' S X Y;
REGR_SYY  : R E G R '_' S Y Y;
STDDEV : S T D D E V;
STDDEV_POP : S T D D E V '_' P O P;
STDDEV_SAMP : S T D D E V '_' S A M P;
VARIANCE : V A R I A N C E;
VAR_POP : V A R '_' P O P;
VAR_SAMP : V A R '_' S A M P;
RAW_COMPOSITION_ENCODE : '_' '_' R A W '_' C O M P O S I T I O N '_' E N C O D E;
CAST : C A S T;
NOW : N O W;
AGE : A G E;
CURRENT_TIME : C U R R E N T '_' T I M E;
CURRENT_DATE : C U R R E N T '_' D A T E;

fragment
ESC_SEQ
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\\"'|'\''|'\\')
    |   UNICODE_ESC
    |   OCTAL_ESC
    ;

fragment
OCTAL_ESC
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;

fragment
UNICODE_ESC
    :   '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;

fragment
HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

fragment
QUOTE   :   '\'';

fragment
DIGIT   :   '0'..'9';

fragment
HEXCHAR :    DIGIT|'a'|'A'|'b'|'B'|'c'|'C'|'d'|'D'|'e'|'E'|'f'|'F';

fragment
LETTER
    :   'a'..'z'|'A'..'Z';

fragment
ALPHANUM
    :   LETTER|DIGIT;

fragment
LETTERMINUSA
    :   'b'..'z'|'B'..'Z';

fragment
LETTERMINUST
    :   'a'..'s'|'A'..'S'|'u'..'z'|'U'..'Z';

fragment
IDCHAR  :   ALPHANUM|'_';

fragment
IDCHARMINUST
    :   LETTERMINUST|DIGIT|'_';

fragment
URISTRING
    :   ALPHANUM|'_'|'-'|'/'|':'|'.'|'?'|'&'|'%'|'$'|'#'|'@'|'!'|'+'|'='|'*';

fragment
REGEXCHAR
    :   URISTRING|'('|')'|'\\'|'^'|'{'|'}'|']'|'[';

fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];


WS  :   ( ' '
        | '\t'
        | '\r'
        | '\n'
        ) -> skip
    ;