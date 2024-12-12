package at.primetshofer.model.Trainer;

import at.primetshofer.model.entities.Kanji;
import at.primetshofer.model.entities.KanjiProgress;
import at.primetshofer.model.util.HibernateUtil;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class KanjiTrainer {

    private static KanjiTrainer instance;
    private List<Kanji> kanjiList;
    // Cache for next review times: updated whenever we add progress
    private Map<Kanji, LocalDateTime> nextReviewCache = new HashMap<>();

    private KanjiTrainer() {
        EntityManager entityManager = HibernateUtil.getEntityManager();
        kanjiList = entityManager.createQuery("SELECT k FROM Kanji k", Kanji.class).getResultList();
    }

    public static KanjiTrainer getInstance() {
        if(instance == null) {
            instance = new KanjiTrainer();
        }
        return instance;
    }

    private int getIntervalDays(int points) {
        if (points <= 50) {
            return 1;
        } else if (points <= 100) {
            return 2;
        } else if (points <= 200) {
            return 5;
        } else {
            return 10;
        }
    }

    /**
     * Determine the maximum allowed points for the Kanji given how long it has been learned and how many times it was reviewed.
     * dynamicMaxPoints = min((daysSinceFirstLearned + reviewCount)*20, 300)
     * Minimum of 20 to allow some initial progress.
     */
    private int getDynamicMaxPoints(Kanji kanji) {
        List<KanjiProgress> progressList = kanji.getProgresses();
        int reviewCount = progressList.size(); // how many times we recorded progress

        if (progressList.isEmpty()) {
            // If never learned, start with a small max
            return 50;
        }

        KanjiProgress firstProgress = progressList.get(0);
        long daysSinceFirstLearned = ChronoUnit.DAYS.between(firstProgress.getLearned(), LocalDateTime.now());
        if (daysSinceFirstLearned < 0) {
            daysSinceFirstLearned = 0;
        }

        int dynamicMax = (int)Math.min((daysSinceFirstLearned + reviewCount) * 20, 300);
        if (dynamicMax < 50) {
            dynamicMax = 50;
        }
        return dynamicMax;
    }

    /**
     * Update the cached next review time for a Kanji.
     */
    private void updateNextReviewTime(Kanji kanji) {
        if (kanji.getProgresses().isEmpty()) {
            // Never learned Kanji doesn't have a nextReviewTime, set to now
            nextReviewCache.put(kanji, LocalDateTime.now());
            return;
        }

        KanjiProgress lastProgress = getLastProgress(kanji);
        int points = lastProgress.getPoints();
        int intervalDays = getIntervalDays(points);
        LocalDateTime nextReviewDate = lastProgress.getLearned().plusDays(intervalDays);
        nextReviewCache.put(kanji, nextReviewDate);
    }

    /**
     * Select the next Kanji for learning.
     * Logic:
     * 1. Due Kanji (nextReviewDate <= now): pick one randomly if available.
     * 2. If none due, pick never-learned Kanji at random.
     * 3. If still none, pick Kanji that will be due soon (e.g. within next day).
     * 4. If still none, pick any Kanji at random.
     */
    public Kanji getNextLearningKanji() {
        LocalDateTime now = LocalDateTime.now();

        // Separate never-learned and learned Kanji
        List<Kanji> neverLearned = new ArrayList<>();
        List<Kanji> learned = new ArrayList<>();

        for (Kanji k : kanjiList) {
            if (k.getProgresses().isEmpty()) {
                neverLearned.add(k);
            } else {
                learned.add(k);
            }
        }

        // Ensure cache is up-to-date for learned Kanji
        for (Kanji k : learned) {
            if (!nextReviewCache.containsKey(k)) {
                updateNextReviewTime(k);
            }
        }

        // For never-learned Kanji, if they are not in the cache, put them as due now
        for (Kanji k : neverLearned) {
            if (!nextReviewCache.containsKey(k)) {
                nextReviewCache.put(k, now);
            }
        }

        // 1. Due Kanji
        List<Kanji> dueKanji = new ArrayList<>();
        for (Kanji k : kanjiList) {
            LocalDateTime reviewTime = nextReviewCache.getOrDefault(k, now);
            if (!reviewTime.isAfter(now)) {
                dueKanji.add(k);
            }
        }

        if (!dueKanji.isEmpty()) {
            Collections.shuffle(dueKanji);
            return dueKanji.get(0);
        }

        // 2. No strictly due Kanji -> try never learned Kanji
        if (!neverLearned.isEmpty()) {
            Collections.shuffle(neverLearned);
            return neverLearned.get(0);
        }

        // 3. Kanji that will be due soon (within next day)
        List<Kanji> soonDueKanji = new ArrayList<>();
        for (Kanji k : learned) {
            LocalDateTime reviewTime = nextReviewCache.get(k);
            if (reviewTime.isBefore(now.plusDays(1))) {
                soonDueKanji.add(k);
            }
        }
        if (!soonDueKanji.isEmpty()) {
            Collections.shuffle(soonDueKanji);
            return soonDueKanji.get(0);
        }

        // 4. Any Kanji at random if no other condition met
        if (!kanjiList.isEmpty()) {
            List<Kanji> all = new ArrayList<>(kanjiList);
            Collections.shuffle(all);
            return all.get(0);
        }

        // If no Kanji at all
        return null;
    }

    /**
     * Add/update Kanji progress for the current day.
     * If a progress entry already exists for today, update it. Otherwise, create a new one.
     * Points increment depends on user performance and scales with review count.
     */
    public Kanji addKanjiProgress(Kanji kanji, int percent) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // Check if there's already a progress entry for today
        KanjiProgress todayProgress = getTodayProgress(kanji, today);

        // Calculate increment and dynamic max points
        int increment = calculatePointsIncrement(kanji, percent);
        int currentPoints = todayProgress != null ? todayProgress.getPoints()
                : (!kanji.getProgresses().isEmpty() ? getLastProgress(kanji).getPoints() : 0);
        int dynamicMaxPoints = getDynamicMaxPoints(kanji);

        int newPoints = currentPoints + increment;
        if (newPoints > dynamicMaxPoints) {
            newPoints = dynamicMaxPoints;
        }

        if (todayProgress != null) {
            // Update the existing today's progress entry
            todayProgress.setPoints(newPoints);
            todayProgress.setLearned(now); // Update time
        } else {
            // Create a new entry for today
            KanjiProgress newProgress = new KanjiProgress();
            newProgress.setKanji(kanji);
            newProgress.setLearned(now);
            newProgress.setPoints(newPoints);
            kanji.getProgresses().add(newProgress);
        }

        // Update the cached next review time
        updateNextReviewTime(kanji);

        return kanji;
    }

    private KanjiProgress getTodayProgress(Kanji kanji, LocalDate today) {
        // Check if last progress entry is from today
        if (!kanji.getProgresses().isEmpty()) {
            KanjiProgress last = getLastProgress(kanji);
            if (last.getLearned().toLocalDate().equals(today)) {
                return last;
            }
        }
        return null;
    }

    private KanjiProgress getLastProgress(Kanji kanji) {
        return kanji.getProgresses().get(kanji.getProgresses().size() - 1);
    }

    private int calculatePointsIncrement(Kanji kanji, int percent) {
        int baseIncrement;
        if (percent >= 90) {
            baseIncrement = 30;
        } else if (percent >= 70) {
            baseIncrement = 20;
        } else if (percent >= 50) {
            baseIncrement = 10;
        } else {
            baseIncrement = 5;
        }

        int reviewCount = kanji.getProgresses().size(); // how many times recorded so far (before adding today's entry)
        double incrementFactor = 1.0 + ((reviewCount-1) * 0.1);
        if(reviewCount == 0 || reviewCount == 1) {
            incrementFactor = 0.5;
        }
        return (int) Math.round(baseIncrement * incrementFactor);
    }
}
