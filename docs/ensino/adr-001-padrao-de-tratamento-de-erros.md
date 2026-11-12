# ADR-001 — Padrão de tratamento de erros nas APIs Spring Boot

<!-- nav:start -->
[← Anterior: Debates e objeções](debates-e-objecoes.md) | [↑ Material de ensino](README.md)
<!-- nav:end -->

> Modelo pronto para o time adaptar. Ajuste os trechos marcados com ✏️ e registre no repositório de
> decisões do time.

- **Status:** Proposto ✏️ (Proposto → Aceito → Substituído)
- **Data:** ✏️
- **Decisores:** ✏️
- **Referência:** [exception-guide](../README.md)

## Contexto

Hoje cada serviço trata erros de um jeito ✏️ (try/catch nos controllers, formatos `{"error"}`,
`{"message"}`, `{"success": false}`; 500 para erros de cliente; stack traces nas respostas).
Consequências: clientes com vários parsers, alertas disparando por erro de cliente, incidentes
difíceis de diagnosticar por falta de correlação entre resposta e log.

## Decisão

1. **Formato:** toda resposta de erro usa **Problem Details (RFC 9457)**, `application/problem+json`,
   com as extensões obrigatórias `code`, `correlationId` e `timestamp`. Erros de validação incluem
   `errors: [{field, message}]`.
2. **Tratamento central:** um `@RestControllerAdvice` por serviço, estendendo
   `ResponseEntityExceptionHandler`, sobrescrevendo `handleExceptionInternal` para que erros do
   framework sigam o mesmo contrato. Controllers e services **não** fazem try/catch para formatar resposta.
3. **Exceções da aplicação:** unchecked, base comum, carregando um `ErrorCode`. O domínio não conhece
   HTTP: cada `ErrorCode` tem uma categoria, e um único mapper na borda traduz categoria em status.
4. **Error codes:** `UPPER_SNAKE_CASE` com prefixo de contexto, um enum por contexto, catálogo
   validado no startup. Um code publicado nunca muda de significado.
5. **Status:** 400 forma; 401/403 autenticação/autorização; 404 inexistente; 409 conflito de estado;
   412 precondição; 422 regra de negócio; 429 limite; 500 bug; 502/503/504 dependências. Status de
   dependência nunca é repassado.
6. **Catch-all:** 500 genérico, sem mensagem interna; respeita `@ResponseStatus` e procura exceções
   da aplicação na causa.
7. **Integrações:** exceções de terceiros são traduzidas no adaptador, sempre preservando a causa.
8. **Log:** uma vez, no handler. 4xx sem stack trace (INFO/DEBUG); 5xx com stack trace (WARN para
   integração, ERROR para bug). Correlation id no MDC, no header e no corpo.
9. **Testes:** todo code tem teste de contrato HTTP; testes negativos garantem que nada interno vaza.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Manter formato próprio (`ApiError`) | Não cobre erros do framework sem trabalho extra; não é padrão |
| `@ResponseStatus` nas exceções | Acopla domínio a HTTP; corpo sai do `/error` |
| Classe de exceção por erro | Explosão de classes; consistência depende de disciplina |
| 200 com `success:false` | Quebra retry, cache, métricas e alertas |

## Consequências

- ✅ Um parser de erro para todos os clientes; decisões por `code`.
- ✅ Alertas confiáveis (5xx = nosso problema).
- ✅ Diagnóstico por `correlationId`.
- ⚠️ Migração: clientes que leem o formato antigo. Plano: manter campos legados como extensões até ✏️ (data).
- ⚠️ Disciplina no catálogo de codes (dono por contexto, revisão em PR).

## Como verificar

- Checklist do [cap. 11](../11-checklist-e-anti-patterns.md) no template de PR.
- Testes de contrato no CI.

---

<!-- nav:start -->
[← Anterior: Debates e objeções](debates-e-objecoes.md) | [↑ Material de ensino](README.md)
<!-- nav:end -->
