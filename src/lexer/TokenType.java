package lexer;
public enum TokenType {
    // Palabras clave
    LET,
    PRINT,

    // Identificadores y valores
    IDENTIFIER,
    NUMBER,

    // Operadores
    PLUS,
    MINUS,
    STAR,
    SLASH,
    EQUAL,

    // Símbolos
    SEMICOLON,
    LEFT_PAREN,
    RIGHT_PAREN,

    // Final
    EOF
}