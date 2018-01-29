package de.uulm.vs.autodetect.mds.framework.model.ipc;

public enum IPCMessageType {
    // detection result management
    DetectionResult,
    DetectionResultSet,
    DetectionTrigger,

    // input types
    InputData,
    InputOpinion,

    // Maat data management
    NewDiff,
    NewSnapshot;
}
