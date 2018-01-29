package de.uulm.vs.autodetect.mds.framework.controller.detectors;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public enum DetectorIndex {
    INSTANCE;

    private final Map<String, AbstractDetectorFactory> index;

    DetectorIndex() {
        this.index = new HashMap<>();
    }

    public void registerDetector(String name, AbstractDetectorFactory factory) {
        this.index.put(name, factory);
    }

    public AbstractDetectorFactory getFactoryByDetectorName(String name) {
        return this.index.get(name);
    }

    public Collection<AbstractDetectorFactory> getAllFactories() {
        return this.index.values();
    }

}