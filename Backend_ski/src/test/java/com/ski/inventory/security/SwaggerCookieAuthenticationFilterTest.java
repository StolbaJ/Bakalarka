package com.ski.inventory.security;

import com.ski.inventory.controller.SwaggerEntryController;
import com.ski.inventory.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SwaggerCookieAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private SwaggerCookieAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SwaggerCookieAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotFilter_swaggerUiPath_returnsFalse() {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void shouldNotFilter_apiDocsPath_returnsFalse() {
        when(request.getRequestURI()).thenReturn("/v3/api-docs/openapi.json");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void shouldNotFilter_otherPath_returnsTrue() {
        when(request.getRequestURI()).thenReturn("/api/technician/skis");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_loginPath_returnsTrue() {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void doFilterInternal_bearerTokenPresent_delegatesToJwtFilter() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer some.jwt.token");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_validSwaggerCookie_setsAdminAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        String cookieToken = "valid.swagger.token";
        Cookie swaggerCookie = new Cookie(SwaggerEntryController.SWAGGER_ACCESS_COOKIE, cookieToken);
        when(request.getCookies()).thenReturn(new Cookie[]{swaggerCookie});
        when(jwtService.validateToken(cookieToken, "swagger")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("swagger");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    void doFilterInternal_invalidSwaggerCookie_noAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        String cookieToken = "invalid.swagger.token";
        Cookie swaggerCookie = new Cookie(SwaggerEntryController.SWAGGER_ACCESS_COOKIE, cookieToken);
        when(request.getCookies()).thenReturn(new Cookie[]{swaggerCookie});
        when(jwtService.validateToken(cookieToken, "swagger")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_noCookies_noAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_emptyCookies_noAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{});

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_wrongCookieName_noAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        Cookie wrongCookie = new Cookie("SOME_OTHER_COOKIE", "some.token");
        when(request.getCookies()).thenReturn(new Cookie[]{wrongCookie});

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_jwtServiceThrows_swallowsException() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        String cookieToken = "broken.token";
        Cookie swaggerCookie = new Cookie(SwaggerEntryController.SWAGGER_ACCESS_COOKIE, cookieToken);
        when(request.getCookies()).thenReturn(new Cookie[]{swaggerCookie});
        when(jwtService.validateToken(cookieToken, "swagger")).thenThrow(new RuntimeException("JWT error"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
