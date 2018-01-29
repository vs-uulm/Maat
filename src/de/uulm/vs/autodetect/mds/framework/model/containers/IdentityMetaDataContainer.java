package de.uulm.vs.autodetect.mds.framework.model.containers;

public class IdentityMetaDataContainer implements MaatContainer {

    private final String metaData;

    public IdentityMetaDataContainer(String metaData) {
        this.metaData = metaData;
    }

    @Override
    public WorldModelDataTypeEnum getDataType() {
        return WorldModelDataTypeEnum.NODE_META_DATA;
    }

    public String getMetaData() {
        return this.metaData;
    }

}
