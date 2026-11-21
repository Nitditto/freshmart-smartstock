package com.freshmart.smartstock.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freshmart.smartstock.config.AppProperties;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final Long USER_ID = 5L;

    @Mock
    private UserRepository userRepository;

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private String token;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties("Asia/Ho_Chi_Minh",
                new AppProperties.Jwt("unit-test-secret-unit-test-secret-1234", 60),
                new AppProperties.Llm("", "", "", 30),
                new AppProperties.Seed(false, ""));
        jwtService = new JwtService(props, Clock.fixed(Instant.now(), ZoneOffset.UTC));
        filter = new JwtAuthenticationFilter(jwtService, userRepository);
        token = jwtService.generateToken(new AuthUser(USER_ID, "u@x.com", Role.STAFF));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenOfActiveUserAuthenticatesTheRequest() throws Exception {
        when(userRepository.existsByIdAndActiveTrue(USER_ID)).thenReturn(true);

        run("Bearer " + token);

        AuthUser principal = (AuthUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals(USER_ID, principal.id());
    }

    @Test
    void validTokenOfLockedOrDeletedUserIsNotAuthenticated() throws Exception {
        when(userRepository.existsByIdAndActiveTrue(USER_ID)).thenReturn(false);

        run("Bearer " + token);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void garbageTokenIsNotAuthenticatedAndSkipsTheDatabase() throws Exception {
        run("Bearer not.a.jwt");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userRepository, never()).existsByIdAndActiveTrue(USER_ID);
    }

    @Test
    void missingHeaderContinuesTheChainUnauthenticated() throws Exception {
        MockFilterChain chain = run(null);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    private MockFilterChain run(String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }
}
