package at.primetshofer.model.util;

import java.util.Collection;
import java.util.List;
import java.util.Random;

public class CollectionUtil {
    private CollectionUtil() { }

    public static <T> void deleteRandomElement(List<T> list) {
        Random random = new Random();
        int randomIndex = random.nextInt(list.size() - 2) + 1;
        list.remove(randomIndex);
    }
}
