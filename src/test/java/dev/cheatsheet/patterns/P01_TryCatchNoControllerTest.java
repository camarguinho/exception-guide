package dev.cheatsheet.patterns;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P01 — try/catch dentro do controller.  ❌ ANTI-PATTERN
 *
 * <p>Cada endpoint monta a própria resposta de erro. Funciona no endpoint onde alguém lembrou de
 * escrever o try/catch; no endpoint vizinho, a mesma exceção escapa e vira outra resposta.
 */
class P01_TryCatchNoControllerTest {

    static class ProductNotFoundException extends RuntimeException {
        ProductNotFoundException(String id) { super("Produto " + id + " não encontrado"); }
    }

    @RestController
    static class ProductController {

        @GetMapping("/products/{id}")
        ResponseEntity<?> get(@PathVariable String id) {
            try {
                throw new ProductNotFoundException(id);
            } catch (ProductNotFoundException e) {
                return ResponseEntity.status(404).body(Map.of("erro", e.getMessage()));
            } catch (Exception e) {
                // ❌ vaza a mensagem interna para o cliente
                return ResponseEntity.status(500).body(Map.of("erro", e.getMessage()));
            }
        }

        /** O colega esqueceu o try/catch neste endpoint. */
        @GetMapping("/products/{id}/price")
        String price(@PathVariable String id) {
            throw new ProductNotFoundException(id);
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProductController()).build();

    @Test
    void endpointComTryCatch_devolveFormatoInventadoNaHora() throws Exception {
        mvc.perform(get("/products/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Produto 42 não encontrado"));   // formato próprio: "erro"
    }

    @Test
    void endpointVizinhoSemTryCatch_aExcecaoEscapa() {
        // Nenhum resolver trata ProductNotFoundException: ela sai do DispatcherServlet.
        // Num servidor real vira 500 na página /error, com OUTRO formato de corpo.
        assertThatThrownBy(() -> mvc.perform(get("/products/42/price")))
                .isInstanceOf(ServletException.class)
                .hasCauseInstanceOf(ProductNotFoundException.class);
    }
}
