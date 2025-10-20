이제 **AsciiPipe** 언어를 완전히 **순수 함수형 언어**로 리폼하여,
실제 **언어 사양 문서(설계 문서)** 형식으로 정리해드리겠습니다.

아래는 **언어 개요 → 철학 → 문법 → 타입 시스템 → 평가 규칙 → 함수 조합 모델 → 표준 함수 계획 → 실행 모델(바이트코드 포함)**
까지 포괄하는 **AsciiPipe Specification v0.1-draft** 입니다.

---

# 🧾 AsciiPipe — Functional String Composition Language

**Version:** 0.1 (Design Draft)
**Status:** Specification-level
**Target Runtime:** JVM (Bytecode compiled)
**Paradigm:** Pure functional, ASCII-only, compositional

---

## 1. 목적 및 철학

**AsciiPipe**는 “모든 것은 문자열”이라는 일관된 규칙 아래 설계된,
**순수 함수형·조합형 언어(Pure Compositional Language)** 이다.

이 언어는 다음을 목표로 한다:

* **직관적 파이프형 데이터 흐름** (`|`)
* **루아처럼 임베드 가능하지만**, 내부는 **순수 함수형**
* **ASCII 제한(0x09, 0x0A, 0x0D, 0x20~0x7E)** 으로 단일 문자셋 환경에 적합
* **모든 표현식은 문자열을 반환**
* **모든 함수는 Str → Str**
* **부작용(입출력, 상태 변경, 예외) 없음**

---

## 2. 핵심 개념 요약

| 개념            | 설명                                                    |                 |
| ------------- | ----------------------------------------------------- | --------------- |
| **단일 타입**     | `Str` (ASCII 문자열)                                     |                 |
| **합성 연산자**    | `                                                     | ` (파이프) = 함수 합성 |
| **함수 호출**     | `func arg1 arg2` (공백 구분)                              |                 |
| **함수 정의**     | `def name(x) = expr`                                  |                 |
| **블록(자식 함수)** | `{ expr₁ ; expr₂ ; … }`                               |                 |
| **부모 함수**     | `sequence`, `join`, `guard`, `first`, `map`, `fold` 등 |                 |
| **상수**        | `"abc"` 또는 `'abc'`                                    |                 |
| **식별자**       | `[A-Za-z_][A-Za-z0-9_]*`                              |                 |

---

## 3. 문법 (Grammar Spec)

### 3.1 Top-level

```
Program   ::= { Definition } Expression
Definition ::= "def" Identifier "(" Identifier ")" "=" Expression
Expression ::= Pipeline
Pipeline  ::= Term { "|" Term }
Term      ::= FunctionCall | Constant | Variable | Group
FunctionCall ::= Identifier { Argument }
Argument  ::= Term
Group     ::= "{" Expression { ";" Expression } "}"
Constant  ::= '"' AsciiText '"' | "'" AsciiText "'"
Variable  ::= Identifier
```

### 3.2 Lexical Rules

```
AsciiText ::= { ASCII_CHAR }
ASCII_CHAR ::= [\x09\x0A\x0D\x20-\x7E]
```

### 3.3 Comments

```
# comment until end of line
```

---

## 4. 타입 시스템

AsciiPipe의 모든 값은 **단일 타입 `Str`** 이며,
모든 함수는 다음 형태로 정의된다.

```
f : Str → Str
```

* **고차 함수**는 `(Str → Str) → Str` 또는 `(Str → Str) → (Str → Str)` 형태로 정의될 수 있다.
* **함수 합성**은 항상 `Str → Str`로 귀결되어야 한다.
* 비ASCII 값은 런타임 오류 (strict ASCII validation).

---

## 5. 평가 규칙 (Evaluation Semantics)

| 표현식               | 의미                         |                        |
| ----------------- | -------------------------- | ---------------------- |
| `"text"`          | 문자열 상수                     |                        |
| `a                | b`                         | 합성: `eval(b)(eval(a))` |
| `f x`             | 함수 호출: `apply(f, eval(x))` |                        |
| `{ e₁ ; e₂ ; … }` | 블록: 부모 함수에 의해 평가 규칙이 정의됨   |                        |
| `def f(x) = e`    | 이름 바인딩 (불변 함수)             |                        |

