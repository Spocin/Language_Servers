import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServersCommunication {

    private final Socket socket;
    private final ConcurrentHashMap<String,Integer> languageServersMap;

    private BufferedReader reader;
    private PrintWriter writer;

    private String identifier;

    public ServersCommunication(Socket socket, ConcurrentHashMap<String,Integer> languageServersMap) {

        this.socket = socket;
        this.languageServersMap = languageServersMap;
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);

            this.identifier = receiveIdentifier();

            answerRequest();
        } catch (IOException e) {
            System.out.println("\tError obtaining streams from socket\n");
        }
    }

    private String receiveIdentifier () {
        try  {
            System.out.println("\tReceiving identifier...");
            return reader.readLine();

        } catch (IOException e) {
            System.out.println("\tError receiving communication\n");
            return "NULL";
        }
    }

    private void answerRequest () {

        boolean receivedKnowIdentifier = false;

        switch (identifier) {
            case "LOGIN":
                System.out.println("\tSuccessfully received identifier: " + identifier);
                receivedKnowIdentifier = true;
                addToLanguageServerMap();
                break;

            case "LOGOUT":
                System.out.println("\tSuccessfully received identifier: " + identifier);
                receivedKnowIdentifier = true;
                removeFromLanguageServerMap();
                break;
        }

        if (!receivedKnowIdentifier) {
            System.out.println("\tReceived unknown identifier");
            System.out.println("\t" + "Closing connection");
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("\tError closing connection");
            }
            System.out.println();
        }
    }

    private void addToLanguageServerMap () {

        try {
            String serverCode = reader.readLine();
            String serverPort = reader.readLine();

            System.out.println("\tLogging in server " + serverCode + " on port: " + serverPort + "...");

            if (languageServersMap.containsKey(serverCode)) {
                writer.println("DENY");
                System.out.println("\tError, server already logged in\n");
            } else {
                writer.println("ACCEPT");
                languageServersMap.put(serverCode,Integer.parseInt(serverPort));
                System.out.println("\tSuccessfully logged in new server\n");
            }

        } catch (IOException e) {
            System.out.println("\tError communicating with server\n");
            e.printStackTrace();
        }
    }

    private void removeFromLanguageServerMap () {

        try {
            String serverCode = reader.readLine();

            if (languageServersMap.containsKey(serverCode)) {
                languageServersMap.remove(serverCode);
                System.out.println("\t" + serverCode + " successfully logged out\n");
                writer.println("SUCCESSFUL");

            } else {

                System.out.println("\tNo such server: " + serverCode + "\n");
                writer.println("NoServer");
            }

        } catch (IOException e) {
            System.out.println("\tError communicating with server\n");
        }
    }
}
