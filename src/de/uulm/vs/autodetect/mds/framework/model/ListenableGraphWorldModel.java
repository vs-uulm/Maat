package de.uulm.vs.autodetect.mds.framework.model;

import de.uulm.vs.autodetect.mds.framework.view.WorldModelChangeListener;

import java.util.LinkedList;
import java.util.List;

public interface ListenableGraphWorldModel {

    final List<WorldModelChangeListener> changeListeners = new LinkedList<>();

    /**
     * The given ComputeGraphNode instance will be notified whenever a change
     * occurs in the world model, either because new data was stored or because
     * new edges were stored.
     *
     * @param listener the listener that will be notified.
     */
    public default void registerChangeListener(WorldModelChangeListener listener) {
        this.changeListeners.add(listener);
    }

    default void notifyChangeListeners() {
        for (WorldModelChangeListener changeListener : this.changeListeners)
            changeListener.update(this);
    }

    public default void removeChangeListener(WorldModelChangeListener listener) {
        this.changeListeners.remove(listener);
    }
}
