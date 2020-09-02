package appbox.serialization;

import com.alibaba.fastjson.JSONWriter;

public interface IJsonSerializable {

    void writeToJson(JSONWriter writer);

}
