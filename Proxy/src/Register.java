import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Register {

    private final Socket socket;
    private final ConcurrentHashMap<String,Integer> languageServersMap;


    public Register (Socket socket, ConcurrentHashMap<String,Integer> languageServersMap) {

        this.socket = socket;
        this.languageServersMap = languageServersMap;

        identifyOperation();
    }

    private void identifyOperation() {

        String identifier;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            identifier = reader.readLine();

            switch (identifier) {
                case "Login":

                    addToLanguageServerMap(reader);
                    break;

                case "Logout":

                    removeFromLanguageServerMap(reader);
                    break;
            }

        } catch (IOException e) {
            System.err.println("Error getting Input Stream");
        }
    }

    private void addToLanguageServerMap (BufferedReader reader) {

        try {
            String key = reader.readLine();
            String value = reader.readLine();

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

                if (languageServersMap.containsKey(key)) {
                    writer.write("Deny");
                    System.out.println("Denied Language Server connection: Server \"" + key + "\" is already registered\n");
                } else {
                    writer.write("Accept");
                    languageServersMap.put(key,Integer.parseInt(value));
                    System.out.println("Server \"" + key + "\" logged IN on port: " + value + "\n");
                }
                writer.newLine();

            } catch (IOException e) {
                System.err.println("Error replaying for login request");
            }

        } catch (IOException e) {
            System.err.println("Error receiving Login information");
        }
    }

    private void removeFromLanguageServerMap (BufferedReader reader) {

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            String key = reader.readLine();

            if (languageServersMap.containsKey(key)) {
                languageServersMap.remove(key);
                System.out.println("Server \"" + key + "\" logged OUT");
                writer.write("Successful");
            } else {
                System.out.println("Error removing server: " + key);
                writer.write("Error");
            }
            writer.flush();


        } catch (IOException e) {
            System.err.println("Error receiving Logout information");
        }
    }
}
