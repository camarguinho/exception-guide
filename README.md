# exception-guide

Guia + projeto de referência (Java 21 · Spring Boot 4 · Maven) sobre **exception handling em
aplicações Spring Boot**: os padrões de mercado, as boas práticas, error codes e HTTP status.

- 📘 **Ebook:** [`docs/`](docs/README.md), em 12 capítulos, cada um apontando para o código daqui.
- 🧩 **Catálogo de patterns:** [`src/test/java/dev/cheatsheet/patterns`](src/test/java/dev/cheatsheet/patterns/),
  com 13 formas de tratar exceções no Spring, cada uma como teste executável e isolado ([cap. 12](docs/12-catalogo-de-patterns.md)).
- 🎓 **Material de ensino:** [`docs/ensino/`](docs/ensino/README.md), com workshop, demos, exercícios com gabarito, kata, quiz e ADR.

## O que o projeto demonstra

- Hierarquia de exceções unchecked com **error code** e **categoria** (o domínio não conhece HTTP).
- Catálogo de códigos validado no startup e publicado em `GET /errors`.
- `@RestControllerAdvice` único, estendendo `ResponseEntityExceptionHandler`, respondendo em
  **Problem Details (RFC 9457)** para todo erro, inclusive os do framework (404 de rota, 405, 415...).
- Validação em três camadas (borda → 400, domínio → 500 se escapar, regra de negócio → 422/409).
- Tradução de exceções de um SDK de terceiro (502/503/504, `Retry-After`, `retryable`), com causa preservada.
- Correlation ID no MDC, no header e no corpo; política de log por categoria.
- Catch-all seguro: respeita `@ResponseStatus` e encontra exceções da aplicação embrulhadas na causa.
- Testes de contrato para cada cenário de erro, mais o catálogo de patterns (78 testes).

## Rodando

```bash
mvn test                 # roda os testes
mvn test -Dtest='P*Test' # só o catálogo de patterns
mvn spring-boot:run      # sobe em http://localhost:8080
```

## Experimente

```bash
# 201 — cria um pedido (guarde o id)
curl -s -H 'Content-Type: application/json' localhost:8080/orders \
  -d '{"customerId":"cust-1","items":[{"sku":"SKU-1","quantity":2,"unitPrice":10.50}]}'

# 400 VALIDATION_FAILED — lista de campos inválidos
curl -s -H 'Content-Type: application/json' localhost:8080/orders \
  -d '{"customerId":"","items":[{"sku":"A","quantity":0,"unitPrice":-1}]}'

# 422 ORDER_TOTAL_LIMIT_EXCEEDED — regra de negócio, com limit/total no corpo
curl -s -H 'Content-Type: application/json' localhost:8080/orders \
  -d '{"customerId":"cust-1","items":[{"sku":"TV","quantity":3,"unitPrice":5000.00}]}'

# 404 ORDER_NOT_FOUND
curl -s localhost:8080/orders/3f2b8c1e-0000-4000-8000-000000000000

# Pagamento: o token define o comportamento do provedor fake
#   tok_ok → 200 | tok_declined → 422 | tok_provider_error / tok_bad_request → 502
#   tok_unavailable → 503 + Retry-After | tok_timeout → 504
curl -si -H 'Content-Type: application/json' localhost:8080/orders/{id}/payment -d '{"paymentToken":"tok_unavailable"}'

# 409 ORDER_INVALID_STATE — cancelar duas vezes
curl -s -X POST localhost:8080/orders/{id}/cancel

# 405, 404 de rota, 400 de UUID inválido: mesmo formato
curl -s -X DELETE localhost:8080/orders/{id}
curl -s localhost:8080/nao-existe
curl -s localhost:8080/orders/nao-e-uuid

# Catálogo de erros
curl -s localhost:8080/errors
curl -s localhost:8080/errors/PAYMENT_PROVIDER_TIMEOUT
```

## Estrutura

```
src/main/java/dev/cheatsheet/exceptions
├── shared/error   ← modelo de erro, sem dependência de web (ErrorCode, ErrorCategory, ApplicationException...)
├── shared/web     ← tradução para HTTP (GlobalExceptionHandler, HttpStatusMapper, CorrelationIdFilter...)
├── catalog        ← ErrorCatalog (validação no startup) + GET /errors
├── order          ← contexto de pedidos: domínio, service, controller, OrderErrorCode
└── payment        ← porta PaymentGateway, adaptador que traduz o SDK, PaymentErrorCode
    └── sdk        ← "SDK de terceiro" fake com exceção checked própria
src/test/java/dev/cheatsheet/patterns   ← catálogo executável P01–P13
docs/              ← o ebook
docs/ensino/       ← material para conduzir workshop
```
