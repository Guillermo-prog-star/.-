package com.integrityfamily;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
@SpringBootTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "app.ai.api-key=test-key"
})
class IntegrityFamilyApplicationTests {
    @Test void contextLoads() {}
}
