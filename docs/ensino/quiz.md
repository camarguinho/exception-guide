# Quiz — 20 perguntas

<!-- nav:start -->
[← Anterior: Kata de refatoração: o controller legado](kata-refatoracao.md) | [↑ Material de ensino](README.md) | [Próximo: Debates e objeções →](debates-e-objecoes.md)
<!-- nav:end -->

Use para revisão no fim de um encontro, avaliação depois de um mês, ou como banco de perguntas de
entrevista. Respostas escondidas.

---

**1.** Por que não se deve capturar `Error` (ex.: `OutOfMemoryError`)?
<details><summary>Resposta</summary>Indica que a JVM está num estado do qual a aplicação não se recupera de forma confiável. O certo é deixar o processo cair e o orquestrador reiniciar.</details>

**2.** Qual o comportamento padrão de `@Transactional` com uma checked exception?
<details><summary>Resposta</summary>Faz **commit**. O rollback padrão só acontece com `RuntimeException` e `Error`; para checked é preciso `rollbackFor`.</details>

**3.** O que há de errado em `throw new AppException("falhou: " + e.getMessage());`?
<details><summary>Resposta</summary>Perde a causa (tipo e stack trace originais). Correto: `new AppException("falhou", e)`.</details>

**4.** O que acontece com uma exceção lançada dentro de uma tarefa submetida com `executor.submit()` se ninguém chamar `get()`?
<details><summary>Resposta</summary>Ela fica guardada no `Future` e some silenciosamente.</details>

**5.** O que fazer ao capturar `InterruptedException`?
<details><summary>Resposta</summary>Restaurar a flag com `Thread.currentThread().interrupt()` e então tratar ou relançar.</details>

**6.** Qual a diferença entre erro esperado e inesperado, e como cada um aparece para o cliente?
<details><summary>Resposta</summary>Esperado faz parte do contrato (pedido não existe) e sai com status, code e mensagem específicos. Inesperado é bug e sai como 500 genérico, sem detalhes, com correlationId.</details>

**7.** Por que o domínio não deve lançar `ResponseStatusException`?
<details><summary>Resposta</summary>Acopla o domínio a HTTP. O mesmo serviço pode ser chamado por fila ou batch, e mudar o status exigiria mexer no domínio.</details>

**8.** Por que existe um error code se já existe o HTTP status?
<details><summary>Resposta</summary>O status é uma família (422 pode significar dez erros diferentes); a mensagem é texto para humanos e muda. O code é o identificador estável que o cliente usa para decidir.</details>

**9.** Renomear um error code é uma mudança compatível?
<details><summary>Resposta</summary>Não, é breaking. Crie um novo e deprecie o antigo. Adicionar codes é compatível se os clientes têm fallback por status.</details>

**10.** 400 ou 422 para "total do pedido acima do limite do cliente"?
<details><summary>Resposta</summary>Pela convenção do projeto, **422**: a requisição é bem formada, mas viola uma regra de negócio que depende de dados. 400 é para forma e sintaxe.</details>

**11.** O provedor externo respondeu 400 para a nossa chamada. Que status devolvemos ao nosso cliente?
<details><summary>Resposta</summary>**502** (ou 500). O 400 significa que nós chamamos errado; não é culpa do nosso cliente.</details>

**12.** Diferença entre 502, 503 e 504?
<details><summary>Resposta</summary>502: a dependência respondeu erro/lixo. 503: indisponível agora (manutenção, circuit breaker), idealmente com `Retry-After`. 504: a dependência não respondeu a tempo.</details>

**13.** Qual a ordem dos `HandlerExceptionResolver` padrão do Spring MVC?
<details><summary>Resposta</summary>1) `ExceptionHandlerExceptionResolver` (`@ExceptionHandler`), 2) `ResponseStatusExceptionResolver`, 3) `DefaultHandlerExceptionResolver`. Se nenhum resolve, o container encaminha para `/error`.</details>

**14.** Um advice tem `@ExceptionHandler(Exception.class)` e o código lança uma exceção anotada com `@ResponseStatus(NOT_FOUND)`. Que status sai?
<details><summary>Resposta</summary>**500**: o catch-all é resolvido no passo 1, antes do `ResponseStatusExceptionResolver` (P09).</details>

**15.** Um handler local no controller e um no advice tratam a mesma exceção. Qual roda?
<details><summary>Resposta</summary>O local (P04).</details>

**16.** Advice com handlers para `AppException` e `NotFoundException extends AppException`. Lança `NotFoundException`. Qual roda? A ordem dos métodos importa?
<details><summary>Resposta</summary>O de `NotFoundException` (tipo mais próximo). A ordem dos métodos não importa (P10).</details>

**17.** Por que estender `ResponseEntityExceptionHandler`?
<details><summary>Resposta</summary>Para herdar o tratamento de todas as exceções do Spring MVC (405, 415, JSON inválido, validação) no formato Problem Details e poder enriquecer todas em `handleExceptionInternal`. Sem ele, esses erros escapam do seu contrato (P05).</details>

**18.** Uma exceção lançada num `Filter` chega ao `@RestControllerAdvice`?
<details><summary>Resposta</summary>Não. Ou ela vai para `/error`, ou o filter delega ao `HandlerExceptionResolver` para reaproveitar o advice (P13).</details>

**19.** Onde a exceção deve ser logada?
<details><summary>Resposta</summary>Uma vez, onde ela é tratada (o advice global). Quem só propaga ou traduz não loga. 4xx sem stack trace, 5xx com.</details>

**20.** Num teste de erro, por que verificar o code e não a mensagem?
<details><summary>Resposta</summary>A mensagem é para humanos e muda (redação, tradução). O code é contrato estável. Testar mensagem gera testes frágeis.</details>

---

<!-- nav:start -->
[← Anterior: Kata de refatoração: o controller legado](kata-refatoracao.md) | [↑ Material de ensino](README.md) | [Próximo: Debates e objeções →](debates-e-objecoes.md)
<!-- nav:end -->
