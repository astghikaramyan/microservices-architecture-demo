package com.example.songservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.cloud.config.enabled=false")
class SongServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
