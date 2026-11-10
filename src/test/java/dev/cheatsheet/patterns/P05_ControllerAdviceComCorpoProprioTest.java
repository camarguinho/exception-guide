package dev.cheatsheet.patterns;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P05 — {@code @RestControllerAdvice} global com um DTO de erro próprio ({@code ApiError}).
 * ⚠️ O padrão "clássico" (pré-Spring 6), ainda muito comum no mercado.
 *
 * <p>✅ Centraliza; controllers ficam limpos.<br>
 * ❌ Formato proprietário (cada empresa inventa o seu).<br>
 * ❌ <b>Buraco:</b> erros que o próprio Spring MVC detecta (405, 415, JSON inválido, 404 de rota)
 *    continuam saindo no formato padrão, porque o advice não estende {@code ResponseEntityExceptionHandler}.
 */
class P05_ControllerAdviceComCorpoProprioTest {

    record ApiError(Instant timestamp, int status, String error, String message, String path) {
    }

    static class ProductNotFoundException extends RuntimeException {
        ProductNotFoundException(String msg) { super(msg); }
    }

    @RestController
    static class ProductController {
        @GetMapping("/products/42")
        String get() { throw new ProductNotFoundException("Produto 42 não encontrado"); }
    }

    @RestControllerAdvice
    static class ApiErrorAdvice {
        @ExceptionHandler(ProductNotFoundException.class)
        ResponseEntity<ApiError> notFound(ProductNotFoundException e, HttpServletRequest request) {
            HttpStatus status = HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(new ApiError(
                    Instant.now(), status.value(), status.getReasonPhrase(), e.getMessage(), request.getRequestURI()));
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProductController())
            .setControllerAdvice(new ApiErrorAdvice()).build();

    @Test
    void erroDeDominio_formatoProprio() throws Exception {
        mvc.perform(get("/products/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Produto 42 não encontrado"))
                .andExpect(jsonPath("$.path").value("/products/42"));
    }

    @Test
    void erroDoFramework_escapaDoFormato() throws Exception {
        // 405 é resolvido pelo DefaultHandlerExceptionResolver: sendError, sem ApiError.
        var result = mvc.perform(delete("/products/42")).andExpect(status().isMethodNotAllowed()).andReturn();
        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }
}
