package hy.demo.pro.controller;

import hy.demo.pro.service.TestService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务Controller统一规范
 * 根路径固定 /api/v1
 * 好处：
 * 1. 版本清晰，后续升级v2直接新增/api/v2，老版本保留，灰度兼容
 * 2. 网关/Ingress路由配置简单，不需要处理context-path
 * 3. 微服务主流标准，OpenAPI文档、接口测试工具识别友好
 * 4. 多实例、K8s转发不会出现路径错位问题
 */
@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @Resource
    private TestService testService;

    @GetMapping("/hello")
    public String test() {
        return testService.test();
    }

}
