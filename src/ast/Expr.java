package ast;

import lexer.Token;
import java.util.List;

public abstract class Expr {

    public static class Number extends Expr {
        public final int value;

        public Number(int value) {
            this.value = value;
        }
    }

    public static class FloatExpr extends Expr {
        public final double value;

        public FloatExpr(double value) {
            this.value = value;
        }
    }

    public static class StringExpr extends Expr {
        public final String value;

        public StringExpr(String value) {
            this.value = value;
        }
    }

    public static class BooleanExpr extends Expr {
        public final boolean value;

        public BooleanExpr(boolean value) {
            this.value = value;
        }
    }

    public static class ArrayExpr extends Expr {
        public final List<Expr> elements;

        public ArrayExpr(List<Expr> elements) {
            this.elements = elements;
        }
    }

    public static class Index extends Expr {
        public final Expr array;
        public final Expr index;

        public Index(Expr array, Expr index) {
            this.array = array;
            this.index = index;
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