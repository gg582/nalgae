package nalgae.parser;

import java.util.ArrayList;
import java.util.List;
import nalgae.ast.*;

public final class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Program parseProgram() {
        List<Definition> definitions = new ArrayList<>();
        while (match(TokenType.DEF)) {
            definitions.add(parseDefinition());
        }
        Expression expression = parseExpression();
        consume(TokenType.EOF, "Expected end of input");
        return new Program(definitions, expression);
    }

    private Definition parseDefinition() {
        Token name = consume(TokenType.IDENTIFIER, "Expected function name");
        consume(TokenType.LPAREN, "Expected '('");
        Token param = consume(TokenType.IDENTIFIER, "Expected parameter name");
        consume(TokenType.RPAREN, "Expected ')'");
        Token eq = consume(TokenType.EQUAL, "Expected '='");
        Expression body = parseExpression();
        return new Definition(name.lexeme, param.lexeme, body, eq.line);
    }

    private Expression parseExpression() {
        return parsePipeline();
    }

    private Expression parsePipeline() {
        Term first = parseTerm();
        List<Term> terms = new ArrayList<>();
        terms.add(first);
        int line = first.line();
        while (match(TokenType.PIPE)) {
            terms.add(parseTerm());
        }
        return new Pipeline(terms, line);
    }

    private Term parseTerm() {
        if (match(TokenType.LBRACE)) {
            List<Expression> expressions = new ArrayList<>();
            if (!check(TokenType.RBRACE)) {
                while (true) {
                    expressions.add(parseExpression());
                    if (match(TokenType.SEMICOLON)) {
                        continue;
                    }
                    if (check(TokenType.RBRACE)) {
                        break;
                    }
                }
            }
            Token brace = consume(TokenType.RBRACE, "Expected '}'");
            return new Group(expressions, brace.line);
        }

        Token token = advance();
        return switch (token.type) {
            case IDENTIFIER -> {
                List<Term> arguments = new ArrayList<>();
                int callLine = token.line;
                while (true) {
                    if (isAtEnd()) {
                        break;
                    }
                    if (peek().line > callLine) {
                        break;
                    }
                    if (check(TokenType.PIPE) || check(TokenType.RBRACE) || check(TokenType.SEMICOLON) || check(TokenType.EOF)) {
                        break;
                    }
                    if (check(TokenType.IDENTIFIER) || check(TokenType.STRING) || check(TokenType.LBRACE)) {
                        arguments.add(parseTerm());
                        continue;
                    }
                    break;
                }
                if (arguments.isEmpty()) {
                    yield new Identifier(token.lexeme, token.line);
                }
                yield new Call(token.lexeme, arguments, token.line);
            }
            case STRING -> new StringLiteral(token.lexeme, token.line);
            case LPAREN -> throw error(token, "Parenthesised expressions are not supported");
            default -> throw error(token, "Unexpected token '" + token.lexeme + "'");
        };
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return type == TokenType.EOF;
        }
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return current >= tokens.size() || tokens.get(current).type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private RuntimeException error(Token token, String message) {
        String location = token.type == TokenType.EOF ? "end" : "'" + token.lexeme + "'";
        return new RuntimeException("[line " + token.line + "] Error at " + location + ": " + message);
    }
}
