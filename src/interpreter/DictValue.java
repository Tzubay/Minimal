package interpreter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DictValue {
    public final Map<String, Value> values = new LinkedHashMap<>();

    @Override
    public String toString() {
        return "{" + values.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", ")) + "}";
    }
}