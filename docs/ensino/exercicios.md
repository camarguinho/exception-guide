# Exercícios com gabarito

[← Material de ensino](README.md)

Todos os exercícios são feitos **no próprio projeto**. Critério de pronto: `mvn test` verde, com
um teste novo cobrindo o que você fez. Gabaritos escondidos: tente antes de abrir.

| # | Tema | Nível |
|---|---|---|
| 1 | Novo erro de negócio de ponta a ponta | ⭐ |
| 2 | Qual é o status? | ⭐ |
| 3 | Preveja o handler | ⭐⭐ |
| 4 | Fechar o buraco do P05 | ⭐⭐ |
| 5 | Novo contexto de erros + catálogo | ⭐⭐ |
| 6 | Exceção da camada web com `ErrorResponseException` | ⭐⭐ |
| 7 | Handler específico para exceção de biblioteca | ⭐⭐ |
| 8 | Filter que delega ao `HandlerExceptionResolver` | ⭐⭐⭐ |
| 9 | Métrica por error code | ⭐⭐⭐ |
| 10 | Mensagens de erro em inglês e português | ⭐⭐⭐ |

---

## 1. Novo erro de negócio de ponta a ponta ⭐

Um pedido não pode ter mais de **20 itens**. Hoje a borda aceita até 50 (`@Size(max = 50)`), e a
regra é de negócio (pode mudar por tipo de cliente), então deve sair como **422** com code
`ORDER_TOO_MANY_ITEMS` e o limite nos detalhes.

<details><summary>Gabarito</summary>

```java
// OrderErrorCode
ORDER_TOO_MANY_ITEMS(ErrorCategory.BUSINESS_RULE,
        "Itens demais no pedido",
        "O pedido tem {0} itens; o máximo é {1}."),

// OrderService.create, antes do limite de valor
static final int MAX_ITEMS = 20;
if (items.size() > MAX_ITEMS) {
    throw new BusinessRuleException(OrderErrorCode.ORDER_TOO_MANY_ITEMS,
            Map.of("maxItems", MAX_ITEMS), items.size(), MAX_ITEMS);
}
```

```java
// OrderApiErrorContractTest.ErrosDaAplicacao
@Test
void itensDemais_422() throws Exception {
    String item = "{\"sku\":\"A\",\"quantity\":1,\"unitPrice\":1}";
    String body = "{\"customerId\":\"c1\",\"items\":[" + String.join(",", Collections.nCopies(21, item)) + "]}";
    var result = postJson("/orders", body);
    assertProblemContract(result, 422, "ORDER_TOO_MANY_ITEMS");
    result.andExpect(jsonPath("$.maxItems").value(20));
}
```

Pontos de discussão: nenhum controller ou handler mudou. O catálogo (`/errors`) já mostra o novo code.
</details>

---

## 2. Qual é o status? ⭐

1. `GET /orders?customerId=xyz` sem resultados.
2. `POST /orders` com `"quantity": "dois"`.
3. Cancelar um pedido já pago.
4. Token JWT expirado.
5. Usuário comum chamando endpoint de admin.
6. Cupom de desconto vencido.
7. O serviço de frete respondeu 400 para a nossa chamada.
8. O serviço de frete não respondeu em 2 s.
9. `PUT` com `If-Match: "v3"`, mas o recurso está na v5.
10. `NullPointerException` no service.

<details><summary>Gabarito</summary>

1. **200** com `[]`. 2. **400** (corpo ilegível/tipo errado). 3. **409** (estado). 4. **401**.
5. **403** (ou 404 para esconder). 6. **422** (regra de negócio). 7. **502** (a culpa é nossa, não do cliente).
8. **504**. 9. **412**. 10. **500**.
</details>

---

## 3. Preveja o handler ⭐⭐

Para cada caso, diga o status **antes** de conferir no [catálogo](../12-catalogo-de-patterns.md).

a) Advice com handlers para `RuntimeException` (→ 500) e `IllegalArgumentException` (→ 400). Lança `NumberFormatException`.
b) Controller com `@ExceptionHandler(MyException)` → 409; advice global com o mesmo → 422. Lança `MyException`.
c) Advice só com `@ExceptionHandler(Exception.class)` → 500. Lança `new ResponseStatusException(HttpStatus.NOT_FOUND)`.
d) Mesmo advice de (c), agora estendendo `ResponseEntityExceptionHandler`. Mesma exceção.
e) Advice com `NotFoundException` → 404, sem catch-all. Lança `new RuntimeException(new NotFoundException())`.

<details><summary>Gabarito</summary>

