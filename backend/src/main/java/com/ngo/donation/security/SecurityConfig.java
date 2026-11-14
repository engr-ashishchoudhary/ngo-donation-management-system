package com.ngo.donation.security;

import com.ngo.donation.repository.AdminRepository;
import com.ngo.donation.repository.DonorRepository;
import com.ngo.donation.service.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationEntryPoint authenticationEntryPoint,
                          JwtAccessDeniedHandler accessDeniedHandler) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                                           AdminRepository adminRepository,
                                                           DonorRepository donorRepository) {
        return new JwtAuthenticationFilter(jwtTokenProvider, adminRepository, donorRepository);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public authentication endpoints (both singular and plural forms)
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/admin/login",
                                "/api/auth/admins/login",
                                "/api/auth/donor/login",
                                "/api/auth/donors/login",
                                "/api/auth/donor/register",
                                "/api/auth/donors/register"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/admin/register",
                                "/api/auth/admins/register"
                        ).hasRole("SUPER_ADMIN")

                        // Public campaign reading
                        .requestMatchers(HttpMethod.GET, "/api/campaigns", "/api/campaigns/*").permitAll()

                        // Error endpoint
                        .requestMatchers("/error").permitAll()

                        // Donor specific endpoints
                        .requestMatchers(HttpMethod.GET, "/api/donations/my").hasRole("DONOR")
                        .requestMatchers(HttpMethod.POST, "/api/donations").hasRole("DONOR")
                        .requestMatchers("/api/donor/**").hasRole("DONOR")

                        // Donation details (accessible by both donor for own donation, and admins)
                        .requestMatchers(HttpMethod.GET, "/api/donations/*").authenticated()

                        // Donation management (admin)
                        .requestMatchers(HttpMethod.GET, "/api/donations").hasAnyRole("SUPER_ADMIN", "CAMPAIGN_MANAGER", "FINANCIAL_OFFICER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/donations/*/status").hasAnyRole("SUPER_ADMIN", "CAMPAIGN_MANAGER", "FINANCIAL_OFFICER", "ADMIN")

                        // Campaign management (admin)
                        .requestMatchers(HttpMethod.POST, "/api/campaigns").hasAnyRole("SUPER_ADMIN", "CAMPAIGN_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/campaigns/*").hasAnyRole("SUPER_ADMIN", "CAMPAIGN_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/campaigns/*/close").hasAnyRole("SUPER_ADMIN", "CAMPAIGN_MANAGER", "ADMIN")

                        // Expense management (admin)
                        .requestMatchers("/api/campaigns/*/expenses").hasAnyRole("SUPER_ADMIN", "FINANCIAL_OFFICER", "ADMIN")

                        // Admin dashboard and management
                        .requestMatchers("/api/admin/admins").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "CAMPAIGN_MANAGER", "FINANCIAL_OFFICER", "ADMIN")

                        // Fallback
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
