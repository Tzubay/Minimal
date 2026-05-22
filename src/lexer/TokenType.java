package lexer;

public enum TokenType {
    LET,
    PRINT,
    IF,
    ELSE,
    WHILE,
    FOR,
    FN,
    RETURN,

    IDENTIFIER,
    NUMBER,
    FLOAT,
    STRING,
    BOOLEAN,

    PLUS,
    MINUS,
    STAR,
    SLASH,

    EQUAL,
    EQUAL_EQUAL,
    BANG_EQUAL,
    LESS,
    LESS_EQUAL,
    GREATER,
    GREATER_EQUAL,

    SEMICOLON,
    COMMA,
    DOT,

    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
    LEFT_BRACKET,
    RIGHT_BRACKET,

    EOF
}