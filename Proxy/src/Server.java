import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    ExecutorService executor;

    ConcurrentHashMap<String,Integer> languageServersMap;

    public void start () {
        executor = Executors.newCachedThreadPool();
        languageServersMap = new ConcurrentHashMap<>();

        initializeLanguageServerListener();
        initializeClientListener();

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("==========================================");
            System.out.println("STOP - turns off the server");
            System.out.println("LIST SERVERS - displays all active servers");
            System.out.println();

            switch (sc.nextLine()) {
                case "STOP":
                    System.out.println("Turning off the server....");
                    executor.shutdown();
                    System.exit(0);

                    break;

                case "LIST SERVERS":
                    System.out.println("Servers online: " + languageServersMap.size());
                    languageServersMap.entrySet().forEach(System.out::println);
                    System.out.println();

                    break;
            }
        }
    }

    public void initializeLanguageServerListener () {

        try {
            ServerSocket serverSocket = new ServerSocket(7777,0, InetAddress.getByName(null));

            executor.submit(() -> {
                while (true) {

                    Socket socket = serverSocket.accept();
                    executor.submit(() -> new Register(socket,languageServersMap));
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error initializing Servers Listener");
            executor.shutdown();
            System.exit(-1);
        }
    }

    public void initializeClientListener () {

        try {
            ServerSocket serverSocket = new ServerSocket(6666,0,InetAddress.getByName(null));

            executor.submit(() -> {
                while (true) {

                    Socket socket = serverSocket.accept();
                    executor.submit(() -> new Communication(socket,languageServersMap));
                }
            });

        } catch (IOException e) {
            System.err.println("Error initializing Client Listener");
            executor.shutdownNow();
            System.exit(-1);
        }
    }
}
