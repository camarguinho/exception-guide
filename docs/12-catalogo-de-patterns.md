# 12 — Catálogo de patterns de exception handler no Spring Boot

[← Checklist](11-checklist-e-anti-patterns.md) · [Índice](README.md) · [Material de ensino →](ensino/README.md)

---

Este capítulo reúne **todas** as formas de tratar exceções no Spring MVC / Spring Boot que você vai
encontrar em projetos reais, das piores às recomendadas. Cada padrão é um **teste executável e
isolado** em [`src/test/java/dev/cheatsheet/patterns`](../src/test/java/dev/cheatsheet/patterns/): abra o teste, leia o Javadoc, rode,
quebre e veja o que muda.

```bash
mvn test -Dtest='P*Test'            # todos os padrões
mvn test -Dtest='P09*'              # um padrão específico
```

> Os padrões usam `MockMvcBuilders.standaloneSetup(...)`: cada um monta só o seu controller e o seu
> advice, então um não interfere no outro nem no `GlobalExceptionHandler` da aplicação.

## 12.1 Visão geral

| # | Padrão | Veredito | Onde fica | Formato do corpo | Cobre erros do framework? | Acopla HTTP ao domínio? |
|---|---|---|---|---|---|---|
| [P01](../src/test/java/dev/cheatsheet/patterns/P01_TryCatchNoControllerTest.java) | try/catch no controller | ❌ | cada endpoint | inventado na hora | não | sim |
| [P02](../src/test/java/dev/cheatsheet/patterns/P02_ResponseStatusAnotacaoTest.java) | `@ResponseStatus` na exceção | ⚠️ | classe da exceção | página `/error` | não | **sim** |
| [P03](../src/test/java/dev/cheatsheet/patterns/P03_ResponseStatusExceptionTest.java) | `ResponseStatusException` | ⚠️ pontual | `throw` no controller | Problem Detail* | — | sim, se no service |
| [P04](../src/test/java/dev/cheatsheet/patterns/P04_ExceptionHandlerLocalTest.java) | `@ExceptionHandler` local | ⚠️ exceção à regra | controller | livre | não | não |
| [P05](../src/test/java/dev/cheatsheet/patterns/P05_ControllerAdviceComCorpoProprioTest.java) | `@RestControllerAdvice` + DTO próprio | ⚠️ legado comum | advice global | proprietário (`ApiError`) | **não** (buraco) | não |
| [P06](../src/test/java/dev/cheatsheet/patterns/P06_EscopoEOrdemDeAdvicesTest.java) | Vários advices: escopo + `@Order` | ✅ em migração | advices | por escopo | — | não |
| [P07](../src/test/java/dev/cheatsheet/patterns/P07_ResponseEntityExceptionHandlerTest.java) | Advice **estendendo `ResponseEntityExceptionHandler`** + `ProblemDetail` | ✅ **recomendado** | advice global | RFC 9457 | **sim** | não |
| [P08](../src/test/java/dev/cheatsheet/patterns/P08_ErrorResponseExceptionTest.java) | Exceção estendendo `ErrorResponseException` | ✅ só na camada web | exceção web | RFC 9457 | — | sim (de propósito) |
| [P09](../src/test/java/dev/cheatsheet/patterns/P09_ArmadilhaDoCatchAllTest.java) | Armadilha do `@ExceptionHandler(Exception.class)` | 🪤 | advice | — | — | — |
| [P10](../src/test/java/dev/cheatsheet/patterns/P10_RegrasDeResolucaoTest.java) | Regras de resolução (especificidade, vários tipos, causa) | 📐 | advice | — | — | — |
| [P11](../src/test/java/dev/cheatsheet/patterns/P11_NegociacaoDeConteudoTest.java) | `@ExceptionHandler(produces = ...)` | ✅ API + HTML | advice | por `Accept` | — | não |
| [P12](../src/test/java/dev/cheatsheet/patterns/P12_HandlerExceptionResolverCustomizadoTest.java) | `HandlerExceptionResolver` customizado | ⚠️ infraestrutura | bean | livre | depende | não |
| [P13](../src/test/java/dev/cheatsheet/patterns/P13_ExcecaoEmFilterTest.java) | Exceção em `Filter` → delegar ao resolver | ✅ | filter | o do advice | — | não |

\* só se existir um advice que estenda `ResponseEntityExceptionHandler`; sem ele, sai como no P02.

## 12.2 Como o Spring decide (o modelo mental para ensinar)

```
exceção sai do controller
        │
        ▼
1. ExceptionHandlerExceptionResolver ─── @ExceptionHandler
        │   a) do próprio controller (P04)
        │   b) dos @ControllerAdvice, em @Order, respeitando o escopo (P06)
        │   dentro de cada um: tipo mais próximo vence; senão, tenta a causa (P10)
        ▼ (ninguém casou)
2. ResponseStatusExceptionResolver ───── @ResponseStatus / ResponseStatusException (P02, P03)
        ▼
3. DefaultHandlerExceptionResolver ───── exceções padrão do Spring MVC (405, 415, 400...) → sendError
        ▼
   container → /error → BasicErrorController (Spring Boot)

Filters ficam ANTES de tudo isso (P13).
```

Duas consequências que caem em toda revisão de código:

1. **Um handler de `Exception` no passo 1 impede que os passos 2 e 3 aconteçam** (P09). Exceções
   com `@ResponseStatus` e `ResponseStatusException` viram 500; com Spring Security, `AccessDeniedException`
   vira 500 em vez de 403. Por isso o catch-all precisa ser escrito com cuidado.