### 평가 전략

* **정적 스코프(static scoping)**
* **지연 평가 없음 (strict evaluation)**
* **함수는 참조투명성 보장**

### 결합법칙

```
(a | b) | c  ≡  a | (b | c)
```

---

## 6. 합성 모델

### 6.1 합성 연산자 (`|`)

* 의미: **함수 합성(function composition)**
* 우결합(Right-associative)
* 정의:

  ```
  eval(a | b) = b(eval(a))
  ```

### 6.2 함수 조합자 (Combinators)

| 이름                          | 타입                                        | 의미                          |
| --------------------------- | ----------------------------------------- | --------------------------- |
| `sequence{...}`             | `{Str→Str}ⁿ → Str`                        | 자식 표현식 순서대로 실행 후 결과 연결      |
| `join sep {...}`            | `Str × {Str→Str}ⁿ → Str`                  | 각 자식 결과를 sep으로 연결           |
| `first{...}`                | `{Str→Str}ⁿ → Str`                        | 첫 번째 비빈 문자열 반환              |
| `guard cond { then; else }` | `(Str→Str) × (Str→Str) × (Str→Str) → Str` | 조건에 따라 분기                   |
| `map sep{...}`              | `Str × (Str→Str) → Str`                   | 입력을 sep 단위로 분할, 각 요소에 함수 적용 |
| `fold sep{...}`             | `Str × (Str→Str) → Str`                   | 누적 fold                     |
| `const "s"`                 | `Str → Str`                               | 입력 무시, s 반환                 |
| `id`                        | `Str → Str`                               | 항등함수                        |

---

## 7. 표준 라이브러리 계획

### 7.1 문자열 함수

| 이름            | 시그니처      | 설명                         |
| ------------- | --------- | -------------------------- |
| `upper`       | `Str→Str` | 대문자 변환                     |
| `lower`       | `Str→Str` | 소문자 변환                     |
| `trim`        | `Str→Str` | 앞뒤 공백 제거                   |
| `append s`    | `Str→Str` | 뒤에 s 추가                    |
| `prepend s`   | `Str→Str` | 앞에 s 추가                    |
| `replace a b` | `Str→Str` | a를 b로 치환                   |
| `slice i j`   | `Str→Str` | i~j 부분 문자열                 |
| `split sep`   | `Str→Str` | sep으로 분리 (내부 구분자 \x1F로 변환) |
| `join sep`    | `Str→Str` | 내부 리스트를 sep으로 결합           |
| `find s`      | `Str→Str` | s가 존재하면 입력 그대로, 아니면 `""`   |
| `match pat`   | `Str→Str` | 정규식 일치 문자열 반환 (없으면 "")     |
| `notempty`    | `Str→Str` | 비어있으면 "", 아니면 "true"       |

### 7.2 조합 함수

| 이름                         | 타입                                        | 설명            |
| -------------------------- | ----------------------------------------- | ------------- |
| `sequence{...}`            | `{Str→Str}ⁿ → Str`                        | 순차 실행 및 연결    |
| `join sep{...}`            | `Str × {Str→Str}ⁿ → Str`                  | 결과를 sep으로 연결  |
| `first{...}`               | `{Str→Str}ⁿ → Str`                        | 첫 번째 비빈 결과 반환 |
| `guard cond{ then; else }` | `(Str→Str) × (Str→Str) × (Str→Str) → Str` | 조건 분기         |
| `map sep{...}`             | `Str × (Str→Str) → Str`                   | sep 단위 매핑     |
| `fold sep{...}`            | `Str × (Str→Str) → Str`                   | sep 단위 축약     |
| `const s`                  | `Str→Str`                                 | 입력 무시 후 s 반환  |
| `id`                       | `Str→Str`                                 | 항등함수          |

---

## 8. 예시

### 8.1 기본

```plaintext
"daegu" | upper | append " city"
→ "DAEGU city"
```

### 8.2 조건문

```plaintext
guard (find "busan") { "Found Busan"; "Not found" }
```

### 8.3 부모-자식 구조

```plaintext
join ", " {
  "alpha" | upper ;
  "beta" | upper ;
  "gamma" | upper
}
→ "ALPHA, BETA, GAMMA"
```

