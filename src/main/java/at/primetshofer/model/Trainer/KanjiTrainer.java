package at.primetshofer.model.Trainer;

import at.primetshofer.model.Controller;
import at.primetshofer.model.entities.Kanji;
import at.primetshofer.model.entities.KanjiProgress;
import at.primetshofer.model.util.HibernateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class KanjiTrainer {

    private static KanjiTrainer instance;
    private List<Kanji> dueKanjiList;
    private List<Kanji> allKanjiList;
    private int todayDueMax;
    private int dueCurrent;
    private int dueTotal;
    private Map<Kanji, LocalDateTime> nextReviewCache;

    private KanjiTrainer() {}

    public void updateKanjiList(){
        dueKanjiList = new ArrayList<>();

        EntityManager entityManager = HibernateUtil.getEntityManager();

        String jpql = "SELECT COUNT(k) FROM Kanji k";
        Query query = entityManager.createQuery(jpql);
        long kanjiCount = (long) query.getSingleResult();

        if(allKanjiList == null || kanjiCount != allKanjiList.size()){
            allKanjiList = entityManager.createQuery("SELECT k FROM Kanji k", Kanji.class).getResultList();

            sortKanjiList(allKanjiList);
        }

        nextReviewCache = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        todayDueMax = 0;
        dueCurrent = 0;
        dueTotal = 0;

        int maxKanji = Controller.getInstance().getSettings().getMaxDailyKanji();

        for (Kanji kanji : allKanjiList) {
            updateNextReviewTime(kanji);

            LocalDateTime reviewTime = nextReviewCache.getOrDefault(kanji, now);
            int daysGoal = getIntervalDays(getDynamicMaxPoints(kanji));
            daysGoal--;

            if(getTodayProgress(kanji, now.toLocalDate()) == null){
                daysGoal = 0;
            }
            if (!reviewTime.toLocalDate().isAfter(now.toLocalDate().plusDays(daysGoal))) {
                dueTotal++;

                if(todayDueMax != maxKanji){
                    todayDueMax++;
                    dueCurrent++;

                    dueKanjiList.add(kanji);
                }
            } else if(getLastProgress(kanji).getLearned().toLocalDate().isEqual(now.toLocalDate())){
                todayDueMax++;
            }
        }
    }

    public void sortKanjiList(List<Kanji> kanjiList) {
        kanjiList.sort((k1, k2) -> {
            // Retrieve the last progress for both Kanji
            KanjiProgress lastProgress1 = getLastProgress(k1);
            KanjiProgress lastProgress2 = getLastProgress(k2);

            LocalDateTime date1 = (lastProgress1 != null) ? lastProgress1.getLearned() : null;
            LocalDateTime date2 = (lastProgress2 != null) ? lastProgress2.getLearned() : null;

            // Custom sorting logic
            if (isToday(date1) && !isToday(date2)) return -1; // Today comes first
            if (!isToday(date1) && isToday(date2)) return 1;

            if (date1 == null && date2 != null) return -1; // Null comes after today's dates
            if (date1 != null && date2 == null) return 1;
            if (date1 == null && date2 == null) return 0;

            return date2.compareTo(date1); // The rest ordered by date descending
        });
    }

    private boolean isToday(LocalDateTime date) {
        return date != null && date.toLocalDate().isEqual(LocalDateTime.now().toLocalDate());
    }

    public int getTodayDueMax() {
        return todayDueMax;
    }

    public int getTodayDueCurrent() {
        return dueCurrent;
    }

    public int getDueTotal() {
        return dueTotal;
    }

    private void updateDueCurrent(Kanji kanji){
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime reviewTime = nextReviewCache.getOrDefault(kanji, now);
        int daysGoal = getIntervalDays(getDynamicMaxPoints(kanji));

        daysGoal--;
        if(getTodayProgress(kanji, now.toLocalDate()) == null){
            daysGoal = 0;
        }
        if (reviewTime.toLocalDate().isAfter(now.toLocalDate().plusDays(daysGoal))) {
            dueCurrent--;
            dueTotal--;
        }
    }

    public static KanjiTrainer getInstance() {
        if(instance == null) {
            instance = new KanjiTrainer();
        }
        return instance;
    }

    private int getIntervalDays(int points) {
        if (points < 30) {
            return 0;
        } else if (points < 60) {
            return 1;
        } else if (points < 100) {
            return 2;
        } else if (points < 175) {
            return 5;
        } else if (points < 300) {
            return 11;
        } else if (points < 500) {
            return 21;
        } else {
            return 31;
        }
    }

    /**
     * Determine the maximum allowed points for the Kanji given how long it has been learned and how many times it was reviewed.
     * dynamicMaxPoints = min((daysSinceFirstLearned + reviewCount)*20, 300)
     * Minimum of 20 to allow some initial progress.
     */
    private int getDynamicMaxPoints(Kanji kanji) {
        List<KanjiProgress> progressList = kanji.getProgresses();
        int reviewCount = 0; // how many times we recorded progress
        for (KanjiProgress kanjiProgress : progressList) {
            reviewCount += kanjiProgress.getCompressedEntries();
        }

        if (progressList.isEmpty()) {
            // If never learned, start with a small max
            return 50;
        }

        KanjiProgress firstProgress = progressList.getFirst();
        long daysSinceFirstLearned = ChronoUnit.DAYS.between(firstProgress.getLearned().toLocalDate(), LocalDate.now());
        if (daysSinceFirstLearned < 0) {
            daysSinceFirstLearned = 0;
        }
        daysSinceFirstLearned /= 4;

        int dynamicMax = (int)Math.min((daysSinceFirstLearned + reviewCount) * 20, 600);
        if(dynamicMax < 0){
            dynamicMax = 600;
        }
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

        for (Kanji k : dueKanjiList) {
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
        for (Kanji k : dueKanjiList) {
            LocalDateTime reviewTime = nextReviewCache.getOrDefault(k, now);
            if (!reviewTime.toLocalDate().isAfter(now.toLocalDate())) {
                dueKanji.add(k);
            }
        }

        if (!dueKanji.isEmpty()) {
            Collections.shuffle(dueKanji);
            System.out.println("from due");
            return dueKanji.get(0);
        }

        // 2. No strictly due Kanji -> try never learned Kanji
        if (!neverLearned.isEmpty()) {
            Collections.shuffle(neverLearned);
            System.out.println("from empty");
            return neverLearned.get(0);
        }

        // 3. Kanji that will be due soon (within next days)
        List<Kanji> soonDueKanji = new ArrayList<>();
        for (Kanji k : learned) {
            LocalDateTime reviewTime = nextReviewCache.get(k);
            int daysGoal = getIntervalDays(getDynamicMaxPoints(k));
            daysGoal--;
            if(getTodayProgress(k, now.toLocalDate()) == null){
                daysGoal = 0;
            }
            if (reviewTime.isBefore(now.plusDays(daysGoal))) {
                soonDueKanji.add(k);
            }
        }
        if (!soonDueKanji.isEmpty()) {
            Collections.shuffle(soonDueKanji);
            System.out.println("from soon");
            return soonDueKanji.get(0);
        }

        // 4. Any Kanji at random if no other condition met
        if (!allKanjiList.isEmpty()) {
            int rand = new Random().nextInt(allKanjiList.size());
            System.out.println("random");
            return allKanjiList.get(rand);
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

        //if kanji wasn't scheduled today it does not get any progress
        LocalDateTime reviewTime = nextReviewCache.getOrDefault(kanji, now);
        int daysGoal = getIntervalDays(getDynamicMaxPoints(kanji));
        daysGoal--;
        if(getTodayProgress(kanji, now.toLocalDate()) == null){
            daysGoal = 0;
        }
        if (reviewTime.toLocalDate().isAfter(now.toLocalDate()) && !reviewTime.isBefore(now.plusDays(daysGoal))) {
            System.out.println("no progress");
            return kanji;
        }

        // Calculate increment and dynamic max points
        int increment = calculatePointsIncrement(kanji, percent);
        int currentPoints = todayProgress != null ? todayProgress.getPoints() : 0;
        int dynamicMaxPoints = getDynamicMaxPoints(kanji);

        int newPoints = currentPoints + increment;
        if (newPoints > dynamicMaxPoints) {
            newPoints = dynamicMaxPoints;
        }

        System.out.println("progress: " + newPoints);

        if (todayProgress != null) {
            // Update the existing today's progress entry
            todayProgress.setPoints(newPoints);
            todayProgress.setLearned(now); // Update time
        } else {
            // Create a new entry for today
            KanjiProgress newProgress = new KanjiProgress();
            newProgress.setCompressedEntries(1);
            newProgress.setKanji(kanji);
            newProgress.setLearned(now);
            newProgress.setPoints(newPoints);
            kanji.getProgresses().add(newProgress);
        }

        // Update the cached next review time and dueCount
        updateNextReviewTime(kanji);
        updateDueCurrent(kanji);

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
        if(kanji.getProgresses().isEmpty()){
            return null;
        }
        return kanji.getProgresses().get(kanji.getProgresses().size() - 1);
    }

    private int calculatePointsIncrement(Kanji kanji, int percent) {
        int baseIncrement;
        if (percent >= 95) {
            baseIncrement = 25;
        } else if (percent >= 80) {
            baseIncrement = 15;
        } else if (percent >= 60) {
            baseIncrement = 10;
        } else {
            baseIncrement = 5;
        }

        int reviewCount = 0; // how many times recorded so far (before adding today's entry)
        for (KanjiProgress kanjiProgress : kanji.getProgresses()) {
            reviewCount += kanjiProgress.getCompressedEntries();
        }

        long daysSinceFirstLearned = 0;

        if(!kanji.getProgresses().isEmpty()){
            KanjiProgress firstProgress = kanji.getProgresses().getFirst();
            daysSinceFirstLearned = ChronoUnit.DAYS.between(firstProgress.getLearned().toLocalDate(), LocalDate.now());
        }

        if (daysSinceFirstLearned < 0) {
            daysSinceFirstLearned = 0;
        }
        daysSinceFirstLearned /= 2;

        double incrementFactor = 1.0 + ((reviewCount + daysSinceFirstLearned) * 0.3);
        if(reviewCount == 0 || reviewCount == 1) {
            incrementFactor = 0.5;
        }
        if(incrementFactor > 10){
            incrementFactor = 10;
        }
        return (int) Math.round(baseIncrement * incrementFactor);
    }
}
