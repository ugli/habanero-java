package se.ugli.habanero.j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class TypeRegistry {

    private final static Map<Class<?>, TypeAdaptor> cache = new ConcurrentHashMap<>();
    private final static List<TypeAdaptor> typeAdaptors = Collections.synchronizedList(new ArrayList<TypeAdaptor>());

    private TypeRegistry() {
    }

    static void add(final TypeAdaptor typeAdaptor, final boolean highestPriority) {
        if (highestPriority)
            typeAdaptors.add(0, typeAdaptor);
        else
            typeAdaptors.add(typeAdaptor);
    }

    static void remove(final TypeAdaptor typeAdaptor) {
        typeAdaptors.remove(typeAdaptor);
        cache.clear();
    }

    static TypeAdaptor get(final Class<?> type) {
        final TypeAdaptor cachedTypeAdaptor = cache.get(type);
        if (cachedTypeAdaptor != null)
            return cachedTypeAdaptor;
        for (final TypeAdaptor typeAdaptor : typeAdaptors)
            if (typeAdaptor.supports(type)) {
                cache.put(type, typeAdaptor);
                return typeAdaptor;
            }
        throw new HabaneroException(type.getName() + " isn't registered.");
    }

}
