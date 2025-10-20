package nalgae.ast;

import java.util.List;

public record Pipeline(List<Term> terms, int line) implements Expression {}
