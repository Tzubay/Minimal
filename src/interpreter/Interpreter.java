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

            if (variables.containsKey(letStmt.name)) {
                throw new RuntimeException("La variable ya existe: " + letStmt.name);
            }

            Value value = evaluate(letStmt.value);
            variables.put(letStmt.name, value);
            return;
        }

        if (stmt instanceof Stmt.Assign) {
            Stmt.Assign assignStmt = (Stmt.Assign) stmt;

            if (!variables.containsKey(assignStmt.name)) {
                throw new RuntimeException("Variable no definida: " + assignStmt.name);
            }

            Value oldValue = variables.get(assignStmt.name);
            Value newValue = evaluate(assignStmt.value);

            if (oldValue.type != newValue.type) {
                throw new RuntimeException(
                    "No se puede asignar " + newValue.type +
                    " a variable " + oldValue.type +
                    ": " + assignStmt.name
                );
            }

            variables.put(assignStmt.name, newValue);
            return;
        }

        if (stmt instanceof Stmt.Print) {
            Stmt.Print printStmt = (Stmt.Print) stmt;
            Value value = evaluate(printStmt.expression);
            System.out.println(value);
            return;
        }

        if (stmt instanceof Stmt.Block) {
            Stmt.Block blockStmt = (Stmt.Block) stmt;

            for (Stmt innerStmt : blockStmt.statements) {
                execute(innerStmt);
            }

            return;
        }

        if (stmt instanceof Stmt.If) {
            Stmt.If ifStmt = (Stmt.If) stmt;

            Value condition = evaluate(ifStmt.condition);

            if (condition.type != Value.Type.BOOLEAN) {
                throw new RuntimeException("La condición del if debe ser booleana.");
            }

            if ((boolean) condition.value) {
                execute(ifStmt.thenBranch);
            } else if (ifStmt.elseBranch != null) {
                execute(ifStmt.elseBranch);
            }

            return;
        }

        if (stmt instanceof Stmt.While) {
            Stmt.While whileStmt = (Stmt.While) stmt;

            while (true) {
                Value condition = evaluate(whileStmt.condition);

                if (condition.type != Value.Type.BOOLEAN) {
                    throw new RuntimeException("La condición del while debe ser booleana.");
                }

                if (!((boolean) condition.value)) {
                    break;
                }

                execute(whileStmt.body);
            }

            return;
        }

        throw new RuntimeException("Instrucción no válida.");
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

        if (expr instanceof Expr.ArrayExpr) {
            Expr.ArrayExpr arrayExpr = (Expr.ArrayExpr) expr;

            List<Value> elements = new java.util.ArrayList<>();

            for (Expr element : arrayExpr.elements) {
                elements.add(evaluate(element));
            }

            return new Value(Value.Type.ARRAY, elements);
        }
        
        if (expr instanceof Expr.Index) {
            Expr.Index indexExpr = (Expr.Index) expr;

            Value arrayValue = evaluate(indexExpr.array);
            Value indexValue = evaluate(indexExpr.index);

            if (arrayValue.type != Value.Type.ARRAY) {
                throw new RuntimeException("Solo se puede indexar un arreglo.");
            }

            if (indexValue.type != Value.Type.INT) {
                throw new RuntimeException("El índice debe ser un entero.");
            }

            List<Value> elements = (List<Value>) arrayValue.value;
            int index = (int) indexValue.value;

            if (index < 0 || index >= elements.size()) {
                throw new RuntimeException("Índice fuera de rango: " + index);
            }

            return elements.get(index);
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

            case LESS:
                return compareOperation(left, right, "<");

            case LESS_EQUAL:
                return compareOperation(left, right, "<=");

            case GREATER:
                return compareOperation(left, right, ">");

            case GREATER_EQUAL:
                return compareOperation(left, right, ">=");

            case EQUAL_EQUAL:
                return equalityOperation(left, right, "==");

            case BANG_EQUAL:
                return equalityOperation(left, right, "!=");

            default:
                throw new RuntimeException("Operador no válido: " + operator.lexeme);
        }
    }

    private Value add(Value left, Value right) {
        if (left.type == Value.Type.STRING && right.type == Value.Type.STRING) {
            return new Value(
                Value.Type.STRING,
                left.value.toString() + right.value.toString()
            );
        }

        return numericOperation(left, right, "+");
    }

    private Value numericOperation(Value left, Value right, String operator) {
        if (!isNumeric(left) || !isNumeric(right)) {
            throw new RuntimeException(
                "Operación numérica inválida entre " + left.type + " y " + right.type
            );
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

    private Value compareOperation(Value left, Value right, String operator) {
        if (!isNumeric(left) || !isNumeric(right)) {
            throw new RuntimeException(
                "Comparación inválida entre " + left.type + " y " + right.type
            );
        }

        double a = toDouble(left);
        double b = toDouble(right);

        boolean result;

        switch (operator) {
            case "<":
                result = a < b;
                break;
            case "<=":
                result = a <= b;
                break;
            case ">":
                result = a > b;
                break;
            case ">=":
                result = a >= b;
                break;
            default:
                throw new RuntimeException("Operador de comparación no válido.");
        }

        return new Value(Value.Type.BOOLEAN, result);
    }

    private Value equalityOperation(Value left, Value right, String operator) {
        boolean result;

        if (left.type != right.type) {
            result = false;
        } else {
            result = left.value.equals(right.value);
        }

        if (operator.equals("!=")) {
            result = !result;
        }

        return new Value(Value.Type.BOOLEAN, result);
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