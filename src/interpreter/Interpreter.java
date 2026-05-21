package interpreter;
import ast.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter {
    private final Map<String, Integer> variables = new HashMap<>();

    public void interpret(List<Stmt> statements) {
        for (Stmt stmt : statements) {
            execute(stmt);
        }
    }

    private void execute(Stmt stmt) {
        if (stmt instanceof Stmt.Let) {
            Stmt.Let letStmt = (Stmt.Let) stmt;
            int value = evaluate(letStmt.value);
            variables.put(letStmt.name, value);
        } 
        else if (stmt instanceof Stmt.Print) {
            Stmt.Print printStmt = (Stmt.Print) stmt;
            int value = evaluate(printStmt.expression);
            System.out.println(value);
        }
    }

    private int evaluate(Expr expr) {
        if (expr instanceof Expr.Number) {
            return ((Expr.Number) expr).value;
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

            int left = evaluate(binary.left);
            int right = evaluate(binary.right);

            switch (binary.operator.type) {
                case PLUS:
                    return left + right;
                case MINUS:
                    return left - right;
                case STAR:
                    return left * right;
                case SLASH:
                    return left / right;
                default:
                    throw new RuntimeException("Operador no válido.");
            }
        }

        throw new RuntimeException("Expresión no válida.");
    }
}