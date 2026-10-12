package hy.demo.pro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SpringSecurity安全配置
 * 规则：
 * 1. /actuator/health 匿名可访问（给k8s liveness/readiness探针使用，探针无法携带账号密码）
 * 2. /actuator/info /actuator/metrics 必须使用监控账号登录访问
 * 3. 所有业务接口【这里示例 /api/**】默认放行，后续业务需要鉴权再单独调整
 *
 * 修改指引：
 * ① 如果后续K8s探针要增加鉴权：需要把health路径也加入登录保护，同时探针配置增加httpHeader
 * ② 如果后续新增actuator端点（如prometheus）：不需要改Security，只要yaml include加上，权限继承/actuator/**规则
 * ③ 如果业务接口后续需要登录保护：修改requestMatchers增加对应的路径，设置.authenticated()
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 从yml读取监控账号
    @Value("${monitor.user.name}")
    private String monitorUserName;

    @Value("${monitor.user.password}")
    private String monitorUserPwd;

    /**
     * 密码加密器
     * 注意：内存用户必须用BCrypt加密，不能明文
     * 如果以后切换到数据库存储用户，这个bean依然可以继续复用，不用删掉
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    /**
     * 内存用户管理器：这里只创建监控专用账号
     * 适用当前阶段：项目初期，不需要数据库存储用户，快速落地
     * 【后续变更点】
     * 项目成熟后，要从数据库读取账号时：删除这个Bean，自定义UserDetailsService从DB查询
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails monitorUser = User.withUsername(monitorUserName)
                .password(passwordEncoder.encode(monitorUserPwd))
                // 角色：ACTUATOR_ADMIN 自定义角色，专门用于actuator权限控制
                .roles("ACTUATOR_ADMIN")
                .build();
        return new InMemoryUserDetailsManager(monitorUser);
    }


    /**
     * 安全过滤主链路，核心路由权限规则
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭csrf：actuator监控接口一般不需要csrf保护（rest接口无表单提交）
                // 如果后面项目有页面表单业务接口，需要重新开启csrf
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 【1】健康检查端点，允许任何人匿名访问（K8s探针核心）
                        .requestMatchers(EndpointRequest.to("health")).permitAll()
                        // 【2】所有其他actuator下的接口，要求登录并且拥有ACTUATOR_ADMIN角色
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ACTUATOR_ADMIN")
                        // 【3】业务接口放行，根据你自己项目包路径修改，例如/api/**
                        // 👉 以后业务接口需要登录鉴权，就删掉这一行，改成 .authenticated()
                        .requestMatchers("/api/**").permitAll()
                        // 【4】其余所有请求，默认需要登录
                        .anyRequest().authenticated()
                )
                // 使用表单登录，浏览器访问actuator会弹出登录框；curl/postman也支持basic auth
                .formLogin(form -> form.permitAll());

        return http.build();
    }

}
