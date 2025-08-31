package com.example.resourceservice.cucumber;

import com.example.resourceservice.ResourceServiceApplication;
import com.example.resourceservice.cucumber.hooks.MockExternalServicesConfig;
import com.example.resourceservice.cucumber.hooks.TestConfig;
import com.example.resourceservice.cucumber.hooks.TestDatasourceConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@CucumberContextConfiguration
@SpringBootTest(
        properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"},
        classes = {ResourceServiceApplication.class,
                TestConfig.class,
                TestDatasourceConfig.class}
)
@Import(MockExternalServicesConfig.class)
public class CucumberSpringConfiguration {
}
