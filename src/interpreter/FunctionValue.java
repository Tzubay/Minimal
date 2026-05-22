package interpreter;

import ast.Stmt;

import java.util.List;

public class FunctionValue {
    public final String name;
    public final List<String> params;
    public final List<Stmt> body;

    public FunctionValue(String name, List<String> params, List<Stmt> body) {
        this.name = name;
        this.params = params;
        this.body = body;
    }
}