# Roteiro de demos ao vivo

<!-- nav:start -->
[← Anterior: Plano de workshop — 3 encontros de 2h](plano-de-workshop.md) | [↑ Material de ensino](README.md) | [Próximo: Exercícios com gabarito →](exercicios.md)
<!-- nav:end -->

Regra de ouro: **pergunte o resultado antes de rodar.** O valor da demo está na surpresa.

---

## Demo 1 — A evolução dos padrões (15 min)

1. Abra [`P01_TryCatchNoControllerTest`](../../src/test/java/dev/cheatsheet/patterns/P01_TryCatchNoControllerTest.java).
   Mostre os dois endpoints. **Pergunte:** "O que o `/price` devolve?"
   Rode `mvn test -Dtest='P01*'`. A exceção escapa. Ponto: consistência depende de disciplina.
2. [`P02`](../../src/test/java/dev/cheatsheet/patterns/P02_ResponseStatusAnotacaoTest.java): status certo, corpo vazio.
   **Pergunte:** "Quem decide o corpo aqui?" (o `/error` do Boot).
3. [`P05`](../../src/test/java/dev/cheatsheet/patterns/P05_ControllerAdviceComCorpoProprioTest.java): advice com `ApiError`.
   **Pergunte:** "E um DELETE numa rota que só tem GET?" Rode `erroDoFramework_escapaDoFormato`. O buraco aparece.
4. [`P07`](../../src/test/java/dev/cheatsheet/patterns/P07_ResponseEntityExceptionHandlerTest.java): o mesmo cenário com
   `ResponseEntityExceptionHandler`. Todos os erros no mesmo formato. Ponto: herde o que o framework já sabe fazer.

## Demo 2 — A armadilha do catch-all (10 min)

1. Abra [`P09`](../../src/test/java/dev/cheatsheet/patterns/P09_ArmadilhaDoCatchAllTest.java), mostre só o `NaiveAdvice`
   e a exceção com `@ResponseStatus(NOT_FOUND)`. **Pergunte:** "Que status sai?"
   A maioria responde 404. Rode: sai **500**.
2. Explique pelo diagrama dos resolvers ([cap. 12 §12.2](../12-catalogo-de-patterns.md#122-como-o-spring-decide-o-modelo-mental-para-ensinar)).
3. Mostre o `SafeAdvice` e rode o segundo teste.
4. Fecho: "Com Spring Security, isso transforma 403 em 500."

## Demo 3 — Regras de resolução (10 min)

[`P10`](../../src/test/java/dev/cheatsheet/patterns/P10_RegrasDeResolucaoTest.java)

1. Dois handlers: `AppException` e `NotFoundException`. **Pergunte:** "Qual roda para `NotFoundException`? Importa a ordem dos métodos?"
2. `/wrapped`: `IllegalStateException` com causa `NotFoundException`. **Pergunte:** "Sem catch-all, qual handler?" (o de `NotFoundException`, pela causa).
3. **Pergunte:** "E se eu adicionar um handler de `Exception`?" Rode `comCatchAll_aCausaNaoEhMaisConsultada`: 500.
4. Mostre como o `GlobalExceptionHandler.handleUnexpected` do projeto resolve (`findCause`).

## Demo 4 — A aplicação real (15 min)

```bash
mvn spring-boot:run
```

```bash
H='Content-Type: application/json'
# 400 validação, com lista de campos
curl -s -H "$H" localhost:8080/orders -d '{"customerId":"","items":[{"sku":"A","quantity":0,"unitPrice":-1}]}'
# 422 regra de negócio, com dados estruturados
curl -s -H "$H" localhost:8080/orders -d '{"customerId":"c1","items":[{"sku":"TV","quantity":3,"unitPrice":5000}]}'
# 405 e 404 de rota: mesmo formato
curl -s -X DELETE localhost:8080/orders/3f2b8c1e-0000-4000-8000-000000000000
curl -s localhost:8080/nao-existe
# crie um pedido e use o id:
ID=$(curl -s -H "$H" localhost:8080/orders -d '{"customerId":"c1","items":[{"sku":"A","quantity":1,"unitPrice":10}]}' | sed 's/.*"id":"\([^"]*\)".*/\1/')
# 503 com Retry-After, 504, 502
curl -si -H "$H" localhost:8080/orders/$ID/payment -d '{"paymentToken":"tok_unavailable"}'
curl -s  -H "$H" localhost:8080/orders/$ID/payment -d '{"paymentToken":"tok_timeout"}'
curl -s  -H "$H" localhost:8080/orders/$ID/payment -d '{"paymentToken":"tok_bad_request"}'
```

Pontos a mostrar:

- O `correlationId` da resposta **aparece no log** do terminal. Copie e busque.
- O 502 do `tok_bad_request`: "o provedor nos devolveu 400, mas para o cliente é 502. Por quê?"
- O log: 4xx sem stack trace, 5xx com stack trace e `Caused by` do SDK.
- `curl localhost:8080/errors`: o catálogo publicado.

## Demo 5 (opcional) — Quebrando o contrato de propósito (5 min)

No `GlobalExceptionHandler`, comente a linha `enrich(problem, code);` dentro de `handleExceptionInternal`
e rode `mvn test -Dtest=OrderApiErrorContractTest`. Vários testes falham (405, 404 de rota, JSON
inválido). Ponto: **teste de contrato é o que impede regressão silenciosa.** Desfaça a alteração.

---

<!-- nav:start -->
[← Anterior: Plano de workshop — 3 encontros de 2h](plano-de-workshop.md) | [↑ Material de ensino](README.md) | [Próximo: Exercícios com gabarito →](exercicios.md)
<!-- nav:end -->
