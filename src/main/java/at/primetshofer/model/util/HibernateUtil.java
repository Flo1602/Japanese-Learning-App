package at.primetshofer.model.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;

public class HibernateUtil {
    private static EntityManager entityManager = null;

    public static EntityManager getEntityManager() {
        if(entityManager == null) {
            entityManager = Persistence.createEntityManagerFactory("JapaneseLearningAppPU").createEntityManager();
        }
        return entityManager;
    }

    public static void startTransaction() {
        getEntityManager().getTransaction().begin();
    }

    public static void commitTransaction() {
        getEntityManager().getTransaction().commit();
    }

    public static void shutdown() {
        entityManager.close();
    }
}
