package utils;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AppUtils {
    /**
     * Returns the transformed list containing the elements which result from applying the mapper to each corresponding
     * value in lists a and b..
     */
    public static <T, Y> List<Y> zipInto(List<T> a, List<T> b, BiFunction<T, T, Y> mapper) {
        return IntStream.range(0, Math.min(a.size(), b.size()))
                .mapToObj(i -> mapper.apply(a.get(i), b.get(i)))
                .collect(Collectors.toList());
    }
}
