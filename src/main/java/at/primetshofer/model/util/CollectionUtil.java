package at.primetshofer.model.util;

import java.util.List;
import java.util.Random;

public class CollectionUtil {
    private CollectionUtil() { }

    public static <T> void deleteMiddleElement(List<T> list) {
        if (list.isEmpty())
            return;

        list.remove(list.size() / 2);
    }
}
