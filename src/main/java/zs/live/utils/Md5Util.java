package zs.live.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Util {
    private static ThreadLocal<MessageDigest> messageDigestHolder = new ThreadLocal();
    private static final char[] hexDigits =
            { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static final int HTTP_RETRIES = 3;

    static {
        try {
            MessageDigest message = MessageDigest.getInstance("MD5");
            messageDigestHolder.set(message);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String md5(String data)
    {
        try
        {
            MessageDigest message = (MessageDigest)messageDigestHolder.get();
            if (message == null) {
                message = MessageDigest.getInstance("MD5");
                messageDigestHolder.set(message);
            }

            message.update(data.getBytes("utf-8"));
            byte[] b = message.digest();

            String digestHexStr = "";
            for (int i = 0; i < 16; i++) {
                digestHexStr = digestHexStr + byte2hexstr(b[i]);
            }

            return digestHexStr;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String byte2hexstr(byte ib) {
        char[] ob = new char[2];
        ob[0] = hexDigits[(ib >>> 4 & 0xF)];
        ob[1] = hexDigits[(ib & 0xF)];

        return new String(ob);
    }
}
