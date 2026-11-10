package dev.cheatsheet.patterns;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P03 — {@code throw new ResponseStatusException(status, reason)}.  ⚠️ Bom para casos pontuais NO CONTROLLER.
 *
 * <p>Não exige classe nova nem anotação. Desde o Spring 6 ela é uma {@code ErrorResponseException}:
 * carrega um {@code ProblemDetail}. Mas o corpo só aparece se houver um advice que estenda
 * {@code ResponseEntityExceptionHandler}; sem ele, cai no mesmo {@code sendError} do P02.
 *
 * <p>No service, é HTTP vazando para o domínio: evite.
 */
class P03_ResponseStatusExceptionTest {

    @RestController
    static class ProductController {
        @GetMapping("/products/{id}")
        String get(@PathVariable String id) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto " + id + " não encontrado");
        }
    }

    @RestControllerAdvice
    static class ProblemAdvice extends ResponseEntityExceptionHandler {
    }

    @Test
    void semAdvice_statusCertoMasSemCorpo() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProductController()).build();
        var result = mvc.perform(get("/products/42")).andExpect(status().isNotFound()).andReturn();
        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }

    @Test
    void comAdviceResponseEntityExceptionHandler_viraProblemDetail() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProductController())
                .setControllerAdvice(new ProblemAdvice()).build();
        mvc.perform(get("/products/42"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Produto 42 não encontrado"));
    }
}
