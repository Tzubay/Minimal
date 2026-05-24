package interpreter;

public class ResponseValue {
    public final int statusCode;
    public final String text;

    public ResponseValue(int statusCode, String text) {
        this.statusCode = statusCode;
        this.text = text;
    }
}