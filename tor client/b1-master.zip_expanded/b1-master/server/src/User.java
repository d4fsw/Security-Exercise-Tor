import java.security.PrivateKey;
import java.security.PublicKey;

class User {


     final String username;
     final String password;
     final PublicKey publicKey;
     final PrivateKey privateKey;

    User(String username, String password, PublicKey publicKey, PrivateKey privateKey) {
        this.username = username;
        this.password = password;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }


    boolean validate(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }

}
