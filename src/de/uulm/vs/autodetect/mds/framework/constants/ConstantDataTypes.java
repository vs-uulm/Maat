package de.uulm.vs.autodetect.mds.framework.constants;

import de.uulm.vs.autodetect.mds.framework.model.containers.WorldModelDataTypeEnum;

import java.util.HashSet;
import java.util.Set;

public class ConstantDataTypes {

    /**
     * an OPINION.
     */
    public final static Set<WorldModelDataTypeEnum> opinionType;

    static {
        opinionType = new HashSet<>(1);
        opinionType.add(WorldModelDataTypeEnum.OPINION);
    }

    /**
     * an empty set that can for instance be used
     * to express that an edge has no opinion.
     */
    public final static Set<WorldModelDataTypeEnum> emptyDataTypesSet;

    static {
        emptyDataTypesSet = new HashSet<>(0);
    }
}
