package interpreter;

import java.util.List;

public class ThreadValue {
    public final FunctionValue function;
    public final List<Value> arguments;

    public Thread thread;
    public boolean started = false;

    public ThreadValue(FunctionValue function, List<Value> arguments) {
        this.function = function;
        this.arguments = arguments;
    }
}