package nalgae.cli;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import nalgae.ast.Program;
import nalgae.compiler.CompiledProgram;
import nalgae.compiler.ProgramCompiler;
import nalgae.parser.Lexer;
import nalgae.parser.Parser;

public final class NalgaeCli {
    private NalgaeCli() {}

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: nalgae <source-file> [input]");
            return;
        }

        Path sourcePath = Path.of(args[0]);
        if (!Files.exists(sourcePath)) {
            System.err.println("Source file not found: " + sourcePath);
            return;
        }

        String input = args.length >= 2 ? args[1] : "";
        ensureAscii(input);

        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        try {
            Program program = parse(source);
            String className = buildClassName(sourcePath);
            ProgramCompiler compiler = new ProgramCompiler(program, className);
            CompiledProgram compiled = compiler.compile();
            String result = execute(compiled, input);
            System.out.println(result);
        } catch (Exception ex) {
            System.err.println("Compilation failed: " + ex.getMessage());
        }
    }

    private static Program parse(String source) {
        Lexer lexer = new Lexer(source);
        var tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        return parser.parseProgram();
    }

    private static String buildClassName(Path path) {
        String base = path.getFileName().toString();
        int dot = base.lastIndexOf('.');
        if (dot >= 0) {
            base = base.substring(0, dot);
        }
        if (base.isEmpty()) {
            base = "Program";
        }
        StringBuilder sanitized = new StringBuilder();
        if (!Character.isJavaIdentifierStart(base.charAt(0))) {
            sanitized.append('P');
        }
        for (int i = 0; i < base.length(); i++) {
            char ch = base.charAt(i);
            sanitized.append(Character.isJavaIdentifierPart(ch) ? ch : '_');
        }
        String suffix = Long.toHexString(System.currentTimeMillis());
        return "nalgae.generated." + sanitized + "_" + suffix;
    }

    private static String execute(CompiledProgram compiled, String input) throws Exception {
        URL classesUrl = compiled.classesDirectory().toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[] { classesUrl }, NalgaeCli.class.getClassLoader())) {
            Class<?> clazz = Class.forName(compiled.className(), true, loader);
            Method run = clazz.getMethod("run", String.class);
            Object result = run.invoke(null, input);
            return (String) result;
        }
    }

    private static void ensureAscii(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int code = ch;
            if (!(code == 0x09 || code == 0x0A || code == 0x0D || (code >= 0x20 && code <= 0x7E))) {
                throw new IllegalArgumentException("Non-ASCII input character: '" + ch + "'");
            }
        }
    }
}
