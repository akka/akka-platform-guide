package shopping.cart;

import static java.util.concurrent.TimeUnit.SECONDS;

import akka.actor.typed.ActorSystem;
import akka.persistence.jdbc.testkit.javadsl.SchemaUtils;
import akka.projection.jdbc.javadsl.JdbcProjection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.repository.HibernateJdbcSession;

public class CreateTableTestUtils {

  /**
   * Test utility to create journal and projection tables for tests environment. JPA/Hibernate
   * tables are auto created (drop-and-create) using settings flag, see persistence-test.conf
   */
  public static void createTables(JpaTransactionManager transactionManager, ActorSystem<?> system)
      throws Exception {

    // create schemas
    // ok to block here, main test thread
    SchemaUtils.dropIfExists(system).toCompletableFuture().get(30, SECONDS);
    SchemaUtils.createIfNotExists(system).toCompletableFuture().get(30, SECONDS);

    JdbcProjection.dropOffsetTableIfExists(
            () -> new HibernateJdbcSession(transactionManager), system)
        .toCompletableFuture()
        .get(30, SECONDS);
    JdbcProjection.createOffsetTableIfNotExists(
            () -> new HibernateJdbcSession(transactionManager), system)
        .toCompletableFuture()
        .get(30, SECONDS);

    SchemaUtils.applyScript(loadFile("ddl-scripts/drop_user_tables.sql"), system);
    SchemaUtils.applyScript(loadFile("ddl-scripts/create_user_tables.sql"), system);

    LoggerFactory.getLogger(CreateTableTestUtils.class).info("Tables created");
  }

  private static String loadFile(String filePath) {
    try {
      return Files.lines(Paths.get(filePath), StandardCharsets.UTF_8).collect(Collectors.joining("\n"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
