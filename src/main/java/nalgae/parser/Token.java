package nalgae.parser;

final class Token {
    final TokenType type;
    final String lexeme;
    final int line;

    Token(TokenType type, String lexeme, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
    }
}
