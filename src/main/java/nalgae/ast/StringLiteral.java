package nalgae.ast;

public record StringLiteral(String value, int line) implements Term {}
