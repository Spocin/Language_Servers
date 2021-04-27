import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Communication {

    private final Socket socket;
    private final ConcurrentHashMap<String,Integer> languageServersMap;

    private String word;
    private String languageCode;
    private String address;
    private String port;

    public Communication (Socket socket, ConcurrentHashMap<String,Integer> languageServersMap) {

        this.socket = socket;
        this.languageServersMap = languageServersMap;

        boolean pass = checkRequestIntegrity();

        if (pass) {
            sendWordToLanguageServer();
            System.out.println("Incoming client request passed: " + word + " | " + languageCode);
        }
    }

    private boolean checkRequestIntegrity() {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            word = reader.readLine();
            languageCode = reader.readLine();
            port = reader.readLine();

            address = socket.getInetAddress().getHostAddress();

            if (!languageServersMap.containsKey(languageCode)) {
                System.out.println("Incoming client request: " + languageCode + " no such language server");
                writer.write("NoServer");
                writer.newLine();
                writer.flush();
                return false;
            }

            writer.write("Ok");
            writer.newLine();
            writer.flush();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void sendWordToLanguageServer() {

        try (Socket socket = new Socket()) {

            socket.connect(new InetSocketAddress("127.0.0.1", languageServersMap.get(languageCode)));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            writer.write(word);
            writer.newLine();
            writer.write(address);
            writer.newLine();
            writer.write(port);
            writer.flush();

        } catch (IOException e) {
            System.err.println("Error sending to language server");
        }

    }
}