a) **400**: `NumberFormatException extends IllegalArgumentException`, o tipo mais próximo vence (P10).
b) **409**: handler local vence o global (P04).
c) **500**: o catch-all roda antes do resolver de `ResponseStatusException` (P09).
d) **404**: a superclasse tem handler para `ErrorResponseException`, mais específico que `Exception` (P07/P09).
e) **404**: sem handler para `RuntimeException`, o Spring tenta a causa (P10).
</details>

---

## 4. Fechar o buraco do P05 ⭐⭐

Em [`P05_ControllerAdviceComCorpoProprioTest`](../../src/test/java/dev/cheatsheet/patterns/P05_ControllerAdviceComCorpoProprioTest.java),
mude o `ApiErrorAdvice` para que o 405 também saia com corpo, e mude o teste `erroDoFramework_escapaDoFormato`
para exigir isso. Mantenha os campos que os clientes legados usam (`message`, `path`).

<details><summary>Gabarito</summary>

```java
@RestControllerAdvice
static class ApiErrorAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail notFound(ProductNotFoundException e, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setProperty("message", e.getMessage());          // compatibilidade
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, status, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            problem.setProperty("message", problem.getDetail());
            problem.setProperty("path", ((ServletWebRequest) request).getRequest().getRequestURI());
        }
        return response;
    }
}
```

Discussão: é a estratégia de migração sem quebra. Os clientes antigos continuam lendo `message` e
os novos passam a ler `detail`/`code`. Depois de migrados, os campos legados são removidos.
</details>

---

## 5. Novo contexto de erros + catálogo ⭐⭐

Crie o contexto de **estoque** com `StockErrorCode` (`STOCK_INSUFFICIENT`, 422; `STOCK_RESERVATION_EXPIRED`, 409).
Registre no catálogo. Depois, **de propósito**, crie `ORDER_NOT_FOUND` também no `StockErrorCode` e
rode a aplicação: o que acontece? Por que isso é bom?

<details><summary>Gabarito</summary>

- Enum em `dev.cheatsheet.exceptions.stock.StockErrorCode implements ErrorCode` (mesmo formato dos outros).
- `ErrorCatalogConfig`: `codes.addAll(List.of(StockErrorCode.values()));`
- `ErrorModelTest.allCodes()`: adicionar também.
- Com o código duplicado, a aplicação **não sobe**: `IllegalStateException: Código de erro duplicado: ORDER_NOT_FOUND`.
  Fail-fast: o erro aparece no build/CI, não para um cliente em produção.
</details>

---

## 6. Exceção da camada web com `ErrorResponseException` ⭐⭐

O pagamento passa a exigir o header `Idempotency-Key`. Sem ele: **400**, code `IDEMPOTENCY_KEY_REQUIRED`.
É um erro que só existe na borda HTTP, então o service não deve saber dele.

<details><summary>Gabarito</summary>

```java
// shared/web/IdempotencyKeyRequiredException.java
public class IdempotencyKeyRequiredException extends ErrorResponseException {
    public IdempotencyKeyRequiredException(URI type) {
        super(HttpStatus.BAD_REQUEST, problem(type), null);
    }
    private static ProblemDetail problem(URI type) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "O header Idempotency-Key é obrigatório.");
        p.setType(type);
        p.setTitle("Idempotency-Key ausente");
        p.setProperty("code", "IDEMPOTENCY_KEY_REQUIRED");
        return p;
    }
}

// OrderController.pay
@PostMapping("/{id}/payment")
OrderResponse pay(@PathVariable UUID id,
                  @RequestHeader(name = "Idempotency-Key", required = false) String key,
                  @Valid @RequestBody PayOrderRequest request) {
    if (key == null || key.isBlank()) {
        throw new IdempotencyKeyRequiredException(URI.create("/errors/IDEMPOTENCY_KEY_REQUIRED"));
    }
    ...
}
```

O `handleExceptionInternal` do `GlobalExceptionHandler` mantém o `code` (já presente) e acrescenta
`correlationId` e `timestamp`. Lembre de atualizar os testes de pagamento existentes para enviar o header.

Por que não `@RequestHeader` obrigatório? Funcionaria (400 `MALFORMED_REQUEST` automático), mas sem
um code específico e com mensagem genérica. As duas opções são válidas; discuta o trade-off.
</details>

---

## 7. Handler específico para exceção de biblioteca ⭐⭐

Algumas libs lançam `UnsupportedOperationException` para funcionalidades não implementadas. Faça
virar **501 Not Implemented** com code `NOT_IMPLEMENTED`, em vez de 500. Onde o handler deve ficar,
e por que não dentro do catch-all com `if (ex instanceof ...)`?

