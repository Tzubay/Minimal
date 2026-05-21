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

        throw new RuntimeException("Se esperaba una instrucción.");
    }

    private Stmt letStatement() {
        Token name = consume(TokenType.IDENTIFIER, "Se esperaba el nombre de la variable.");

        consume(TokenType.EQUAL, "Se esperaba '=' después del nombre.");

        Expr value = expression();

        consume(TokenType.SEMICOLON, "Se esperaba ';' al final.");

        return new Stmt.Let(name.lexeme, value);
    }

    private Stmt printStatement() {
        Expr value = expression();

        consume(TokenType.SEMICOLON, "Se esperaba ';' al final.");

        return new Stmt.Print(value);
    }


    private Expr expression() {
        return addition();
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

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous().lexeme);
        }

        throw new RuntimeException("Se esperaba un número, string, booleano o variable.");
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