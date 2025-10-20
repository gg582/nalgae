package nalgae.parser;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {
    private final String source;
    private int position = 0;
    private int line = 1;

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (!isAtEnd()) {
            skipWhitespace();
            if (isAtEnd()) {
                break;
            }
            int start = position;
            int tokenLine = line;
            char ch = advance();
            switch (ch) {
                case '"' -> {
                    if (!isAtEnd() && peek() == '"' && peekNext() == '"') {
                        advance();
                        advance();
                        tokens.add(readMultilineString(tokenLine));
                    } else {
                        tokens.add(readString('"', tokenLine));
                    }
                }
                case '\'' -> tokens.add(readString('\'', tokenLine));
                case '(' -> tokens.add(new Token(TokenType.LPAREN, "(", line));
                case ')' -> tokens.add(new Token(TokenType.RPAREN, ")", line));
                case '=' -> tokens.add(new Token(TokenType.EQUAL, "=", line));
                case '{' -> tokens.add(new Token(TokenType.LBRACE, "{", line));
                case '}' -> tokens.add(new Token(TokenType.RBRACE, "}", line));
                case '|' -> tokens.add(new Token(TokenType.PIPE, "|", line));
                case ';' -> tokens.add(new Token(TokenType.SEMICOLON, ";", line));
                case '#' -> skipComment();
                default -> {
                    if (isIdentifierStart(ch)) {
                        String ident = readIdentifier(start);
                        TokenType type = ident.equals("def") ? TokenType.DEF : TokenType.IDENTIFIER;
                        tokens.add(new Token(type, ident, tokenLine));
                    } else {
                        throw error("Unexpected character '" + ch + "'");
                    }
                }
            }
        }
        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }

    private void skipWhitespace() {
        while (!isAtEnd()) {
            char ch = peek();
            if (ch == ' ' || ch == '\t' || ch == '\r') {
                advance();
            } else if (ch == '\n') {
                advance();
                line++;
            } else {
                break;
            }
        }
    }

    private void skipComment() {
        while (!isAtEnd() && peek() != '\n') {
            advance();
        }
    }

    private Token readString(char quote, int startLine) {
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd()) {
            char ch = advance();
            if (ch == quote) {
                return new Token(TokenType.STRING, sb.toString(), startLine);
            }
            if (ch == '\n') {
                throw error("Unterminated string literal");
            }
            if (!isAsciiAllowed(ch)) {
                throw error("Non-ASCII character in string literal");
            }
            sb.append(ch);
        }
        throw error("Unterminated string literal");
    }

    private Token readMultilineString(int startLine) {
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd()) {
            char ch = advance();
            if (ch == '"' && match('"') && match('"')) {
                return new Token(TokenType.STRING, sb.toString(), startLine);
            }
            if (!isAsciiAllowed(ch)) {
                throw error("Non-ASCII character in string literal");
            }
            if (ch == '\n') {
                sb.append('\n');
                line++;
            } else {
                sb.append(ch);
            }
        }
        throw error("Unterminated multi-line string literal");
    }

    private String readIdentifier(int start) {
        while (!isAtEnd() && isIdentifierPart(peek())) {
            advance();
        }
        return source.substring(start, position);
    }

    private boolean isIdentifierStart(char ch) {
        return ch == '_' || Character.isLetter(ch);
    }

    private boolean isIdentifierPart(char ch) {
        return isIdentifierStart(ch) || Character.isDigit(ch);
    }

    private boolean isAsciiAllowed(char ch) {
        int code = ch;
        return code == 0x09 || code == 0x0A || code == 0x0D || (code >= 0x20 && code <= 0x7E);
    }

    private boolean match(char expected) {
        if (isAtEnd()) {
            return false;
        }
        if (source.charAt(position) != expected) {
            return false;
        }
        position++;
        return true;
    }

    private char peekNext() {
        if (position + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(position + 1);
    }

    private boolean isAtEnd() {
        return position >= source.length();
    }

    private char advance() {
        return source.charAt(position++);
    }

    private char peek() {
        return source.charAt(position);
    }

    private RuntimeException error(String message) {
        return new RuntimeException("[line " + line + "] " + message);
    }
}
