import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.*;

public class Proxy {

    ExecutorService mainExecutor;
    ScheduledExecutorService pingExecutor;

    ConcurrentHashMap<String,Integer> languageServersMap;

    public void start () {
        mainExecutor = Executors.newCachedThreadPool();
        languageServersMap = new ConcurrentHashMap<>();

        initializeServersCommunication();
        initializeClientsCommunication();
        initializeServerPing();

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("==========================================");
            System.out.println("STOP - turns off the server");
            System.out.println("LIST SERVERS - displays all active servers");
            System.out.println();

            switch (sc.nextLine()) {
                case "STOP":
                    System.out.println("Turning off the server....");
                    mainExecutor.shutdownNow();
                    pingExecutor.shutdownNow();
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

    public void initializeServersCommunication () {

        System.out.println("Initializing listener for servers requests...");

        try {
            ServerSocket serverSocket = new ServerSocket(7777, 10, InetAddress.getByName(null));

            mainExecutor.submit(() -> {
                while (true) {
                    Socket socket = serverSocket.accept();
                    mainExecutor.submit(() -> new ServersCommunication(socket, languageServersMap));
                    System.out.println("Accepted server connection...");
                }
            });

            System.out.println("\tSuccessfully created listener for servers\n");

        } catch (IOException e) {
            System.out.println("\tError initializing listener for servers\n");
            mainExecutor.shutdownNow();
            System.exit(-1);
        }
    }

    public void initializeClientsCommunication () {

        System.out.println("Initializing listener for clients requests...");

        try {
            ServerSocket serverSocket = new ServerSocket(6666,10,InetAddress.getByName(null));

            mainExecutor.submit(() -> {
                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("Accepted Client connection...");
                    mainExecutor.submit(() -> new ClientsCommunication(socket,languageServersMap));
                }
            });

            System.out.println("\tSuccessfully created listener for clients\n");

        } catch (IOException e) {
            System.out.println("\tError initializing listener for servers\n");
            stop(-1);
        }
    }

    public void initializeServerPing () {

        System.out.println("Initializing server pinger...");

        pingExecutor = Executors.newScheduledThreadPool(1);

        try {
            pingExecutor.scheduleWithFixedDelay(() -> new PingServers(mainExecutor,languageServersMap),1,1,TimeUnit.SECONDS);

            System.out.println("\tSuccessfully created pinger\n");

        } catch (Exception e) {
            System.out.println("\u001B[31m\t" + "Error creating pinger" + "\u001B[0m\n");
        }
    }

    private void stop(int exitCode) {
        System.out.println("Shutting down proxy...");
        mainExecutor.shutdownNow();
        pingExecutor.shutdownNow();
        System.exit(exitCode);
    }
}
