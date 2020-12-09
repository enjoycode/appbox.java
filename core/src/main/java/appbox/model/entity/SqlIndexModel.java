package appbox.model.entity;

import appbox.model.EntityModel;

public final class SqlIndexModel extends IndexModelBase {

    SqlIndexModel(EntityModel owner) {
        super(owner);
    }

    public SqlIndexModel(EntityModel owner, String name, boolean unique,
                         FieldWithOrder[] fields, short[] storingFields) {
        super(owner, name, unique, fields, storingFields);
    }

}
