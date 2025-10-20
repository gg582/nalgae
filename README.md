# nalgae

`nalgae` compiles nalgae language sources into JVM bytecode and executes the resulting program immediately through a CLI tool. The language now supports authoring structured text such as QML layouts by combining pipelines, multi-line string blocks, and layout-aware helpers.

## Highlights

* Pure string pipelines that compile to Java bytecode and run as `Function<String, String>` implementations.
* Triple quoted multi-line string literals with `trim_indent` for indentation-aware authoring.
* Block expressions (`{ ... }`) that concatenate nested pipelines with automatic newlines, ideal for generating templated documents.
* Formatting helpers like `indent` and `wrap` that make it easier to emit QML, HTML, or other structured text from nalgae code.

## Build

Install Gradle 8.5+ locally and run the build with it (the wrapper JAR is not checked into this repository).

```bash
gradle build
```

## Run the CLI

Provide a nalgae program file and `nalgae` will compile it to a JVM class, invoke the generated `run` method, and print the resulting string to standard output.

```bash
gradle run --args "examples/hello.nal '  hello daegu  '"
```

You can also install the distribution and run the generated script.

```bash
gradle installDist
./build/install/nalgae/bin/nalgae examples/hello.nal "busan"
```

## Sample Qt Quick dashboard

The repository includes `examples/qt_dashboard.nal`, a nalgae program that assembles a small Qt Quick dashboard using the new language features. The CLI prints the generated QML; redirect it to a file and open it with `qmlscene` (from a local Qt installation) to see the interface.

```bash
gradle run --args examples/qt_dashboard.nal > build/qt_dashboard.qml
# Then, if Qt is available on your machine:
# qmlscene build/qt_dashboard.qml
```

No Qt binaries are bundled—install Qt separately to run the dashboard.

## Language snippets

```nalgae
# Multi-line literals with indentation trimming
"""
    Button {
        text: "Launch"
    }
""" | trim_indent

# Group expressions append newline-separated sections
{
  "Section A"
  "Section B" | indent "    "
}

# Compose helper functions
def button(label) = {
  "Button {"
  label
    | wrap '"' '"'
    | prepend "    text: "
  "    Layout.fillWidth: true"
  "}"
}
```

All nalgae programs operate on ASCII strings; the runtime enforces this and will reject non-ASCII output.
