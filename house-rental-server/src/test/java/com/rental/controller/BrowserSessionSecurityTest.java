package com.rental.controller;

import com.rental.config.LoginInterceptor;
import com.rental.config.SecurityConfig;
import com.rental.config.WebMvcConfig;
import com.rental.entity.SysUser;
import com.rental.service.RequestLimitService;
import com.rental.service.TokenService;
import com.rental.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BrowserSessionSecurityTest {
    private static final String TOKEN = "a".repeat(32);
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfig.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
        SysUser user = new SysUser();
        user.setId(8L);
        user.setRoleCode("TENANT");
        user.setStatus(1);
        UserService users = context.getBean(UserService.class);
        when(users.login(any())).thenReturn(user);
        when(users.getById(8L)).thenReturn(user);
        TokenService tokens = context.getBean(TokenService.class);
        when(tokens.createToken(any())).thenReturn(TOKEN);
        when(tokens.getUserIdByToken(TOKEN)).thenReturn(8L);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void loginRequiresCsrfEvenWithForgedBearerHeader() throws Exception {
        mvc.perform(post("/auth/login").servletPath("/auth/login")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType("application/json").content("{\"username\":\"test\",\"password\":\"test-password\"}"))
                .andExpect(status().isForbidden());
        verify(context.getBean(UserService.class), never()).login(any());
    }

    @Test
    void browserLoginUsesHttpOnlyCookieAndNoResponseToken() throws Exception {
        Cookie csrf = csrfCookie();
        var response = mvc.perform(post("/auth/login").servletPath("/auth/login")
                        .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType("application/json").content("{\"username\":\"test\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse();
        Cookie session = response.getCookie("rental_session");
        assertNotNull(session);
        assertTrue(session.isHttpOnly());
        assertTrue(response.getHeaders("Set-Cookie").stream().anyMatch(value -> value.contains("SameSite=Strict")));
        assertFalse(response.getContentAsString().contains(TOKEN));
        mvc.perform(get("/auth/me").servletPath("/auth/me").cookie(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(8));
    }

    @Test
    void cookieMutationRequiresCsrfButInternalBearerDoesNot() throws Exception {
        Cookie session = new Cookie("rental_session", TOKEN);
        mvc.perform(post("/tenant/check").servletPath("/tenant/check").cookie(session))
                .andExpect(status().isForbidden());
        Cookie csrf = csrfCookie();
        mvc.perform(post("/tenant/check").servletPath("/tenant/check").cookie(session, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isOk()).andExpect(content().string("ok"));
        mvc.perform(post("/tenant/check").servletPath("/tenant/check").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk());
        mvc.perform(post("/tenant/check").servletPath("/tenant/check").header("Authorization", "Bearer " + "b".repeat(32)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesServerSessionAndClearsCookie() throws Exception {
        Cookie csrf = csrfCookie();
        var response = mvc.perform(post("/auth/logout").servletPath("/auth/logout")
                        .cookie(new Cookie("rental_session", TOKEN), csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isOk()).andReturn().getResponse();
        verify(context.getBean(TokenService.class)).deleteToken(TOKEN);
        assertEquals(0, response.getCookie("rental_session").getMaxAge());
    }

    private Cookie csrfCookie() throws Exception {
        Cookie cookie = mvc.perform(get("/auth/csrf").servletPath("/auth/csrf"))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        return cookie;
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, WebMvcConfig.class})
    static class TestConfig {
        @Bean UserService users() { return mock(UserService.class); }
        @Bean TokenService tokens() { return mock(TokenService.class); }
        @Bean RequestLimitService limits() { return mock(RequestLimitService.class); }
        @Bean AuthController auth(UserService users, TokenService tokens, RequestLimitService limits) {
            return new AuthController(users, tokens, limits);
        }
        @Bean LoginInterceptor login(TokenService tokens, UserService users) {
            return new LoginInterceptor(tokens, users, new ObjectMapper());
        }
        @Bean MutationController mutation() { return new MutationController(); }
    }

    @RestController
    static class MutationController {
        @PostMapping("/tenant/check")
        public String check() { return "ok"; }
    }
}
