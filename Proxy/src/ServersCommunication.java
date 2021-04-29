import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServersCommunication {

    private final Socket socket;
    private final ConcurrentHashMap<String,Integer> languageServersMap;

    private final String identifier;

    public ServersCommunication(Socket socket, ConcurrentHashMap<String,Integer> languageServersMap) {

        this.socket = socket;
        this.languageServersMap = languageServersMap;

        this.identifier = receiveIdentifier();

        answerRequest();
    }

    private String receiveIdentifier () {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            System.out.println("\tReceiving identifier...");
            return reader.readLine();

        } catch (IOException e) {
            System.out.println("\u001B[31m\t" + "Error receiving communication" + "\u001B[0m\n");
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
            System.out.println("\u001B[31m\t" + "Received unknown identifier" + "\u001B[0m");
            System.out.println("\u001B[31m\t" + "Closing connection" + "\u001B[0m");
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("\u001B[31m\t" + "Error closing connection" + "\u001B[0m");
            }
            System.out.println();
        }
    }

    private void addToLanguageServerMap () {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true)) {

                String serverCode = reader.readLine();
                String serverPort = reader.readLine();

                System.out.println("\tLogging in server: " + serverCode + " on port: " + serverPort);

                if (languageServersMap.containsKey(serverCode)) {
                    writer.println("DENY");
                    System.out.println("\u001B[31m\t" + "Error, server already logged in" + "\u001B[0m\n");
                } else {
                    writer.write("ACCEPT");
                    languageServersMap.put(serverCode,Integer.parseInt(serverPort));
                    System.out.println("\tSuccessfully logged in new server");
                }
            }

        } catch (IOException e) {
            System.out.println("\u001B[31m\t" + "Error communicating with server" + "\u001B[0m\n");
        }
    }

    private void removeFromLanguageServerMap () {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true)) {

                String serverCode = reader.readLine();

                if (languageServersMap.containsKey(serverCode)) {
                    languageServersMap.remove(serverCode);
                    System.out.println("\t" + serverCode + " successfully logged out");
                    writer.println("SUCCESSFUL");

                } else {

                    System.out.println("\u001B[31m\t" + "No such server: " + serverCode + "\u001B[0m\n");
                    writer.println("NoServer");
                }
            }

        } catch (IOException e) {
            System.out.println("\u001B[31m\t" + "Error communicating with server" + "\u001B[0m\n");
        }
    }
}
