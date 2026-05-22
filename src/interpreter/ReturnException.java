package interpreter;

public class ReturnException extends RuntimeException {
    public final Value value;

    public ReturnException(Value value) {
        super(null, null, false, false);
        this.value = value;
    }
}