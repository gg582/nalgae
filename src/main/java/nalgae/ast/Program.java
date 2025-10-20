package nalgae.ast;

import java.util.List;

public record Program(List<Definition> definitions, Expression expression) {}
