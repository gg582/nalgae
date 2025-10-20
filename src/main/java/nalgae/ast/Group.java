package nalgae.ast;

import java.util.List;

public record Group(List<Expression> expressions, int line) implements Term {}
