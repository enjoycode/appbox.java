package appbox.store;

/** 存储于MetaCF的Assembly的类型 */
public enum MetaAssemblyType {
    Service(KVUtil.METACF_SERVICE_ASSEMBLY_PREFIX),
    View(KVUtil.METACF_VIEW_ASSEMBLY_PREFIX),
    Application(KVUtil.METACF_APP_ASSEMBLY_PREFIX);

    public final byte value;

    MetaAssemblyType(byte v) {
        value = v;
    }

}
