package dev.keyup.recruiterbot.config

import dev.keyup.recruiterbot.web.ApiProblemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfiguration {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(
        encoder: PasswordEncoder,
        @Value($$"${recruiterbot.security.user.username}") username: String,
        @Value($$"${recruiterbot.security.user.password}") password: String,
        @Value($$"${recruiterbot.security.admin.username}") adminUsername: String,
        @Value($$"${recruiterbot.security.admin.password}") adminPassword: String,
    ): UserDetailsService = InMemoryUserDetailsManager(
        User.withUsername(username).password(encoder.encode(password)).roles("USER").build(),
        User.withUsername(adminUsername).password(encoder.encode(adminPassword)).roles("USER", "ADMIN").build(),
    )

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .httpBasic(Customizer.withDefaults())
        .exceptionHandling { errors ->
            errors.authenticationEntryPoint { _, response, _ ->
                ApiProblemWriter.write(response, HttpStatus.UNAUTHORIZED, "Authentication is required.", "unauthorized")
            }
            errors.accessDeniedHandler { _, response, _ ->
                ApiProblemWriter.write(
                    response,
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to access this resource.",
                    "forbidden"
                )
            }
        }
        .authorizeHttpRequests { auth ->
            auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
            auth.requestMatchers("/actuator/health", "/actuator/info").permitAll()
            auth.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            auth.requestMatchers("/actuator/metrics/**").hasRole("ADMIN")
            auth.requestMatchers("/actuator/**").denyAll()
            auth.anyRequest().authenticated()
        }
        .build()
}
