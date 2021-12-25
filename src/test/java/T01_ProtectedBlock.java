import dev.petrus.hibernate.teste01.ProtectedBlock;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class T01_ProtectedBlock {

    private static SessionFactory factory;

    @BeforeAll
    public static void setup() {
        Configuration configuration = new Configuration()
                .addAnnotatedClass(ProtectedBlock.class)
                .setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC")
                .setProperty("hibernate.dialect", "org.sqlite.hibernate.dialect.SQLiteDialect")
                //.setProperty("hibernate.hbm2ddl.auto", "create-drop") //Disabled for "@Test(0) createTable"
                .setProperty("hibernate.show_sql", "true")
                .setProperty("hibernate.format_sql", "true")
                .setProperty("hibernate.connection.url", "jdbc:sqlite:databases/protectedblock.sqlite.db");
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

    @Test
    @Order(1)
    public void createOneProtectecBlock() {
        Session session = factory.openSession();

        EntityTransaction transaction = session.getTransaction();
        transaction.begin();

        ProtectedBlock protectedBlock = new ProtectedBlock();
        protectedBlock.setWorld("FirstWorld");
        protectedBlock.getBlockPosID().setPosX(-1);
        protectedBlock.getBlockPosID().setPosY(-1);
        protectedBlock.getBlockPosID().setPosZ(-1);
        protectedBlock.setTimeStart(0L);
        protectedBlock.setTimeDuration(5L);

        session.persist(protectedBlock);

        transaction.commit();
        session.close();
    }

    @Test
    @Order(2)
    public void listProtectedBlocksOnDatabase() {
        Session session = factory.openSession();

        TypedQuery<ProtectedBlock> typedQuery = session.createNamedQuery("ProtectedBlock.getAll", ProtectedBlock.class);

        List<ProtectedBlock> protectedBlocks = typedQuery.getResultList();

        for (ProtectedBlock protectedBlock : protectedBlocks) {
            System.out.println(protectedBlock);
        }
        System.out.println("Size: " + protectedBlocks.size());

        session.close();
    }

    @Test
    @Order(3)
    public void createManyProtectecBlock() {
        Session session = factory.openSession();

        for (int i = 0; i < 50; i++) {
            EntityTransaction transaction = session.getTransaction();
            transaction.begin();
            ProtectedBlock protectedBlock = new ProtectedBlock();
            protectedBlock.setWorld("ProtectedTest");
            protectedBlock.getBlockPosID().setPosX(i);
            protectedBlock.getBlockPosID().setPosY(0);
            protectedBlock.getBlockPosID().setPosZ(-i);
            protectedBlock.setTimeStart(System.nanoTime());
            protectedBlock.setTimeDuration(5L);
            session.persist(protectedBlock);
            transaction.commit();
        }

        session.close();
    }

    @Test
    @Order(4)
    public void listProtectedBlocksOnDatabaseAgain() {
        listProtectedBlocksOnDatabase();
    }

    @Test
    @Order(5)
    public void listProtectedBlocksOnDatabase_ByTimeStart() {
        Session session = factory.openSession();

        TypedQuery<ProtectedBlock> typedQuery = session.createNamedQuery("ProtectedBlock.byTimeStart", ProtectedBlock.class);

        List<ProtectedBlock> protectedBlocks = typedQuery.getResultList();

        for (ProtectedBlock protectedBlock : protectedBlocks) {
            System.out.println(protectedBlock);
        }
        System.out.println("Size: " + protectedBlocks.size());

        session.close();
    }

    @Test
    @Order(6)
    public void deleteWithLowestTimeStart() {
        Session session = factory.openSession();

        TypedQuery<ProtectedBlock> typedQuery = session.createNamedQuery("ProtectedBlock.byTimeStart", ProtectedBlock.class);
        typedQuery.setMaxResults(1);
        ProtectedBlock protectedBlock = typedQuery.getSingleResult();

        EntityTransaction transaction = session.getTransaction();
        transaction.begin();
        session.remove(protectedBlock);
        transaction.commit();
        session.close();

        System.out.println("Lowest Removed" + protectedBlock);
    }

    @Test
    @Order(7)
    public void listProtectedBlocksOnDatabaseAgainAgain() {
        listProtectedBlocksOnDatabase();
    }

    private List<ProtectedBlock> getAllBlocks(){
        Session session = factory.openSession();
        TypedQuery<ProtectedBlock> typedQuery = session.createNamedQuery("ProtectedBlock.getAll", ProtectedBlock.class);
        List<ProtectedBlock> protectedBlocks = typedQuery.getResultList();
        session.close();
        return protectedBlocks;
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
    @Order(8)
    public void deleteAllUsingMultipleThreads() { //This will test a "Concurrent Enviroment" editing a SQLIte Server that does not allow multiples sessions
        List<ProtectedBlock> protectedBlocks = getAllBlocks();
        //
        CountDownLatch latch = new CountDownLatch(protectedBlocks.size());
        ExecutorService executorService = Executors.newFixedThreadPool(20);

        Long start = System.currentTimeMillis();
        for (ProtectedBlock protectedBlock : protectedBlocks) {
            executorService.submit(() -> {
                try {
                    executeQuery(session -> {
                        EntityTransaction transaction = session.getTransaction();
                        transaction.begin();
                        session.remove(protectedBlock);
                        transaction.commit();
                    });
                    System.out.println("Deleted " + latch.getCount());
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Deleted all files in : " + (System.currentTimeMillis() - start) + " ms");

        protectedBlocks = getAllBlocks();
        System.out.println("Remaining: " + protectedBlocks.size());
    }

}
