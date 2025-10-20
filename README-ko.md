# nalgae

`nalgae`는 nalgae 언어 소스 코드를 JVM 바이트코드로 컴파일한 뒤 즉시 실행해 주는 CLI 도구입니다. 파이프라인, 멀티라인 문자열, 들여쓰기 도우미를 활용해 QML 같은 구조화된 텍스트를 손쉽게 생성할 수 있도록 언어가 확장되었습니다.

## 특징

* 모든 프로그램은 ASCII 문자열 파이프라인으로 구성되며, Java `Function<String, String>` 구현으로 컴파일됩니다.
* `trim_indent`와 함께 사용하는 삼중 따옴표(`"""`) 멀티라인 문자열을 지원합니다.
* 블록 표현식(`{ ... }`)이 하위 파이프라인을 자동으로 줄바꿈하며 연결해 템플릿 문서를 만들기 좋습니다.
* `indent`, `wrap` 등 들여쓰기/레이아웃 헬퍼로 QML, HTML 등을 nalgae 코드에서 직접 생성할 수 있습니다.

## 빌드

Gradle 8.5 이상을 로컬에 설치하고 이를 이용해 빌드하세요. (바이너리 제약으로 래퍼 JAR은 저장소에 포함되지 않습니다.)

```bash
gradle build
```

## CLI 실행

nalgae 프로그램 파일을 지정하면 JVM 클래스로 컴파일한 후 `run` 메서드를 호출하고, 결과 문자열을 표준 출력으로 돌려줍니다.

```bash
gradle run --args "examples/hello.nal '  hello daegu  '"
```

배포판을 설치한 다음 생성된 실행 스크립트를 사용할 수도 있습니다.

```bash
gradle installDist
./build/install/nalgae/bin/nalgae examples/hello.nal "busan"
```

## Qt Quick 대시보드 샘플

`examples/qt_dashboard.nal`은 확장된 언어 기능을 사용해 Qt Quick 대시보드를 조립하는 nalgae 프로그램입니다. CLI 결과를 파일로 리다이렉트한 뒤, 로컬에 Qt가 설치돼 있다면 `qmlscene`으로 열어 볼 수 있습니다.

```bash
gradle run --args examples/qt_dashboard.nal > build/qt_dashboard.qml
# Qt가 설치된 경우:
# qmlscene build/qt_dashboard.qml
```

저장소에는 Qt 바이너리가 포함되지 않으니, 실행하려면 별도로 Qt를 설치해야 합니다.

## 언어 스니펫

```nalgae
# 들여쓰기를 제거하는 멀티라인 문자열
"""
    Button {
        text: "Launch"
    }
""" | trim_indent

# 블록 표현식은 줄바꿈을 자동으로 삽입합니다.
{
  "Section A"
  "Section B" | indent "    "
}

# 헬퍼 함수 조합
def button(label) = {
  "Button {"
  label
    | wrap '"' '"'
    | prepend "    text: "
  "    Layout.fillWidth: true"
  "}"
}
```

모든 nalgae 프로그램은 ASCII 문자열만 다루며, 런타임에서 비ASCII 문자를 감지하면 예외를 발생시킵니다.
