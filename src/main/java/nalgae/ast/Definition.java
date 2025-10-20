package nalgae.ast;

public record Definition(String name, String parameter, Expression body, int line) {}
