package interpreter;
import java.util.Random;

import ast.*;
import lexer.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Interpreter {
    private final Map<String, Value> globalScope = new HashMap<>();

    private final ThreadLocal<List<Map<String, Value>>> scopes =
        ThreadLocal.withInitial(() -> {
            List<Map<String, Value>> list = new ArrayList<>();
            list.add(globalScope);
            return list;
        });

    private final Map<String, FunctionValue> functions = new HashMap<>();
    private final Scanner scanner = new Scanner(System.in);

    public Interpreter() {
    /*     scopes.add(new HashMap<>());*/
    }

    public void interpret(List<Stmt> statements) {
        for (Stmt stmt : statements) {
            execute(stmt);
        }
    }
private List<Map<String, Value>> scopeStack() {

    return scopes.get();

}
    private Map<String, Value> currentScope() {

        List<Map<String, Value>> stack = scopeStack();

        return stack.get(stack.size() - 1);

    }

private boolean variableExistsInAnyScope(String name) {
    List<Map<String, Value>> stack = scopeStack();

    for (int i = stack.size() - 1; i >= 0; i--) {
        if (stack.get(i).containsKey(name)) {
            return true;
        }
    }

    return false;
}

private Value getVariable(String name) {

    List<Map<String, Value>> stack = scopeStack();

    for (int i = stack.size() - 1; i >= 0; i--) {

        Map<String, Value> scope = stack.get(i);

        if (scope.containsKey(name)) {

            return scope.get(name);

        }

    }

    throw new RuntimeException("Variable no definida: " + name);

}



private void assignVariable(String name, Value newValue) {

    List<Map<String, Value>> stack = scopeStack();

    for (int i = stack.size() - 1; i >= 0; i--) {

        Map<String, Value> scope = stack.get(i);

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

        if (stmt instanceof Stmt.Import) {
            Stmt.Import importStmt = (Stmt.Import) stmt;

            if (!isKnownModule(importStmt.moduleName)) {
                throw new RuntimeException("Módulo no encontrado: " + importStmt.moduleName);
            }

            currentScope().put(
                importStmt.moduleName,
                new Value(Value.Type.MODULE, new ModuleValue(importStmt.moduleName))
            );

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

    List<Map<String, Value>> stack = scopeStack();

    stack.add(new HashMap<>());

    try {

        for (Stmt stmt : statements) {

            execute(stmt);

        }

    } finally {

        stack.remove(stack.size() - 1);

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

    if (functionName.equals("thread")) {
        return nativeThread(callExpr.arguments);
    }

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

    if (object.type == Value.Type.MODULE) {
        ModuleValue module = (ModuleValue) object.value;

        if (module.name.equals("threading") && methodCall.methodName.equals("thread")) {
            return nativeThread(methodCall.arguments);
        }
    }

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

    List<Map<String, Value>> stack = scopeStack();
    stack.add(functionScope);

    try {
        for (Stmt stmt : function.body) {
            execute(stmt);
        }
    } catch (ReturnException returnValue) {
        stack.remove(stack.size() - 1);
        return returnValue.value;
    }

    stack.remove(stack.size() - 1);

    throw new RuntimeException("La función " + function.name + " no retornó ningún valor.");
}

private boolean isNativeFunction(String name) {
    return name.equals("input") ||
           name.equals("len") ||
           name.equals("start") ||
           name.equals("join") ||
           name.equals("sleep");
}

private Value callModuleMethod(String moduleName, String methodName, List<Value> arguments) {
    switch (moduleName) {
        case "time":
            return callTimeModule(methodName, arguments);

        case "threading":
            return callThreadingModule(methodName, arguments);

        case "matrix":
            return callMatrixModule(methodName, arguments);

        default:
            throw new RuntimeException("Módulo no encontrado: " + moduleName);
    }
}

private Value nativeTime(List<Value> arguments) {
    if (arguments.size() != 0) {
        throw new RuntimeException("time.time() no recibe argumentos.");
    }

    double seconds = System.currentTimeMillis() / 1000.0;

    return new Value(Value.Type.FLOAT, seconds);
}

private Value callTimeModule(String methodName, List<Value> arguments) {
    switch (methodName) {
        case "sleep":
            return nativeSleep(arguments);

        case "time":
            return nativeTime(arguments);

        default:
            throw new RuntimeException("El módulo time no tiene método: " + methodName);
    }
}

private Value callThreadingModule(String methodName, List<Value> arguments) {
    switch (methodName) {
        case "start":
            return nativeStart(arguments);

        case "join":
            return nativeJoin(arguments);

        default:
            throw new RuntimeException("El módulo threading no tiene método: " + methodName);
    }
}

private Value callMatrixModule(String methodName, List<Value> arguments) {
    switch (methodName) {
        case "int":
            return matrixInt(arguments);

        case "zeros":
            return matrixZeros(arguments);

        case "ones":
            return matrixOnes(arguments);

        case "random":
            return matrixRandom(arguments);

        case "fill":
            return matrixFill(arguments);

        case "fill_parallel":
            return matrixFillParallel(arguments);

        case "add":
            return matrixAdd(arguments);

        case "transpose":
            return matrixTranspose(arguments);

        case "get":
            return matrixGet(arguments);

        case "set":
            return matrixSet(arguments);

        case "rows":
            return matrixRows(arguments);

        case "multiply":
            return matrixMultiply(arguments);

        case "matmul":
            return matrixMatmul(arguments);

        case "matmul_parallel":
            return matrixMatmulParallel(arguments);
            
        case "sum":
            return matrixSum(arguments);

        case "mean":
            return matrixMean(arguments);

        case "cols":
            return matrixCols(arguments);

        case "print_sample":
            return matrixPrintSample(arguments);

        default:
            throw new RuntimeException("El módulo matrix no tiene método: " + methodName);
    }
}

private Value callNativeFunction(String name, List<Value> arguments) {
    switch (name) {
        case "input":
            return nativeInput(arguments);

        case "len":
            return nativeLen(arguments);

        case "start":
            return nativeStart(arguments);

        case "join":
            return nativeJoin(arguments);

        case "sleep":
            return nativeSleep(arguments);

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
private Value nativeThread(List<Expr> rawArguments) {
    if (rawArguments.size() < 1) {
        throw new RuntimeException("thread() necesita al menos el nombre de una función.");
    }

    Expr firstArg = rawArguments.get(0);

    if (!(firstArg instanceof Expr.Variable)) {
        throw new RuntimeException("El primer argumento de thread() debe ser el nombre de una función.");
    }

    String functionName = ((Expr.Variable) firstArg).name;

    if (!functions.containsKey(functionName)) {
        throw new RuntimeException("Función no definida para thread(): " + functionName);
    }

    FunctionValue function = functions.get(functionName);

    List<Value> argumentValues = new ArrayList<>();

    for (int i = 1; i < rawArguments.size(); i++) {
        argumentValues.add(evaluate(rawArguments.get(i)));
    }

    if (argumentValues.size() != function.params.size()) {
        throw new RuntimeException(
            "La función " + functionName + " esperaba " +
            function.params.size() + " argumentos, pero recibió " +
            argumentValues.size()
        );
    }

    ThreadValue threadValue = new ThreadValue(function, argumentValues);

    return new Value(Value.Type.THREAD, threadValue);
}

private boolean isKnownModule(String name) {
    return name.equals("time") ||
           name.equals("threading") ||
           name.equals("matrix");
}

private Value nativeStart(List<Value> arguments) {
    if (arguments.size() != 1) {
        throw new RuntimeException("start() recibe exactamente 1 argumento.");
    }

    Value value = arguments.get(0);

    if (value.type != Value.Type.THREAD) {
        throw new RuntimeException("start() solo recibe THREAD.");
    }

    ThreadValue threadValue = (ThreadValue) value.value;

    if (threadValue.started) {
        throw new RuntimeException("Este hilo ya fue iniciado.");
    }

    Thread thread = new Thread(() -> {
        try {
            callFunction(threadValue.function, threadValue.arguments);
        } catch (RuntimeException e) {
            System.err.println("Error en hilo: " + e.getMessage());
        }
    });

    threadValue.thread = thread;
    threadValue.started = true;

    thread.start();

    return new Value(Value.Type.UNDEFINED, null);
}

private Value matrixInt(List<Value> arguments) {
    if (arguments.size() != 2) {
        throw new RuntimeException("matrix.int(rows, cols) recibe exactamente 2 argumentos.");
    }

    Value rowsValue = arguments.get(0);
    Value colsValue = arguments.get(1);

    if (rowsValue.type != Value.Type.INT || colsValue.type != Value.Type.INT) {
        throw new RuntimeException("matrix.int(rows, cols) necesita INT, INT.");
    }

    int rows = (int) rowsValue.value;
    int cols = (int) colsValue.value;

    MatrixValue matrix = new MatrixValue(rows, cols);

    return new Value(Value.Type.MATRIX, matrix);
}


private Value matrixFillParallel(List<Value> arguments) {
    if (arguments.size() != 2) {
        throw new RuntimeException("matrix.fill_parallel(matriz, hilos) recibe exactamente 2 argumentos.");
    }

    Value matrixValue = arguments.get(0);
    Value threadsValue = arguments.get(1);

    if (matrixValue.type != Value.Type.MATRIX) {
        throw new RuntimeException("El primer argumento de matrix.fill_parallel debe ser MATRIX.");
    }

    if (threadsValue.type != Value.Type.INT) {
        throw new RuntimeException("El segundo argumento de matrix.fill_parallel debe ser INT.");
    }

    MatrixValue matrix = (MatrixValue) matrixValue.value;
    int threadCount = (int) threadsValue.value;

    if (threadCount <= 0) {
        throw new RuntimeException("La cantidad de hilos debe ser mayor que 0.");
    }

    Thread[] threads = new Thread[threadCount];

    int chunk = matrix.rows / threadCount;

    for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        final int startRow = i * chunk;
        final int endRow = (i == threadCount - 1) ? matrix.rows : startRow + chunk;

        threads[i] = new Thread(() -> {
            for (int row = startRow; row < endRow; row++) {
                int base = row * matrix.cols;

                for (int col = 0; col < matrix.cols; col++) {
                    matrix.data[base + col] = row + col;
                }
            }
        });

        threads[i].start();
    }

    for (int i = 0; i < threadCount; i++) {
        try {
            threads[i].join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("matrix.fill_parallel fue interrumpido.");
        }
    }

    return new Value(Value.Type.UNDEFINED, null);
}

private int requireInt(Value value, String message) {
    if (value.type != Value.Type.INT) {
        throw new RuntimeException(message);
    }

    return (int) value.value;
}

private MatrixValue requireMatrix(Value value, String message) {
    if (value.type != Value.Type.MATRIX) {
        throw new RuntimeException(message);
    }

    return (MatrixValue) value.value;
}

private Value matrixZeros(List<Value> arguments) {
    if (arguments.size() != 2) {
        throw new RuntimeException("matrix.zeros(rows, cols) recibe exactamente 2 argumentos.");
    }

    int rows = requireInt(arguments.get(0), "matrix.zeros necesita rows INT.");
    int cols = requireInt(arguments.get(1), "matrix.zeros necesita cols INT.");

    MatrixValue matrix = new MatrixValue(rows, cols);

    return new Value(Value.Type.MATRIX, matrix);
}

private Value matrixOnes(List<Value> arguments) {
    if (arguments.size() != 2) {
        throw new RuntimeException("matrix.ones(rows, cols) recibe exactamente 2 argumentos.");
    }

    int rows = requireInt(arguments.get(0), "matrix.ones necesita rows INT.");
    int cols = requireInt(arguments.get(1), "matrix.ones necesita cols INT.");

    MatrixValue matrix = new MatrixValue(rows, cols);

    for (int i = 0; i < matrix.data.length; i++) {
        matrix.data[i] = 1;
    }

    return new Value(Value.Type.MATRIX, matrix);
}

private Value matrixFill(List<Value> arguments) {
    if (arguments.size() != 2) {
        throw new RuntimeException("matrix.fill(matriz, value) recibe exactamente 2 argumentos.");
    }

    MatrixValue matrix = requireMatrix(
        arguments.get(0),
        "El primer argumento de matrix.fill debe ser MATRIX."
    );

    int value = requireInt(
        arguments.get(1),
        "El segundo argumento de matrix.fill debe ser INT."
    );

    for (int i = 0; i < matrix.data.length; i++) {
        matrix.data[i] = value;
    }

    return new Value(Value.Type.UNDEFINED, null);
}

private Value matrixRandom(List<Value> arguments) {
    if (arguments.size() != 2) {
        throw new RuntimeException("matrix.random(rows, cols) recibe exactamente 2 argumentos.");
    }

    int rows = requireInt(arguments.get(0), "matrix.random necesita rows INT.");
    int cols = requireInt(arguments.get(1), "matrix.random necesita cols INT.");

    MatrixValue matrix = new MatrixValue(rows, cols);
    Random random = new Random();

    for (int i = 0; i < matrix.data.length; i++) {
        matrix.data[i] = random.nextInt(100);
    }

    return new Value(Value.Type.MATRIX, matrix);
}

private Value matrixAdd(List<Value> arguments) {
    if (arguments.size() != 2) {
        throw new RuntimeException("matrix.add(a, b) recibe exactamente 2 argumentos.");
    }

    MatrixValue a = requireMatrix(
        arguments.get(0),
        "El primer argumento de matrix.add debe ser MATRIX."
    );

    MatrixValue b = requireMatrix(
        arguments.get(1),
        "El segundo argumento de matrix.add debe ser MATRIX."
    );

    if (a.rows != b.rows || a.cols != b.cols) {
        throw new RuntimeException("matrix.add requiere matrices con las mismas dimensiones.");
    }

    MatrixValue result = new MatrixValue(a.rows, a.cols);

    for (int i = 0; i < a.data.length; i++) {
        result.data[i] = a.data[i] + b.data[i];
    }

    return new Value(Value.Type.MATRIX, result);
}

private Value matrixTranspose(List<Value> arguments) {
    if (arguments.size() != 1) {
        throw new RuntimeException("matrix.transpose(matriz) recibe exactamente 1 argumento.");
    }

    MatrixValue matrix = requireMatrix(
        arguments.get(0),
        "matrix.transpose necesita MATRIX."
    );

    MatrixValue result = new MatrixValue(matrix.cols, matrix.rows);

    for (int row = 0; row < matrix.rows; row++) {
        for (int col = 0; col < matrix.cols; col++) {
            result.set(col, row, matrix.get(row, col));
        }
    }

    return new Value(Value.Type.MATRIX, result);
}

private Value matrixGet(List<Value> arguments) {
    if (arguments.size() != 3) {
        throw new RuntimeException("matrix.get(matriz, row, col) recibe exactamente 3 argumentos.");
    }

    Value matrixValue = arguments.get(0);
    Value rowValue = arguments.get(1);
    Value colValue = arguments.get(2);

    if (matrixValue.type != Value.Type.MATRIX) {
        throw new RuntimeException("El primer argumento de matrix.get debe ser MATRIX.");
    }

    if (rowValue.type != Value.Type.INT || colValue.type != Value.Type.INT) {
        throw new RuntimeException("matrix.get necesita índices INT.");
    }

    MatrixValue matrix = (MatrixValue) matrixValue.value;
    int row = (int) rowValue.value;
    int col = (int) colValue.value;

    return new Value(Value.Type.INT, matrix.get(row, col));
}

private Value matrixSet(List<Value> arguments) {
    if (arguments.size() != 4) {
        throw new RuntimeException("matrix.set(matriz, row, col, value) recibe exactamente 4 argumentos.");
    }

    Value matrixValue = arguments.get(0);
    Value rowValue = arguments.get(1);
    Value colValue = arguments.get(2);
    Value newValue = arguments.get(3);

    if (matrixValue.type != Value.Type.MATRIX) {
        throw new RuntimeException("El primer argumento de matrix.set debe ser MATRIX.");
    }

    if (rowValue.type != Value.Type.INT || colValue.type != Value.Type.INT) {
        throw new RuntimeException("matrix.set necesita índices INT.");
    }

    if (newValue.type != Value.Type.INT) {
        throw new RuntimeException("matrix.set solo acepta valores INT por ahora.");
    }

    MatrixValue matrix = (MatrixValue) matrixValue.value;
    int row = (int) rowValue.value;
    int col = (int) colValue.value;
    int value = (int) newValue.value;

    matrix.set(row, col, value);

    return new Value(Value.Type.UNDEFINED, null);
}

private Value matrixMultiply(List<Value> arguments) {
    if (arguments.size() != 2) {
        throw new RuntimeException("matrix.multiply(a, b) recibe exactamente 2 argumentos.");
    }

    MatrixValue a = requireMatrix(
        arguments.get(0),
        "El primer argumento de matrix.multiply debe ser MATRIX."
    );

    MatrixValue b = requireMatrix(
        arguments.get(1),
        "El segundo argumento de matrix.multiply debe ser MATRIX."
    );

    if (a.rows != b.rows || a.cols != b.cols) {
        throw new RuntimeException("matrix.multiply requiere matrices con las mismas dimensiones.");
    }

    MatrixValue result = new MatrixValue(a.rows, a.cols);

    for (int i = 0; i < a.data.length; i++) {
        result.data[i] = a.data[i] * b.data[i];
    }

    return new Value(Value.Type.MATRIX, result);
}

private Value matrixMatmul(List<Value> arguments) {
    if (arguments.size() != 2) {
        throw new RuntimeException("matrix.matmul(a, b) recibe exactamente 2 argumentos.");
    }

    MatrixValue a = requireMatrix(
        arguments.get(0),
        "El primer argumento de matrix.matmul debe ser MATRIX."
    );

    MatrixValue b = requireMatrix(
        arguments.get(1),
        "El segundo argumento de matrix.matmul debe ser MATRIX."
    );

    if (a.cols != b.rows) {
        throw new RuntimeException(
            "matrix.matmul requiere que columnas de A sean iguales a filas de B."
        );
    }

    MatrixValue result = new MatrixValue(a.rows, b.cols);

    for (int row = 0; row < a.rows; row++) {
        for (int col = 0; col < b.cols; col++) {
            int sum = 0;

            for (int k = 0; k < a.cols; k++) {
                sum += a.get(row, k) * b.get(k, col);
            }

            result.set(row, col, sum);
        }
    }

    return new Value(Value.Type.MATRIX, result);
}

private Value matrixMatmulParallel(List<Value> arguments) {
    if (arguments.size() != 3) {
        throw new RuntimeException("matrix.matmul_parallel(a, b, hilos) recibe exactamente 3 argumentos.");
    }

    MatrixValue a = requireMatrix(
        arguments.get(0),
        "El primer argumento de matrix.matmul_parallel debe ser MATRIX."
    );

    MatrixValue b = requireMatrix(
        arguments.get(1),
        "El segundo argumento de matrix.matmul_parallel debe ser MATRIX."
    );

    int threadCount = requireInt(
        arguments.get(2),
        "El tercer argumento de matrix.matmul_parallel debe ser INT."
    );

    if (a.cols != b.rows) {
        throw new RuntimeException(
            "matrix.matmul_parallel requiere que columnas de A sean iguales a filas de B."
        );
    }

    if (threadCount <= 0) {
        throw new RuntimeException("La cantidad de hilos debe ser mayor que 0.");
    }

    MatrixValue result = new MatrixValue(a.rows, b.cols);

    if (threadCount > a.rows) {
        threadCount = a.rows;
    }

    Thread[] threads = new Thread[threadCount];

    int chunk = a.rows / threadCount;

    for (int i = 0; i < threadCount; i++) {
        final int startRow = i * chunk;
        final int endRow = (i == threadCount - 1) ? a.rows : startRow + chunk;

        threads[i] = new Thread(() -> {
            for (int row = startRow; row < endRow; row++) {
                int resultBase = row * result.cols;

                for (int col = 0; col < b.cols; col++) {
                    int sum = 0;

                    for (int k = 0; k < a.cols; k++) {
                        sum += a.data[row * a.cols + k] * b.data[k * b.cols + col];
                    }

                    result.data[resultBase + col] = sum;
                }
            }
        });

        threads[i].start();
    }

    for (int i = 0; i < threadCount; i++) {
        try {
            threads[i].join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("matrix.matmul_parallel fue interrumpido.");
        }
    }

    return new Value(Value.Type.MATRIX, result);
}

private Value matrixSum(List<Value> arguments) {
    if (arguments.size() != 1) {
        throw new RuntimeException("matrix.sum(m) recibe exactamente 1 argumento.");
    }

    MatrixValue matrix = requireMatrix(
        arguments.get(0),
        "matrix.sum necesita MATRIX."
    );

    long sum = 0;

    for (int i = 0; i < matrix.data.length; i++) {
        sum += matrix.data[i];
    }

    if (sum <= Integer.MAX_VALUE && sum >= Integer.MIN_VALUE) {
        return new Value(Value.Type.INT, (int) sum);
    }

    return new Value(Value.Type.FLOAT, (double) sum);
}

private Value matrixMean(List<Value> arguments) {
    if (arguments.size() != 1) {
        throw new RuntimeException("matrix.mean(m) recibe exactamente 1 argumento.");
    }

    MatrixValue matrix = requireMatrix(
        arguments.get(0),
        "matrix.mean necesita MATRIX."
    );

    long sum = 0;

    for (int i = 0; i < matrix.data.length; i++) {
        sum += matrix.data[i];
    }

    double mean = sum / (double) matrix.data.length;

    return new Value(Value.Type.FLOAT, mean);
}

private Value matrixRows(List<Value> arguments) {
    if (arguments.size() != 1) {
        throw new RuntimeException("matrix.rows(matriz) recibe exactamente 1 argumento.");
    }

    Value matrixValue = arguments.get(0);

    if (matrixValue.type != Value.Type.MATRIX) {
        throw new RuntimeException("matrix.rows necesita MATRIX.");
    }

    MatrixValue matrix = (MatrixValue) matrixValue.value;

    return new Value(Value.Type.INT, matrix.rows);
}

private Value matrixCols(List<Value> arguments) {
    if (arguments.size() != 1) {
        throw new RuntimeException("matrix.cols(matriz) recibe exactamente 1 argumento.");
    }

    Value matrixValue = arguments.get(0);

    if (matrixValue.type != Value.Type.MATRIX) {
        throw new RuntimeException("matrix.cols necesita MATRIX.");
    }

    MatrixValue matrix = (MatrixValue) matrixValue.value;

    return new Value(Value.Type.INT, matrix.cols);
}

private Value matrixPrintSample(List<Value> arguments) {
    if (arguments.size() != 3) {
        throw new RuntimeException("matrix.print_sample(matriz, rows, cols) recibe exactamente 3 argumentos.");
    }

    Value matrixValue = arguments.get(0);
    Value rowsValue = arguments.get(1);
    Value colsValue = arguments.get(2);

    if (matrixValue.type != Value.Type.MATRIX) {
        throw new RuntimeException("matrix.print_sample necesita MATRIX.");
    }

    if (rowsValue.type != Value.Type.INT || colsValue.type != Value.Type.INT) {
        throw new RuntimeException("matrix.print_sample necesita rows y cols INT.");
    }

    MatrixValue matrix = (MatrixValue) matrixValue.value;

    int sampleRows = (int) rowsValue.value;
    int sampleCols = (int) colsValue.value;

    if (sampleRows > matrix.rows) {
        sampleRows = matrix.rows;
    }

    if (sampleCols > matrix.cols) {
        sampleCols = matrix.cols;
    }

    for (int row = 0; row < sampleRows; row++) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        for (int col = 0; col < sampleCols; col++) {
            builder.append(matrix.get(row, col));

            if (col < sampleCols - 1) {
                builder.append(", ");
            }
        }

        if (sampleCols < matrix.cols) {
            builder.append(", ...");
        }

        builder.append("]");

        System.out.println(builder.toString());
    }

    if (sampleRows < matrix.rows) {
        System.out.println("...");
    }

    return new Value(Value.Type.UNDEFINED, null);
}

private Value nativeJoin(List<Value> arguments) {
    if (arguments.size() != 1) {
        throw new RuntimeException("join() recibe exactamente 1 argumento.");
    }

    Value value = arguments.get(0);

    if (value.type != Value.Type.THREAD) {
        throw new RuntimeException("join() solo recibe THREAD.");
    }

    ThreadValue threadValue = (ThreadValue) value.value;

    if (!threadValue.started || threadValue.thread == null) {
        throw new RuntimeException("No puedes hacer join() de un hilo que no ha iniciado.");
    }

    try {
        threadValue.thread.join();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("El hilo fue interrumpido.");
    }

    return new Value(Value.Type.UNDEFINED, null);
}

private Value nativeSleep(List<Value> arguments) {
    if (arguments.size() != 1) {
        throw new RuntimeException("sleep() recibe exactamente 1 argumento en milisegundos.");
    }

    Value milliseconds = arguments.get(0);

    if (milliseconds.type != Value.Type.INT) {
        throw new RuntimeException("sleep() necesita un INT.");
    }

    try {
        Thread.sleep((int) milliseconds.value);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("sleep() fue interrumpido.");
    }

    return new Value(Value.Type.UNDEFINED, null);
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

    if (object.type == Value.Type.MODULE) {
        ModuleValue module = (ModuleValue) object.value;
        return callModuleMethod(module.name, methodName, arguments);
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