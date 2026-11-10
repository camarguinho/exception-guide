package dev.cheatsheet.patterns;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P06 — Vários advices: escopo ({@code assignableTypes}/{@code basePackages}/{@code annotations}) e {@code @Order}.
 * ✅ Útil em migração: a API v1 mantém o formato legado enquanto a v2 usa Problem Details.
 *
 * <p>Regra: o Spring percorre os advices em ordem ({@code @Order}, menor primeiro) e usa o
 * PRIMEIRO que (a) se aplica ao controller e (b) tem um handler para a exceção.
 * Sem {@code @Order}, a ordem entre advices é indefinida: sempre declare quando houver mais de um.
 */
class P06_EscopoEOrdemDeAdvicesTest {

    static class NotFoundException extends RuntimeException {
    }

    @RestController
    static class V1Controller {
        @GetMapping("/v1/products/42")
        String get() { throw new NotFoundException(); }
    }

    @RestController
    static class V2Controller {
        @GetMapping("/v2/products/42")
        String get() { throw new NotFoundException(); }
    }

    /** Só para o V1, e antes de qualquer outro. */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @RestControllerAdvice(assignableTypes = V1Controller.class)
    static class LegacyV1Advice {
        @ExceptionHandler(NotFoundException.class)
        ResponseEntity<Map<String, Object>> handle(NotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("errorCode", 1001, "errorMessage", "not found"));
        }
    }

    /** Todo o resto. */
    @Order(Ordered.LOWEST_PRECEDENCE)
    @RestControllerAdvice
    static class GlobalProblemAdvice {
        @ExceptionHandler(NotFoundException.class)
        ProblemDetail handle(NotFoundException e) {
            return ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new V1Controller(), new V2Controller())
            .setControllerAdvice(new GlobalProblemAdvice(), new LegacyV1Advice())
            .build();

    @Test
    void v1UsaFormatoLegado() throws Exception {
        mvc.perform(get("/v1/products/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(1001));
    }

    @Test
    void v2UsaProblemDetail() throws Exception {
        mvc.perform(get("/v2/products/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.errorCode").doesNotExist());
    }
}
