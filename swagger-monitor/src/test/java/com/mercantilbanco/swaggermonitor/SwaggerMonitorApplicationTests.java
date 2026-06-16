package com.mercantilbanco.swaggermonitor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.mercantilbanco.swaggermonitor.config.InitEnvironment;


@SpringBootTest
public class SwaggerMonitorApplicationTests {

    @BeforeAll
    static void init() {
        System.setProperty("BM_HOME", "src/test/resources");
        InitEnvironment.getConfig();
    }

    @Test
    void contextLoads() {
    }
}
