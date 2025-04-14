package at.primetshofer.model.Trainer;

import at.primetshofer.model.Controller;
import at.primetshofer.model.entities.Word;
import at.primetshofer.model.entities.WordProgress;
import at.primetshofer.model.util.HibernateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.apache.log4j.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class VocabTrainer {

    private final static Logger logger = Logger.getLogger(VocabTrainer.class);

    private static VocabTrainer instance;
    private List<Word> dueWordList;
    private List<Word> allWordList;
    private List<Word> tmpDueWordList;
    private int todayDueMax;
    private int dueCurrent;
    private int dueTotal;
    private int tmpDueIncrease;
    private Map<Word, LocalDateTime> nextReviewCache;

    private VocabTrainer() {
        tmpDueIncrease = 0;
    }

    public static VocabTrainer getInstance() {
        if (instance == null) {
            instance = new VocabTrainer();
        }
        return instance;
    }

    public void updateWordList() {
        dueWordList = new ArrayList<>();
        tmpDueWordList = new ArrayList<>();

        EntityManager entityManager = HibernateUtil.getEntityManager();

        String jpql = "SELECT COUNT(w) FROM Word w";
        Query query = entityManager.createQuery(jpql);
        long wordCount = (long) query.getSingleResult();

        if (allWordList == null || wordCount != allWordList.size()) {
            allWordList = entityManager.createQuery("SELECT w FROM Word w left join fetch w.progresses", Word.class).getResultList();

            sortWordList(allWordList);
        }

        nextReviewCache = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        todayDueMax = 0;
        dueCurrent = 0;
        dueTotal = 0;

        int maxWords = Controller.getInstance().getSettings().getMaxDailyWords();
        int extraWordsRemaining = tmpDueIncrease;

        for (Word word : allWordList) {
            updateNextReviewTime(word);

            LocalDateTime reviewTime = nextReviewCache.getOrDefault(word, now);
            int daysGoal = getIntervalDays(getDynamicMaxPoints(word));
            daysGoal--;

            if (getTodayProgress(word, now.toLocalDate()) == null) {
                daysGoal = 0;
            }

            WordProgress lastProgress = getLastProgress(word);

            if (!reviewTime.toLocalDate().isAfter(now.toLocalDate().plusDays(daysGoal))) {
                dueTotal++;

                if (todayDueMax < maxWords || (lastProgress != null && isToday(lastProgress.getLearned()))) {
                    todayDueMax++;
                    dueCurrent++;

                    dueWordList.add(word);
                } else if (extraWordsRemaining > 0) {
                    extraWordsRemaining--;

                    todayDueMax++;
                    dueCurrent++;

                    dueWordList.add(word);
                    tmpDueWordList.add(word);
                }
            } else if (lastProgress != null && isToday(getLastProgress(word).getLearned())) {
                todayDueMax++;
            }
        }
    }

    public void tmpDueIncrease(int tmpDueIncrease) {
        this.tmpDueIncrease += tmpDueIncrease;
    }

    public void sortWordList(List<Word> wordsList) {
        wordsList.sort((k1, k2) -> {
            WordProgress lastProgress1 = getLastProgress(k1);
            WordProgress lastProgress2 = getLastProgress(k2);

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

    private void updateDueCurrent(Word word) {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime reviewTime = nextReviewCache.getOrDefault(word, now);
        int daysGoal = getIntervalDays(getDynamicMaxPoints(word));

        daysGoal--;
        if (getTodayProgress(word, now.toLocalDate()) == null) {
            daysGoal = 0;
        }
        if (reviewTime.toLocalDate().isAfter(now.toLocalDate().plusDays(daysGoal))) {
            if (dueCurrent > 0) {
                dueCurrent--;
            }
            if (dueTotal > 0) {
                dueTotal--;
            }
        }
        if (tmpDueWordList.contains(word)) {
            tmpDueIncrease--;
            tmpDueWordList.remove(word);
        }
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

    private int getDynamicMaxPoints(Word word) {
        List<WordProgress> progressList = word.getProgresses();
        int reviewCount = 0; // how many times we recorded progress
        for (WordProgress WordProgress : progressList) {
            reviewCount += WordProgress.getCompressedEntries();
        }

        if (progressList.isEmpty()) {
            // If never learned, start with a small max
            return 50;
        }

        WordProgress firstProgress = progressList.getFirst();
        long daysSinceFirstLearned = ChronoUnit.DAYS.between(firstProgress.getLearned().toLocalDate(), LocalDate.now());
        if (daysSinceFirstLearned < 0) {
            daysSinceFirstLearned = 0;
        }
        daysSinceFirstLearned /= 4;

        int dynamicMax = (int) Math.min((daysSinceFirstLearned + reviewCount) * 20, 600);
        if (dynamicMax < 0) {
            dynamicMax = 600;
        }

        dynamicMax -= getLastProgress(word).getNegativePoints();

        if (dynamicMax < 50) {
            dynamicMax = 50;
        }

        return dynamicMax;
    }

    private void updateNextReviewTime(Word word) {
        if (word.getProgresses().isEmpty()) {
            nextReviewCache.put(word, LocalDateTime.now());
            return;
        }

        WordProgress lastProgress = getLastProgress(word);
        int points = lastProgress.getPoints();
        int intervalDays = getIntervalDays(points);
        LocalDateTime nextReviewDate = lastProgress.getLearned().plusDays(intervalDays);
        nextReviewCache.put(word, nextReviewDate);
    }

    private List<Word> calcNextLearningWord(int count) {
        Set<Word> words = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        List<Word> neverLearned = new ArrayList<>();
        List<Word> learned = new ArrayList<>();

        for (Word w : dueWordList) {
            if (w.getProgresses().isEmpty()) {
                neverLearned.add(w);
            } else {
                learned.add(w);
            }
        }

        for (Word w : learned) {
            if (!nextReviewCache.containsKey(w)) {
                updateNextReviewTime(w);
            }
        }

        for (Word w : neverLearned) {
            if (!nextReviewCache.containsKey(w)) {
                nextReviewCache.put(w, now);
            }
        }

        List<Word> dueWords = new ArrayList<>();
        for (Word w : dueWordList) {
            LocalDateTime reviewTime = nextReviewCache.getOrDefault(w, now);
            if (!reviewTime.toLocalDate().isAfter(now.toLocalDate())) {
                dueWords.add(w);
            }
        }

        Collections.shuffle(dueWords);
        while (!dueWords.isEmpty() && words.size() < count) {
            words.add(dueWords.removeFirst());
        }

        Collections.shuffle(neverLearned);
        while (!neverLearned.isEmpty() && words.size() < count) {
            words.add(neverLearned.removeFirst());
        }

        if (dueWords.size() >= count) {
            return new ArrayList<>(words);
        }

        List<Word> soonDueWord = new ArrayList<>();
        for (Word w : learned) {
            LocalDateTime reviewTime = nextReviewCache.get(w);
            int daysGoal = getIntervalDays(getDynamicMaxPoints(w));
            daysGoal--;
            if (getTodayProgress(w, now.toLocalDate()) == null) {
                daysGoal = 0;
            }
            if (reviewTime.isBefore(now.plusDays(daysGoal))) {
                soonDueWord.add(w);
            }
        }

        Collections.shuffle(soonDueWord);
        while (!soonDueWord.isEmpty() && words.size() < count) {
            words.add(soonDueWord.removeFirst());
        }

        if (!words.isEmpty()) {
            return new ArrayList<>(words);
        }

        int tries = 50;
        Random random = new Random();
        while (!allWordList.isEmpty() && words.size() < count && tries > 0) {
            int rand = random.nextInt(allWordList.size());
            Word word = allWordList.get(rand);
            if (!words.contains(word)) {
                words.add(word);
            } else {
                tries--;
            }
        }

        return new ArrayList<>(words);
    }

    public List<Word> getNextLearningWord(int count) {
        return calcNextLearningWord(count);
    }

    public Word addWordProgress(Word word, int percent) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // Check if there's already a progress entry for today
        WordProgress todayProgress = getTodayProgress(word, today);

        //if word wasn't scheduled today it does not get any progress
        LocalDateTime reviewTime = nextReviewCache.getOrDefault(word, now);
        int daysGoal = getIntervalDays(getDynamicMaxPoints(word));
        daysGoal--;
        if (getTodayProgress(word, now.toLocalDate()) == null) {
            daysGoal = 0;
        }
        if (reviewTime.toLocalDate().isAfter(now.toLocalDate()) && !reviewTime.isBefore(now.plusDays(daysGoal))) {
            logger.info("No progress made for word: '" + word.getEnglish() + "'");
            return word;
        }

        // Calculate increment and dynamic max points
        int increment = calculatePointsIncrement(word, percent, false);
        int negativePoints = calculatePointsIncrement(word, percent, true);
        int currentPoints = todayProgress != null ? todayProgress.getPoints() : 0;
        int dynamicMaxPoints = getDynamicMaxPoints(word);

        int newPoints = currentPoints + increment;
        if (newPoints > dynamicMaxPoints) {
            newPoints = dynamicMaxPoints;
        }

        logger.info("Progress made for kanji '" + word.getEnglish() + "': " + newPoints + "pts");

        if (todayProgress != null) {
            // Update the existing today's progress entry
            todayProgress.setPoints(newPoints);
            todayProgress.setLearned(now); // Update time
            todayProgress.setNegativePoints(todayProgress.getNegativePoints() + negativePoints);
        } else {
            // Create a new entry for today
            WordProgress newProgress = new WordProgress();
            newProgress.setCompressedEntries(1);
            newProgress.setWord(word);
            newProgress.setLearned(now);
            newProgress.setPoints(newPoints);
            newProgress.setNegativePoints(negativePoints);
            word.getProgresses().add(newProgress);
        }

        // Update the cached next review time and dueCount
        updateNextReviewTime(word);
        updateDueCurrent(word);

        return word;
    }

    private WordProgress getTodayProgress(Word word, LocalDate today) {
        // Check if last progress entry is from today
        if (!word.getProgresses().isEmpty()) {
            WordProgress last = getLastProgress(word);
            if (last.getLearned().toLocalDate().equals(today)) {
                return last;
            }
        }
        return null;
    }

    private WordProgress getLastProgress(Word word) {
        if (word.getProgresses().isEmpty()) {
            return null;
        }
        return word.getProgresses().get(word.getProgresses().size() - 1);
    }

    private int calculatePointsIncrement(Word word, int percent, boolean negtive) {
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

        if (negtive) {
            baseIncrement = 25 - baseIncrement;
        }

        int reviewCount = 0; // how many times recorded so far (before adding today's entry)
        for (WordProgress wordProgress : word.getProgresses()) {
            reviewCount += wordProgress.getCompressedEntries();
        }

        long daysSinceFirstLearned = 0;

        if (!word.getProgresses().isEmpty()) {
            WordProgress firstProgress = word.getProgresses().getFirst();
            daysSinceFirstLearned = ChronoUnit.DAYS.between(firstProgress.getLearned().toLocalDate(), LocalDate.now());
        }

        if (daysSinceFirstLearned < 0) {
            daysSinceFirstLearned = 0;
        }
        daysSinceFirstLearned /= 2;

        double incrementFactor = 1.0 + ((reviewCount + daysSinceFirstLearned) * 0.3);
        if (reviewCount == 0 || reviewCount == 1) {
            incrementFactor = 0.5;
        }
        if (incrementFactor > 20) {
            incrementFactor = 20;
        }
        return (int) Math.round(baseIncrement * incrementFactor);
    }

    public List<Word> getRandomWords(int number) {
        List<Word> words = new ArrayList<>();

        int half = number / 2;

        half = Math.min(half, dueWordList.size());
        half = (dueWordList.size() == 1) ? 0 : half;

        int otherHalf = number - half;

        Random random = new Random();

        getRandomWordsFromList(words, dueWordList, half, random);

        getRandomWordsFromList(words, allWordList, otherHalf, random);

        return words;
    }

    private void getRandomWordsFromList(List<Word> toList, List<Word> fromList, int amount, Random random) {
        for (int i = 0; i < amount; i++) {
            Word word = fromList.get(random.nextInt(fromList.size()));
            int tries = 0;

            while ((toList.contains(word)) && tries < 10) {
                word = fromList.get(random.nextInt(fromList.size()));
                tries++;
            }

            toList.add(word);
        }
    }
}
