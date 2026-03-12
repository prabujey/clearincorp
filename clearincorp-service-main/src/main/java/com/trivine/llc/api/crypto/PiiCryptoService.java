package com.trivine.llc.api.crypto;

import com.trivine.llc.api.config.CryptoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

import static com.trivine.llc.api.crypto.PiiTypes.GCM_TAG_BITS;

/**
 * PII encryption service using local AES-256-GCM envelope encryption.
 *
 * Uses the same envelope-encryption pattern as the previous AWS KMS
 * implementation:
 * 1. Generate a random Data Encryption Key (DEK) for each encrypt() call
 * 2. Encrypt the plaintext with the DEK using AES-256-GCM
 * 3. Encrypt (wrap) the DEK with a local Master Key using AES-256-GCM
 * 4. Store the wrapped DEK alongside the ciphertext
 *
 * The EncryptResult format is fully backward-compatible.
 * The master key is read from the config property: crypto.local.master-key-b64
 */
@Slf4j
@Component
public class PiiCryptoService {
    private final SecretKey masterKey;
    private final SecureRandom rnd = new SecureRandom();

    public PiiCryptoService(CryptoProperties props) {
        byte[] keyBytes = Base64.getDecoder().decode(props.getMasterKeyB64());
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "Master key must be exactly 32 bytes (256 bits). Got: " + keyBytes.length);
        }
        this.masterKey = new SecretKeySpec(keyBytes, "AES");
        log.info("PiiCryptoService initialized with local AES-256-GCM envelope encryption");
    }

    public EncryptResult encrypt(String plaintext) throws Exception {
        // 1. Generate a random 256-bit Data Encryption Key (DEK)
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256, rnd);
        SecretKey dek = keyGen.generateKey();
        byte[] dekBytes = dek.getEncoded();

        // 2. Encrypt the plaintext with the DEK
        byte[] iv = new byte[12];
        rnd.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(dekBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ct = cipher.doFinal(plaintext.getBytes());

        // 3. Wrap (encrypt) the DEK with the master key
        byte[] wrapIv = new byte[12];
        rnd.nextBytes(wrapIv);
        Cipher wrapCipher = Cipher.getInstance("AES/GCM/NoPadding");
        wrapCipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, wrapIv));
        byte[] wrappedDek = wrapCipher.doFinal(dekBytes);

        // 4. Combine wrapIv + wrappedDek into encryptedDekB64 (so we can unwrap later)
        byte[] encDek = new byte[wrapIv.length + wrappedDek.length];
        System.arraycopy(wrapIv, 0, encDek, 0, wrapIv.length);
        System.arraycopy(wrappedDek, 0, encDek, wrapIv.length, wrappedDek.length);

        // Zero out the plain DEK from memory
        java.util.Arrays.fill(dekBytes, (byte) 0);

        EncryptResult r = new EncryptResult();
        r.setCiphertextB64(Base64.getEncoder().encodeToString(ct));
        r.setIvB64(Base64.getEncoder().encodeToString(iv));
        r.setEncryptedDekB64(Base64.getEncoder().encodeToString(encDek));
        return r;
    }

    public String decrypt(EncryptResult encryptResult) throws Exception {
        // 1. Decode the wrapped DEK (wrapIv + wrappedDek)
        byte[] encDek = Base64.getDecoder().decode(encryptResult.getEncryptedDekB64());
        byte[] wrapIv = new byte[12];
        byte[] wrappedDek = new byte[encDek.length - 12];
        System.arraycopy(encDek, 0, wrapIv, 0, 12);
        System.arraycopy(encDek, 12, wrappedDek, 0, wrappedDek.length);

        // 2. Unwrap (decrypt) the DEK with the master key
        Cipher unwrapCipher = Cipher.getInstance("AES/GCM/NoPadding");
        unwrapCipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, wrapIv));
        byte[] dekBytes = unwrapCipher.doFinal(wrappedDek);

        // 3. Decrypt the ciphertext with the DEK
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(dekBytes, "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, Base64.getDecoder().decode(encryptResult.getIvB64())));
        byte[] pt = cipher.doFinal(Base64.getDecoder().decode(encryptResult.getCiphertextB64()));

        java.util.Arrays.fill(dekBytes, (byte) 0);
        return new String(pt);
    }

}
