import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientWindowController {

    @FXML
    TextField wordTextField, languageCodeField;

    @FXML
    Button translateButton;

    @FXML
    Label answerLabel;

    ExecutorService executor;
    private ServerSocket serverSocket;

    public void initialize() {
        this.executor = Executors.newFixedThreadPool(1);

        setupListener();
        setupTranslateButton();
    }

    private void setupListener() {

        executor.submit(() -> {
            this.serverSocket = new ServerSocket(0,0,InetAddress.getByName(null));

            while (true) {
                Socket socket = serverSocket.accept();
                receiveTranslation(socket);
            }
        });

    }

    private void setupTranslateButton() {
        this.translateButton.setOnAction(event -> {

            System.out.print("Verifying given input: ");
            if (verifyIntegrity()) {
                sendInformation();
            }
        });
    }

    private boolean verifyIntegrity() {
        if (wordTextField.getText().isEmpty()) {
            System.out.print("word field is empty\n");
            return false;
        }

        if (languageCodeField.getText().isEmpty()) {
            System.out.print("language server field is empty\n");
            return false;
        }

        System.out.println("Ok");
        return true;
    }

    private void sendInformation() {
        System.out.println("Sending request to proxy...");

        try (Socket socket = new Socket()){
            try {
                socket.connect(new InetSocketAddress("127.0.0.1",6666),500);
                System.out.println("\tConnected to proxy");
            } catch (IOException e) {
                System.err.println("Error connecting to Proxy");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(),true);

            writer.println(wordTextField.getText());
            writer.println(languageCodeField.getText());
            writer.println(serverSocket.getLocalPort());

            String response = reader.readLine();

            switch (response) {
                case "Ok":
                    System.out.println("\tProxy accepted request");
                    break;

                case "NoServer":
                    System.out.println("\tNo such language server is online: " + languageCodeField.getText());
                    System.out.println("End of connection\n");
                    break;
            }

        } catch (IOException e) {
            System.err.println("Error creating Sender socket");
        }
    }

    private void receiveTranslation(Socket socket) {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String translation = reader.readLine();
            System.out.println("\tReceived translation: " + translation);

            if (translation.equals("NULL")) {
                Platform.runLater(() -> answerLabel.setText("Cannot translate"));
            } else {
                Platform.runLater(() -> answerLabel.setText(translation));
            }

            System.out.println("End of connection\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop () {
        System.out.println("Turning off client...");
        executor.shutdownNow();
        System.exit(1);
    }
}
