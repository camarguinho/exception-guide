package dev.cheatsheet.patterns;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P09 — Armadilha: {@code @ExceptionHandler(Exception.class)} "engole" o que outros resolvers tratariam.
 *
 * <p>O {@code ExceptionHandlerExceptionResolver} (dos {@code @ExceptionHandler}) roda ANTES do
 * {@code ResponseStatusExceptionResolver} e do {@code DefaultHandlerExceptionResolver}. Um handler
 * de {@code Exception} casa com tudo, então:
 * <ul>
 *   <li>exceções com {@code @ResponseStatus} viram 500;</li>
 *   <li>{@code ResponseStatusException} vira 500 (se o advice não estender {@code ResponseEntityExceptionHandler});</li>
 *   <li>o mesmo acontece com {@code AccessDeniedException} do method security (vira 500 em vez de 403).</li>
 * </ul>
 * Correções: estender {@code ResponseEntityExceptionHandler} (cobre as exceções do Spring) e,
 * no catch-all, respeitar {@code @ResponseStatus}.
 */
class P09_ArmadilhaDoCatchAllTest {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class LegacyNotFoundException extends RuntimeException {
    }

    @RestController
    static class Controller {
        @GetMapping("/annotated")
        String annotated() { throw new LegacyNotFoundException(); }

        @GetMapping("/rse")
        String rse() { throw new ResponseStatusException(HttpStatus.GONE); }
    }

    /** ❌ Ingênuo. */
    @RestControllerAdvice
    static class NaiveAdvice {
        @ExceptionHandler(Exception.class)
        ResponseEntity<ProblemDetail> any(Exception e) {
            return ResponseEntity.internalServerError().body(ProblemDetail.forStatus(500));
        }
    }

    /** ✅ Corrigido. */
    @RestControllerAdvice
    static class SafeAdvice extends ResponseEntityExceptionHandler {   // ResponseStatusException → handler herdado, mais específico
        @ExceptionHandler(Exception.class)
        ResponseEntity<ProblemDetail> any(Exception e) {
            ResponseStatus annotation = AnnotatedElementUtils.findMergedAnnotation(e.getClass(), ResponseStatus.class);
            HttpStatus status = annotation != null ? annotation.code() : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(ProblemDetail.forStatus(status));
        }
    }

    @Test
    void ingenuo_transformaTudoEm500() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Controller()).setControllerAdvice(new NaiveAdvice()).build();
        mvc.perform(get("/annotated")).andExpect(status().isInternalServerError());   // deveria ser 404
        mvc.perform(get("/rse")).andExpect(status().isInternalServerError());         // deveria ser 410
    }

    @Test
    void corrigido_respeitaOStatusPretendido() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Controller()).setControllerAdvice(new SafeAdvice()).build();
        mvc.perform(get("/annotated")).andExpect(status().isNotFound());
        mvc.perform(get("/rse")).andExpect(status().isGone());
    }
}
