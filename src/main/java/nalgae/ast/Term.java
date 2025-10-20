package nalgae.ast;

public sealed interface Term permits Identifier, StringLiteral, Call, Group {
    int line();
}
