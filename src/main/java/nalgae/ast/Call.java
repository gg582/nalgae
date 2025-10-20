package nalgae.ast;

import java.util.List;

public record Call(String target, List<Term> arguments, int line) implements Term {}
