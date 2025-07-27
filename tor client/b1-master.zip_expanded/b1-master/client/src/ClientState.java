import java.io.BufferedReader;

class ClientState {
    private static final long TIMEOUT = 5000L;

    State current = State.Idle;
    private boolean working = true;
    private boolean loggedIn = false;


    private long commandStart = 0;
    String userName = null;
    String password = null;
    String file = null;

    boolean isLoggedIn() {
        return loggedIn;
    }

    boolean hasTimedOut() {
        return System.currentTimeMillis() - commandStart > TIMEOUT;
    }



    boolean isWorking() {
        return working;
    }



    void setUserName(String userName) {
        this.userName = userName;
    }

    void setPassword(String password) {
        this.password = password;
    }

    void commandStarted() {
        this.commandStart = System.currentTimeMillis();
    }

    void clearCredentials() {
        userName = null;
        password = null;
        loggedIn = false;
    }

    void userLoggedIn() {
        loggedIn = true;
    }

    void stopWorking() {
        working = false;
    }


}
