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

    /**
     * 首字母转化大写
     */
    public static String firstUpperCase(String str) {
        if ((str == null) || (str.length() == 0))
            return str;
        char[] ch = str.toCharArray();
        if (ch[0] >= 'a' && ch[0] <= 'z') {
            ch[0] = (char) (ch[0] - 32);
        }
        return new String(ch);
    }

    /** 获取utf8编码长度 */
    public static int getUtf8Size(String value) throws Exception {
        if (value == null | value.length() == 0)
            return 0;

        //TODO:能否判断底层是否单字节编码，这样可以直接返回长度

        int size = 0;
        int  srcPos = 0;
        int  srcLen = value.length();
        char c, d;
        int  uc, ip;
        while (srcPos < srcLen) {
            c = value.charAt(srcPos++);
            if (c < 0x80) {
                // Have at most seven bits
                size++;
            } else if (c < 0x800) {
                // 2 bytes, 11 bits
                size += 2;
            } else if (Character.isSurrogate(c)) {
                ip = srcPos - 1;
                if (Character.isHighSurrogate(c)) {
                    if (srcLen - ip < 2) {
                        uc = -1;
                    } else {
                        d = value.charAt(ip + 1);
                        if (Character.isLowSurrogate(d)) {
                            uc = Character.toCodePoint(c, d);
                        } else {
                            throw new Exception();
                        }
                    }
                } else {
                    if (Character.isLowSurrogate(c)) {
                        throw new Exception();
                    } else {
                        uc = c;
                    }
                }

                if (uc < 0) {
                    size ++;
                } else {
                    size += 4;
                    srcPos++; // 2 chars
                }
            } else {
                // 3 bytes, 16 bits
                size += 3;
            }
        }

        return size;
    }
}
