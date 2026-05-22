package lexer;
import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private void string() {

    StringBuilder builder = new StringBuilder();

    while (!isAtEnd() && peek() != '"') {
        builder.append(advance());
    }

    if (isAtEnd()) {
        throw new RuntimeException("String sin cerrar.");
    }

    advance();

    addToken(TokenType.STRING, builder.toString());
}
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int current = 0;

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            char c = advance();

            switch (c) {
                case '+':
                    addToken(TokenType.PLUS, "+");
                    break;
                case '-':
                    addToken(TokenType.MINUS, "-");
                    break;
                case '*':
                    addToken(TokenType.STAR, "*");
                    break;
                case '/':
                    addToken(TokenType.SLASH, "/");
                    break;
          //      case '=':
          //          addToken(TokenType.EQUAL, "=");
          //          break;
                case ';':
                    addToken(TokenType.SEMICOLON, ";");
                    break;
                case '(':
                    addToken(TokenType.LEFT_PAREN, "(");
                    break;
                case ')':
                    addToken(TokenType.RIGHT_PAREN, ")");
                    break;
                case '=':
                    addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL, 
                            source.substring(current - 1, current));
                    break;

                case '!':
                    if (match('=')) {
                        addToken(TokenType.BANG_EQUAL, "!=");
                    } else {
                        throw new RuntimeException("Caracter no válido: !");
                    }
                    break;

                case '<':
                    addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS,
                            source.substring(current - 1, current));
                    break;

                case '>':
                    addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER,
                            source.substring(current - 1, current));
                    break;

                case '{':
                    addToken(TokenType.LEFT_BRACE, "{");
                    break;

                case '}':
                    addToken(TokenType.RIGHT_BRACE, "}");
                    break;
                case ',':
                    addToken(TokenType.COMMA, ",");
                    break;
                case '.':
                    addToken(TokenType.DOT, ".");
                    break;
                case '[':
                    addToken(TokenType.LEFT_BRACKET, "[");
                    break;

                case ']':
                    addToken(TokenType.RIGHT_BRACKET, "]");
                    break;
                case '"':
                    string();
                    break;
                    
                case ' ':
                case '\t':
                case '\n':
                case '\r':
                    break;

                default:
                    if (isDigit(c)) {
                        number(c);
                    } else if (isLetter(c)) {
                        identifier(c);
                    } else {
                        throw new RuntimeException("Caracter no válido: " + c);
                    }
                    break;
            }
        }

        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }

    private void number(char firstChar) {

        StringBuilder builder = new StringBuilder();
        builder.append(firstChar);

        boolean isFloat = false;

        while (!isAtEnd()) {

            char c = peek();

            if (isDigit(c)) {
                builder.append(advance());
            }
            else if (c == '.' && !isFloat) {
                isFloat = true;
                builder.append(advance());
            }
            else {
                break;
            }
        }

        if (isFloat) {
            addToken(TokenType.FLOAT, builder.toString());
        } else {
            addToken(TokenType.NUMBER, builder.toString());
        }
    }

    private void identifier(char firstChar) {

        StringBuilder builder = new StringBuilder();
        builder.append(firstChar);

        while (!isAtEnd() && isAlphaNumeric(peek())) {
            builder.append(advance());
        }

        String text = builder.toString();

        switch (text) {
            case "let":
                addToken(TokenType.LET, text);
                break;
            case "print":
                addToken(TokenType.PRINT, text);
                break;
            case "true":
            case "false":
                addToken(TokenType.BOOLEAN, text);
                break;
            case "if":
                addToken(TokenType.IF, text);
                break;
            case "else":
                addToken(TokenType.ELSE, text);
                break;
            case "while":
                addToken(TokenType.WHILE, text);
                break;
            case "for":
                addToken(TokenType.FOR, text);
                break;
            case "fn":
                addToken(TokenType.FN, text);
                break;
            case "return":
                addToken(TokenType.RETURN, text);
                break;
            default:
                addToken(TokenType.IDENTIFIER, text);
                break;
        }
    }

    private char advance() {
        return source.charAt(current++);
    }

    private char peek() {
        return source.charAt(current);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void addToken(TokenType type, String lexeme) {
        tokens.add(new Token(type, lexeme));
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isLetter(char c) {
        return 
            (c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
            c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isLetter(c) || isDigit(c);
    }
}