import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class IdentifyRequest {

    private final Socket socket;
    private final String identifier;
    private final ConcurrentHashMap<String,String> wordsMap;

    public IdentifyRequest (Socket socket, ConcurrentHashMap<String,String> wordsMap) {
        this.socket = socket;
        this.wordsMap = wordsMap;

        this.identifier = receiveIdentifier();
        answerRequest();

        System.out.println();
    }

    private String receiveIdentifier () {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            System.out.println("\tReceiving identifier...");
            return reader.readLine();

        } catch (IOException e) {
            System.out.println("\u001B[31m\t" + "Error receiving identifier" + "\u001B[0m\n");
            return null;
        }
    }

    private void answerRequest () {

        boolean receivedKnowIdentifier = false;

        switch (identifier) {
            case "PING":
                System.out.println("\tSuccessfully received identifier: " + identifier);
                receivedKnowIdentifier = true;
                answerPing();
                break;

            case "TRANSLATE":
                System.out.println("\tSuccessfully received identifier: " + identifier);
                receivedKnowIdentifier = true;
                answerTranslate();
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

    private void answerPing() {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true)) {

            writer.println("PONG");
            System.out.println("\tAnswered ping");

        } catch (IOException e) {
            System.out.println("\u001B[31m\t" + "Error answering PING request" + "\u001B[0m\n");
        }
    }

    private void answerTranslate() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true)) {

                String word = reader.readLine();
                String address = reader.readLine();
                String port = reader.readLine();
                System.out.println("\tTranslation request: " + word + " | from: " + address + ":" + port );

                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(address,Integer.parseInt(port)),500);

                    if (wordsMap.containsKey(word)) {
                        writer.println(wordsMap.get(word));
                        System.out.println("\tSent translation");
                    } else {
                        writer.println("NULL");
                        System.out.println("\tNo translation in dictionary");
                    }

                } catch (IOException e) {
                    System.out.println("\u001B[31m\t" + "Error connection to client" + "\u001B[0m");
                }
            }

        } catch (IOException e) {
            System.out.println("\u001B[31m\t" + "Error reading/writing from client" + "\u001B[0m\n");
        }
    }
}
