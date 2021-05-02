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
            System.out.println("==[Listening on port: " + serverSocket.getLocalPort() + "]===========");
            System.out.println("STOP - turns off the server");
            System.out.println("LOGIN - signs up on main server\n");

            switch (sc.nextLine()) {
                case "STOP":
                    System.out.println("Turning off the server....");
                    executor.shutdown();
                    System.exit(0);
                    break;

                case "LOGIN":
                    logInToProxy();
                    break;
            }
        }
    }


    private String getPathFromUser () {
        Scanner sc = new Scanner(System.in);
        String dictionaryPath = "";

        boolean pathPass = false;
        while (!pathPass) {
            System.out.print("Please enter path to a dictionary: ");
            dictionaryPath = sc.nextLine();
            pathPass = validatePath(dictionaryPath);
        }

        return dictionaryPath;
    }

    private boolean validatePath (String path) {
        try {
            Paths.get(path);
        } catch (InvalidPathException | NullPointerException e) {
            System.out.println("Invalid path");
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
            System.out.println();

            System.out.println("\tLoading successful\n");

        } catch (Exception e) {
            wordsMap.clear();
            System.out.println("\t" + e.getMessage());
            loadDictionary(getPathFromUser());
        }
    }


    private void startListening () {
        System.out.println("Creating listener for incoming connections...");

        try {
            this.serverSocket = new ServerSocket(0,10,InetAddress.getByName(null));

        } catch (IOException e) {
            System.out.println("Error creating Server Socket");
            System.out.println("Shutting down...\n");
            executor.shutdownNow();
            System.exit(-1);
        }

        executor.submit(requestsLogic);
        System.out.println("\tSuccessfully created listener\n");
    }

    private void logInToProxy() {
        System.out.println("Logging in to Proxy...");

        try (Socket socket = new Socket()) {
            try {
                socket.connect(new InetSocketAddress("127.0.0.1", 7777),500);
                System.out.println("\tConnected to proxy");
            } catch (IOException e) {
                System.out.println("\tError connecting to proxy");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true)) {

                    writer.println("LOGIN");
                    writer.println(languageCode);
                    writer.println(serverSocket.getLocalPort());
                    System.out.println("\tSent info");

                    String reply = reader.readLine();
                    System.out.println("\tReceived answer: " + reply);

                    switch (reply) {
                        case "ACCEPT":
                            System.out.println("\tSuccessfully logged in\n");
                            break;

                        case "DENY":
                            System.out.println("\tServer for such language already exists\n");
                            break;
                    }

                }
            } catch (IOException e) {
                System.out.println("\tError communicating with proxy\n");
            }
        }
        catch (IOException e) {
            System.out.println("\tError logging in to Proxy\n");
        }
    }

    Runnable requestsLogic = () -> {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                executor.submit(() -> new IdentifyRequest(socket,wordsMap));

            } catch (IOException e) {
                System.out.println("\tError accepting request\n");
            }
        }
    };
}
