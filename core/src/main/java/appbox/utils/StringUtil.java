package appbox.utils;

public final class StringUtil {
    private StringUtil() {
    }

    /**
     * 获取字符串HashCode,用以消除平台实现的差异性(与C#一致)
     */
    public static int getHashCode(String value) {
        int hash1 = 5381;
        int hash2 = hash1;

        int c = 0;
        for (int i = 0; i < value.length(); i++) {
            c     = value.charAt(i);
            hash1 = ((hash1 << 5) + hash1) ^ c;
            if (i + 1 == value.length()) {
                break;
            }
            c     = value.charAt(i + 1);
            hash2 = ((hash2 << 5) + hash2) ^ c;
            i++;
        }

        return hash1 + (hash2 * 1566083941);
    }
}
