package dev.cheatsheet.patterns;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P08 — Exceção própria estendendo {@code ErrorResponseException} (Spring 6+).
 * ✅ Para exceções que NASCEM na camada web (headers, versão de API, idempotency key).
 * ❌ Não use em exceções de domínio: seria HTTP dentro do domínio.
 *
 * <p>A exceção já carrega status, headers e o {@code ProblemDetail} pronto; o
 * {@code ResponseEntityExceptionHandler} sabe renderizá-la sem nenhum handler novo.
 */
class P08_ErrorResponseExceptionTest {

    /** Ex.: endpoint exige header Idempotency-Key. */
    static class MissingIdempotencyKeyException extends ErrorResponseException {
        MissingIdempotencyKeyException() {
            super(HttpStatus.BAD_REQUEST, problem(), null);
            getHeaders().add("Link", "</docs/idempotency>; rel=\"help\"");
        }

        private static ProblemDetail problem() {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                    "O header Idempotency-Key é obrigatório neste endpoint.");
            problem.setType(URI.create("/errors/IDEMPOTENCY_KEY_REQUIRED"));
            problem.setTitle("Idempotency-Key ausente");
            problem.setProperty("code", "IDEMPOTENCY_KEY_REQUIRED");
            return problem;
        }
    }

    @RestController
    static class PaymentController {
        @GetMapping("/payments")
        String pay(@RequestHeader(name = "Idempotency-Key", required = false) String key) {
            if (key == null) {
                throw new MissingIdempotencyKeyException();
            }
            return "ok";
        }
    }

    @RestControllerAdvice
    static class ProblemAdvice extends ResponseEntityExceptionHandler {
        // nenhum @ExceptionHandler extra: a superclasse já trata qualquer ErrorResponseException
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PaymentController())
            .setControllerAdvice(new ProblemAdvice()).build();

    @Test
    void excecaoCarregaOProprioProblemDetailEHeaders() throws Exception {
        mvc.perform(get("/payments"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"))
                .andExpect(jsonPath("$.type").value("/errors/IDEMPOTENCY_KEY_REQUIRED"))
                .andExpect(header().string("Link", "</docs/idempotency>; rel=\"help\""));
    }
}
