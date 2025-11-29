package com.example.library;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class IntegrationMySqlTest {

    @Container
    public static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withStartupTimeout(Duration.ofMinutes(2))           // ← 1. Чекаємо до 2 хвилин
            .waitingFor(Wait.forListeningPort())                 // ← 2. Чекаємо саме порт 3306
            .withReuse(true);                                    // ← 3. Прискорює повторні запуски

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url", mysql::getJdbcUrl);
        reg.add("spring.datasource.username", mysql::getUsername);
        reg.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private DataSource dataSource;

    @Test
    void testDatabaseConnection() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            Statement st = conn.createStatement();
            st.execute("CREATE TABLE test_table (id INT PRIMARY KEY AUTO_INCREMENT, val VARCHAR(50));");
            st.execute("INSERT INTO test_table (val) VALUES ('hello');");
            ResultSet rs = st.executeQuery("SELECT val FROM test_table WHERE id=1");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("val")).isEqualTo("hello");
        }
    }
}
