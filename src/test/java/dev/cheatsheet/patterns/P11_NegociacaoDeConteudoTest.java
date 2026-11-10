package dev.cheatsheet.patterns;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P11 — Negociação de conteúdo no handler: {@code @ExceptionHandler(produces = ...)} (Spring 6.2+).
 * ✅ Quando a mesma aplicação atende API (JSON) e navegador (HTML).
 *
 * <p>O Spring escolhe o handler pelo header {@code Accept} da requisição.
 */
class P11_NegociacaoDeConteudoTest {

    static class NotFoundException extends RuntimeException { }

    @RestController
    static class Controller {
        @GetMapping("/report") String report() { throw new NotFoundException(); }
    }

    @RestControllerAdvice
    static class Advice {
        @ExceptionHandler(exception = NotFoundException.class, produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        ResponseEntity<ProblemDetail> json(NotFoundException e) {
            return ResponseEntity.status(404).body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Relatório não encontrado"));
        }

        @ExceptionHandler(exception = NotFoundException.class, produces = MediaType.TEXT_HTML_VALUE)
        ResponseEntity<String> html(NotFoundException e) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_HTML)
                    .body("<h1>Relatório não encontrado</h1>");
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new Controller()).setControllerAdvice(new Advice()).build();

    @Test
    void clienteApi_recebeProblemJson() throws Exception {
        mvc.perform(get("/report").accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Relatório não encontrado"));
    }

    @Test
    void navegador_recebeHtml() throws Exception {
        mvc.perform(get("/report").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<h1>")));
    }
}
