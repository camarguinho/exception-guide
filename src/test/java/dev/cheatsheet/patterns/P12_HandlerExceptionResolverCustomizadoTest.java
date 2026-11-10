package dev.cheatsheet.patterns;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P12 — {@code HandlerExceptionResolver} customizado.  ⚠️ Baixo nível: raramente necessário.
 *
 * <p>É o mecanismo por baixo de {@code @ExceptionHandler}, {@code @ResponseStatus} etc. Implementar
 * um faz sentido para infraestrutura transversal (ex.: uma lib interna que trata uma família de
 * exceções em todas as aplicações da empresa) ou integração com código legado. Retornar
 * {@code null} = "não é comigo, próximo resolver". Retornar um {@code ModelAndView} vazio =
 * "já escrevi a resposta".
 *
 * <p>Num app Spring Boot, basta registrar como {@code @Bean}; a ordem ({@link Ordered}) define se ele
 * roda antes ou depois dos resolvers padrão.
 */
class P12_HandlerExceptionResolverCustomizadoTest {

    static class TenantSuspendedException extends RuntimeException { }

    @RestController
    static class Controller {
        @GetMapping("/data") String data() { throw new TenantSuspendedException(); }
    }

    static class TenantSuspendedResolver implements HandlerExceptionResolver, Ordered {

        @Override
        public @Nullable ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                                       @Nullable Object handler, Exception ex) {
            if (!(ex instanceof TenantSuspendedException)) {
                return null;                                   // não é comigo
            }
            try {
                response.setStatus(403);
                response.setContentType("application/problem+json");
                response.getWriter().write("{\"status\":403,\"code\":\"TENANT_SUSPENDED\"}");
            } catch (IOException e) {
                return null;
            }
            return new ModelAndView();                         // resposta já escrita
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }

    @Test
    void resolverEscreveARespostaDiretamente() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Controller())
                .setHandlerExceptionResolvers(List.of(new TenantSuspendedResolver()))
                .build();
        mvc.perform(get("/data"))
                .andExpect(status().isForbidden())
                .andExpect(content().json("{\"code\":\"TENANT_SUSPENDED\"}"));
    }
}
