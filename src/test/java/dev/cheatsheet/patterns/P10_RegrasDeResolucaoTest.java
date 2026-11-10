package dev.cheatsheet.patterns;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P10 — Como o Spring escolhe QUAL {@code @ExceptionHandler} roda.
 *
 * <ol>
 *   <li><b>O tipo mais próximo vence</b> (menor distância na hierarquia), não a ordem dos métodos.</li>
 *   <li>Um handler pode declarar <b>vários tipos</b>; o parâmetro deve ser o supertipo comum.</li>
 *   <li><b>Causa:</b> se NENHUM handler casa com a exceção lançada, o Spring tenta a causa
 *       (e a causa da causa...). O handler recebe a exceção da causa.</li>
 *   <li>Por isso um handler de {@code Exception} desliga o casamento por causa: a exceção de fora
 *       sempre casa com ele primeiro.</li>
 * </ol>
 */
class P10_RegrasDeResolucaoTest {

    static class AppException extends RuntimeException { }
    static class NotFoundException extends AppException { }
    static class GoneException extends AppException { }
    static class TimeoutException extends RuntimeException { }

    @RestController
    static class Controller {
        @GetMapping("/not-found") String notFound() { throw new NotFoundException(); }
        @GetMapping("/gone") String gone() { throw new GoneException(); }
        @GetMapping("/timeout") String timeout() { throw new TimeoutException(); }
        @GetMapping("/wrapped") String wrapped() { throw new IllegalStateException("embrulho", new NotFoundException()); }
    }

    @RestControllerAdvice
    static class Advice {
        @ExceptionHandler(AppException.class)
        ProblemDetail app(AppException e) { return problem(HttpStatus.UNPROCESSABLE_CONTENT, "AppException"); }

        @ExceptionHandler(NotFoundException.class)        // mais específico que AppException: vence para NotFound
        ProblemDetail notFound(NotFoundException e) { return problem(HttpStatus.NOT_FOUND, "NotFoundException"); }

        @ExceptionHandler({GoneException.class, TimeoutException.class})   // vários tipos, parâmetro supertipo
        ProblemDetail many(RuntimeException e) { return problem(HttpStatus.SERVICE_UNAVAILABLE, e.getClass().getSimpleName()); }
    }

    @RestControllerAdvice
    static class AdviceWithCatchAll extends Advice {
        @ExceptionHandler(Exception.class)
        ProblemDetail any(Exception e) { return problem(HttpStatus.INTERNAL_SERVER_ERROR, "catch-all"); }
    }

    private static ProblemDetail problem(HttpStatus status, String handler) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setProperty("handler", handler);
        return problem;
    }

    @Test
    void tipoMaisEspecificoVence() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Controller()).setControllerAdvice(new Advice()).build();
        mvc.perform(get("/not-found")).andExpect(jsonPath("$.handler").value("NotFoundException"));
    }

    @Test
    void handlerComVariosTipos() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Controller()).setControllerAdvice(new Advice()).build();
        mvc.perform(get("/gone")).andExpect(jsonPath("$.handler").value("GoneException"));
        mvc.perform(get("/timeout")).andExpect(jsonPath("$.handler").value("TimeoutException"));
    }

    @Test
    void semCatchAll_casaPelaCausa() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Controller()).setControllerAdvice(new Advice()).build();
        mvc.perform(get("/wrapped"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.handler").value("NotFoundException"));
    }

    @Test
    void comCatchAll_aCausaNaoEhMaisConsultada() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Controller()).setControllerAdvice(new AdviceWithCatchAll()).build();
        mvc.perform(get("/wrapped"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.handler").value("catch-all"));
    }
}
