package de.uulm.vs.autodetect.mds.framework.model.containers;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

public class DataContainer extends HashMap<WorldModelDataTypeEnum, MaatContainer> {
    private static final long serialVersionUID = -8964438079667936529L;

    public final long version;

    private final String messageID;

    public DataContainer(long t, String messageID) {
        this.version = t;
        this.messageID = messageID;
    }

    /**
     * Returns a set of types associated with this container. Detectors can use
     * this to determine whether this container is relevant. It can also be used
     * to design queries.
     *
     * @return
     */
    public Set<WorldModelDataTypeEnum> getDataTypes() {
        return this.keySet();
    }

    public String getMessageID() {
        return messageID;
    }

    /************** Inner class: DataContainerComparator **********************/
    /**
     * Simple Comparator that orders DataContainers by version
     */
    public static class DataContainerComparator implements Comparator<DataContainer> {
        @Override
        public int compare(DataContainer arg0, DataContainer arg1) {
            return Long.compare(arg0.version, arg1.version);
        }
    }

    public static final DataContainerComparator COMPARE = new DataContainerComparator();
}
