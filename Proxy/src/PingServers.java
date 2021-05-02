import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class PingServers {

    private ExecutorService mainExecutor;
    private ConcurrentHashMap<String,Integer> languageServersMap;

    List<Future<String>> pingResponseList;

    public PingServers(ExecutorService mainExecutor,ConcurrentHashMap<String,Integer> languageServersMap) {
        if (!languageServersMap.isEmpty()) {
            this.mainExecutor = mainExecutor;
            this.languageServersMap = languageServersMap;

            this.pingResponseList = Collections.synchronizedList(new ArrayList<>());

            pingAllServers();

            //Delete comment to show pinging process on console
            //printResult();
        }
    }

    private void pingAllServers () {

        languageServersMap.forEach((serverCode,serverPort) -> pingResponseList.add(mainExecutor.submit(() -> {

            try (Socket socket = new Socket()) {

                socket.connect(new InetSocketAddress("127.0.0.1", serverPort), 200);
                socket.setSoTimeout(200);

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);

                writer.println("PING");
                String response = reader.readLine();

                if (response.equals("PONG")) {
                    return "\t" + serverCode + ": answered\n";
                } else {
                    languageServersMap.remove(serverCode);
                    System.out.println("Removed: " + serverCode);
                    return "\t" + serverCode + ": didn't answer\n";
                }

            } catch (IOException e) {
                languageServersMap.remove(serverCode);
                System.out.println("Removed: " + serverCode);
                return "\t" + serverCode + ": error pinging\n";
            }

        })));
    }

    private void printResult () {

        StringBuilder sb = new StringBuilder();

        sb.append("Ping report: \n");

        pingResponseList.forEach(server -> {
            try {
                sb.append(server.get());
            } catch (InterruptedException | ExecutionException e) {
                sb.append("\texecution error\n");
            }
        });

        sb.append("\n");

        System.out.println(sb.toString());
    }
}
