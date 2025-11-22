/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.example.simplechatsimulator;

/**
 *
 * @author MALIK
 */

// --- IMPORTS (Ye sabse zaroori hissa hai) ---
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// ----------------- Factory Method -----------------
interface Message {
    String getSender();
    String getContent();
    String getTimestamp();
    String getSummary();
}

class TextMessage implements Message {
    private String sender;
    private String content;
    private LocalDateTime timestamp;

    public TextMessage(String sender, String content) {
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public String getSender() { return sender; }
    public String getContent() { return content; }
    public String getTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    public String getSummary() {
        return "[" + getTimestamp() + "] " + sender + ": " + content;
    }
}

class MessageFactory {
    public static Message createMessage(String type, String sender, String content) {
        if(type.equalsIgnoreCase("text")) {
            return new TextMessage(sender, content);
        }
        return null;
    }
}

// ----------------- Decorator -----------------
abstract class MessageDecorator implements Message {
    protected Message wrap;
    public MessageDecorator(Message wrap) { this.wrap = wrap; }
    public String getSender() { return wrap.getSender(); }
    public String getContent() { return wrap.getContent(); }
    public String getTimestamp() { return wrap.getTimestamp(); }
    public String getSummary() { return wrap.getSummary(); }
}

class TimestampDecorator extends MessageDecorator {
    public TimestampDecorator(Message wrap) { super(wrap); }
    @Override
    public String getSummary() {
        return "[" + getTimestamp() + "] " + getSender() + ": " + getContent();
    }
}

class StyleDecorator extends MessageDecorator {
    public StyleDecorator(Message wrap) { super(wrap); }
    @Override
    public String getSummary() {
        // For simplicity, bold style simulated with ** **
        return "**" + wrap.getSummary() + "**";
    }
}

// ----------------- Builder -----------------
class ChatSession {
    private String user;
    private String peer;
    private List<Message> messages;
    public ChatSession(String user, String peer, List<Message> messages) {
        this.user = user;
        this.peer = peer;
        this.messages = messages;
    }
    public List<Message> getMessages() { return messages; }
}

class ChatSessionBuilder {
    private String user;
    private String peer;
    private List<Message> messages = new ArrayList<>();
    public ChatSessionBuilder setUser(String user) { this.user = user; return this; }
    public ChatSessionBuilder setPeer(String peer) { this.peer = peer; return this; }
    public ChatSessionBuilder addInitialMessage(Message m) { messages.add(m); return this; }
    public ChatSession build() { return new ChatSession(user, peer, messages); }
}

// ----------------- Observer -----------------
interface ChatObserver {
    void update(Message msg);
}

class NotificationObserver implements ChatObserver {
    @Override
    public void update(Message msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("New Message");
            alert.setHeaderText(msg.getSender() + " sent a message");
            alert.setContentText(msg.getContent());
            alert.showAndWait();
        });
    }
}

// ----------------- Singleton -----------------
class ChatEngine {
    private static ChatEngine instance;
    private List<Message> history = new ArrayList<>();
    private List<ChatObserver> observers = new ArrayList<>();

    private ChatEngine() {}

    public static ChatEngine getInstance() {
        if(instance == null) instance = new ChatEngine();
        return instance;
    }

    public void sendMessage(Message msg) {
        history.add(msg);
        notifyObservers(msg);
    }

    public void attachObserver(ChatObserver o) { observers.add(o); }
    public void detachObserver(ChatObserver o) { observers.remove(o); }

    private void notifyObservers(Message msg) {
        for(ChatObserver o : observers) o.update(msg);
    }

    public List<Message> getHistory() { return history; }
}

// ----------------- Main JavaFX Application -----------------
public class SimpleChatSimulator extends Application {

    private VBox messageArea;
    private TextField inputField;
    private ChatEngine engine;
    private String user = "You";
    private String bot = "Ali";

    @Override
    public void start(Stage primaryStage) {
        engine = ChatEngine.getInstance();
        engine.attachObserver(new NotificationObserver());

        // GUI Layout
        messageArea = new VBox(5);
        messageArea.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(messageArea);
        scrollPane.setFitToWidth(true);

        inputField = new TextField();
        inputField.setPrefWidth(300);

        Button sendBtn = new Button("Send");
        sendBtn.setOnAction(e -> sendUserMessage());

        HBox inputBox = new HBox(5, inputField, sendBtn);
        inputBox.setPadding(new Insets(10));

        VBox root = new VBox(5, scrollPane, inputBox);

        Scene scene = new Scene(root, 400, 500);
        primaryStage.setTitle("Simple Chat Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void sendUserMessage() {
        String text = inputField.getText().trim();
        if(text.isEmpty()) return;
        inputField.clear();

        // Factory + Decorator
        Message msg = MessageFactory.createMessage("text", user, text);
        msg = new TimestampDecorator(msg);
        displayMessage(msg);
        engine.sendMessage(msg);

        // Bot auto-reply after 1 second
        new Thread(() -> {
            try { Thread.sleep(1000); } catch(Exception ex) {}
            Message botMsg = MessageFactory.createMessage("text", bot, "Bot says: " + text);
            // Yahan variable ko final ya effectively final banane ke liye direct use kar rahe hain
            Message botMsgFinal = new TimestampDecorator(botMsg);
            Platform.runLater(() -> {
                displayMessage(botMsgFinal);
                engine.sendMessage(botMsgFinal);
            });
        }).start();
    }

    private void displayMessage(Message msg) {
        Label label = new Label(msg.getSummary());
        messageArea.getChildren().add(label);
    }

    public static void main(String[] args) {
        launch(args);
    }
}