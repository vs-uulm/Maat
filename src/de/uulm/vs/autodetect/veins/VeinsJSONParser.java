package de.uulm.vs.autodetect.veins;

import de.uulm.vs.autodetect.mds.framework.model.containers.MaatContainer;
import org.json.simple.JSONObject;

/**
 * Created by namnatulco on 3/30/17.
 */
public interface VeinsJSONParser {
    public MaatContainer parse(JSONObject obj) throws IllegalArgumentException;
}
