import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;

import net.sf.T0rlib4j.controller.network.JavaTorRelay;
import net.sf.T0rlib4j.controller.network.TorServerSocket;


public class ServerTor {
    private static ServerSocket serverSocket;
    private static final int hiddenservicedirport = 80;
    private static final int localport = 1313;

    public static void main(String[] args) {
        DataManager db = new DataManager();
        try {
            db.loadData();
            File dir = new File("torfiles");

            JavaTorRelay node = new JavaTorRelay(dir);
            TorServerSocket torServerSocket = node.createHiddenService(localport, hiddenservicedirport);

            System.out.println("Hidden Service Binds to   " + torServerSocket.getHostname() + " ");
           // System.out.println("Tor Service Listen to RemotePort  " + torServerSocket.getServicePort());
           // System.out.println("Tor Service Listen to LocalPort  " + node.getLocalPort());
            
            serverSocket = torServerSocket.getServerSocket();
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