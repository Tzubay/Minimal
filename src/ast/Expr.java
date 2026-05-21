package ast;
import lexer.*;

public abstract class Expr {

    public static class Number extends Expr {
        public final int value;

        public Number(int value) {
            this.value = value;
        }
    }

    public static class Variable extends Expr {
        public final String name;

        public Variable(String name) {
            this.name = name;
        }
    }

    public static class Binary extends Expr {
        public final Expr left;
        public final Token operator;
        public final Expr right;

        public Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
    }
}