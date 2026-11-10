# 🎓 Material de ensino: exception handling em Spring Boot

[← Ebook](../README.md)

Material para quem vai **liderar** o tema no time: conduzir um workshop, revisar código, fechar um
padrão e responder às objeções que vão aparecer. Tudo aponta para o código executável do projeto.

## Conteúdo

| Arquivo | Para quê |
|---|---|
| [Plano de workshop](plano-de-workshop.md) | 3 encontros de 2h: objetivos, agenda cronometrada, demos e exercícios |
| [Roteiro de demos ao vivo](roteiro-de-demos.md) | Passo a passo de cada demo, com o que mostrar e o que perguntar |
| [Exercícios com gabarito](exercicios.md) | 10 exercícios práticos no próprio projeto, do básico ao avançado |
| [Kata de refatoração](kata-refatoracao.md) | Código "legado" cheio de anti-patterns para refatorar em grupo |
| [Quiz](quiz.md) | 20 perguntas para fixação, avaliação ou entrevista |
| [Debates e objeções](debates-e-objecoes.md) | As perguntas difíceis que o time vai fazer, com respostas fundamentadas |
| [ADR-001: padrão de tratamento de erros](adr-001-padrao-de-tratamento-de-erros.md) | Decisão pronta para o time adaptar e adotar |

## Como se preparar (antes do primeiro encontro)

1. Leia o ebook na ordem rápida: [04](../04-http-status-codes.md) → [05](../05-tratamento-de-erros-no-spring-mvc.md) → [12](../12-catalogo-de-patterns.md) → [11](../11-checklist-e-anti-patterns.md).
2. Rode tudo: `mvn test` (todos verdes) e `mvn spring-boot:run`.
3. Faça você mesmo os exercícios 1, 4 e 7. Quem ensina precisa ter errado antes.
4. Ensaie as demos 1 a 3 do [roteiro](roteiro-de-demos.md). Elas carregam o encontro 2.
5. Leia [Debates e objeções](debates-e-objecoes.md): é ali que a sua autoridade no tema é testada.

## Objetivos de aprendizagem da trilha

Ao final, cada participante consegue:

1. Explicar a diferença entre erro **esperado** e **inesperado** e como cada um é tratado.
2. Escolher o **HTTP status** correto e justificar (400 vs 422 vs 409; 502 vs 503 vs 504).
3. Descrever a **ordem de resolução** de exceções do Spring MVC e prever o resultado de um handler.
4. Implementar um `@RestControllerAdvice` estendendo `ResponseEntityExceptionHandler` com Problem Details.
5. Reconhecer e corrigir os anti-patterns em code review (try/catch no controller, log-and-rethrow, catch-all ingênuo).
6. Escrever testes de **contrato** de erro.

## Dicas de condução

- **Mostre falhando antes de mostrar certo.** Cada padrão do catálogo tem a versão ruim; rode-a primeiro.
- **Pergunte antes de rodar.** "O que vocês acham que este teste devolve?" e só então execute.
  O P09 e o P10 quase sempre surpreendem a sala.
- **Traga código do time** (anonimizado) para o kata. Reconhecer o próprio código motiva mais que exemplo pronto.
- **Feche com uma decisão** registrada (ADR). Workshop sem decisão vira palestra.
