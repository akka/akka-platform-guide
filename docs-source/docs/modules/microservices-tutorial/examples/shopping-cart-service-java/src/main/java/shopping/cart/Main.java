package shopping.cart;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
// tag::SendOrderProjection[]
import akka.grpc.GrpcClientSettings;
// end::SendOrderProjection[]
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import com.typesafe.config.Config;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import shopping.cart.proto.ShoppingCartService;
import shopping.cart.repository.HibernateJdbcSession;
import shopping.cart.repository.ItemPopularityRepository;
import shopping.cart.repository.SpringIntegration;
// tag::SendOrderProjection[]
import shopping.order.proto.ShoppingOrderService;
import shopping.order.proto.ShoppingOrderServiceClient;

import java.util.function.Supplier;

// end::SendOrderProjection[]

public class Main {

  public static void main(String[] args) {
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "ShoppingCartService");
    init(system, orderServiceClient(system));
  }

  public static void init(ActorSystem<Void> system, ShoppingOrderService orderService) {
    AkkaManagement.get(system).start();
    ClusterBootstrap.get(system).start();

    ShoppingCart.init(system);

    ApplicationContext springContext =
            SpringIntegration.applicationContext(system.settings().config());
    JpaTransactionManager transactionManager = springContext.getBean(JpaTransactionManager.class);
    ItemPopularityRepository itemPopularityRepository =
            springContext.getBean(ItemPopularityRepository.class);
    Supplier<HibernateJdbcSession> sessionSupplier = () -> new HibernateJdbcSession(transactionManager);

    ItemPopularityProjection.init(system, sessionSupplier, itemPopularityRepository);
    PublishEventsProjection.init(system, sessionSupplier);
    SendOrderProjection.init(system, sessionSupplier, orderService);

    Config config = system.settings().config();
    String grpcInterface = config.getString("shopping-cart-service.grpc.interface");
    int grpcPort = config.getInt("shopping-cart-service.grpc.port");
    ShoppingCartService grpcService = new ShoppingCartServiceImpl(system, itemPopularityRepository);
    ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService);
  }

  // tag::SendOrderProjection[]
  static ShoppingOrderService orderServiceClient(ActorSystem<?> system) {
    GrpcClientSettings orderServiceClientSettings =
        GrpcClientSettings.connectToServiceAt(
                system.settings().config().getString("shopping-order-service.host"),
                system.settings().config().getInt("shopping-order-service.port"),
                system)
            .withTls(false);

    return ShoppingOrderServiceClient.create(orderServiceClientSettings, system);
  }
  // end::SendOrderProjection[]

}
