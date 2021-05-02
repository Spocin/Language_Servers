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
        System.out.println("Creating listener...");

        try {
            this.serverSocket = new ServerSocket(0, 10, InetAddress.getByName(null));
            System.out.println("\tSuccessfully created ServerSocket");

            executor.submit(() -> {
                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("Accepted server request...");
                    receiveTranslation(socket);
                }
            });

            System.out.println("\tSuccessfully created listener\n");

        } catch (IOException e) {
            System.out.println("\tError creating socket\n");
            stop(-1);
        }
    }

    private void setupTranslateButton() {
        this.translateButton.setOnAction(event -> {
            System.out.println("Sending request...");

            System.out.print("\tVerifying given input: ");
            if (verifyIntegrity()) {
                sendInformation();
            }
        });
    }

    private boolean verifyIntegrity() {
        if (wordTextField.getText().isEmpty()) {
            System.out.println("word field is empty");
            return false;
        }

        if (languageCodeField.getText().isEmpty()) {
            System.out.println("language server field is empty");
            return false;
        }

        System.out.println("Ok");
        return true;
    }

    private void sendInformation() {
        System.out.println("\tSending request to proxy...");

        try (Socket socket = new Socket()){
            try {
                socket.connect(new InetSocketAddress("127.0.0.1",6666),500);
                System.out.println("\tConnected to proxy");

            } catch (IOException e) {
                System.out.println("\tError connecting to Proxy\n");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                try (PrintWriter writer = new PrintWriter(socket.getOutputStream(),true)) {

                    writer.println(wordTextField.getText());
                    writer.println(languageCodeField.getText());
                    writer.println(serverSocket.getLocalPort());
                    System.out.println("\tSent data");

                    String response = reader.readLine();
                    System.out.println("\tReceived response");

                    switch (response) {
                        case "OK":
                            System.out.println("\tProxy accepted request\n");
                            break;

                        case "NOSERVER":
                            System.out.println("\tNo such language server is online: " + languageCodeField.getText() + "\n");
                            break;
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error creating Sender socket");
        }
    }

    private void receiveTranslation(Socket socket) {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String translation = reader.readLine();
            System.out.println("\tReceived translation: " + translation + "\n");

            if (translation.equals("NOWORD")) {
                Platform.runLater(() -> answerLabel.setText("Cannot translate"));
            } else {
                Platform.runLater(() -> answerLabel.setText(translation));
            }

            System.out.println("End of connection\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop (int exitCode) {
        System.out.println("Turning off client...");
        executor.shutdownNow();
        System.exit(exitCode);
    }
}
