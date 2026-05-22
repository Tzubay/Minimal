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

    private Stmt expressionStatement() {
        Expr expr = expression();

        if (match(TokenType.EQUAL)) {
            Expr value = expression();

            consume(TokenType.SEMICOLON, "Se esperaba ';' al final de la asignación.");

            if (expr instanceof Expr.Variable) {
                Expr.Variable variable = (Expr.Variable) expr;
                return new Stmt.Assign(variable.name, value);
            }

            if (expr instanceof Expr.Index) {
                Expr.Index indexExpr = (Expr.Index) expr;
                return new Stmt.IndexAssign(indexExpr.array, indexExpr.index, value);
            }

            throw new RuntimeException("Destino de asignación inválido.");
        }

        consume(TokenType.SEMICOLON, "Se esperaba ';' al final de la expresión.");

        return new Stmt.Expression(expr);
    }

    private Stmt breakStatement() {
        consume(TokenType.SEMICOLON, "Se esperaba ';' después de break.");
        return new Stmt.Break();
    }

    private Stmt switchStatement() {
        consume(TokenType.LEFT_PAREN, "Se esperaba '(' después de switch.");

        Expr condition = expression();

        consume(TokenType.RIGHT_PAREN, "Se esperaba ')' después de la expresión del switch.");
        consume(TokenType.LEFT_BRACE, "Se esperaba '{' después del switch.");

        List<Stmt.SwitchCase> cases = new ArrayList<>();
        List<Stmt> defaultStatements = null;

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {

            if (match(TokenType.CASE)) {
                Expr caseValue = expression();

                consume(TokenType.COLON, "Se esperaba ':' después del case.");

                List<Stmt> statements = new ArrayList<>();

                while (
                    !check(TokenType.CASE) &&
                    !check(TokenType.DEFAULT) &&
                    !check(TokenType.RIGHT_BRACE) &&
                    !isAtEnd()
                ) {
                    statements.add(statement());
                }

                cases.add(new Stmt.SwitchCase(caseValue, statements));
            }
            else if (match(TokenType.DEFAULT)) {
                consume(TokenType.COLON, "Se esperaba ':' después de default.");

                defaultStatements = new ArrayList<>();

                while (
                    !check(TokenType.CASE) &&
                    !check(TokenType.RIGHT_BRACE) &&
                    !isAtEnd()
                ) {
                    defaultStatements.add(statement());
                }
            }
            else {
                throw new RuntimeException("Se esperaba 'case', 'default' o '}' dentro del switch.");
            }
        }

        consume(TokenType.RIGHT_BRACE, "Se esperaba '}' al final del switch.");

        return new Stmt.Switch(condition, cases, defaultStatements);
    }
private Stmt functionStatement() {
    Token name = consume(TokenType.IDENTIFIER, "Se esperaba el nombre de la función.");

    consume(TokenType.LEFT_PAREN, "Se esperaba '(' después del nombre de la función.");

    List<String> params = new ArrayList<>();

    if (!check(TokenType.RIGHT_PAREN)) {
        do {
            Token param = consume(TokenType.IDENTIFIER, "Se esperaba nombre del parámetro.");
            params.add(param.lexeme);
        } while (match(TokenType.COMMA));
    }

    consume(TokenType.RIGHT_PAREN, "Se esperaba ')' después de los parámetros.");
    consume(TokenType.LEFT_BRACE, "Se esperaba '{' antes del cuerpo de la función.");

    List<Stmt> body = block();

    return new Stmt.Function(name.lexeme, params, body);
}

private Stmt returnStatement() {
    List<Expr> values = new ArrayList<>();

    values.add(expression());

    while (match(TokenType.COMMA)) {
        values.add(expression());
    }

    consume(TokenType.SEMICOLON, "Se esperaba ';' después del return.");

    return new Stmt.Return(values);
}

    private Stmt statement() {

        if (match(TokenType.SWITCH)) {
            return switchStatement();
        }

        if (match(TokenType.BREAK)) {
            return breakStatement();
        }
        if (match(TokenType.FN)) {
            return functionStatement();
        }

        if (match(TokenType.RETURN)) {
            return returnStatement();
        }
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

    /*    if (check(TokenType.IDENTIFIER)) {
            return assignmentStatement();
        }*/
        return expressionStatement();

      /*   throw new RuntimeException("Se esperaba una instrucción.");*/
    }

    private Stmt letStatement() {
        Token name = consume(TokenType.IDENTIFIER, "Se esperaba el nombre de la variable.");

        Expr value = null;

        if (match(TokenType.EQUAL)) {
            value = expression();
        }

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
        List<Expr> expressions = new ArrayList<>();

        expressions.add(expression());

        while (match(TokenType.COMMA)) {
            expressions.add(expression());
        }

        consume(TokenType.SEMICOLON, "Se esperaba ';' al final.");

        return new Stmt.Print(expressions);
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

        if (check(TokenType.IDENTIFIER)) {
            Expr target = postfix();

            consume(TokenType.EQUAL, "Se esperaba '=' en incremento.");

            Expr value = expression();

            if (target instanceof Expr.Variable) {
                Expr.Variable variable = (Expr.Variable) target;
                increment = new Stmt.Assign(variable.name, value);
            } else if (target instanceof Expr.Index) {
                Expr.Index indexExpr = (Expr.Index) target;
                increment = new Stmt.IndexAssign(indexExpr.array, indexExpr.index, value);
            } else {
                throw new RuntimeException("Incremento inválido en el for.");
            }
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
        Expr expr = postfix();

        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token operator = previous();
            Expr right = postfix();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
private Expr postfix() {
    Expr expr = primary();

    while (true) {
        if (match(TokenType.LEFT_BRACKET)) {
            Expr index = expression();
            consume(TokenType.RIGHT_BRACKET, "Se esperaba ']' después del índice.");

            expr = new Expr.Index(expr, index);
        } 
        else if (match(TokenType.LEFT_PAREN)) {
            List<Expr> arguments = new ArrayList<>();

            if (!check(TokenType.RIGHT_PAREN)) {
                do {
                    arguments.add(expression());
                } while (match(TokenType.COMMA));
            }

            consume(TokenType.RIGHT_PAREN, "Se esperaba ')' después de los argumentos.");

            expr = new Expr.Call(expr, arguments);
        }
        else if (match(TokenType.DOT)) {
            Token methodName = consume(TokenType.IDENTIFIER, "Se esperaba nombre del método después de '.'.");

            consume(TokenType.LEFT_PAREN, "Se esperaba '(' después del nombre del método.");

            List<Expr> arguments = new ArrayList<>();

            if (!check(TokenType.RIGHT_PAREN)) {
                do {
                    arguments.add(expression());
                } while (match(TokenType.COMMA));
            }

            consume(TokenType.RIGHT_PAREN, "Se esperaba ')' después de los argumentos del método.");

            expr = new Expr.MethodCall(expr, methodName.lexeme, arguments);
        }
        else {
            break;
        }
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