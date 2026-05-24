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

    public static class MethodCall extends Expr {
        public final Expr object;
        public final String methodName;
        public final List<Expr> arguments;

        public MethodCall(Expr object, String methodName, List<Expr> arguments) {
            this.object = object;
            this.methodName = methodName;
            this.arguments = arguments;
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

    public static class DictExpr extends Expr {
        public final List<String> keys;
        public final List<Expr> values;

        public DictExpr(List<String> keys, List<Expr> values) {
            this.keys = keys;
            this.values = values;
        }
    }

    public static class Get extends Expr {
        public final Expr object;
        public final String name;

        public Get(Expr object, String name) {
            this.object = object;
            this.name = name;
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
    public static class Index extends Expr {
        public final Expr array;
        public final Expr index;

        public Index(Expr array, Expr index) {
            this.array = array;
            this.index = index;
        }
    }

    public static class Call extends Expr {
        public final Expr callee;
        public final List<Expr> arguments;

        public Call(Expr callee, List<Expr> arguments) {
            this.callee = callee;
            this.arguments = arguments;
        }
    }
}