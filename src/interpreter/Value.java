package interpreter;

public class Value {

    public enum Type {
        INT,
        FLOAT,
        STRING,
        BOOLEAN
    }

    public final Type type;
    public final Object value;

    public Value(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}