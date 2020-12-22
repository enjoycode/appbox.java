package appbox.model;

import appbox.data.PersistentState;
import appbox.serialization.*;

public class EnumModelItem implements IJsonSerializable, IBinSerializable {

    //region ====Fields & Properties====
    public String name;

    public int value;

    public String comment;
    //endregion
    //region ====Ctor====
    public EnumModelItem() { }

    public EnumModelItem(String name, int value)
    {
        name = name;
        value = value;
    }
    //endregion
    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeString(name);
        bs.writeInt(value);
        if(comment!=null&&!comment.equals("")){
            bs.writeString(comment);
        }
        bs.finishWriteFields();
    }

    @Override
    public void readFrom(IInputStream bs) {
        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
                case 1:
                    name = bs.readString();
                    break;
                case 2:
                    value = bs.readInt();
                    break;
                case 3:
                    comment = bs.readString();
                    break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id:" + propIndex);
            }
        } while (propIndex != 0);
    }

    @Override
    public void writeToJson(IJsonWriter writer) {
        //TODO
    }
    //endregion


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
