package parser;

import ast.*;
import lexer.*;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();

        while (!isAtEnd()) {
            statements.add(statement());
        }

        return statements;
    }

    private Stmt statement() {
        if (match(TokenType.LET)) {
            return letStatement();
        }

        if (match(TokenType.PRINT)) {
            return printStatement();
        }

        if (match(TokenType.IF)) {
            return ifStatement();
        }

        if (match(TokenType.WHILE)) {
            return whileStatement();
        }

        if (match(TokenType.FOR)) {
            return forStatement();
        }

        if (match(TokenType.LEFT_BRACE)) {
            return new Stmt.Block(block());
        }

        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.EQUAL)) {
            return assignStatement();
        }

        throw new RuntimeException("Se esperaba una instrucción.");
    }

    private Stmt letStatement() {
        Token name = consume(TokenType.IDENTIFIER, "Se esperaba el nombre de la variable.");

        consume(TokenType.EQUAL, "Se esperaba '=' después del nombre.");

        Expr value = expression();

        consume(TokenType.SEMICOLON, "Se esperaba ';' al final.");

        return new Stmt.Let(name.lexeme, value);
    }

    private Stmt assignStatement() {
        Token name = consume(TokenType.IDENTIFIER, "Se esperaba el nombre de la variable.");

        consume(TokenType.EQUAL, "Se esperaba '=' en la asignación.");

        Expr value = expression();

        consume(TokenType.SEMICOLON, "Se esperaba ';' al final de la asignación.");

        return new Stmt.Assign(name.lexeme, value);
    }

    private Stmt printStatement() {
        Expr value = expression();

        consume(TokenType.SEMICOLON, "Se esperaba ';' al final.");

        return new Stmt.Print(value);
    }

    private Stmt ifStatement() {
        Expr condition = expression();

        consume(TokenType.LEFT_BRACE, "Se esperaba '{' después de la condición del if.");

        Stmt thenBranch = new Stmt.Block(block());

        Stmt elseBranch = null;

        if (match(TokenType.ELSE)) {
            consume(TokenType.LEFT_BRACE, "Se esperaba '{' después de else.");
            elseBranch = new Stmt.Block(block());
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStatement() {
        Expr condition = expression();

        consume(TokenType.LEFT_BRACE, "Se esperaba '{' después de la condición del while.");

        Stmt body = new Stmt.Block(block());

        return new Stmt.While(condition, body);
    }

    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Se esperaba '(' después de for.");

        Stmt initializer;

        if (match(TokenType.LET)) {
            initializer = letStatement();
        } else if (check(TokenType.IDENTIFIER) && checkNext(TokenType.EQUAL)) {
            initializer = assignStatement();
        } else {
            throw new RuntimeException("Se esperaba inicializador en el for.");
        }

        Expr condition = expression();

        consume(TokenType.SEMICOLON, "Se esperaba ';' después de la condición del for.");

        Stmt increment;

        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.EQUAL)) {
            Token name = consume(TokenType.IDENTIFIER, "Se esperaba variable en incremento.");
            consume(TokenType.EQUAL, "Se esperaba '=' en incremento.");
            Expr value = expression();
            increment = new Stmt.Assign(name.lexeme, value);
        } else {
            throw new RuntimeException("Se esperaba incremento en el for.");
        }

        consume(TokenType.RIGHT_PAREN, "Se esperaba ')' después del for.");

        consume(TokenType.LEFT_BRACE, "Se esperaba '{' después del for.");

        List<Stmt> bodyStatements = block();

        bodyStatements.add(increment);

        Stmt body = new Stmt.Block(bodyStatements);

        return new Stmt.Block(List.of(initializer, new Stmt.While(condition, body)));
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(statement());
        }

        consume(TokenType.RIGHT_BRACE, "Se esperaba '}' al final del bloque.");

        return statements;
    }

    private Expr expression() {
        return equality();
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = addition();

        while (match(
                TokenType.LESS,
                TokenType.LESS_EQUAL,
                TokenType.GREATER,
                TokenType.GREATER_EQUAL
        )) {
            Token operator = previous();
            Expr right = addition();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr addition() {
        Expr expr = multiplication();

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = multiplication();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr multiplication() {
        Expr expr = primary();

        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token operator = previous();
            Expr right = primary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr primary() {
        if (match(TokenType.NUMBER)) {
            return new Expr.Number(Integer.parseInt(previous().lexeme));
        }

        if (match(TokenType.FLOAT)) {
            return new Expr.FloatExpr(Double.parseDouble(previous().lexeme));
        }

        if (match(TokenType.STRING)) {
            return new Expr.StringExpr(previous().lexeme);
        }

        if (match(TokenType.BOOLEAN)) {
            return new Expr.BooleanExpr(Boolean.parseBoolean(previous().lexeme));
        }

        if (match(TokenType.LEFT_BRACKET)) {
            return arrayLiteral();
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous().lexeme);
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Se esperaba ')' después de la expresión.");
            return expr;
        }

        throw new RuntimeException("Se esperaba un número, string, booleano, arreglo, variable o expresión entre paréntesis.");
    }
    
    private Expr arrayLiteral() {
        List<Expr> elements = new ArrayList<>();

        if (!check(TokenType.RIGHT_BRACKET)) {
            do {
                elements.add(expression());
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RIGHT_BRACKET, "Se esperaba ']' al final del arreglo.");

        return new Expr.ArrayExpr(elements);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }

        throw new RuntimeException(message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}