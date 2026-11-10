package dev.cheatsheet.patterns;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P13 — Exceção lançada num {@code Filter} (antes do DispatcherServlet).
 *
 * <p>O {@code @RestControllerAdvice} NÃO vê exceções de filters (ex.: validação de API key,
 * rate limit, Spring Security). Duas saídas:
 * <ol>
 *   <li>Deixar escapar: o container encaminha para {@code /error} ({@code BasicErrorController});
 *       configure {@code server.error.*} e {@code ErrorAttributes} para não vazar nada.</li>
 *   <li>✅ Capturar no filter e <b>delegar ao {@code HandlerExceptionResolver}</b>: o mesmo advice
 *       formata a resposta, e o contrato fica único. É o mesmo truque usado com o
 *       {@code AuthenticationEntryPoint} do Spring Security.</li>
 * </ol>
 * Num app Boot, injete o resolver com {@code @Qualifier("handlerExceptionResolver")}. Aqui ele é montado à mão.
 */
class P13_ExcecaoEmFilterTest {

    static class InvalidApiKeyException extends RuntimeException { }

    @RestController
    static class Controller {
        @GetMapping("/data") String data() { return "ok"; }
    }

    @RestControllerAdvice
    static class Advice {
        @ExceptionHandler(InvalidApiKeyException.class)
        ProblemDetail invalidKey(InvalidApiKeyException e) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "API key inválida");
            problem.setProperty("code", "INVALID_API_KEY");
            return problem;
        }
    }

    /** ❌ Deixa a exceção escapar do filter. */
    static class NaiveApiKeyFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            if (req.getHeader("X-Api-Key") == null) {
                throw new InvalidApiKeyException();
            }
            chain.doFilter(req, res);
        }
    }

    /** ✅ Delega ao HandlerExceptionResolver: o advice formata. */
    static class DelegatingApiKeyFilter extends OncePerRequestFilter {
        private final HandlerExceptionResolver resolver;

        DelegatingApiKeyFilter(HandlerExceptionResolver resolver) { this.resolver = resolver; }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            if (req.getHeader("X-Api-Key") == null) {
                resolver.resolveException(req, res, null, new InvalidApiKeyException());
                return;                                         // não segue a cadeia
            }
            chain.doFilter(req, res);
        }
    }

    private static HandlerExceptionResolver resolverWithAdvice() {
        StaticWebApplicationContext context = new StaticWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.registerSingleton("advice", Advice.class);
        context.refresh();
        ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
        // Montado à mão, o resolver não tem conversor JSON; o bean "handlerExceptionResolver" do Boot já tem.
        resolver.setMessageConverters(List.of(new JacksonJsonHttpMessageConverter()));
        resolver.setApplicationContext(context);
        resolver.afterPropertiesSet();
        return resolver;
    }

    @Test
    void filterIngenuo_adviceNaoEnxerga() {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Controller()).setControllerAdvice(new Advice())
                .addFilters(new NaiveApiKeyFilter()).build();
        // A exceção sai crua (num servidor real: /error com o corpo padrão do Boot).
        assertThatThrownBy(() -> mvc.perform(get("/data"))).isInstanceOf(InvalidApiKeyException.class);
    }

    @Test
    void filterDelegando_mesmoContratoDoAdvice() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Controller())
                .addFilters(new DelegatingApiKeyFilter(resolverWithAdvice())).build();
        mvc.perform(get("/data"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"));
    }

    @Test
    void comApiKey_passa() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Controller())
                .addFilters(new DelegatingApiKeyFilter(resolverWithAdvice())).build();
        mvc.perform(get("/data").header("X-Api-Key", "k")).andExpect(status().isOk());
    }
}
