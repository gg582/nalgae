package nalgae.runtime;

public final class Builtins {
    private Builtins() {}

    private static String ensureAscii(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            int code = ch;
            if (!(code == 0x09 || code == 0x0A || code == 0x0D || (code >= 0x20 && code <= 0x7E))) {
                throw new IllegalArgumentException("Non-ASCII character detected: '" + ch + "'");
            }
        }
        return value;
    }

    public static String id(String input) {
        return ensureAscii(input);
    }

    public static String upper(String input) {
        return ensureAscii(input.toUpperCase());
    }

    public static String lower(String input) {
        return ensureAscii(input.toLowerCase());
    }

    public static String trim(String input) {
        return ensureAscii(input.trim());
    }

    public static String append(String input, String suffix) {
        return ensureAscii(ensureAscii(input) + ensureAscii(suffix));
    }

    public static String prepend(String input, String prefix) {
        return ensureAscii(ensureAscii(prefix) + ensureAscii(input));
    }

    public static String replace(String input, String target, String replacement) {
        return ensureAscii(ensureAscii(input).replace(ensureAscii(target), ensureAscii(replacement)));
    }

    public static String indent(String input, String prefix) {
        String validatedPrefix = ensureAscii(prefix);
        String[] lines = ensureAscii(input).split("\\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(validatedPrefix).append(lines[i]);
        }
        return ensureAscii(sb.toString());
    }

    public static String trimIndent(String input) {
        String[] lines = ensureAscii(input).split("\\n", -1);
        int start = 0;
        int end = lines.length - 1;
        while (start <= end && lines[start].trim().isEmpty()) {
            start++;
        }
        while (end >= start && lines[end].trim().isEmpty()) {
            end--;
        }
        if (start > end) {
            return "";
        }
        int indent = Integer.MAX_VALUE;
        for (int i = start; i <= end; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                continue;
            }
            int count = 0;
            while (count < line.length()) {
                char ch = line.charAt(count);
                if (ch == ' ' || ch == '\t') {
                    count++;
                } else {
                    break;
                }
            }
            indent = Math.min(indent, count);
        }
        if (indent == Integer.MAX_VALUE) {
            indent = 0;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; i++) {
            if (i > start) {
                sb.append('\n');
            }
            String line = lines[i];
            int cut = Math.min(indent, line.length());
            sb.append(line.substring(cut));
        }
        return ensureAscii(sb.toString());
    }

    public static String wrap(String input, String prefix, String suffix) {
        return ensureAscii(ensureAscii(prefix) + ensureAscii(input) + ensureAscii(suffix));
    }

    public static String constValue(String value) {
        return ensureAscii(value);
    }
}
