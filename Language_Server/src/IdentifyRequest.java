import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class IdentifyRequest {

    private final Socket socket;
    private final ConcurrentHashMap<String,String> wordsMap;

    private BufferedReader reader;
    private PrintWriter writer;

    private String identifier;

    public IdentifyRequest (Socket socket, ConcurrentHashMap<String,String> wordsMap) {
        this.socket = socket;
        this.wordsMap = wordsMap;

        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);

            socket.setSoTimeout(200);

            this.identifier = receiveIdentifier();
            answerRequest();

        } catch (IOException e) {
            System.out.println("\tError obtaining streams from socket\n");
        }
    }

    private String receiveIdentifier () throws IOException {

        try {
            return reader.readLine();

        } catch (IOException e) {
            System.out.println("\tError receiving identifier\n");
            socket.close();
            return null;
        }
    }

    private void answerRequest () {

        boolean receivedKnowIdentifier = false;

        switch (identifier) {
            case "PING":
                receivedKnowIdentifier = true;
                answerPing();
                break;

            case "TRANSLATE":
                System.out.println("\tProxy connected with translation request...");
                System.out.println("\tSuccessfully received identifier: " + identifier);
                receivedKnowIdentifier = true;
                answerTranslate();
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

    private void answerPing() {

        writer.println("PONG");
    }

    private void answerTranslate() {
        try {
            String word = reader.readLine();
            String address = reader.readLine();
            String port = reader.readLine();
            System.out.println("\tTranslation request: " + word + " | from: " + address + ":" + port );

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(address,Integer.parseInt(port)),500);
                System.out.println("\tConnected to client");

                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);

                if (wordsMap.containsKey(word)) {
                    writer.println(wordsMap.get(word));
                    System.out.println("\tSent translation\n");
                } else {
                    writer.println("NOWORD");
                    System.out.println("\tNo translation in dictionary\n");
                }

            } catch (IOException e) {
                System.out.println("\t" + "Error connection to client");
            }

        } catch (IOException e) {
            System.out.println("\tError reading/writing from client\n");
        }
    }
}
