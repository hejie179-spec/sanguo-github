package com.sanguo.config;

import com.sanguo.security.JwtFilter;
import com.sanguo.security.RestAccessDeniedHandler;
import com.sanguo.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.Resource;

/**
 * Spring Security安全配置类
 * 
 * 角色权限体系：
 * - SUPER_ADMIN（超级管理员）：所有权限，可管理用户、角色分配、系统配置
 * - ADMIN（管理员）：管理论坛内容、查看系统通知
 * - CONTENT_ADMIN（内容管理员）：人物/事件/史料/典故的增删改查
 * - REVIEW_ADMIN（审核管理员）：文章/评论/论坛的审核管理
 * - 普通用户：浏览、发帖、评论、收藏等基础功能
 * 
 * 本项目采用JWT无状态认证，登录成功后返回token，后续请求通过Authorization头携带
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Resource
    private JwtFilter jwtFilter;

    @Resource
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Resource
    private RestAccessDeniedHandler restAccessDeniedHandler;

    private static final String[] WHITE_LIST = {
            "/auth/register", "/auth/login",
            "/article/list", "/article/detail/**",
            "/forum/topic/list", "/forum/topic/detail/**", "/forum/reply/list/**",
            "/comment/list/**",
            "/config/site",
            "/uploads/**",
            "/notification/subscribe",
            "/doc.html", "/swagger-resources/**", "/v3/api-docs/**", "/webjars/**", "/swagger-ui/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(WHITE_LIST).permitAll()
                .antMatchers(HttpMethod.GET, "/person/**", "/event/**", "/literature/**", "/allusion/**").permitAll()
                // 管理员可查看用户列表（但不能执行用户管理操作）
                .antMatchers(HttpMethod.GET, "/admin/user/list", "/admin/user/roles").hasAnyRole("SUPER_ADMIN", "ADMIN")
                // 超级管理员：用户管理、系统配置
                .antMatchers("/admin/user/**").hasAnyRole("SUPER_ADMIN")
                .antMatchers("/admin/config/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                // 内容管理员：人物/事件/史料/典故的增删改查
                .antMatchers("/person/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "CONTENT_ADMIN")
                .antMatchers("/event/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "CONTENT_ADMIN")
                .antMatchers("/literature/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "CONTENT_ADMIN")
                .antMatchers("/allusion/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "CONTENT_ADMIN")
                // 审核管理员：文章/评论/论坛的审核管理
                .antMatchers("/admin/article/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "REVIEW_ADMIN")
                .antMatchers("/admin/comment/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "REVIEW_ADMIN")
                .antMatchers("/admin/forum/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "REVIEW_ADMIN")
                // 管理后台仪表盘/统计：所有管理员均可访问
                .antMatchers("/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "CONTENT_ADMIN", "REVIEW_ADMIN")
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
                .and()
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
