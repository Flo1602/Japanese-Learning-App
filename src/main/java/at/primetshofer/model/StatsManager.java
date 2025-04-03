package at.primetshofer.model;

import at.primetshofer.model.entities.LearnTimeStats;
import at.primetshofer.model.util.HibernateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public class StatsManager {

    private static EntityManager entityManager;

    private static void checkEntityManager(){
        if(entityManager == null) {
            entityManager = HibernateUtil.getEntityManager();
        }
    }

    protected static void addDurationToday(Duration duration) {
        checkEntityManager();
        LearnTimeStats today = getTodayStats();

        if(today == null) {
            today = new LearnTimeStats();
            today.setDate(LocalDate.now());
            today.setExercisesCount(1);
            today.setDuration(duration);
        } else {
            today.addDuration(duration);
            today.incrementExercisesCount();
        }

        entityManager.getTransaction().begin();
        entityManager.merge(today);
        entityManager.getTransaction().commit();
    }

    public static LearnTimeStats getTodayStats() {
        checkEntityManager();
        return entityManager.find(LearnTimeStats.class, LocalDate.now());
    }

    public static List<LearnTimeStats> getAllTimeStats() {
        checkEntityManager();
        return entityManager.createQuery("SELECT l FROM LearnTimeStats l", LearnTimeStats.class).getResultList();
    }

    public static long getTotalKanjiCount() {
        checkEntityManager();
        String jpql = "SELECT COUNT(k) FROM Kanji k";
        return (Long) entityManager.createQuery(jpql).getSingleResult();
    }

    public static long getTotalWordCount() {
        checkEntityManager();
        String jpql = "SELECT COUNT(w) FROM Word w";
        return (Long) entityManager.createQuery(jpql).getSingleResult();
    }

    public static LearnTimeStats getStatsForDay(LocalDate day) {
        TypedQuery<LearnTimeStats> query = entityManager.createQuery(
                "SELECT l FROM LearnTimeStats l WHERE l.date = :date",
                LearnTimeStats.class
        );
        query.setParameter("date", day);

        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