<details><summary>Gabarito</summary>

```java
// GlobalExceptionHandler
@ExceptionHandler(UnsupportedOperationException.class)
ResponseEntity<ProblemDetail> handleNotImplemented(UnsupportedOperationException ex) {
    HttpStatus status = HttpStatus.NOT_IMPLEMENTED;
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Funcionalidade não disponível.");
    enrich(problem, "NOT_IMPLEMENTED");
    logException(ex, status, "NOT_IMPLEMENTED");
    return ResponseEntity.status(status).body(problem);
}
```

Um `@ExceptionHandler` próprio é mais específico que `Exception`, então vence sem `if`. O catch-all
cheio de `instanceof` é o "switch gigante" que o mecanismo de resolução existe para evitar.
Mesma lógica vale para `AccessDeniedException` (403) com Spring Security.
Cuidado: não vaze `ex.getMessage()`. A mensagem de libs pode conter detalhes internos.
</details>

---

## 8. Filter que delega ao `HandlerExceptionResolver` ⭐⭐⭐

Crie um `ApiKeyFilter` que, quando `app.api-key.enabled=true`, exige `X-Api-Key` em `POST /orders`.
Sem a chave: **401** com code `INVALID_API_KEY`, **no mesmo formato** dos outros erros. Teste com
`@SpringBootTest(properties = "app.api-key.enabled=true")`. Base: [P13](../../src/test/java/dev/cheatsheet/patterns/P13_ExcecaoEmFilterTest.java).

<details><summary>Gabarito</summary>

```java
@Component
@ConditionalOnProperty(name = "app.api-key.enabled", havingValue = "true")
class ApiKeyFilter extends OncePerRequestFilter {

    private final HandlerExceptionResolver resolver;

    ApiKeyFilter(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && "/orders".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (req.getHeader("X-Api-Key") == null) {
            resolver.resolveException(req, res, null, new InvalidApiKeyException());
            return;
        }
        chain.doFilter(req, res);
    }
}
```

`InvalidApiKeyException` pode ser uma `ApplicationException` com um `ErrorCode` de categoria
`UNAUTHENTICATED` (crie `CommonErrorCode.INVALID_API_KEY`). Como ela chega ao advice pelo resolver,
recebe `code`, `correlationId` e o log padrão. O `CorrelationIdFilter` tem precedência maior, então o
id já está no MDC.
</details>

---

## 9. Métrica por error code ⭐⭐⭐

Adicione `spring-boot-starter-actuator` e publique o contador `app.errors` com as tags `code` e `status`
para **toda** resposta de erro. Verifique em `/actuator/metrics/app.errors`.

<details><summary>Gabarito</summary>

```java
// GlobalExceptionHandler: injete MeterRegistry no construtor, torne logException um método de instância
// e registre ali, o único ponto por onde todo erro passa
private void logException(Exception ex, HttpStatusCode status, String code) {
    meterRegistry.counter("app.errors", "code", code, "status", String.valueOf(status.value())).increment();
    ... // log atual
}
```

```yaml
management.endpoints.web.exposure.include: health,metrics
```

Por que dá para fazer em um lugar só? Porque **todo** erro passa por `logException`, já que o handler
é central. Num projeto com try/catch espalhado, isso seria impossível.
Nunca use `detail` como tag: cardinalidade explode (ids, valores).
</details>

---

## 10. Mensagens de erro em inglês e português ⭐⭐⭐

Respeite `Accept-Language`: `en` devolve título e detalhe em inglês; o padrão continua em português.

<details><summary>Gabarito</summary>

1. Guardar os argumentos na exceção: `ApplicationException` ganha `Object[] args` + `getArgs()`.
2. `src/main/resources/messages_en.properties`:
   ```properties
   error.ORDER_NOT_FOUND.title=Order not found
   error.ORDER_NOT_FOUND.detail=Order {0} was not found.
   ```
   e `messages.properties` (pode ficar vazio; o padrão é o texto do enum).
3. `application.yml`: `spring.messages.fallback-to-system-locale: false`. Sem isso, uma JVM em inglês
   escolheria `messages_en` para todo mundo.
4. No `problemFor` do handler:
   ```java
   Locale locale = LocaleContextHolder.getLocale();
   String title  = messageSource.getMessage("error." + code.code() + ".title", null, code.title(), locale);
   String detail = messageSource.getMessage("error." + code.code() + ".detail", args, code.format(args), locale);
   ```
5. Teste: `get(...).header("Accept-Language", "en")` → `$.title == "Order not found"`.

Ponto de discussão: o **code nunca é traduzido**, só o texto. É por isso que o cliente decide pelo code.
</details>