2. **Um handler de `Exception` também desliga o casamento por causa** (P10): uma
   `CompletionException` embrulhando `NotFoundException` casa com `Exception` antes de o Spring
   olhar a causa.

O [`GlobalExceptionHandler`](../src/main/java/dev/cheatsheet/exceptions/shared/web/GlobalExceptionHandler.java)
do projeto trata as duas: estende `ResponseEntityExceptionHandler` e, no catch-all, procura uma
`ApplicationException` na cadeia de causas e respeita `@ResponseStatus`
(testes em [`UnexpectedErrorTest`](../src/test/java/dev/cheatsheet/exceptions/shared/UnexpectedErrorTest.java)).

## 12.3 Cada padrão em uma frase

**P01 — try/catch no controller.** Duplica código, cada endpoint responde de um jeito e o endpoint
onde alguém esqueceu responde de outro. Só tem lugar quando o controller realmente se **recupera** do erro.

**P02 — `@ResponseStatus`.** Status certo com zero código, mas o corpo é o da página `/error` e a
exceção de domínio passa a depender de `spring-web`. Aceitável em protótipo; você vai encontrar em
legado e em libs (por isso o catch-all precisa respeitá-lo).

**P03 — `ResponseStatusException`.** Bom para um erro pontual que só existe na borda HTTP. Em
service, é HTTP no domínio. Desde o Spring 6 carrega um `ProblemDetail`.

**P04 — `@ExceptionHandler` local.** Vence o advice global. Use só quando um controller precisa
**mesmo** de um tratamento diferente, e documente o motivo: é uma exceção à regra.

**P05 — Advice com DTO próprio.** O padrão mais comum em projetos pré-Spring 6. Centraliza, mas o
formato é proprietário e os erros do framework (405, 415, JSON inválido) escapam dele.

**P06 — Escopo e ordem.** `assignableTypes`, `basePackages` e `annotations` limitam o advice;
`@Order` define quem tenta primeiro. Sem `@Order`, a ordem entre advices é indefinida.
Caso de uso: migrar de formato sem quebrar a API v1.

**P07 — `ResponseEntityExceptionHandler` + `ProblemDetail`.** ✅ **O padrão.** Um advice só,
herdando o tratamento de todas as exceções do Spring MVC no formato RFC 9457, com
`handleExceptionInternal` como funil para enriquecer todas as respostas.

**P08 — `ErrorResponseException`.** A exceção já carrega status, headers e o `ProblemDetail`.
Ótima para erros que nascem na web (header obrigatório, versão de API). Não use no domínio.

**P09 — Armadilha do catch-all.** Ver 12.2. Corrija estendendo `ResponseEntityExceptionHandler` e
respeitando `@ResponseStatus`.

**P10 — Regras de resolução.** Tipo mais específico vence; um handler pode listar vários tipos;
sem catch-all, o Spring tenta a causa.

**P11 — `produces`.** Desde o Spring 6.2, o mesmo erro pode ter um handler JSON para a API e um
HTML para o navegador, escolhido pelo `Accept`.

**P12 — `HandlerExceptionResolver`.** O mecanismo por baixo de tudo. Use em infraestrutura
compartilhada entre aplicações, não no dia a dia.

**P13 — Exceção em filter.** O advice não enxerga. Capture no filter e **delegue ao
`HandlerExceptionResolver`** (bean `handlerExceptionResolver`): o mesmo advice formata a resposta.
É o padrão usado no `AuthenticationEntryPoint` do Spring Security (ver [cap. 05](05-tratamento-de-erros-no-spring-mvc.md#56-o-que-o-advice-não-alcança)).

## 12.4 Guia de decisão

```
Preciso tratar um erro numa API Spring Boot.
│
├─ É um erro do domínio/aplicação?
│    → lance uma exceção da aplicação (com ErrorCode) e deixe o advice global (P07) traduzir.
│
├─ Só existe na borda HTTP (header, parâmetro de protocolo)?
│    → ErrorResponseException (P08) ou ResponseStatusException no controller (P03).
│
├─ Um controller específico precisa de tratamento diferente?
│    → @ExceptionHandler local (P04), com justificativa no código.
│
├─ Duas famílias de API com formatos diferentes (v1 legado / v2)?
│    → advices com escopo + @Order (P06).
│
├─ Mesmo erro para API e navegador?
│    → @ExceptionHandler(produces = ...) (P11).
│
├─ A exceção nasce num Filter / Spring Security?
│    → delegar ao HandlerExceptionResolver (P13).
│
└─ Biblioteca interna que deve tratar uma família de exceções em vários apps?
     → HandlerExceptionResolver próprio (P12) ou um advice auto-configurado na lib.
```

## 12.5 Boas práticas que valem para qualquer padrão

1. **Um único contrato de erro** para todas as respostas, inclusive as do framework.
2. **Um único lugar** que decide status, formato e log (o advice global).
3. **Catch-all sempre presente, sempre genérico**: 500 sem mensagem interna, com correlationId,
   e escrito com as duas proteções da seção 12.2.
4. **Handlers específicos antes do catch-all** para exceções de bibliotecas que têm status próprio
   (`AccessDeniedException`, `AuthenticationException`, `OptimisticLockingFailureException`...).
5. **Teste o contrato** de cada erro, inclusive o que não pode aparecer (ver [cap. 10](10-testando-erros.md)).
6. **`@Order` explícito** sempre que houver mais de um advice.
7. **Nada de try/catch para formatar resposta** em controllers e services.
