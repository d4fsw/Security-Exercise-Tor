import java.net.ServerSocket;
import java.net.Socket;


public class Server {
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        DataManager db = new DataManager();
        try {
            db.loadData();
            serverSocket = new ServerSocket(1313);
            System.out.println("Server started.");
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection : " + clientSocket);
                Thread t = new Thread(new ClientConnection(clientSocket, db));
                t.start();
            } catch (Exception e) {
                System.err.println("Error in connection attempt.");
            }
        }
    }
}