*** Settings ***
Library     BuiltIn

*** Variables ***
${myVar1}   Value1
${myVar2}   Value2
${myVar2}   Value3

*** Test Cases ***
Display all variables
    Log     ${myVar1}
    Log     ${myVar2}
    Log     ${myVar3}