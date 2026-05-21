import lexer.*;
import parser.*;
import ast.*;
import interpreter.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Uso: minimal archivo.min");
            return;
        }

        String code = Files.readString(Paths.get(args[0]));

        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.scanTokens();

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        Interpreter interpreter = new Interpreter();
        interpreter.interpret(statements);
    }
}