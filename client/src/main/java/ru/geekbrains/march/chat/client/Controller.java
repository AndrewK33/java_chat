package ru.geekbrains.march.chat.client;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    TextField msgField, loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    TextArea msgArea;

    @FXML
    HBox loginPanel, msgPanel;

    @FXML
    ListView<String> clientsList;



    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private String path = new File("MessageHistory.txt").getAbsolutePath();

    public void setUsername(String username) {
        this.username = username;
        boolean usernameIsNull = username == null;

        loginPanel.setVisible(usernameIsNull);
        loginPanel.setManaged(usernameIsNull);
        msgPanel.setVisible(!usernameIsNull);
        msgPanel.setManaged(!usernameIsNull);
        clientsList.setVisible(!usernameIsNull);
        clientsList.setManaged(!usernameIsNull);
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUsername(null);
    }

    public void login() {
        if (loginField.getText().isEmpty()) {
            showErrorAler("Nickname cannot be empty ");
            return;
        }
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/login " + loginField.getText() + " " + passwordField.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/login_ok ")) {
                            setUsername(msg.split("\\s")[1]);
                            readMessageHistory();
                            msgArea.appendText("\n");
                            break;
                        }
                        if (msg.startsWith("/login_failed ")) {
                            String reason = msg.split("\\s", 2)[1];
                            msgArea.appendText(reason + "\n");
                        }
                    }

                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/")) {
                            if (msg.startsWith("/clients_list ")) {
                                String[] tokens = msg.split("\\s");

                                Platform.runLater(() -> {
                                    /*System.out.println(Thread.currentThread().getName());*/
                                    clientsList.getItems().clear();
                                    for (int i = 1; i < tokens.length; i++) {
                                        clientsList.getItems().add(tokens[i]);
                                    }
                                });
                            }
                            continue;
                        }
                        msgArea.appendText(msg + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }
            });
            t.start();
        } catch (IOException e) {
            showErrorAler("Connection not possible, try again");
        }
    }


    public void sendMsg() {
        try {
            out.writeUTF(msgField.getText());
            writeMessageHistory();
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            showErrorAler("Couldn't send message");
        }
    }

    public void readMessageHistory() {
        try (InputStreamReader in = new InputStreamReader(new FileInputStream(path))){
            int x = 0;
            while ((x = in.read()) !=-1) {
                msgArea.appendText(String.valueOf((char) x));
            }
            msgArea.appendText("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void writeMessageHistory() {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(path, true))){
            out.write(msgField.getText().getBytes(StandardCharsets.UTF_8));
            out.write("\n".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void disconnect() {
        setUsername(null);
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private  void showErrorAler (String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.setTitle("Chat FX");
        alert.setHeaderText(null);
        alert.showAndWait();
    }


}
