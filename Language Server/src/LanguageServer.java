import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LanguageServer {

    private final HashMap<String,String> wordsMap;
    private String languageCode;

    private final ExecutorService executor;

    private ServerSocket serverSocket;

    public LanguageServer () {
        this.wordsMap = new HashMap<>();
        this.languageCode = "";
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() {
        Scanner sc = new Scanner(System.in);
        String dictionaryPath = "";

        boolean pathPass = false;
        while (!pathPass) {
            System.out.println("Please enter a path to a dictionary: ");
            dictionaryPath = sc.nextLine();
            pathPass = validatePath(dictionaryPath);
        }

        loadDictionary(dictionaryPath);
        startListening();

        while (true) {
            System.out.println("==========================================");
            System.out.println("STOP - turns off the server");
            System.out.println("LOGIN - signs up on main server");
            System.out.println("LOGOUT - signs out from main server");
            System.out.println();

            switch (sc.nextLine()) {
                case "STOP":
                    System.out.println("Turning off the server....");
                    logoutFromMainServer();
                    executor.shutdown();
                    System.exit(0);

                    break;

                case "LOGIN":
                    System.out.println("Logging in...");
                    boolean result1 = loginOnMainServer();

                    if (result1) {
                        System.out.println("Successfully logged in \n");
                    } else {
                        System.out.println("Error logging in \n");
                    }

                    break;

                case "LOGOUT":
                    System.out.println("Logging out...");
                    boolean result2 = logoutFromMainServer();

                    if (result2) {
                        System.out.println("Successfully logged out \n");
                    } else {
                        System.out.println("Error logging out \n");
                    }

                    break;
            }
        }
    }

    private boolean validatePath (String path) {
        try {
            Paths.get(path);
        } catch (InvalidPathException | NullPointerException e) {
            System.out.println("Invalid path\n");
            return false;
        }

        File file = new File(path);

        if (file.isDirectory()) {
            System.out.println("Invalid path: Path is directory\n");
            return false;
        }

        if (!file.exists()) {
            System.out.println("Invalid path: File doesn't exist\n");
            return false;
        }

        return true;
    }

    private void loadDictionary (String path) {
        try {
            File dictionary = new File(path);
            Scanner reader = new Scanner(dictionary);

            languageCode = dictionary.getName().replaceFirst("[.][^.]+$","");

            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                String[] words = line.split(",");

                if (words.length == 2) {
                    wordsMap.put(words[0],words[1]);
                } else {
                    System.out.println("Syntax error: " + line);
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("File not found");
            System.exit(-1);
        } catch (Exception e) {
            System.err.println("Error loading dictionary");
            System.exit(-1);
        }
    }

    private boolean loginOnMainServer () {

        try (Socket socket = new Socket()) {

            socket.connect(new InetSocketAddress("127.0.0.1",7777));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            writer.write("Login");
            writer.newLine();
            writer.write(languageCode);
            writer.newLine();
            writer.write(String.valueOf(serverSocket.getLocalPort()));
            writer.newLine();
            writer.flush();

            String reply = reader.readLine();

            switch (reply) {
                case "Accept":
                    return true;

                case "Deny":
                    System.out.println("Server for such language already exists");
                    return false;
            }

        } catch (IOException e) {
            System.err.println("Error logging in on main server");
            return false;
        }

        return false;
    }

    private boolean logoutFromMainServer () {

        try (Socket socket = new Socket()) {

            socket.connect(new InetSocketAddress("127.0.0.1",7777));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            writer.write("Logout");
            writer.newLine();
            writer.write(languageCode);
            writer.newLine();
            writer.flush();

            String response = reader.readLine();

            switch (response) {
                case "Successful":
                    return true;

                case "Error":
                    return false;
            }

        } catch (IOException e) {
            System.err.println("Error logging out from main server");
            return false;
        }

        return false;
    }

    private void startListening () {

        executor.submit(() -> {

            try {
                this.serverSocket = new ServerSocket(0,0, InetAddress.getByName(null));
                System.out.println("Listening on port: " + serverSocket.getLocalPort());
                System.out.println();
            } catch (IOException e) {
                System.err.println("Error creating socket");
                System.exit(-1);
            }

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Incoming request");
                executor.submit(() -> new Reply(socket,wordsMap));
            }
        });
    }
}
