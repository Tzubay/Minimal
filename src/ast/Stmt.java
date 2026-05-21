package ast;

import java.util.List;

public abstract class Stmt {

    public static class Let extends Stmt {
        public final String name;
        public final Expr value;

        public Let(String name, Expr value) {
            this.name = name;
            this.value = value;
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

    public static class Print extends Stmt {
        public final Expr expression;

        public Print(Expr expression) {
            this.expression = expression;
        }
    }

    public static class Block extends Stmt {
        public final List<Stmt> statements;

        public Block(List<Stmt> statements) {
            this.statements = statements;
        }
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