import java.io.*;
import java.net.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LanguageServer {

    private ConcurrentHashMap<String,String> wordsMap;

    private String languageCode;

    private ExecutorService executor;

    private ServerSocket serverSocket;

    public LanguageServer () {
        this.wordsMap = new ConcurrentHashMap<>();
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() {
        loadDictionary(getPathFromUser());
        startListening();

        logInToProxy();

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("==[Listening on port: " + serverSocket.getLocalPort() + "===========");
            System.out.println("STOP - turns off the server");
            System.out.println("LOGIN - signs up on main server");
            System.out.println("LOGOUT - signs out from main server\n");

            switch (sc.nextLine()) {
                case "STOP":
                    System.out.println("Turning off the server....");
                    executor.shutdown();
                    System.exit(0);
                    break;

                case "LOGIN":
                    logInToProxy();
                    break;

                case "LOGOUT":
                    logOutFromProxy();
                    break;
            }
        }
    }


    private String getPathFromUser () {
        Scanner sc = new Scanner(System.in);
        String dictionaryPath = "";

        boolean pathPass = false;
        while (!pathPass) {
            System.out.print("Please enter a path to a dictionary: ");
            dictionaryPath = sc.nextLine();
            pathPass = validatePath(dictionaryPath);
        }

        return dictionaryPath;
    }

    private boolean validatePath (String path) {
        try {
            Paths.get(path);
        } catch (InvalidPathException | NullPointerException e) {
            System.out.println("\u001B[31m" + "Invalid path" + "\u001B[0m");
            return false;
        }

        File file = new File(path);

        if (file.isDirectory()) {
            System.out.println("\u001B[31m" + "Invalid path: Path is directory\n" + "\u001B[0m");
            return false;
        }

        if (!file.exists()) {
            System.out.println("\u001B[31m" + "Invalid path: File doesn't exist\n" + "\u001B[0m");
            return false;
        }

        return true;
    }

    private void loadDictionary (String path) {
        System.out.println("\tLoading dictionary...");
        try {
            File dictionary = new File(path);
            Scanner reader = new Scanner(dictionary);

            //Gets language code from the file name
            languageCode = dictionary.getName().replaceFirst("[.][^.]+$","");

            int lineCount = 1;
            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                String[] words = line.split(",");

                if (words.length != 2) {
                    throw new Exception("Syntax error at line: " + lineCount);
                }
                wordsMap.put(words[0],words[1]);
            }

            System.out.println("\tLoading successful\n");

        } catch (Exception e) {
            wordsMap.clear();
            System.out.println("\u001B[31m\t" + e.getMessage() + "\u001B[0m\n");
            loadDictionary(getPathFromUser());
        }
    }


    private void startListening () {
        System.out.println("Creating listener for incoming connections...");

        try {
            this.serverSocket = new ServerSocket(0,10,InetAddress.getByName(null));

        } catch (IOException e) {
            System.out.println("\u001B[31m\t" + "Error creating Server Socket" + "\u001B[0m");
            System.out.println("\u001B[31m\t" + "Shutting down..." + "\u001B[0m\n");
            executor.shutdownNow();
            System.exit(-1);
        }

        executor.submit(() -> requestsLogic);
        System.out.println("\tSuccessfully created listener");
    }

    private void logInToProxy() {
        System.out.println("Logging in to Proxy...");

        try (Socket socket = new Socket()) {
            try {
                socket.connect(new InetSocketAddress("127.0.0.1", 7777));
                System.out.println("\tConnected to proxy");
            } catch (IOException e) {
                System.out.println("\u001B[31m\t" + "Error connecting to proxy" + "\u001B[0m");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true)) {

                    writer.println("LOGIN");
                    writer.println(languageCode);
                    writer.println(serverSocket.getLocalPort());
                    System.out.println("\tSent info");

                    String reply = reader.readLine();
                    System.out.println("\tReceived answer");

                    switch (reply) {
                        case "ACCEPT":
                            System.out.println("\tSuccessfully logged in\n");

                        case "DENY":
                            System.out.println("\tServer for such language already exists\n");
                    }

                }
            } catch (IOException e) {
                System.out.println("\u001B[31m\t" + "Error communicating with" + "\u001B[0m");
            }
        }
        catch (IOException e) {
            System.out.println("\u001B[31m\t" + "Error logging in to Proxy" + "\u001B[0m\n");
        }
    }

    private void logOutFromProxy() {
        System.out.println("Logging out from Proxy...");

        try (Socket socket = new Socket()) {
            try {
                socket.connect(new InetSocketAddress("127.0.0.1",7777));
            } catch (IOException e) {
                System.out.println("\u001B[31m\t" + "Error connecting to proxy" + "\u001B[0m");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true)) {

                    writer.println("LOGOUT");
                    writer.println(languageCode);
                    System.out.println("\tSent info");

                    String response = reader.readLine();
                    System.out.println("\tReceived answer");

                    switch (response) {
                        case "Successful":
                            System.out.println("\tSuccessfully logged out\n");
                            break;

                        case "NoServer":
                            System.out.println("\tServer not logged in\n");
                            break;
                    }
                }
            } catch (IOException e) {
                System.out.println("\u001B[31m\t" + "Error reading/writing to Proxy" + "\u001B[0m");
            }

        } catch (IOException e) {
            System.err.println("\u001B[31m\t" + "Error logging out from Proxy" + "\u001B[0m\n");
        }
    }


    Runnable requestsLogic = () -> {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("Accepted server connection");
                executor.submit(() -> new IdentifyRequest(socket,wordsMap));

            } catch (IOException e) {
                System.out.println("\u001B[31m\t" + "Error accepting request" + "\u001B[0m\n");
            }
        }
    };
}
