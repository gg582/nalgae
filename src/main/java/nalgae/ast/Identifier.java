package nalgae.ast;

public record Identifier(String name, int line) implements Term {}
