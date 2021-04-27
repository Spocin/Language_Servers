import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;

public class Reply {

    private final Socket socket;
    private final HashMap<String,String> wordsMap;

    private String word;
    private String address;
    private String port;

    public Reply (Socket socket, HashMap<String,String> wordsMap) {
        this.socket = socket;
        this.wordsMap = wordsMap;

        receiveInformation();
        sendToClient();
        System.out.println();
    }

    private void receiveInformation() {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            word = reader.readLine();
            address = reader.readLine();
            port = reader.readLine();

            System.out.println("Received request: " + word + " | " + address + " | " + port);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendToClient() {

        try (Socket socket = new Socket()) {

            socket.connect(new InetSocketAddress(address,Integer.parseInt(port)));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));


            if (wordsMap.containsKey(word)) {
                String translation = wordsMap.get(word);
                writer.write(translation);
                System.out.println("Sent translation: " + translation + " | " + address + " | " + port);
            } else {
                writer.write("NULL");
                System.out.println("No translation for: " + word);
            }
            writer.newLine();
            writer.flush();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
