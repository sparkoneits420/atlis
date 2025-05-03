package org.atlis.common.security;

import java.security.PublicKey;
import javax.crypto.Cipher;

public class RSA {

    public static byte[] encrypt(byte[] data, PublicKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    public static int[] generateSeedArray(long seed) {
        int[] result = new int[256];
        for (int i = 0; i < result.length; i++) {
            result[i] = (int) ((seed >> ((i % 8) * 8)) ^ (seed * (i + 1)));
        }
        return result;
    }
}
