import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ClientsCommunication {

    private final Socket socket;
    private final ConcurrentHashMap<String,Integer> serversMap;

    private String word;
    private String serverCode;
    private String port;

    private BufferedReader reader;
    private PrintWriter writer;

    public ClientsCommunication(Socket socket, ConcurrentHashMap<String,Integer> serversMap) {

        this.socket = socket;
        this.serversMap = serversMap;

        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            receiveData();
            boolean forwardRequest = checkIfTargetServerIsOnline();

            if (forwardRequest) {
                System.out.println("\tForwarding request...");
                forwardRequestToServer();
            }

        } catch (IOException e) {
            System.out.println("\tError obtaining streams from socket\n");
        }
    }

    private void receiveData () {

        try {
            this.word = reader.readLine();
            this.serverCode = reader.readLine();
            this.port = reader.readLine();

            System.out.println("\tReceived data: " + word + " | " + serverCode + " | " + port);

        } catch (IOException e) {
            System.out.println("\tError receiving data\n");
        }
    }

    private boolean checkIfTargetServerIsOnline() {

        if (serversMap.containsKey(serverCode)) {
            writer.println("OK");
            System.out.println("\tTarget server is online\n");
            return true;
        }

        writer.println("NOSERVER");
        System.out.println("\tTarget server is offline\n");
        return false;
    }

    private void forwardRequestToServer() {

        try (Socket forwardSocket = new Socket()) {

            try {
                forwardSocket.connect(new InetSocketAddress("127.0.0.1", serversMap.get(serverCode)), 500);
                System.out.println("\tConnected to server");

            } catch (IOException e) {
                System.out.println("\tError connecting to server\n");
            }

            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(forwardSocket.getOutputStream()),true)) {

                writer.println("TRANSLATE");
                writer.println(word);
                writer.println(socket.getLocalAddress().getHostAddress());
                writer.println(port);

                System.out.println("\tSuccessfully forwarded request\n");

            } catch (IOException e) {
                System.out.println("\tError sending data\n");
            }

        } catch (IOException e) {
            System.out.println("\tError forwarding request\n");
        }

    }
}
