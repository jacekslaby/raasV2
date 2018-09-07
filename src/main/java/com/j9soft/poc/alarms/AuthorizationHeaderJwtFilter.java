package com.j9soft.poc.alarms;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A filter to extract domain and adapter name from the "Authorization" header of a servlet request.
 *
 * This Filter is used in dev mode, i.e. in development environments. (as "default")
 *  (BTW: In prod (i.e. production) mode a different one may be used. E.g. in case when no other filtering is installed to verify token's signature.)
 *
 * How to generate a dev token - https://jwt.io/
 * Example token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkb21haW4iOiJDQWRldiIsImFkYXB0ZXJOYW1lIjoiQWRhcHRlclRlc3QiLCJpYXQiOjE1MTYyMzkwMjJ9.gnwRsh2DMZQEr46p2MKM6UMVHrHqLqpCyseh8yh-j_A
 *  (created with 'secretkey')
 * Example request should have header "Authorization: Bearer <put here the above token>".   (Remember about "Bearer: " !!)
 *
 * Inspiration - https://aboullaite.me/spring-boot-token-authentication-using-jwt/
 */
@Profile({"default","kafka-dev"})
@Component
class AuthorizationHeaderJwtFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;
        final String authHeader = request.getHeader("authorization");  // "HTTP header names are case-insensitive, according to RFC 2616"

        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);

            filterChain.doFilter(servletRequest, servletResponse);
        } else {

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                onMissingAuthorizationHeader(request);

            } else {

                final String token = authHeader.substring(7);
                final Claims claims;

                try {
                    // @TODO do something about secretkey
                    claims = Jwts.parser().setSigningKey("secretkey").parseClaimsJws(token).getBody();
                } catch (final SignatureException e) {
                    throw new ServletException("Invalid token - wrong Signature");
                }

                final String domain = claims.get("domain", String.class);
                final String adapterName = claims.get("adapterName", String.class);

                if (domain == null || domain.trim().isEmpty()) {
                    throw new ServletException("Invalid token - wrong domain:" + domain);
                }
                if (adapterName == null || adapterName.trim().isEmpty()) {
                    throw new ServletException("Invalid token - wrong adapterName:" + adapterName);
                }

                request.setAttribute("partitionDefinition", new RawAlarmsPartitionDefinition(domain.trim(), adapterName.trim()));
            }

            filterChain.doFilter(request, response);
        }
    }

    /*
     * Launched if request has missing "Bearer " string in "Authorization" HTTP header field.
     * This method should be overwritten by production-mode class(es).
     */
    void onMissingAuthorizationHeader(HttpServletRequest request) throws ServletException {
        // In development mode, if client does not provide JWT token, then we assume defaults.
        //  (In order to make it easier for newbie developers.)
        //
        request.setAttribute("partitionDefinition", new RawAlarmsPartitionDefinition("dev", "AdapterTest"));
    }
}
