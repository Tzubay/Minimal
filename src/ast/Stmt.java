package ast;
public abstract class Stmt {

    public static class Let extends Stmt {
        public final String name;
        public final Expr value;

        public Let(String name, Expr value) {
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
}