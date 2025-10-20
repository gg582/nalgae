package nalgae.compiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import nalgae.ast.*;

public final class ProgramCompiler {
    private final Program program;
    private final String className;

    public ProgramCompiler(Program program, String className) {
        this.program = program;
        this.className = className;
    }

    public CompiledProgram compile() {
        try {
            SourceLayout layout = buildSource();
            Path sourceFile = layout.sourceFile();
            Files.createDirectories(sourceFile.getParent());
            Files.writeString(sourceFile, layout.source(), StandardCharsets.UTF_8);

            Path classesDir = layout.classesDir();
            Files.createDirectories(classesDir);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException("JDK tools are required to compile");
            }
            List<String> options = new ArrayList<>();
            String classpath = System.getProperty("java.class.path");
            if (classpath != null && !classpath.isEmpty()) {
                options.add("-classpath");
                options.add(classpath);
            }
            options.add("-d");
            options.add(classesDir.toString());
            options.add(sourceFile.toString());
            int result = compiler.run(null, null, null, options.toArray(new String[0]));
            if (result != 0) {
                throw new IllegalStateException("Java compilation failed with exit code " + result);
            }

            Path classFile = classesDir.resolve(layout.classRelativePath());
            byte[] bytecode = Files.readAllBytes(classFile);
            return new CompiledProgram(className, bytecode, classFile, classesDir);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to compile program: " + ex.getMessage(), ex);
        }
    }

    private SourceLayout buildSource() {
        int dot = className.lastIndexOf('.');
        String packageName = dot >= 0 ? className.substring(0, dot) : null;
        String simpleName = dot >= 0 ? className.substring(dot + 1) : className;
        StringBuilder sb = new StringBuilder();
        if (packageName != null && !packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }
        sb.append("import nalgae.runtime.Builtins;\n");
        sb.append("import java.util.function.Function;\n\n");
        sb.append("public final class ").append(simpleName).append(" implements Function<String, String> {\n");

        Map<String, Definition> definitionMap = new HashMap<>();
        for (Definition def : program.definitions()) {
            definitionMap.put(def.name(), def);
        }

        for (Definition definition : program.definitions()) {
            sb.append("    private static String ")
                .append(fnName(definition.name()))
                .append("(String ")
                .append(definition.parameter())
                .append(") {\n");
            CodeBuilder builder = new CodeBuilder();
            Scope scope = new Scope(definition.parameter(), definition.parameter(), definitionMap);
            String resultVar = compileExpression(definition.body(), scope, builder);
            builder.appendTo(sb, 2);
            sb.append("        return ").append(resultVar).append(";\n");
            sb.append("    }\n\n");
        }

        sb.append("    public static String run(String input) {\n");
        CodeBuilder topBuilder = new CodeBuilder();
        Scope topScope = new Scope("it", "input", definitionMap);
        String topResult = compileExpression(program.expression(), topScope, topBuilder);
        topBuilder.appendTo(sb, 2);
        sb.append("        return ").append(topResult).append(";\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n    public String apply(String input) {\n        return run(input);\n    }\n\n");

        sb.append("    public static void main(String[] args) {\n");
        sb.append("        String input = args.length > 0 ? args[0] : \"\";\n");
        sb.append("        String result = run(input);\n");
        sb.append("        System.out.println(result);\n");
        sb.append("    }\n");

        sb.append("}\n");

        Path sourceDir = Path.of("build", "nalgae", "sources");
        String relativePath = className.replace('.', '/') + ".java";
        Path sourceFile = sourceDir.resolve(relativePath);
        Path classesDir = Path.of("build", "nalgae", "classes");
        String classRelativePath = className.replace('.', '/') + ".class";
        return new SourceLayout(sb.toString(), sourceFile, classesDir, Path.of(classRelativePath));
    }

    private String compileExpression(Expression expression, Scope scope, CodeBuilder builder) {
        if (expression instanceof Pipeline pipeline) {
            return compilePipeline(pipeline, scope, builder);
        }
        throw new IllegalStateException("Unsupported expression type: " + expression.getClass());
    }

    private String compilePipeline(Pipeline pipeline, Scope scope, CodeBuilder builder) {
        if (pipeline.terms().isEmpty()) {
            throw new IllegalArgumentException("Empty pipeline at line " + pipeline.line());
        }
        String current = compileValueTerm(pipeline.terms().get(0), scope, builder);
        for (int i = 1; i < pipeline.terms().size(); i++) {
            current = compilePipelineStep(pipeline.terms().get(i), scope, builder, current);
        }
        return current;
    }

    private String compileValueTerm(Term term, Scope scope, CodeBuilder builder) {
        if (term instanceof Identifier identifier) {
            if (identifier.name().equals(scope.parameterName())) {
                return scope.parameterVariable();
            }
            throw error(term.line(), "Unknown identifier '" + identifier.name() + "' in value context");
        }
        if (term instanceof StringLiteral literal) {
            return builder.newTemp(quote(literal.value()));
        }
        if (term instanceof Call call) {
            return compileValueCall(call, scope, builder);
        }
        if (term instanceof Group) {
            return compileGroup((Group) term, scope, builder, null);
        }
        throw new IllegalStateException("Unhandled term: " + term);
    }

    private String compileValueCall(Call call, Scope scope, CodeBuilder builder) {
        BuiltinRegistry.Descriptor builtin = BuiltinRegistry.find(call.target());
        if (builtin != null) {
            if (call.arguments().size() != builtin.argCount + 1) {
                throw error(call.line(), "Builtin '" + call.target() + "' expects " + (builtin.argCount + 1) + " arguments in value context");
            }
            List<String> args = new ArrayList<>();
            for (Term arg : call.arguments()) {
                args.add(compileValueTerm(arg, scope, builder));
            }
            return builder.newTemp(buildCall("Builtins." + builtin.methodName, args));
        }
        Definition definition = scope.definitions().get(call.target());
        if (definition != null) {
            if (call.arguments().size() != 1) {
                throw error(call.line(), "Function '" + call.target() + "' expects exactly one argument");
            }
            String argument = compileValueTerm(call.arguments().get(0), scope, builder);
            return builder.newTemp(fnName(call.target()) + "(" + argument + ")");
        }
        throw error(call.line(), "Unknown function '" + call.target() + "'");
    }

    private String compilePipelineStep(Term term, Scope scope, CodeBuilder builder, String currentVar) {
        if (term instanceof Identifier identifier) {
            if (identifier.name().equals(scope.parameterName())) {
                throw error(term.line(), "Cannot call parameter '" + identifier.name() + "' as a function");
            }
            BuiltinRegistry.Descriptor builtin = BuiltinRegistry.find(identifier.name());
            if (builtin != null) {
                if (builtin.argCount != 0) {
                    throw error(term.line(), "Builtin '" + identifier.name() + "' requires arguments");
                }
                return builder.newTemp("Builtins." + builtin.methodName + "(" + currentVar + ")");
            }
            Definition definition = scope.definitions().get(identifier.name());
            if (definition != null) {
                return builder.newTemp(fnName(identifier.name()) + "(" + currentVar + ")");
            }
            throw error(term.line(), "Unknown function '" + identifier.name() + "'");
        }
        if (term instanceof StringLiteral literal) {
            return builder.newTemp("Builtins.constValue(" + quote(literal.value()) + ")");
        }
        if (term instanceof Call call) {
            return compilePipelineCall(call, scope, builder, currentVar);
        }
        if (term instanceof Group) {
            return compileGroup((Group) term, scope, builder, currentVar);
        }
        throw new IllegalStateException("Unhandled term: " + term);
    }

    private String compileGroup(Group group, Scope scope, CodeBuilder builder, String pipelineValue) {
        if (group.expressions().isEmpty()) {
            return builder.newTemp("\"\"");
        }
        Scope innerScope = pipelineValue == null
            ? scope
            : new Scope(scope.parameterName(), pipelineValue, scope.definitions());
        String sbVar = builder.newTypedTemp("StringBuilder", "new StringBuilder()");
        for (int i = 0; i < group.expressions().size(); i++) {
            Expression expression = group.expressions().get(i);
            String value = compileExpression(expression, innerScope, builder);
            builder.add(sbVar + ".append(" + value + ");");
            if (i < group.expressions().size() - 1) {
                builder.add(sbVar + ".append('\\n');");
            }
        }
        return builder.newTemp(sbVar + ".toString()");
    }

    private String compilePipelineCall(Call call, Scope scope, CodeBuilder builder, String currentVar) {
        BuiltinRegistry.Descriptor builtin = BuiltinRegistry.find(call.target());
        if (builtin != null) {
            if (call.arguments().size() != builtin.argCount) {
                throw error(call.line(), "Builtin '" + call.target() + "' expects " + builtin.argCount + " argument(s) in pipeline context");
            }
            List<String> args = new ArrayList<>();
            args.add(currentVar);
            for (Term arg : call.arguments()) {
                args.add(compileValueTerm(arg, scope, builder));
            }
            return builder.newTemp(buildCall("Builtins." + builtin.methodName, args));
        }
        Definition definition = scope.definitions().get(call.target());
        if (definition != null) {
            if (!call.arguments().isEmpty()) {
                throw error(call.line(), "User function '" + call.target() + "' does not accept additional arguments in pipeline");
            }
            return builder.newTemp(fnName(call.target()) + "(" + currentVar + ")");
        }
        throw error(call.line(), "Unknown function '" + call.target() + "'");
    }

    private static String buildCall(String target, List<String> arguments) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String arg : arguments) {
            joiner.add(arg);
        }
        return target + "(" + joiner + ")";
    }

    private static String quote(String value) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(ch);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private RuntimeException error(int line, String message) {
        return new RuntimeException("[line " + line + "] " + message);
    }

    private static String fnName(String name) {
        return "fn_" + name;
    }

    private record Scope(String parameterName, String parameterVariable, Map<String, Definition> definitions) {}

    private record SourceLayout(String source, Path sourceFile, Path classesDir, Path classRelativePath) {}
}
