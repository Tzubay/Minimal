package ast;

import java.util.List;


public abstract class Stmt {


public static class SwitchCase {
    public final Expr value;
    public final List<Stmt> statements;

    public SwitchCase(Expr value, List<Stmt> statements) {
        this.value = value;
        this.statements = statements;
    }
}

    public static class Let extends Stmt {
        public final String name;
        public final Expr value;

        public Let(String name, Expr value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class Import extends Stmt {
        public final String moduleName;

        public Import(String moduleName) {
            this.moduleName = moduleName;
        }
    }
    
    public static class Expression extends Stmt {
        public final Expr expression;

        public Expression(Expr expression) {
            this.expression = expression;
        }
    }

    public static class Function extends Stmt {
        public final String name;
        public final List<String> params;
        public final List<Stmt> body;

        public Function(String name, List<String> params, List<Stmt> body) {
            this.name = name;
            this.params = params;
            this.body = body;
        }
    }

    public static class Return extends Stmt {
        public final List<Expr> values;

        public Return(List<Expr> values) {
            this.values = values;
        }
    }

    public static class Assign extends Stmt {
        public final String name;
        public final Expr value;

        public Assign(String name, Expr value) {
            this.name = name;
            this.value = value;
        }
    }

public static class IndexAssign extends Stmt {
    public final Expr array;
    public final Expr index;
    public final Expr value;

    public IndexAssign(Expr array, Expr index, Expr value) {
        this.array = array;
        this.index = index;
        this.value = value;
    }
}

    public static class Print extends Stmt {
        public final List<Expr> expressions;

        public Print(List<Expr> expressions) {
            this.expressions = expressions;
        }
    }
    public static class Switch extends Stmt {
        public final Expr condition;
        public final List<SwitchCase> cases;
        public final List<Stmt> defaultStatements;

        public Switch(Expr condition, List<SwitchCase> cases, List<Stmt> defaultStatements) {
            this.condition = condition;
            this.cases = cases;
            this.defaultStatements = defaultStatements;
        }
    }

    public static class Block extends Stmt {
        public final List<Stmt> statements;

        public Block(List<Stmt> statements) {
            this.statements = statements;
        }
    }
    public static class Break extends Stmt {
        public Break() {}
    }

    public static class If extends Stmt {
        public final Expr condition;
        public final Stmt thenBranch;
        public final Stmt elseBranch;

        public If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }
    }

    public static class While extends Stmt {
        public final Expr condition;
        public final Stmt body;

        public While(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }
    }
}