import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Formatter;

class Encryption {

    /**
     * Loads the data from a byte array and calculates a hex string with its sha
     *
     * @param data the data whose SHA should be calculated
     * @return a string hex of the hash
     */
    static String GenerateHash(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(data);
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    /**
     * Generates a new AES key
     *
     * @return the new key
     */
    static SecretKey GenerateAESKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(128);
        return generator.generateKey();
    }

    /**
     * Generate a pair of public/private keys
     *
     * @return the pair of keys
     */
    static KeyPair GenerateKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Saves a key to a file
     *
     * @param key  the key to save
     * @param file the file to save the key to
     */
    static void SaveKey(Key key, File file) throws IOException {
        file.getParentFile().mkdir();
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            fos.write(key.getEncoded());
            fos.flush();
        }
    }

    /**
     * Load a public key from a file
     *
     * @param file the file to load from
     * @return the public key
     */
    static PublicKey LoadPublicKey(File file) throws Exception {
        if (!file.exists())
            throw new Exception("Key file not found");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = fis.readAllBytes();
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
        }
    }

    /**
     * Load a private key from a file
     *
     * @param file the file to load from
     * @return the private key
     */
    static PrivateKey LoadPrivateKey(File file) throws Exception {
        if (!file.exists())
            throw new Exception("Key file not found");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = fis.readAllBytes();
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
        }
    }

    /**
     * Save a byte array to a file using a specific key to encrypt it
     *
     * @param bytes the byte array to save
     * @param file  the file to save to
     * @param key   the key that will be used for encryption
     */
    static void SaveEncryptedWithRSA(byte[] bytes, File file, PrivateKey key) throws GeneralSecurityException, IOException {
        final Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(cipher.doFinal(bytes));
            fos.flush();
        }
    }

    /**
     * Loads a file and decrypts it using a key
     *
     * @param file the file to load
     * @param key  the key to use to decrypt
     * @return the decrypted file as a byte array
     */
    static byte[] LoadDecryptedWithRSA(File file, PublicKey key) throws GeneralSecurityException, IOException {
        final Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] bytes;
        try (FileInputStream fis = new FileInputStream(file)) {
            bytes = fis.readAllBytes();
        }
        return cipher.doFinal(bytes);
    }

    static byte[] GenerateIV() {
        byte[] iv = new byte[128 / 8];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    static void SaveEncryptedWithAES(byte[] bytes, File file, SecretKey aes, PrivateKey privateKey, byte[] iv) throws GeneralSecurityException, IOException {
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        try (FileOutputStream fos = new FileOutputStream(file)) {//open the output file for saving
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            byte[] keyBytes = cipher.doFinal(aes.getEncoded());//encode the AES key with the private key
            fos.write(keyBytes); //save the AES key to the start of the file
            fos.write(iv); //save the iv next
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, aes, ivSpec);
            fos.write(cipher.doFinal(bytes)); //save the file using the aes key and the iv spec
            fos.flush();
        }
    }

    static byte[] LoadDecryptedWithAES(File file, PublicKey publicKey) throws GeneralSecurityException, IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            byte[] b = new byte[256];
            fis.read(b); //load the first 256 bytes from the file which contain the key
            byte[] keyBytes = cipher.doFinal(b); //and decode them
            SecretKeySpec aes = new SecretKeySpec(keyBytes, "AES"); //to produce the AES key
            byte[] iv = new byte[128 / 8];
            fis.read(iv);//read the next 128/8 bytes  to get the iv
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, aes, ivSpec);
            return cipher.doFinal(fis.readAllBytes()); //read the rest of the bytes decoded with the AES key
        }


    }

}
