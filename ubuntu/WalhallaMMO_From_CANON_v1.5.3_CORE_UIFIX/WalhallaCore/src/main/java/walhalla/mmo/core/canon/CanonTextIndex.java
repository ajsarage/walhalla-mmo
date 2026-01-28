package walhalla.mmo.core.canon;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal structured index extracted from canon annex texts.
 * Phase 7 focuses on safely consuming IDs and caps without inventing any new data.
 */
public final class CanonTextIndex {

    private static final Pattern ITEM_ID = Pattern.compile("\\bIT_[A-Z0-9_]+\\b");
    private static final Pattern STATION_ID = Pattern.compile("\\bST_[A-Z0-9_]+\\b");

    private final Set<String> itemIds;
    private final Set<String> stationIds;

    public CanonTextIndex(Set<String> itemIds, Set<String> stationIds) {
        this.itemIds = Collections.unmodifiableSet(new HashSet<>(itemIds));
        this.stationIds = Collections.unmodifiableSet(new HashSet<>(stationIds));
    }

    public Set<String> itemIds() { return itemIds; }
    public Set<String> stationIds() { return stationIds; }

    public static CanonTextIndex buildFromAllText(Iterable<String> texts) {
        Set<String> items = new HashSet<>();
        Set<String> stations = new HashSet<>();
        for (String t : texts) {
            if (t == null || t.isBlank()) continue;
            Matcher m1 = ITEM_ID.matcher(t);
            while (m1.find()) items.add(m1.group());
            Matcher m2 = STATION_ID.matcher(t);
            while (m2.find()) stations.add(m2.group());
        }
        return new CanonTextIndex(items, stations);
    }
}
