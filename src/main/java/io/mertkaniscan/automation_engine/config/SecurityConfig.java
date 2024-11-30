package io.mertkaniscan.automation_engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;



@Configuration
@EnableWebSecurity
public class SecurityConfig{

    //@Bean
    //public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    //    http
    //            .authorizeHttpRequests(authorize -> authorize
    //                    .requestMatchers("/login", "/css/**", "/js/**").permitAll()
    //                    .anyRequest().authenticated()
    //            )
    //            .formLogin(form -> form
    //                    .loginPage("/login") // Custom login page
    //                    .defaultSuccessUrl("/dashboard", true)
    //                    .permitAll()
    //            )
    //            .logout(logout -> logout
    //                    .logoutUrl("/logout")
    //                    .logoutSuccessUrl("/login")
    //                    .permitAll()
    //            );
    //    return http.build();
    //}

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((requests) -> requests
                        .anyRequest().permitAll()  // Allow all requests without authentication
                )
                .csrf(AbstractHttpConfigurer::disable)  // Disable CSRF protection
                .formLogin(AbstractHttpConfigurer::disable);  // Disable the login page

        return http.build();
    }


    //@Bean
    //public PasswordEncoder passwordEncoder() {
    //    return new BCryptPasswordEncoder();
    //}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

}
