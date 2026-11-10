/**
 * Catálogo executável dos padrões de tratamento de exceção no Spring MVC / Spring Boot.
 *
 * <p>Cada classe {@code PNN_...Test} isola UM padrão: controller, exceção e (quando há) advice
 * próprios, montados com {@code MockMvcBuilders.standaloneSetup}. Assim um padrão não interfere
 * no outro nem no {@code GlobalExceptionHandler} da aplicação.
 *
 * <p>O pacote fica FORA de {@code dev.cheatsheet.exceptions} de propósito: assim o component scan
 * da aplicação não registra estes controllers nos testes {@code @SpringBootTest}.
 *
 * <p>Leia junto com docs/12-catalogo-de-patterns.md.
 */
package dev.cheatsheet.patterns;
