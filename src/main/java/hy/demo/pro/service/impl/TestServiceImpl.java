package hy.demo.pro.service.impl;

import hy.demo.pro.service.TestService;
import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl implements TestService {

    @Override
    public String test() {
        return "Hello Jamie, Welcome!";
    }
}
