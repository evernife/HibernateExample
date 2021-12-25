import dev.petrus.hibernate.teste02.Cart;
import dev.petrus.hibernate.teste02.Item;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.*;

import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.metamodel.EntityType;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class T02_CommercialTest {

    private static SessionFactory factory;

    @BeforeAll
    public static void setup() {
        Configuration configuration = new Configuration()
                .addAnnotatedClass(Cart.class)
                .addAnnotatedClass(Item.class)
                .setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC")
                .setProperty("hibernate.dialect", "org.sqlite.hibernate.dialect.SQLiteDialect")
                //.setProperty("hibernate.hbm2ddl.auto", "create-drop") //Disabled for "@Test(0) createTable"
                .setProperty("hibernate.show_sql", "true")
                .setProperty("hibernate.format_sql", "true")
                .setProperty("hibernate.connection.url", "jdbc:sqlite:databases/commercials.sqlite.db");
        //configuration.configure(); //We don't have a config file in this exmaple
        factory  = configuration.buildSessionFactory();
    }

    @Test
    @Order(0)
    public void createTables() {
        MetadataSources metadata = new MetadataSources(factory.getSessionFactoryOptions().getServiceRegistry());

        for (EntityType<?> entity : factory.getMetamodel().getEntities()) {
            metadata.addAnnotatedClass(entity.getBindableJavaType());
        }

        EnumSet<TargetType> enumSet = EnumSet.of(TargetType.DATABASE);
        SchemaExport schemaExport = new SchemaExport();
        schemaExport.execute(enumSet,
                SchemaExport.Action.BOTH, //This will DROP and CREATE the tables
                metadata.buildMetadata());
    }

    private final ReentrantLock lock = new ReentrantLock(true);
    public synchronized void executeQuery(Consumer<Session> consumer){
        lock.lock();
        Session session = factory.openSession();
        Throwable throwable = null;
        try {
            consumer.accept(session);
        }catch (Throwable e){
            throwable = e;
        }finally {
            session.close();
            lock.unlock();
        }
        if (throwable != null){
            throw new RuntimeException(throwable);
        }
    }

    @Test
    @Order(1)
    public void createCartAndItems() {

        Cart cart = new Cart();
        cart.setOwner(UUID.randomUUID());
        cart.setId1(UUID.randomUUID());
        cart.setId2(UUID.randomUUID());
        cart.setId3(UUID.randomUUID());

        for (int i = 0; i < 10; i++) {
            Item item = new Item();
            cart.addToCart(item);
        }

        System.out.println(cart);

        executeQuery(session -> {
            EntityTransaction transaction = session.getTransaction();
            transaction.begin();
            session.persist(cart);
            transaction.commit();
        });

        System.out.println(cart);
    }

    @Test
    @Order(2)
    public void listCarts() {

        executeQuery(session -> {
            TypedQuery<Cart> typedQuery = session.createNamedQuery("Cart.getAll", Cart.class);
            List<Cart> carts = typedQuery.getResultList();

            for (Cart cart : carts) {
                System.out.println(cart);
            }

        });
    }

    @Test
    @Order(3)
    public void listItems() {

        executeQuery(session -> {
            TypedQuery<Item> typedQuery = session.createNamedQuery("Item.getAll", Item.class);
            List<Item> items = typedQuery.getResultList();

            for (Item item : items) {
                System.out.println(item);
            }

        });
    }

    @Test
    @Order(4)
    public void deleteHalfTheItems() {

        final AtomicReference<Cart> atomicCart = new AtomicReference<>();
        executeQuery(session -> {
            TypedQuery<Cart> typedQuery = session.createNamedQuery("Cart.getAll", Cart.class);
            typedQuery.setMaxResults(1);
            Cart cart = typedQuery.getResultList().get(0);
            atomicCart.set(cart);
        });

        final Cart cart = atomicCart.get();

        executeQuery(session -> {
            EntityTransaction transaction = session.getTransaction();
            for (int i = 0; i < cart.getItemSet().size() / 2; i++) {
                transaction.begin();
                session.remove(cart);
                transaction.commit();
            }
        });

        System.out.println("Listing Carts");
        listCarts();
        System.out.println("Listing Items");
        listItems();
    }

    @Test
    @Order(5)
    public void deleteCart() {

        executeQuery(session -> {
            TypedQuery<Cart> typedQuery = session.createNamedQuery("Cart.getAll", Cart.class);
            typedQuery.setMaxResults(1);

            Cart cart = typedQuery.getResultList().get(0);

            EntityTransaction transaction = session.getTransaction();
            transaction.begin();
            session.remove(cart);
            transaction.commit();

        });

        System.out.println("Listing Carts");
        listCarts();
        System.out.println("Listing Items");
        listItems();
    }
}
