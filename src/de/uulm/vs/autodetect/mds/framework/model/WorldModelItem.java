package de.uulm.vs.autodetect.mds.framework.model;

import de.uulm.vs.autodetect.mds.framework.model.containers.DataContainer;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public abstract class WorldModelItem {

    protected long versionID;
    private final SortedSet<DataContainer> history;

    public long getVersionID() {
        return this.versionID;
    }

    public boolean isVertex() {
        return this instanceof WorldModelVertex;
    }

    public boolean isEdge() {
        return this instanceof WorldModelEdge;
    }

    public WorldModelItem() {
        this.history = new TreeSet<>(java.util.Collections.reverseOrder(DataContainer.COMPARE));
    }

    // *****************************************
    // HISTORY MANAGEMENT
    // *****************************************

    public WorldModelItem(WorldModelItem item) {
        this.history = new TreeSet<>(item.history);
        this.versionID = item.versionID;
    }

    /**
     * Query the amount of entries in the history of this Item, if any.
     *
     * @return
     */
    public int historySize() {
        return this.history.size();
    }

    protected void addToHistory(DataContainer item) {
        if (!this.history.add(item)) {
            // false means the element was not added because it already exists
            // inside the set => remove old occurrence first, then add again:
            // (Note that for SortedSet, o1 and o2 are equal iff
            // c.compare(o1,o2) == 0, with c being their comparator
            // => (item being removed).equals(item being added) == false)
            this.history.remove(item);
            this.history.add(item);
        }
    }

    protected void addAllToHistory(Set<DataContainer> otherHistory) {
        for (DataContainer item : otherHistory) {
            addToHistory(item);
        }
    }

    /**
     * Query a DataContainer corresponding to the given version. Returns null if
     * no such container exists.
     *
     * @param version
     * @return
     */
    public DataContainer getFromHistory(long version) {
        for (DataContainer dc : this.history) {
            if (dc.version == version)
                return dc;
        }
        return null;
    }

    public DataContainer getNewestFromHistory(long version) {
        DataContainer match = null;
        this.history.iterator();
        for (DataContainer dc : this.history) {
            if (dc.version <= version) {
                // note: history is in reverse order, so the first match is the
                // one we want
                match = dc;
                break;
            }
        }
        return match;
    }

    public DataContainer getLatest() {
        return this.history.first();
    }

    public SortedSet<DataContainer> getHistory() {
        return this.history;
    }

    public Set<DataContainer> getHistoryUpTo(long version) {
        return this.history.stream().filter(dc -> dc.version < version).collect(Collectors.toSet());
    }

    public boolean existsAt(long version) {
        return this.history.last().version <= version;
    }
}