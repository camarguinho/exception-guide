package dev.cheatsheet.patterns;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P04 — {@code @ExceptionHandler} dentro do controller.  ⚠️ Só para tratamento específico daquele controller.
 *
 * <p>Regras mostradas aqui:
 * <ol>
 *   <li>O handler local só vale para o próprio controller.</li>
 *   <li>Handler local tem <b>precedência</b> sobre o de um {@code @ControllerAdvice}.</li>
 * </ol>
 */
class P04_ExceptionHandlerLocalTest {

    static class StockException extends RuntimeException {
    }

    @RestController
    static class StockController {
        @GetMapping("/stock")
        String stock() { throw new StockException(); }

        /** Caso de uso real: este endpoint precisa de um tratamento diferente do resto da API. */
        @ExceptionHandler(StockException.class)
        ProblemDetail handle(StockException e) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "tratado LOCALMENTE");
        }
    }

    @RestController
    static class CartController {
        @GetMapping("/cart")
        String cart() { throw new StockException(); }
    }

    @RestControllerAdvice
    static class GlobalAdvice {
        @ExceptionHandler(StockException.class)
        ProblemDetail handle(StockException e) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, "tratado GLOBALMENTE");
        }
    }

    @Test
    void handlerLocalNaoAlcancaOutrosControllers() {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new StockController(), new CartController()).build();
        assertThatThrownBy(() -> mvc.perform(get("/cart"))).isInstanceOf(ServletException.class);
    }

    @Test
    void localVenceGlobal_globalCobreOResto() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new StockController(), new CartController())
                .setControllerAdvice(new GlobalAdvice()).build();

        mvc.perform(get("/stock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("tratado LOCALMENTE"));
        mvc.perform(get("/cart"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("tratado GLOBALMENTE"));
    }
}
