package com.j9soft.poc.alarms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import javax.servlet.Filter;

/**
 * Class putting all project together.
 * Based on Spring Boot annotations all required dependencies are injected (autowired).
 */
@SpringBootApplication
class RaasApplication {

    @Autowired
    private AuthorizationHeaderJwtFilter authorizationHeaderFilter;   // based on Spring Profile we have here a filter with/without JWT token verification

    @Bean
    public FilterRegistrationBean jwtFilter() {
        final FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(this.authorizationHeaderFilter);
        registrationBean.addUrlPatterns("/*");

        return registrationBean;
    }

    public static void main(String[] args) {

        SpringApplication.run(RaasApplication.class, args);
    }
}
