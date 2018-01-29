package de.uulm.vs.autodetect.mds.framework.inputAPI;

import de.uulm.vs.autodetect.mds.framework.model.WorldModelEdge;
import no.uio.subjective_logic.opinion.SubjectiveOpinion;

/**
 * This wrapper stores a tuple representing an {@link SubjectiveOpinion}. The
 * opinion always originates from an Identity, and references <b>either</b>
 * another Identity <b>or</b> a Measurement. This class also has a time stamp,
 * to allow future versions of Maat to maintain a history of opinions, and to
 * make sure only the latest opinion is used.
 *
 * @author Rens van der Heijden
 * @see WorldModelEdge
 */
public class ExternalOpinion {

    public ExternalOpinion(Identity id, SubjectiveOpinion o, Measurement data) {
        this.opinionHolder = id;
        this.data = data;
        this.subject = null;
        this.opinion = o;
    }

    public ExternalOpinion(Identity id, SubjectiveOpinion o, Identity subject) {
        this.opinionHolder = id;
        this.data = null;
        this.subject = subject;
        this.opinion = o;
    }

    protected final Identity opinionHolder;
    protected final Measurement data;
    protected final Identity subject;
    protected final SubjectiveOpinion opinion;

    public boolean isAboutData() {
        return this.data != null && this.subject == null;
    }

    public boolean isAboutIdentity() {
        return this.subject != null && this.data == null;
    }

    public Identity getOpinionHolder() {
        return this.opinionHolder;
    }

    public Measurement getData() {
        return this.data;
    }

    public Identity getSubject() {
        return this.subject;
    }

    public SubjectiveOpinion getOpinion() {
        return this.opinion;
    }
}
