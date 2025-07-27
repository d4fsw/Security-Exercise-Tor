import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
class DataManager {
    private static final String WORK_PATH = "c:\\temp\\server\\";
    private static PublicKey serverPublicKey;
    private static PrivateKey serverPrivateKey;
    private static final String USER_FILE = WORK_PATH + "Users.txt";
    private static final String KEY_FILE_SUFFIX = ".key";
    private static final String SEPARATOR = ":";
    private List<User> users = new ArrayList<>();

    /**
     * Checks if the user trying to log in is valid
     *
     * @param username the user name of the user trying to log in
     * @param password the password of the user trying to log in
     * @return true if the user exists
     */
    synchronized boolean isValid(String username, String password) {
        return users.stream().anyMatch(u -> u.validate(username, password));
    }

    /**
     * Used to register a new user
     *
     * @param username the user name of the new user
     * @param password the password of the new user
     * @throws Exception an exception describing what went wrong
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    synchronized void registerUser(String username, String password) throws Exception {
        if (users.stream().anyMatch(u -> u.username.equalsIgnoreCase(username))) { //check if the user already exists
            throw new Exception("User already exists");
        }
        KeyPair keys = Encryption.GenerateKeys(); //generate keys for the user
        if (keys == null)
            throw new Exception("Error while generating keys");
        final User user = new User(username, password, keys.getPublic(), keys.getPrivate()); //and create a new user object
        File directory = new File(username.toLowerCase()); //create the directory where the user's data will be saved
        if (!directory.exists())
            directory.mkdir();

        users.add(user); //add the user to our list of users

        if (!saveData()) { //and try to save all data
            users.remove(user); //if the saving failed then remove the new user
            throw new Exception("Error while saving user");
        }
    }

    /**
     * Loads the private/public keys for the server, as well as all the users that are saved
     */
    synchronized void loadData() throws Exception {
        //load the keys for the server
        File publicKeyFile = new File(WORK_PATH, "public" + KEY_FILE_SUFFIX);
        File privateKeyFile = new File(WORK_PATH, "private" + KEY_FILE_SUFFIX);

        if (!publicKeyFile.exists() || !privateKeyFile.exists()) { //if they do not exist
            KeyPair serverKeys = Encryption.GenerateKeys(); //generate new ones
            if (serverKeys == null)
                throw new Exception("Error while creating server keys");
            serverPrivateKey = serverKeys.getPrivate();
            serverPublicKey = serverKeys.getPublic();
            Encryption.SaveKey(serverPrivateKey, privateKeyFile); //and save them to the respective files
            Encryption.SaveKey(serverPublicKey, publicKeyFile);
        } else {
            serverPrivateKey = Encryption.LoadPrivateKey(privateKeyFile); //otherwise load the keys from the file
            serverPublicKey = Encryption.LoadPublicKey(publicKeyFile);
        }

        File file = new File(USER_FILE); //check to see if the file containing all the users exists
        if (!file.exists()) { //if it does not exist
            if (!file.createNewFile()) { //try to create it
                throw new IOException("Could not create storage file");
            }
        } else if (file.length() > 0) { //if it exists and iut has data
            String[] userData = new String(Encryption.LoadDecryptedWithRSA(file, serverPublicKey)).split("\\r?\\n"); //load the file data using the server public key
            for (String user : userData) { //parse each line
                try {
                    String username = user.substring(0, user.indexOf(SEPARATOR)); //and retrieve the user name
                    String password = user.substring(user.indexOf(SEPARATOR) + 1); //and the password
                    PublicKey publicKey = Encryption.LoadPublicKey(new File(WORK_PATH, username.toLowerCase() + "\\public.key")); //then load the public and private keys from the user's folder
                    PrivateKey privateKey = Encryption.LoadPrivateKey(new File(WORK_PATH, username.toLowerCase() + "\\private.key"));
                    users.add(new User(username, password, publicKey, privateKey)); //if all went well we add the user to our valid list of users
                } catch (Exception ignored) { //if there was an error that means that there was something wrong with the loading, we skip to the next user
                }
            }
        }
    }

    /**
     * Saves the list of all users that are currently stored in memory
     *
     * @return true if the save was successful
     */
    private boolean saveData() {
        try {
            StringBuilder userData = new StringBuilder();
            for (User u : users) {
                userData.append(u.username).append(SEPARATOR).append(u.password).append("\r\n");
                File userFolder = new File(WORK_PATH, u.username.toLowerCase());
                Encryption.SaveKey(u.publicKey, new File(userFolder, "public.key"));
                Encryption.SaveKey(u.privateKey, new File(userFolder, "private.key"));
            }
            Encryption.SaveEncryptedWithRSA(userData.toString().getBytes(StandardCharsets.UTF_8), new File(USER_FILE), serverPrivateKey);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * saves a file from a specific stream into a file encrypted
     *
     * @param userName the user whose repository will be used
     * @param fileName the name of the file to save
     * @param length   the length of the file that should be read
     * @param in       the stream that will be read for the data
     * @return null if there is no problem otherwise the error message
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    String saveFile(String userName, String fileName, long length, DataInputStream in) {
        try {
            byte[] fileBytes = StreamUtils.readData(in, length); //load all file data
            User user = users.stream().filter(u -> u.username.equalsIgnoreCase(userName)).findFirst().orElseThrow();//load user information
            File file = new File(WORK_PATH + user.username.toLowerCase() + "\\" + fileName + ".enc");
            File fileSHA = new File(WORK_PATH + user.username.toLowerCase() + "\\" + fileName + ".sha");
            if (file.exists())
                file.delete();
            if (fileSHA.exists())
                fileSHA.delete();

            String hash = Encryption.GenerateHash(fileBytes); //generate SHA for the file
            Encryption.SaveEncryptedWithRSA(hash.getBytes(StandardCharsets.UTF_8), fileSHA, user.privateKey); //save the file after encoding it
            SecretKey aes = Encryption.GenerateAESKey(); //generate an AES key
            byte[] iv = Encryption.GenerateIV();//generate an iv
            Encryption.SaveEncryptedWithAES(fileBytes, file, aes, user.privateKey, iv); //encode the file with a combination of AES and RSA and save it
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }

    }

    byte[] loadFile(String userName, String fileName) throws Exception {
        User user = users.stream().filter(u -> u.username.equalsIgnoreCase(userName)).findFirst().orElseThrow();//load user information
        File file = new File(WORK_PATH + user.username.toLowerCase() + "\\" + fileName + ".enc");
        File fileSHA = new File(WORK_PATH + user.username.toLowerCase() + "\\" + fileName + ".sha");
        if (!file.exists() || !fileSHA.exists())
            throw new Exception("Could not locate file " + fileName);
        byte[] fileData = Encryption.LoadDecryptedWithAES(file, user.publicKey);//decrypt the file and load it
        String hash = Encryption.GenerateHash(fileData);
        String originalHash = new String(Encryption.LoadDecryptedWithRSA(fileSHA, user.publicKey)); //load the originally saved SHA
        if (!hash.equals(originalHash)) {
            throw new Exception("File has been corrupted");
        }
        return fileData;
    }


}
