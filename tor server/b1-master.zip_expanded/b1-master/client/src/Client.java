import java.io.*;
import java.net.Socket;

public class Client {
    private static final String WORK_FOLDER = "C:\\temp\\client\\";

    public static void main(String[] args) throws IOException {
        //create a connection to the server
        final Socket sock = connectToServer();
        //if the connection could not be created then exit;
        if (sock == null)
            System.exit(1);
        //create the reader that will read user data
        final BufferedReader userInput = createUserInput();
        //create the writer that will write data to the user
        final PrintStream userOutput = createUserOutput();
        //create the stream that will send data to the server
        final PrintStream serverOutput = createServerOutput(sock);
        //create the stream that the server will answer to
        final DataInputStream serverInput = createServerInput(sock);
        if (userInput == null || serverOutput == null || serverInput == null)
            System.exit(1);
        final ClientState state = new ClientState();
        final Thread reader = new Thread(() -> readData(serverInput, userOutput, state));
        try {
            //start the reader
            reader.start();
            while (state.isWorking()) {
                try {
                    printMenu(userOutput, state);
                    String input = userInput.readLine();
                    state.current = parseUserInput(input, state, userOutput, serverOutput);
                } catch (Exception e) {
                    System.err.println("Error while performing action :" + e.getMessage());
                }
            }
        } finally {
            if (!sock.isClosed())
                sock.close();
        }
    }

    //region set up communications
    private static void readData(DataInputStream serverInput, PrintStream userOutput, ClientState state) {
        while (state.isWorking()) {
            try {
                String data = serverInput.readLine();
                parseResponse(userOutput, serverInput, data, state);
            } catch (Exception e) {
                userOutput.println("SERVER ERROR: " + e.getMessage());
                state.stopWorking();
            }
        }
    }

    private static Socket connectToServer() {
        try {
            return new Socket("localhost", 1313);
        } catch (Exception e) {
            System.err.println("Error while connecting to server: " + e.getMessage());
            return null;
        }
    }

    private static DataInputStream createServerInput(Socket sock) {
        try {
            return new DataInputStream(sock.getInputStream());
        } catch (Exception e) {
            System.err.println("Error while creating server input stream");
            return null;
        }
    }

    private static PrintStream createServerOutput(Socket sock) {
        try {
            return new PrintStream(sock.getOutputStream());
        } catch (Exception e) {
            System.err.println("Error while creating server output stream");
            return null;
        }
    }

    private static BufferedReader createUserInput() {
        try {
            return new BufferedReader(new InputStreamReader(System.in));
        } catch (Exception e) {
            System.err.println("Error while creating client input stream.");
            return null;
        }
    }

    private static PrintStream createUserOutput() {

        try {
            return System.out;
        } catch (Exception e) {
            System.err.println("Error while creating client input stream.");
            return null;
        }
    }
    //endregion

    private static void printMenu(PrintStream os, ClientState state) {
        switch (state.current) {
            case Idle:
                if (state.isLoggedIn()) {
                    os.println("1. Send File");
                    os.println("2. Retrieve File");
                    os.println("3. Exit");
                } else {
                    os.println("1. Log in");
                    os.println("2. Register user");
                    os.println("3. Exit");
                }
                break;
            case Waiting:
                os.println("Waiting for response...");
                break;
            case Registering:
                if (state.userName == null) {
                    os.println("Enter new user name:");
                } else if (state.password == null) {
                    os.println("Enter new password:");
                } else {
                    os.println("Enter password again:");
                }
                break;
            case LoggingIn:
                if (state.userName == null) {
                    os.println("Enter user name:");
                } else {
                    os.println("Enter password:");
                }
                break;
            case Sending:
                if (state.file == null) {
                    os.println("Enter file to send:");
                }
                break;
            case Receiving:
                if (state.file == null) {
                    os.println("Enter file to retrieve:");
                }
                break;
        }
    }

    private static State parseUserInput(String input, ClientState state, PrintStream userOutput, PrintStream serverOutput) {
        if (state.current == State.Waiting) {
            if (state.hasTimedOut()) {
                return State.Idle;
            }
            return State.Waiting;
        } else if (state.current == State.Idle && state.isLoggedIn()) {
            switch (input.toLowerCase()) {
                case "1":
                    return State.Sending;
                case "2":
                    return State.Receiving;
                case "3":
                    userOutput.println("Good bye");
                    state.stopWorking();
                    return State.Waiting;
            }
        } else if (state.current == State.Idle && !state.isLoggedIn()) {
            switch (input.toLowerCase()) {
                case "1":
                    state.clearCredentials();
                    return State.LoggingIn;
                case "2":
                    state.clearCredentials();
                    return State.Registering;
                case "3":
                    userOutput.println("Good bye");
                    state.stopWorking();
                    return State.Waiting;
            }
        } else if (state.current == State.LoggingIn) {
            if (state.userName == null) {
                if (StringUtils.isNullOrBlank(input)) {
                    return State.Idle;
                } else {
                    state.setUserName(input);
                }
            } else {
                if (StringUtils.isNullOrBlank(input)) {
                    state.setUserName(null);
                } else {
                    state.setPassword(input);
                    sendLogIn(serverOutput, state);
                    return State.Waiting;
                }
            }
            return State.LoggingIn;
        } else if (state.current == State.Registering) {
            if (state.userName == null) {
                if (StringUtils.isNullOrBlank(input)) {
                    return State.Idle;
                } else {
                    state.setUserName(input);
                }
            } else if (state.password == null) {
                if (StringUtils.isNullOrBlank(input)) {
                    state.setUserName(null);
                } else {
                    state.setPassword(input);
                }
            } else {
                if (StringUtils.isNullOrBlank(input)) {
                    state.setUserName(null);
                } else if (!state.password.equals(input)) {
                    userOutput.println("The two passwords do not match, please try again");
                    state.setPassword(null);
                } else {
                    sendRegister(serverOutput, state);
                    return State.Waiting;
                }
            }
            return State.Registering;
        } else if (state.current == State.Sending) {
            File toSend = new File(WORK_FOLDER, input);
            if (!toSend.exists()) {
                userOutput.println("File " + input + " does not exist");
                return State.Idle;
            }
            byte[] fileData;
            try (FileInputStream fis = new FileInputStream(toSend)) {
                fileData = fis.readAllBytes();
            } catch (Exception e) {
                userOutput.println("Error while reading file " + input + " : " + e.getMessage());
                return State.Idle;
            }
            state.file = input;
            sendFile(serverOutput, fileData, state);
            return State.Waiting;
        } else if (state.current == State.Receiving) {
            if (StringUtils.isNullOrBlank(input)) {
                return State.Idle;
            }
            state.file = input;
            retrieveFile(serverOutput, state);
        }
        return state.current;
    }

