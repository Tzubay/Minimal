package interpreter;

import ast.*;
import lexer.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter {
    private final List<Map<String, Value>> scopes = new ArrayList<>();
    private final Map<String, FunctionValue> functions = new HashMap<>();
    private final Scanner scanner = new Scanner(System.in);

    public Interpreter() {
        scopes.add(new HashMap<>());
    }

    public void interpret(List<Stmt> statements) {
        for (Stmt stmt : statements) {
            execute(stmt);
        }
    }

    private Map<String, Value> currentScope() {
        return scopes.get(scopes.size() - 1);
    }

    private boolean variableExistsInAnyScope(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return true;
            }
        }

        return false;
    }

    private Value getVariable(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Value> scope = scopes.get(i);

            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }

        throw new RuntimeException("Variable no definida: " + name);
    }

    private void assignVariable(String name, Value newValue) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Value> scope = scopes.get(i);

            if (scope.containsKey(name)) {
                Value oldValue = scope.get(name);

            if (oldValue.type != Value.Type.UNDEFINED && oldValue.type != newValue.type) {
                throw new RuntimeException(
                    "No se puede asignar " + newValue.type +
                    " a variable " + oldValue.type +
                    ": " + name
                );
            }

                scope.put(name, newValue);
                return;
            }
        }

        throw new RuntimeException("Variable no definida: " + name);
    }

    private void execute(Stmt stmt) {
        if (stmt instanceof Stmt.Break) {
            throw new BreakException();
        }
        if (stmt instanceof Stmt.Function) {
            Stmt.Function functionStmt = (Stmt.Function) stmt;

            if (functions.containsKey(functionStmt.name)) {
                throw new RuntimeException("La función ya existe: " + functionStmt.name);
            }

            functions.put(
                functionStmt.name,
                new FunctionValue(functionStmt.name, functionStmt.params, functionStmt.body)
            );
            return;
        }

        if (stmt instanceof Stmt.Return) {
            Stmt.Return returnStmt = (Stmt.Return) stmt;

            Value value;

            if (returnStmt.values.size() == 1) {
                value = evaluate(returnStmt.values.get(0));
            } else {
                StringBuilder builder = new StringBuilder();

                for (Expr expr : returnStmt.values) {
                    builder.append(evaluate(expr).toString());
                }

                value = new Value(Value.Type.STRING, builder.toString());
            }

            throw new ReturnException(value);
        }

        if (stmt instanceof Stmt.Let) {
            Stmt.Let letStmt = (Stmt.Let) stmt;

            if (currentScope().containsKey(letStmt.name)) {
                throw new RuntimeException("La variable ya existe en este scope: " + letStmt.name);
            }

        Value value;

        if (letStmt.value == null) {
            value = new Value(Value.Type.UNDEFINED, null);
        } else {
            value = evaluate(letStmt.value);
        }

        currentScope().put(letStmt.name, value);
            return;
        }

        if (stmt instanceof Stmt.Assign) {
            Stmt.Assign assignStmt = (Stmt.Assign) stmt;
            Value newValue = evaluate(assignStmt.value);
            assignVariable(assignStmt.name, newValue);
            return;
        }

        if (stmt instanceof Stmt.IndexAssign) {
            Stmt.IndexAssign indexAssign = (Stmt.IndexAssign) stmt;

            Value arrayValue = evaluate(indexAssign.array);
            Value indexValue = evaluate(indexAssign.index);
            Value newValue = evaluate(indexAssign.value);

            if (arrayValue.type != Value.Type.ARRAY) {
                throw new RuntimeException("Solo se puede asignar por índice sobre un arreglo.");
            }

            if (indexValue.type != Value.Type.INT) {
                throw new RuntimeException("El índice debe ser un entero.");
            }

            List<Value> elements = (List<Value>) arrayValue.value;
            int index = (int) indexValue.value;

            if (index < 0 || index >= elements.size()) {
                throw new RuntimeException("Índice fuera de rango: " + index);
            }

            elements.set(index, newValue);
            return;
        }

        if (stmt instanceof Stmt.Print) {
            Stmt.Print printStmt = (Stmt.Print) stmt;

            StringBuilder output = new StringBuilder();

            for (Expr expression : printStmt.expressions) {
                Value value = evaluate(expression);
                output.append(value.toString());
            }

            System.out.println(output.toString());
            return;
        }

        if (stmt instanceof Stmt.Block) {
            Stmt.Block blockStmt = (Stmt.Block) stmt;

            executeBlock(blockStmt.statements);

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

                try {
                    execute(whileStmt.body);
                } catch (BreakException breakException) {
                    break;
                }
            }

            return;
        }
        if (stmt instanceof Stmt.Expression) {
            Stmt.Expression exprStmt = (Stmt.Expression) stmt;
            evaluate(exprStmt.expression);
            return;
        }

        if (stmt instanceof Stmt.Switch) {
            Stmt.Switch switchStmt = (Stmt.Switch) stmt;

            Value conditionValue = evaluate(switchStmt.condition);

            boolean executed = false;

            try {
                for (Stmt.SwitchCase switchCase : switchStmt.cases) {
                    Value caseValue = evaluate(switchCase.value);

                    if (valuesEqual(conditionValue, caseValue)) {
                        for (Stmt caseStmt : switchCase.statements) {
                            execute(caseStmt);
                        }

                        executed = true;
                        break;
                    }
                }

                if (!executed && switchStmt.defaultStatements != null) {
                    for (Stmt defaultStmt : switchStmt.defaultStatements) {
                        execute(defaultStmt);
                    }
                }

            } catch (BreakException breakException) {
                // break solo sale del switch
            }

            return;
        }
        throw new RuntimeException("Instrucción no válida.");
    }
    private void executeBlock(List<Stmt> statements) {
        scopes.add(new HashMap<>());

        try {
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } finally {
            scopes.remove(scopes.size() - 1);
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

        if (expr instanceof Expr.ArrayExpr) {
            Expr.ArrayExpr arrayExpr = (Expr.ArrayExpr) expr;

            List<Value> elements = new ArrayList<>();

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
            return getVariable(name);
        }

        if (expr instanceof Expr.Call) {
            Expr.Call callExpr = (Expr.Call) expr;

            if (!(callExpr.callee instanceof Expr.Variable)) {
                throw new RuntimeException("Solo se pueden llamar funciones por nombre.");
            }

            String functionName = ((Expr.Variable) callExpr.callee).name;

            List<Value> argumentValues = new ArrayList<>();

            for (Expr argument : callExpr.arguments) {
                argumentValues.add(evaluate(argument));
            }

            if (isNativeFunction(functionName)) {
                return callNativeFunction(functionName, argumentValues);
            }

            if (!functions.containsKey(functionName)) {
                throw new RuntimeException("Función no definida: " + functionName);
            }

            FunctionValue function = functions.get(functionName);

            if (argumentValues.size() != function.params.size()) {
                throw new RuntimeException(
                    "La función " + functionName + " esperaba " +
                    function.params.size() + " argumentos, pero recibió " +
                    argumentValues.size()
                );
            }

            return callFunction(function, argumentValues);
        }
        if (expr instanceof Expr.MethodCall) {
            Expr.MethodCall methodCall = (Expr.MethodCall) expr;

            Value object = evaluate(methodCall.object);

            List<Value> argumentValues = new ArrayList<>();

            for (Expr argument : methodCall.arguments) {
                argumentValues.add(evaluate(argument));
            }

            return callMethod(object, methodCall.methodName, argumentValues);
        }
        if (expr instanceof Expr.Binary) {
            Expr.Binary binary = (Expr.Binary) expr;

            Value left = evaluate(binary.left);
            Value right = evaluate(binary.right);

            return evaluateBinary(left, binary.operator, right);
        }

        throw new RuntimeException("Expresión no válida.");
    }

    private Value callFunction(FunctionValue function, List<Value> argumentValues) {
        Map<String, Value> functionScope = new HashMap<>();

        for (int i = 0; i < function.params.size(); i++) {
            functionScope.put(function.params.get(i), argumentValues.get(i));
        }

        scopes.add(functionScope);

        try {
            for (Stmt stmt : function.body) {
                execute(stmt);
            }
        } catch (ReturnException returnValue) {
            scopes.remove(scopes.size() - 1);
            return returnValue.value;
        }

        scopes.remove(scopes.size() - 1);

        throw new RuntimeException("La función " + function.name + " no retornó ningún valor.");
    }

    private boolean isNativeFunction(String name) {
        return name.equals("input") || name.equals("len");
    }

    private Value callNativeFunction(String name, List<Value> arguments) {
        switch (name) {
            case "input":
                return nativeInput(arguments);

            case "len":
                return nativeLen(arguments);

            default:
                throw new RuntimeException("Función nativa no definida: " + name);
        }
    }

    private Value nativeInput(List<Value> arguments) {
        if (arguments.size() > 1) {
            throw new RuntimeException("input() recibe máximo 1 argumento.");
        }

        if (arguments.size() == 1) {
            Value prompt = arguments.get(0);

            if (prompt.type != Value.Type.STRING) {
                throw new RuntimeException("El argumento de input() debe ser STRING.");
            }

            System.out.print(prompt.value.toString());
        }

        String text = scanner.nextLine();

        return inferInputValue(text);
    }

    private Value inferInputValue(String text) {
        if (text.equals("true")) {
            return new Value(Value.Type.BOOLEAN, true);
        }

        if (text.equals("false")) {
            return new Value(Value.Type.BOOLEAN, false);
        }

        if (isInteger(text)) {
            return new Value(Value.Type.INT, Integer.parseInt(text));
        }

        if (isFloat(text)) {
            return new Value(Value.Type.FLOAT, Double.parseDouble(text));
        }

        return new Value(Value.Type.STRING, text);
    }

    private boolean isInteger(String text) {
        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isFloat(String text) {
        try {
            Double.parseDouble(text);
            return text.contains(".");
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Value nativeLen(List<Value> arguments) {
        if (arguments.size() != 1) {
            throw new RuntimeException("len() recibe exactamente 1 argumento.");
        }

        Value value = arguments.get(0);

        if (value.type == Value.Type.ARRAY) {
            List<Value> elements = (List<Value>) value.value;
            return new Value(Value.Type.INT, elements.size());
        }

        if (value.type == Value.Type.STRING) {
            String text = (String) value.value;
            return new Value(Value.Type.INT, text.length());
        }

        throw new RuntimeException("len() solo funciona con ARRAY o STRING.");
    }

private Value callMethod(Value object, String methodName, List<Value> arguments) {
    if (object.type == Value.Type.ARRAY) {
        switch (methodName) {
            case "append":
                return arrayAppend(object, arguments);
            default:
                throw new RuntimeException("Método no definido para ARRAY: " + methodName);
        }
    }

    throw new RuntimeException("El tipo " + object.type + " no tiene métodos.");
}

private Value arrayAppend(Value arrayValue, List<Value> arguments) {
    if (arguments.size() != 1) {
        throw new RuntimeException("append() recibe exactamente 1 argumento.");
    }

    List<Value> elements = (List<Value>) arrayValue.value;
    elements.add(arguments.get(0));

    return arrayValue;
}
    private boolean valuesEqual(Value a, Value b) {
        if (a.type != b.type) {
            return false;
        }

        if (a.type == Value.Type.UNDEFINED) {
            return b.type == Value.Type.UNDEFINED;
        }

        return a.value.equals(b.value);
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