package com.tenten.studybadge.common.config;

import com.tenten.studybadge.common.jwt.JwtTokenFilter;
import com.tenten.studybadge.common.jwt.JwtTokenProvider;
import com.tenten.studybadge.common.oauth2.CustomOAuth2UserService;
import com.tenten.studybadge.common.oauth2.OAuth2FailureHandler;
import com.tenten.studybadge.common.oauth2.OAuth2SuccessHandler;
import com.tenten.studybadge.common.security.CustomAuthenticationEntryPoint;
import com.tenten.studybadge.common.security.LoginUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate redisTemplate;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final LoginUserArgumentResolver loginUserArgumentResolver;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
  
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
  
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests( requests -> requests
                        .requestMatchers("/api/members/sign-up", "/api/members/auth/**", "/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**", "/api/members/login/**", "/error", "/health-check").permitAll()
                        .requestMatchers("/api/token/oauth2/**", "/favicon.ico", "/oauth2/**", "/api/payments/success/**", "/api/payments/cancel/**", "/api/members/password/**", "/api/members/resend/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/study-channels", "/api/study-channels/{studyChannelId:\\d+}").permitAll()
                        .requestMatchers("/api/members/logout", "api/study-channels/*/places", "/api/study-channels/**", "/api/token/re-issue"
                                , "/api/members/my-info", "/api/members/my-info/update", "/api/payments/**", "/api/participation/**",
                            "/api/study-channels/*/single-schedules/**", "/api/study-channels/*/repeat-schedules/**",
                            "/api/study-channels/*/schedules/**", "/api/points/my-point/**", "/api/members/my-apply/**", "/api/notifications/**", "/api/members/my-study", "/api/members/withdrawal", "/api/cert/account/**").hasRole("USER"))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))

                .cors(cors -> new CorsConfig())
                .headers(headers -> headers // h2-console 페이지 접속을 위한 설정
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("frame-ancestors 'self'")))

                .oauth2Login(oauth2Login ->
                        oauth2Login
                                .redirectionEndpoint( endpoint -> endpoint.baseUri("/oauth2/callback/*"))
                                .userInfoEndpoint(userInfoEndpoint ->
                                        userInfoEndpoint.userService(customOAuth2UserService)
                                )
                                .successHandler(oAuth2SuccessHandler)
                                .failureHandler(oAuth2FailureHandler)
                )

                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))

                .addFilterBefore(new JwtTokenFilter(jwtTokenProvider, redisTemplate), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
  
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(loginUserArgumentResolver);
    }
}
