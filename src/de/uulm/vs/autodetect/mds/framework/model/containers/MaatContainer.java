package de.uulm.vs.autodetect.mds.framework.model.containers;

/**
 * This abstract interface is at the top of the Maat container hierarchy.
 * Extensions of this interface represent individual data types; for aggregate
 * types, use the {@link DataContainer} interface, which maps different
 * {@link WorldModelDataTypeEnum} type identifiers to the respective data (in a
 * {@link MaatContainer}).
 *
 * @author Rens van der Heijden
 */
public interface MaatContainer {

    /**
     * Returns the data type associated with this MaatContainer
     *
     * @return
     */
    public WorldModelDataTypeEnum getDataType();
}
