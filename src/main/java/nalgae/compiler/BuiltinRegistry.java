package nalgae.compiler;

import java.util.HashMap;
import java.util.Map;

final class BuiltinRegistry {
    static final class Descriptor {
        final String name;
        final String methodName;
        final int argCount;

        Descriptor(String name, String methodName, int argCount) {
            this.name = name;
            this.methodName = methodName;
            this.argCount = argCount;
        }
    }

    private static final Map<String, Descriptor> BUILTINS = new HashMap<>();

    static {
        register("id", "id", 0);
        register("upper", "upper", 0);
        register("lower", "lower", 0);
        register("trim", "trim", 0);
        register("append", "append", 1);
        register("prepend", "prepend", 1);
        register("replace", "replace", 2);
        register("const", "constValue", 1);
        register("indent", "indent", 1);
        register("trim_indent", "trimIndent", 0);
        register("wrap", "wrap", 2);
    }

    private static void register(String name, String method, int argCount) {
        BUILTINS.put(name, new Descriptor(name, method, argCount));
    }

    static Descriptor find(String name) {
        return BUILTINS.get(name);
    }

    private BuiltinRegistry() {}
}
