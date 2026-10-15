package cl.duoc.andesstay.audit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica que el contexto completo (JPA, seguridad, RabbitMQ) arranca.
 * Los listeners no se inician para no requerir un broker en la prueba.
 */
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.datasource.url=jdbc:h2:mem:contexttest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class MsAndesstayAuditApplicationTests {

    @Test
    void contextLoads() {
    }
}
