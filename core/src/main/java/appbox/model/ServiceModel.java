package appbox.model;

public final class ServiceModel extends ModelBase {
    //public enum Language {
    //    Java(1), CSharp(0);
    //
    //    public final byte value;
    //
    //    Language(int v) {
    //        value = (byte) v;
    //    }
    //}
    //
    //private Language _language;

    @Override
    public ModelType modelType() {
        return ModelType.Service;
    }
}
