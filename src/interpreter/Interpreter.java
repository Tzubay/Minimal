package interpreter;

import ast.*;
import lexer.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter {
    private final Map<String, Value> variables = new HashMap<>();

    public void interpret(List<Stmt> statements) {
        for (Stmt stmt : statements) {
            execute(stmt);
        }
    }

    private void execute(Stmt stmt) {
        if (stmt instanceof Stmt.Let) {
            Stmt.Let letStmt = (Stmt.Let) stmt;
            Value value = evaluate(letStmt.value);
            variables.put(letStmt.name, value);
        } 
        else if (stmt instanceof Stmt.Print) {
            Stmt.Print printStmt = (Stmt.Print) stmt;
            Value value = evaluate(printStmt.expression);
            System.out.println(value);
        }
    }

    private Value evaluate(Expr expr) {
        if (expr instanceof Expr.Number) {
            return new Value(Value.Type.INT, ((Expr.Number) expr).value);
        }

        if (expr instanceof Expr.FloatExpr) {
            return new Value(Value.Type.FLOAT, ((Expr.FloatExpr) expr).value);
        }

        if (expr instanceof Expr.StringExpr) {
            return new Value(Value.Type.STRING, ((Expr.StringExpr) expr).value);
        }

        if (expr instanceof Expr.BooleanExpr) {
            return new Value(Value.Type.BOOLEAN, ((Expr.BooleanExpr) expr).value);
        }

        if (expr instanceof Expr.Variable) {
            String name = ((Expr.Variable) expr).name;

            if (!variables.containsKey(name)) {
                throw new RuntimeException("Variable no definida: " + name);
            }

            return variables.get(name);
        }

        if (expr instanceof Expr.Binary) {
            Expr.Binary binary = (Expr.Binary) expr;

            Value left = evaluate(binary.left);
            Value right = evaluate(binary.right);

            return evaluateBinary(left, binary.operator, right);
        }

        throw new RuntimeException("Expresión no válida.");
    }

    private Value evaluateBinary(Value left, Token operator, Value right) {
        switch (operator.type) {
            case PLUS:
                return add(left, right);

            case MINUS:
                return numericOperation(left, right, "-");

            case STAR:
                return numericOperation(left, right, "*");

            case SLASH:
                return numericOperation(left, right, "/");

            default:
                throw new RuntimeException("Operador no válido.");
        }
    }

    private Value add(Value left, Value right) {
        if (left.type == Value.Type.STRING && right.type == Value.Type.STRING) {
            return new Value(Value.Type.STRING, left.value.toString() + right.value.toString());
        }

        return numericOperation(left, right, "+");
    }

    private Value numericOperation(Value left, Value right, String operator) {
        if (!isNumeric(left) || !isNumeric(right)) {
            throw new RuntimeException("Operación inválida entre " + left.type + " y " + right.type);
        }

        if (left.type == Value.Type.FLOAT || right.type == Value.Type.FLOAT) {
            double a = toDouble(left);
            double b = toDouble(right);

            switch (operator) {
                case "+":
                    return new Value(Value.Type.FLOAT, a + b);
                case "-":
                    return new Value(Value.Type.FLOAT, a - b);
                case "*":
                    return new Value(Value.Type.FLOAT, a * b);
                case "/":
                    return new Value(Value.Type.FLOAT, a / b);
            }
        }

        int a = (int) left.value;
        int b = (int) right.value;

        switch (operator) {
            case "+":
                return new Value(Value.Type.INT, a + b);
            case "-":
                return new Value(Value.Type.INT, a - b);
            case "*":
                return new Value(Value.Type.INT, a * b);
            case "/":
                return new Value(Value.Type.INT, a / b);
        }

        throw new RuntimeException("Operador numérico no válido.");
    }

    private boolean isNumeric(Value value) {
        return value.type == Value.Type.INT || value.type == Value.Type.FLOAT;
    }

    private double toDouble(Value value) {
        if (value.type == Value.Type.INT) {
            return (int) value.value;
        }

        return (double) value.value;
    }
}