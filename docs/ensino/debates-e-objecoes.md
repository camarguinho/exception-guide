# Debates e objeções

[← Material de ensino](README.md)

As perguntas que **vão** aparecer quando você propuser o padrão ao time, com respostas fundamentadas.
Cada resposta tem a posição recomendada **e** quando ela não se aplica: quem lidera precisa reconhecer
as exceções à regra, senão perde credibilidade.

---

### "Por que não devolver 200 com `success: false`? É mais simples para o front."

O status é lido por toda a infraestrutura: retry de clientes HTTP, cache de CDN, métricas de SLO,
alertas, tracing, gateways. Com 200 em erro, o dashboard mostra 100% de sucesso durante um incidente.
Para o front, o custo é o mesmo: em vez de `if (!body.success)`, é `if (!response.ok)`.
**Quando não se aplica:** GraphQL e APIs batch (um 200 com sucesso/falha por item, porque o
transporte deu certo e cada item tem seu resultado).

### "400 ou 422? Todo mundo usa 400 para tudo."

Não há consenso global. O que importa é **consistência documentada**. A convenção proposta (400 =
forma, 422 = negócio) ajuda o cliente a saber se deve corrigir o formulário ou mostrar uma mensagem
de regra. Se o time preferir 400 para tudo, tudo bem, **desde que o code distinga os casos**.

### "Checked exceptions são mais seguras: o compilador obriga a tratar."

Na teoria. Na prática, degeneram em `catch (Exception e) {}` ou `throws Exception`, não compõem com
lambdas, vazam implementação e, no Spring, **não fazem rollback** por padrão. Use checked quando o
caller imediato consegue se recuperar de verdade (raro numa API web).

### "Por que não uma classe de exceção por erro? Fica mais explícito."

Leva a dezenas de classes, cada uma precisando de handler ou anotação, e nada garante formato
consistente. Base + categoria + code dá o mesmo poder de `catch` seletivo onde importa, com um
handler só. Crie uma classe nova quando alguém for **capturá-la especificamente**.

### "Result/Either é melhor que exceção."

Para erros que fazem parte do fluxo normal **e** que o caller imediato sempre trata (ex.: validação
que vira formulário), `Result` é uma boa opção. Para erros que atravessam várias camadas até a borda,
exceção + handler central é mais simples em Spring. Os dois convivem; ver a techpill ERR.3.

### "Expor o error code não é vazar informação?"

O code descreve o **tipo** de erro, que o cliente precisa saber para reagir. O que não pode vazar é
detalhe **interno**: stack, SQL, nome de classe, mensagem de terceiros. Atenção só a codes que revelam
existência de dado sensível (ex.: `EMAIL_ALREADY_REGISTERED` permite enumerar usuários: avalie com segurança).

### "A gente já tem um formato próprio (`ApiError`). Vale migrar para Problem Details?"

Vale, sem big bang: estenda `ResponseEntityExceptionHandler`, mantenha os campos legados como
extensões por um tempo (exercício 4) ou use advices com escopo por versão (P06). Ganho: padrão IETF,
suporte nativo no Spring e em clientes, erros do framework cobertos.

### "Logar só no handler? E se eu precisar de contexto do service?"

Coloque o contexto **na exceção** (mensagem com ids, `details`) e no MDC (correlationId, tenant). O
handler loga tudo de uma vez. Logar em cada camada gera três stack traces do mesmo erro e alertas duplicados.
**Exceção:** quando você trata e segue (fallback), logue ali, porque a exceção morre ali.

### "Catch-all em `Exception` é perigoso. Melhor não ter?"

Sem ele, bugs caem no `/error` com outro formato e sem correlationId. O perigo não é existir, é ser
**ingênuo**: estenda `ResponseEntityExceptionHandler`, tenha handlers específicos para exceções de
libs com status próprio, respeite `@ResponseStatus` e procure exceções da aplicação na causa (P09, P10).

### "Por que 500 para `IllegalArgumentException` do domínio? A entrada estava errada."

Porque a borda deveria ter barrado. Se a invariante do domínio disparou a partir de uma requisição,
existe um buraco na validação: é **nosso** bug. Devolver 400 esconderia o bug do monitoramento.

### "Retry resolve falhas de integração."

Só as transitórias (503, 504, conexão), só em operação idempotente, com backoff e limite. Repetir
502 por bug nosso ou 4xx só multiplica a carga e pode duplicar efeitos (cobrança dupla). Por isso o
contrato marca `retryable`.

### "Precisamos mesmo de teste para resposta de erro?"

O formato de erro é API pública e é a parte que mais quebra cliente sem ninguém perceber, porque
quase nunca é testada. A demo 5 do roteiro mostra: uma linha removida no handler e ninguém notaria sem o teste.
