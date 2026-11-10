package dev.cheatsheet.patterns;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P07 — {@code @RestControllerAdvice} estendendo {@code ResponseEntityExceptionHandler} + {@code ProblemDetail}.
 * ✅ O PADRÃO RECOMENDADO (Spring 6+ / Boot 3+). É o que o {@code GlobalExceptionHandler} do projeto usa.
 *
 * <p>Você herda o tratamento de TODAS as exceções do Spring MVC (405, 415, 400 de JSON inválido,
 * validação...) já no formato RFC 9457, e sobrescreve {@code handleExceptionInternal} para
 * acrescentar seus campos em todas elas. Fecha o "buraco" do P05.
 */
class P07_ResponseEntityExceptionHandlerTest {

    record CreateProduct(@NotBlank String name) {
    }

    static class ProductAlreadyExistsException extends RuntimeException {
    }

    @RestController
    static class ProductController {
        @PostMapping("/products")
        String create(@Valid @RequestBody CreateProduct request) { throw new ProductAlreadyExistsException(); }
    }

    @RestControllerAdvice
    static class ProblemAdvice extends ResponseEntityExceptionHandler {

        /** Erros da aplicação. */
        @ExceptionHandler(ProductAlreadyExistsException.class)
        ProblemDetail conflict(ProductAlreadyExistsException e) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Produto já existe");
            problem.setProperty("code", "PRODUCT_ALREADY_EXISTS");
            return problem;
        }

        /** Funil de tudo que a superclasse trata: aqui todo erro do framework ganha um "code". */
        @Override
        protected @Nullable ResponseEntity<Object> handleExceptionInternal(
                Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
            ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, status, request);
            if (response != null && response.getBody() instanceof ProblemDetail problem) {
                problem.setProperty("code", "HTTP_" + status.value());
            }
            return response;
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProductController())
            .setControllerAdvice(new ProblemAdvice()).build();

    @Test
    void erroDeDominio() throws Exception {
        mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("PRODUCT_ALREADY_EXISTS"));
    }

    @Test
    void validacao_mesmoFormato() throws Exception {
        mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("HTTP_400"));
    }

    @Test
    void jsonMalformado_mesmoFormato() throws Exception {
        mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HTTP_400"));
    }

    @Test
    void metodoNaoSuportado_mesmoFormato() throws Exception {
        mvc.perform(delete("/products"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("HTTP_405"));
    }

    @Test
    void mediaTypeNaoSuportado_mesmoFormato() throws Exception {
        mvc.perform(post("/products").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("HTTP_415"));
    }
}
