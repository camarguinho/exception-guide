package dev.cheatsheet.patterns;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P02 — {@code @ResponseStatus} na classe da exceção.  ⚠️ OK para protótipo; ruim como padrão.
 *
 * <p>Tratado pelo {@code ResponseStatusExceptionResolver}, que chama {@code response.sendError(status, reason)}.
 * Resultado: o status sai certo, mas o corpo é o da página {@code /error} do Spring Boot, não o seu
 * contrato. E a exceção (domínio) passa a depender de {@code spring-web}.
 */
class P02_ResponseStatusAnotacaoTest {

    @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Produto não encontrado")
    static class ProductNotFoundException extends RuntimeException {
    }

    /** A anotação é herdada pelas subclasses. */
    static class DiscontinuedProductException extends ProductNotFoundException {
    }

    @RestController
    static class ProductController {
        @GetMapping("/products/{id}")
        String get(@PathVariable String id) { throw new ProductNotFoundException(); }

        @GetMapping("/products/{id}/legacy")
        String legacy(@PathVariable String id) { throw new DiscontinuedProductException(); }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProductController()).build();

    @Test
    void statusVemDaAnotacao_corpoNao() throws Exception {
        var result = mvc.perform(get("/products/42"))
                .andExpect(status().isNotFound())
                .andReturn();
        // sendError(404, reason): sem corpo JSON aqui; num servidor real, o container
        // encaminha para /error e o BasicErrorController escreve o corpo padrão do Boot.
        assertThat(result.getResponse().getErrorMessage()).isEqualTo("Produto não encontrado");
        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }

    @Test
    void subclasseHerdaAAnotacao() throws Exception {
        mvc.perform(get("/products/42/legacy")).andExpect(status().isNotFound());
    }
}