### 8.4 합성형 함수 정의

```plaintext
def highlight(x) = x | find "core" | upper | append "!"
```

---

## 9. 실행 모델

### 9.1 컴파일 단계

1. **파싱** → AST 구성
2. **타입 검사** → 모든 식 `Str→Str` 보장
3. **IR 변환** → 함수형 연산 그래프
4. **자바 바이트코드 생성**:

   * 각 함수 → `Fn` 인터페이스 구현 (`String → String`)
   * `|` → `Fn.compose`
   * `{}` → `Fn[]` 인자로 전달

### 9.2 바이트코드 명령어 (초안)

| 명령             | 의미        |
| -------------- | --------- |
| `PUSH_CONST s` | 문자열 상수 푸시 |
| `LOAD_VAR i`   | 지역변수 로드   |
| `CALL name n`  | 함수 호출     |
| `PIPE`         | 함수 합성     |
| `SEQ n`        | 블록 시작     |
| `ENDSEQ`       | 블록 종료     |
| `RET`          | 반환        |

---

## 10. 정합성 (Formal Consistency)

1. **참조투명성**

   ```
   let f(x) = x | upper
   f("abc") == "abc" | upper
   ```

   항상 동일 결과를 보장한다.

2. **결합법칙**

   ```
   (a | b) | c ≡ a | (b | c)
   ```

3. **항등원 존재**

   ```
   id | f ≡ f | id ≡ f
   ```

4. **순수성**
   모든 함수는 외부 상태, 입출력, 전역 참조 불가.

---

## 11. 비ASCII 처리 정책

* 모든 입력은 ASCII(0x09, 0x0A, 0x0D, 0x20~0x7E)로 제한.
* 비ASCII 발생 시:

  * **Strict 모드**: 런타임 오류 (IllegalCharacterException)
  * **Lenient 모드**: `?`로 대체

---

## 12. 임베드 및 확장성

* **루아처럼 내장 가능**:
  Java에서 `AsciiPipe.compile(String source)` → `Fn` 반환
* **JVM 바이트코드 내장**:
  `Fn`은 인터페이스(`String apply(String)`)
  → JIT/Interpreter 선택 가능
* **FFI 확장 가능**:
  C/C++/Rust로 작성된 순수 함수 등록 가능 (ASCII 검증 필요)

---

## 13. 예시 — DSL 수준

```plaintext
def pipeline(x) = join ", " {
  x | find "daegu" | upper ;
  x | find "busan" | upper
}

pipeline("daegu-busan-seoul")
→ "DAEGU, BUSAN"
```

---

## 14. 버전 계획

| 버전   | 추가 예정 기능                              |
| ---- | ------------------------------------- |
| v0.2 | 정규식 지원(`match re`), 문자열 맵핑(`mapchar`) |
| v0.3 | 부분적 lazy 평가, AST-level macro          |
| v0.4 | FFI 확장, 사용자 라이브러리 등록                  |
| v1.0 | 안정화 및 JVM 바이트코드 표준화                   |

---

## ✅ 요약

| 항목           | 요약                              |                 |
| ------------ | ------------------------------- | --------------- |
| **언어 이름**    | AsciiPipe                       |                 |
| **패러다임**     | Pure functional                 |                 |
| **데이터 모델**   | ASCII string-only               |                 |
| **주요 연산자**   | `                               | ` (composition) |
| **함수 정의**    | 단일 인자, 순수 함수                    |                 |
| **부모-자식 모델** | `{}` 기반 함수 조합자                  |                 |
| **컴파일 타깃**   | JVM bytecode                    |                 |
| **임베드 환경**   | Lua-level, Java-level embedding |                 |
| **철학**       | 직관적 파이프 + 수학적 함수합성 일관성          |                 |

---

이 버전은 언어의 **사상과 규칙의 완성된 기초 설계 문서**입니다.

* **표준 라이브러리 함수 50여 개 상세 시그니처 목록**
* **IR 및 Bytecode Encoding 규격서**
* **함수 결합 규칙 formal proof (λ-계산 기반)**

을 추가한 *AsciiPipe Spec v0.2 (Formal Edition)* 도 이어서 설계합니다.
이어서 그 확장판으로 진행합니다.

