package de.uulm.vs.autodetect.mds.framework.model.containers;

import no.uio.subjective_logic.opinion.SubjectiveOpinion;

public class OpinionContainer implements MaatContainer {

    private final SubjectiveOpinion opinion;

    public OpinionContainer(SubjectiveOpinion o) {
        this.opinion = o;
        if (o == null)
            throw new NullPointerException();
    }

    public SubjectiveOpinion getOpinion() {
        return this.opinion;
    }

    @Override
    public WorldModelDataTypeEnum getDataType() {
        return WorldModelDataTypeEnum.OPINION;
    }

}