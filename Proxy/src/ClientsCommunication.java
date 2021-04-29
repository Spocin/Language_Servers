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

    public ClientsCommunication(Socket socket, ConcurrentHashMap<String,Integer> serversMap) {

        this.socket = socket;
        this.serversMap = serversMap;

        receiveData();
        boolean forwardRequest = checkIfTargetServerIsOnline();

        if (forwardRequest) {
            forwardRequestToServer();
        }
    }

    private void receiveData () {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            this.word = reader.readLine();
            this.serverCode = reader.readLine();
            this.port = reader.readLine();

            System.out.println("\tReceived data: " + word + " | " + serverCode + " | " + port);

        } catch (IOException e) {
            System.out.println("\u001B[31m\t" + "Error receiving data" + "\u001B[0m\n");
        }
    }

    private boolean checkIfTargetServerIsOnline() {

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true)) {

            if (serversMap.containsKey(serverCode)) {
                writer.println("OK");
                System.out.println("\tTarget server is online");
                return true;
            }

            writer.println("NOSERVER");
            System.out.println("\tTarget server is offline");
            return false;

        } catch (IOException e) {
            System.out.println("\u001B[31m\t" + "Error verifying if target server is online" + "\u001B[0m\n");
        }

        return false;
    }

    private void forwardRequestToServer() {

        try (Socket forwardSocket = new Socket()) {

            try {
                forwardSocket.connect(new InetSocketAddress("127.0.0.1", serversMap.get(serverCode)), 500);
                System.out.println("\tConnected to server");

            } catch (IOException e) {
                System.out.println("\u001B[31m\t" + "Error connecting to server" + "\u001B[0m\n");
            }

            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(forwardSocket.getOutputStream()),true)) {

                writer.println("TRANSLATE");
                writer.println(word);
                writer.println(socket.getLocalAddress().getHostAddress());
                writer.println(port);

                System.out.println("\tForwarded request");
            } catch (IOException e) {
                System.out.println("\u001B[31m\t" + "Error sending data" + "\u001B[0m");
            }

        } catch (IOException e) {
            System.out.println("\u001B[31m\t" + "Error forwarding request" + "\u001B[0m\n");
        }

    }
}