    private static void parseResponse(PrintStream userOutput, DataInputStream serverInput, String data, ClientState state) {
        Command cmd = Command.fromMessage(data);
        if (cmd == null) {
            userOutput.println("Invalid response received:" + data);
        } else {
            switch (cmd.getType()) {
                case Command.REGISTER:
                    if (cmd.getParameter(Command.ERROR) != null) {
                        userOutput.println("ERROR:" + cmd.getParameter(Command.ERROR));
                        userOutput.println("Press enter key to continue");
                        state.clearCredentials();
                        state.current = State.Idle;
                    } else if (cmd.getParameter(Command.SUCCESS) != null) {
                        userOutput.println("User successfully registered");
                        userOutput.println("Press enter key to continue");
                        state.clearCredentials();
                        state.current = State.Idle;
                    }
                    break;
                case Command.LOGIN:
                    if (cmd.getParameter(Command.ERROR) != null) {
                        userOutput.println("ERROR:" + cmd.getParameter(Command.ERROR));
                        userOutput.println("Press enter key to continue");
                        state.clearCredentials();
                        state.current = State.Idle;
                    } else if (cmd.getParameter(Command.SUCCESS) != null) {
                        userOutput.println("User successfully logged in");
                        userOutput.println("Press enter key to continue");
                        state.userLoggedIn();
                        state.current = State.Idle;
                    }
                    break;
                case Command.SEND:
                    if (cmd.getParameter(Command.ERROR) != null) {
                        userOutput.println("ERROR:" + cmd.getParameter(Command.ERROR));
                        userOutput.println("Press enter key to continue");
                        state.current = State.Idle;
                    } else if (cmd.getParameter(Command.SUCCESS) != null) {
                        userOutput.println("File " + state.file + " sent successfully");
                        userOutput.println("Press enter key to continue");
                        state.current = State.Idle;
                        state.file = null;
                    }
                    break;
                case Command.RECEIVE:
                    if (cmd.getParameter(Command.ERROR) != null) {
                        userOutput.println("ERROR:" + cmd.getParameter(Command.ERROR));
                        userOutput.println("Press enter key to continue");
                        state.current = State.Idle;
                    } else if (cmd.getParameter(Command.SUCCESS) != null) {
                        try {
                            long size = Long.parseLong(cmd.getParameter(Command.SUCCESS));
                            byte[] fileData = StreamUtils.readData(serverInput, size);
                            try (FileOutputStream fos = new FileOutputStream(new File(WORK_FOLDER, state.file))) {
                                fos.write(fileData);
                            }
                            userOutput.println("File " + state.file + " received successfully");
                            userOutput.println("Press enter key to continue");
                            state.current = State.Idle;
                            state.file = null;
                        } catch (Exception e) {
                            userOutput.println("Error while saving file " + state.file);
                            userOutput.println("Press enter key to continue");
                            state.current = State.Idle;
                            state.file = null;
                        }
                    }
                    break;
            }
        }
    }

    //region messages to the server
    private static void sendLogIn(PrintStream os, ClientState state) {
        state.commandStarted();
        Command cmd =
                new Command(Command.LOGIN)
                        .addParameter(Command.USERNAME, state.userName)
                        .addParameter(Command.PASSWORD, state.password);
        os.println(cmd.toMessage());
    }

    private static void sendRegister(PrintStream os, ClientState state) {
        state.commandStarted();
        Command cmd =
                new Command(Command.REGISTER)
                        .addParameter(Command.USERNAME, state.userName)
                        .addParameter(Command.PASSWORD, state.password);
        os.println(cmd.toMessage());
    }

    private static void sendFile(PrintStream os, byte[] fileData, ClientState state) {
        state.commandStarted();
        Command cmd = new Command(Command.SEND)
                .addParameter(Command.USERNAME, state.userName)
                .addParameter(Command.PASSWORD, state.password)
                .addParameter(Command.LENGTH, String.valueOf(fileData.length))
                .addParameter(Command.FILE, state.file);
        os.println(cmd.toMessage());
        try {
            os.write(fileData);
        } catch (Exception ignored) {

        }

    }

    private static void retrieveFile(PrintStream os, ClientState state) {
        state.commandStarted();
        Command cmd = new Command(Command.RECEIVE)
                .addParameter(Command.USERNAME, state.userName)
                .addParameter(Command.PASSWORD, state.password)
                .addParameter(Command.FILE, state.file);
        os.println(cmd.toMessage());
    }

    //endregion
}