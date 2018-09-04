package com.j9soft.poc.alarms;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * This Filter is used in production mode, i.e. it verifies signatures for JWT.
 *
 * Inspiration: https://aboullaite.me/spring-boot-token-authentication-using-jwt/
 *
 * @see AuthorizationHeaderJwtFilter
 */
@Profile("prod")
@Component
class AuthorizationHeaderJwtFilterVerifying extends AuthorizationHeaderJwtFilter {

    @Override
    void onMissingAuthorizationHeader(HttpServletRequest request) throws ServletException {
        throw new ServletException("Missing or invalid Authorization header");
    }
}
