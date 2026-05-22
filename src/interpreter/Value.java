package interpreter;

import java.util.List;
import java.util.stream.Collectors;

public class Value {

    public enum Type {
        INT,
        FLOAT,
        STRING,
        BOOLEAN,
        ARRAY
    }

    public final Type type;
    public final Object value;

    public Value(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        if (type == Type.ARRAY) {
            List<Value> values = (List<Value>) value;

            return "[" + values.stream()
                    .map(Value::toString)
                    .collect(Collectors.joining(", ")) + "]";
        }

        return value.toString();
    }
}