package nalgae.compiler;

import java.util.ArrayList;
import java.util.List;

final class CodeBuilder {
    private final List<String> statements = new ArrayList<>();
    private int tempCounter = 0;

    String newTemp(String expression) {
        return newTypedTemp("String", expression);
    }

    String newTypedTemp(String type, String expression) {
        String name = "tmp" + tempCounter++;
        statements.add(type + " " + name + " = " + expression + ";");
        return name;
    }

    void add(String statement) {
        statements.add(statement);
    }

    void appendTo(StringBuilder sb, int indentLevel) {
        String indent = "    ".repeat(indentLevel);
        for (String statement : statements) {
            sb.append(indent).append(statement).append('\n');
        }
    }
}
