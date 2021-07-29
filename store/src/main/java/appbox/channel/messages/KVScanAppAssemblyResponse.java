package appbox.channel.messages;

import appbox.serialization.IInputStream;

import java.nio.charset.StandardCharsets;

public final class KVScanAppAssemblyResponse extends KVScanResponse {

    public String[] assemblyNames;

    @Override
    public void readFrom(IInputStream bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        if (errorCode != 0)
            return;

        skipped = bs.readInt();
        length  = bs.readInt();

        int    keySize, valueSize = 0;
        byte[] key                = new byte[1024]; //TODO:最长的
        assemblyNames = new String[length];
        for (int i = 0; i < length; i++) {
            keySize = bs.readNativeVariant(); //Row's KeySize
            bs.readBytes(key, 0, keySize); //Row's Key
            //从Key中解析名称
            assemblyNames[i] = new String(key, 1, keySize - 1, StandardCharsets.UTF_8);

            //TODO:服务端不返回Value
            valueSize = bs.readNativeVariant(); //Row's ValueSize
            bs.skip(valueSize);
        }
    }

}
