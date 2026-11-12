# Plano de workshop — 3 encontros de 2h

<!-- nav:start -->
[↑ Material de ensino](README.md) | [Próximo: Roteiro de demos ao vivo →](roteiro-de-demos.md)
<!-- nav:end -->

Público: devs que já usam Spring Boot. Formato: presencial ou remoto, com o projeto clonado em cada máquina.
Pré-requisito técnico: JDK 21, Maven, `mvn test` passando.

---

## Encontro 1 — Fundamentos e desenho (Java puro)

**Objetivo:** todos saem distinguindo erro esperado de inesperado e sabendo por que a aplicação
usa exceções unchecked com error code.

| Tempo | Atividade | Material |
|---|---|---|
| 0:00–0:10 | **Abertura:** mostre uma resposta de erro ruim real do time (500 com stack trace, 200 com `success:false`). Pergunta: "o que o cliente faz com isso?" | — |
| 0:10–0:35 | Hierarquia, checked vs unchecked, causa, try-with-resources, `InterruptedException` | [cap. 01](../01-fundamentos-de-excecoes-em-java.md) |
| 0:35–0:45 | **Quiz relâmpago** (perguntas 1–5) | [quiz](quiz.md) |
| 0:45–1:10 | Esperado vs inesperado; hierarquia base + categoria + código; domínio não conhece HTTP; traduzir na fronteira | [cap. 02](../02-desenhando-excecoes-da-aplicacao.md) |
| 1:10–1:25 | Error codes: por que o status não basta; catálogo; evolução | [cap. 03](../03-error-codes.md) |
| 1:25–1:50 | **Exercícios 1 e 2** em duplas | [exercícios](exercicios.md) |
| 1:50–2:00 | Fechamento: cada dupla diz uma coisa que faria diferente no código atual | — |

**Tarefa de casa:** encontrar no código do time um exemplo de cada: exceção engolida, causa perdida,
log-and-rethrow. Trazer para o encontro 3 (kata).

---

## Encontro 2 — HTTP status e os patterns do Spring

**Objetivo:** todos conseguem prever qual handler o Spring vai chamar e escolher o padrão certo.

| Tempo | Atividade | Material |
|---|---|---|
| 0:00–0:25 | HTTP status: famílias, árvore de decisão, 400 vs 422, 5xx, "nunca repasse o status da dependência" | [cap. 04](../04-http-status-codes.md) |
| 0:25–0:35 | **Debate** 400 vs 422 (5 min cada lado, depois a convenção do projeto) | [debates](debates-e-objecoes.md) |
| 0:35–0:50 | Como o Spring resolve uma exceção (o diagrama dos resolvers) | [cap. 05](../05-tratamento-de-erros-no-spring-mvc.md), [cap. 12 §12.2](../12-catalogo-de-patterns.md) |
| 0:50–1:30 | **Demo ao vivo** do catálogo: P01 → P02 → P05 → P07, depois as armadilhas P09 e P10. Antes de cada teste: "o que vocês acham que sai?" | [roteiro, demos 1–3](roteiro-de-demos.md) |
| 1:30–1:50 | **Exercícios 3 e 4** | [exercícios](exercicios.md) |
| 1:50–2:00 | Fechamento: guia de decisão do cap. 12 §12.4 | — |

---

## Encontro 3 — Contrato, validação, operação e adoção

**Objetivo:** o time sai com um padrão decidido e um plano de adoção.

| Tempo | Atividade | Material |
|---|---|---|
| 0:00–0:20 | Problem Details (RFC 9457) e o contrato do projeto | [cap. 06](../06-problem-details-rfc-9457.md) |
| 0:20–0:35 | Validação em três camadas | [cap. 07](../07-validacao.md) |
| 0:35–0:50 | Logging (logar uma vez, nível por categoria, correlation id) e métricas por code | [cap. 09](../09-logging-e-observabilidade.md) |
| 0:50–1:00 | Testes de contrato (demo da asserção reutilizável) | [cap. 10](../10-testando-erros.md), [roteiro, demo 4](roteiro-de-demos.md) |
| 1:00–1:35 | **Kata de refatoração** em grupos de 3, com o código trazido pelo time ou o kata pronto | [kata](kata-refatoracao.md) |
| 1:35–1:55 | **Decisão:** revisar o ADR-001 juntos e ajustar ao contexto do time | [ADR-001](adr-001-padrao-de-tratamento-de-erros.md) |
| 1:55–2:00 | Próximos passos: quem implementa o advice, prazo, checklist no template de PR | [checklist](../11-checklist-e-anti-patterns.md) |

---

## Depois do workshop

- Adicione o [checklist do cap. 11](../11-checklist-e-anti-patterns.md) ao template de PR.
- Nas primeiras semanas, revise pessoalmente os PRs que mexem em tratamento de erro.
- Exercícios 8–10 como desafio para quem quiser se aprofundar.
- Aplique o [quiz](quiz.md) completo depois de um mês para medir retenção.

---

<!-- nav:start -->
[↑ Material de ensino](README.md) | [Próximo: Roteiro de demos ao vivo →](roteiro-de-demos.md)
<!-- nav:end -->
