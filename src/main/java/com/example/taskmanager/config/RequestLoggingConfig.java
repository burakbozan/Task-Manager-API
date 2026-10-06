package com.example.taskmanager.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RequestLoggingConfig implements WebMvcConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingConfig.class);
    private static final String START_TIME_ATTRIBUTE = RequestLoggingConfig.class.getName() + ".startTime";

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Object handler) {
                request.setAttribute(START_TIME_ATTRIBUTE, System.nanoTime());
                logger.info("Task API request method={} path={}",
                        request.getMethod(), request.getRequestURI());
                return true;
            }

            @Override
            public void afterCompletion(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Object handler,
                    Exception exception) {
                Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
                long elapsedMs = startTime instanceof Long startedAt
                        ? (System.nanoTime() - startedAt) / 1_000_000
                        : -1;
                if (exception == null) {
                    logger.info("Task API response method={} path={} status={} durationMs={}",
                            request.getMethod(), request.getRequestURI(),
                            response.getStatus(), elapsedMs);
                } else {
                    logger.warn("Task API response method={} path={} status={} durationMs={} error={}",
                            request.getMethod(), request.getRequestURI(),
                            response.getStatus(), elapsedMs, exception.getClass().getSimpleName());
                }
            }
        }).addPathPatterns("/api/tasks", "/api/tasks/**");
    }
}
