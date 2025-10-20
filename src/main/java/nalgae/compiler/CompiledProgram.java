package nalgae.compiler;

import java.nio.file.Path;

public record CompiledProgram(String className, byte[] bytecode, Path classFile, Path classesDirectory) {}
