import java.net.*;
import java.io.*;


public class ClientConnection implements Runnable {
    private final DataManager dataManager;
    private Socket clientSocket;


    ClientConnection(Socket client, DataManager dataManager) {
        this.clientSocket = client;
        this.dataManager = dataManager;
    }

    @Override
    public void run() {
        try {
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            PrintStream out = new PrintStream(clientSocket.getOutputStream());
            String message;
            while ((message = in.readLine()) != null) {
                Command cmd = Command.fromMessage(message);
                if (cmd == null)
                    continue;
                switch (cmd.getType()) {
                    case Command.LOGIN:
                        doLogin(cmd.getParameter(Command.USERNAME), cmd.getParameter(Command.PASSWORD), out);
                        break;
                    case Command.REGISTER:
                        doRegister(cmd.getParameter(Command.USERNAME), cmd.getParameter(Command.PASSWORD), out);
                        break;
                    case Command.SEND:
                        doStartReceivingFile(cmd.getParameter(Command.USERNAME)
                                , cmd.getParameter(Command.PASSWORD)
                                , cmd.getParameter(Command.FILE)
                                , Long.parseLong(cmd.getParameter(Command.LENGTH))
                                , out, in);
                        break;
                    case Command.RECEIVE:
                        doStartSendingFile(cmd.getParameter(Command.USERNAME)
                                , cmd.getParameter(Command.PASSWORD)
                                , cmd.getParameter(Command.FILE)
                                , out);
                        break;
                    default:
                        System.out.println("Incorrect command received.");
                        break;
                }
            }
        } catch (IOException ex) {
            System.out.println("Error:" + ex.getMessage());
        }
    }

    private void doRegister(String username, String password, PrintStream out) {
        if (StringUtils.isNullOrBlank(username) || StringUtils.isNullOrBlank(password)) {
            Command cmd = new Command(Command.REGISTER).addParameter(Command.ERROR, "Invalid user name or password");
            out.println(cmd.toMessage());
            return;
        }
        try {
            dataManager.registerUser(username, password);
            Command cmd = new Command(Command.REGISTER).addParameter(Command.SUCCESS, "");
            out.println(cmd.toMessage());
        } catch (Exception e) {
            Command cmd = new Command(Command.REGISTER).addParameter(Command.ERROR, e.getMessage());
            out.println(cmd.toMessage());
        }
    }

    private void doLogin(String username, String password, PrintStream out) {
        if (StringUtils.isNullOrBlank(username) || StringUtils.isNullOrBlank(password)) {
            Command cmd = new Command(Command.LOGIN).addParameter(Command.ERROR, "Invalid user name or password");
            out.println(cmd.toMessage());
            return;
        }
        if (dataManager.isValid(username, password)) {
            Command cmd = new Command(Command.LOGIN).addParameter(Command.SUCCESS, "");
            out.println(cmd.toMessage());
        } else {
            Command cmd = new Command(Command.LOGIN).addParameter(Command.ERROR, "Invalid user name or password");
            out.println(cmd.toMessage());
        }
    }

    private void doStartReceivingFile(String username, String password, String fileName, long length, PrintStream out, DataInputStream in) {
        if (!dataManager.isValid(username, password)) {
            Command cmd = new Command(Command.SEND).addParameter(Command.ERROR, "Invalid user name or password");
            out.println(cmd.toMessage());
            return;
        }
        String error = dataManager.saveFile(username, fileName, length, in);
        if (error == null) {
            Command cmd = new Command(Command.SEND).addParameter(Command.SUCCESS, "");
            out.println(cmd.toMessage());
        } else {
            Command cmd = new Command(Command.SEND).addParameter(Command.ERROR, error);
            out.println(cmd.toMessage());
        }
    }

    private void doStartSendingFile(String username, String password, String filename, PrintStream out) {
        if (!dataManager.isValid(username, password)) {
            Command cmd = new Command(Command.RECEIVE).addParameter(Command.ERROR, "Invalid user name or password");
            out.println(cmd.toMessage());
            return;
        }
        try {
            byte[] data = dataManager.loadFile(username, filename);
            Command cmd = new Command(Command.RECEIVE).addParameter(Command.SUCCESS, String.valueOf(data.length));
            out.println(cmd.toMessage());
            out.write(data);
        } catch (Exception e) {
            Command cmd = new Command(Command.RECEIVE).addParameter(Command.ERROR, e.getMessage());
            out.println(cmd.toMessage());
        }
    }

    public void receiveFile() {
        try {
            int bytesRead;

            DataInputStream clientData = new DataInputStream(clientSocket.getInputStream());

            String fileName = clientData.readUTF();
            OutputStream output = new FileOutputStream(("received_from_client_" + fileName));
            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }

            output.close();
            clientData.close();

            System.out.println("File " + fileName + " received from client.");
        } catch (IOException ex) {
            System.err.println("Client error. Connection closed.");
        }
    }


    public void sendFile(String fileName) {
        try {
            //handle file read
            File myFile = new File(fileName);
            byte[] mybytearray = new byte[(int) myFile.length()];

            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            //bis.read(mybytearray, 0, mybytearray.length);

            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(mybytearray, 0, mybytearray.length);

            //handle file send over socket
            OutputStream os = clientSocket.getOutputStream();

            //Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(myFile.getName());
            dos.writeLong(mybytearray.length);
            dos.write(mybytearray, 0, mybytearray.length);
            dos.flush();
            System.out.println("File " + fileName + " sent to client.");
        } catch (Exception e) {
            System.err.println("File does not exist!");
        }
    }


}
    
     



