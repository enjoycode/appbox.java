package appbox.serialization.serializers;

import appbox.model.ViewModel;
import appbox.serialization.*;

public class ViewModelSerializer extends TypeSerializer {

    public static final ViewModelSerializer instance = new ViewModelSerializer();

    public ViewModelSerializer() {
        super(PayloadType.ViewModel, ViewModel.class, null);
    }

    @Override
    public void write(IOutputStream bs, Object value) {
        ((IBinSerializable) value).writeTo(bs);
    }

    @Override
    public Object read(IInputStream bs, Object value) {
        ((IBinSerializable) value).readFrom(bs);
        return value;
    }
}
