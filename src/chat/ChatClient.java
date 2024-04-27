package chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.security.*;

import javax.crypto.*;
import java.util.Base64;
import java.util.Random;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import encryption.Encryption;
import javax.swing.text.*;

public class ChatClient extends JFrame {

    private static final int PORT = 9898;
    private static final String SERVER_ADDRESS = "localhost";
    private static final String SERVER_PUBLIC_KEY = "MIGeMA0GCSqGSIb3DQEBAQUAA4GMADCBiAKBgGk9wUQ4G9PChyL5SUkCyuHjTNOglEy5h4KEi0xpgjxi/UbIH27NXLXOr94JP1N5pa1BbaVSxlvpuCDF0jF9jlZw5IbBg1OW2R1zUACK+NrUIAYHWtagG7KB/YcyNXHOZ6Icv2lXXd7MbIao3ShrUVXo3u+5BJFCEibd8a/JD/KpAgMBAAE=";
    private DataOutputStream output;
    private DataInputStream input;
    private Socket connection;
    private PublicKey serverPublicKey;
    private Key communicationKey;
    private JTextPane messagePane;
    private StyledDocument doc;
    private JTextField inputField;
    private boolean connected;
    private Color clientColor = Color.BLUE; // Assign a specific color to the client


    public ChatClient() {
        super("Client");
        buildClientUI();
        try {
            serverPublicKey = Encryption.readPublicKey(SERVER_PUBLIC_KEY);			
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Public Key Error : " + e.getMessage());
        }
        connected = false;
    }

    private void buildClientUI() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem connectItem = new JMenuItem("Connect");
        JMenuItem exitItem = new JMenuItem("Exit");
        connectItem.addActionListener((ActionEvent e) -> {
            if (connected) {
                logMessage("Connected already", false);
            } else {
                startClient();
            }
        });
        exitItem.addActionListener((ActionEvent e) -> {
            System.exit(0);
        });
        fileMenu.add(connectItem);
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        this.setJMenuBar(menuBar);

        // Add connection status to the menu bar
        // Add connection status to the menu bar
        connectionStatus = new JLabel("Connection ");
        connectionStatus.setIcon(new ImageIcon(createStatusIcon(Color.RED)));
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(connectionStatus);

        this.setLayout(new BorderLayout());
        messagePane = new JTextPane(); // Changed from JTextArea to JTextPane
        messagePane.setEditable(false);
        doc = messagePane.getStyledDocument(); // Get the StyledDocument from JTextPane
        this.add(new JScrollPane(messagePane), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.setEditable(false);
        inputField.addActionListener((ActionEvent e) -> {
            System.out.println(inputField.getText());
            sendMessage(inputField.getText());
            inputField.setText("");
        });
        inputPanel.add(inputField, BorderLayout.CENTER);

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener((ActionEvent e) -> {
            System.out.println(inputField.getText());
            sendMessage(inputField.getText());
            inputField.setText("");
        });
        inputPanel.add(sendButton, BorderLayout.EAST);

        this.add(inputPanel, BorderLayout.SOUTH);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(400, 300);
    }

    private JLabel connectionStatus; // Declare the connectionStatus variable

    private void updateConnectionStatus(boolean isConnected) {
        SwingUtilities.invokeLater(() -> {
            connectionStatus.setIcon(new ImageIcon(createStatusIcon(isConnected ? Color.GREEN : Color.RED)));
        });
    }

    private Image createStatusIcon(Color color) {
        int size = 16;
        int padding = 4;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, size, size);
        g2.setColor(color);
        g2.fillOval(padding, padding, size - 2 * padding, size - 2 * padding);
        g2.dispose();
        return image;
    }
    
    private void startClient() {
        Runnable clientTask = () -> {
            try {
                connection = new Socket(SERVER_ADDRESS, PORT);
                output = new DataOutputStream(connection.getOutputStream());
                output.flush();
                input = new DataInputStream(connection.getInputStream());

                output.writeUTF("HELLO");
                output.flush();
                try {
                    String response = (String) input.readUTF();
                    if ("Connected".equals(response)) {
                        updateConnectionStatus(true);
                        byte[] seed = Encryption.generateSeed();
                        byte[] eseed = Encryption.pkEncrypt(serverPublicKey, seed);

                        output.writeUTF(Base64.getEncoder().encodeToString(eseed));
                        output.flush();
                        communicationKey = (SecretKey) Encryption.generateAESKey(seed);
                        inputField.setEditable(true);
                        connected = true;
                        while (connected) {
                            try {
                                String encryptedMsg = (String) input.readUTF();
                                String message = Encryption.decrypt(communicationKey, encryptedMsg);
                                logMessage(message, false);
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.err.println("Error: " + e.getMessage());
                                System.exit(1);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Handshake Error: " + e.getMessage());
                    System.exit(1);
                }
            } catch (EOFException eofException) {
                logMessage("Client left", false);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                try {
                    output.close();
                    input.close();
                    connection.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        };
        new Thread(clientTask).start();
    }

    private void sendMessage(String message) {
        try {
            output.writeUTF(Encryption.encrypt(communicationKey, message));
            output.flush();
            logMessage(message, true); // Added second parameter to indicate message was sent by client
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void logMessage(String msg, boolean isSentByClient) {
        SwingUtilities.invokeLater(() -> {
            // Create a JPanel as a chat bubble
            JPanel bubble = new JPanel();
            bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
            bubble.setBorder(new EmptyBorder(10, 10, 10, 10));
            bubble.setBackground(new Color(0.9f, 0.9f, 0.9f)); // Lighter gray color

            // Add the name to the bubble
            JLabel nameLabel = new JLabel(isSentByClient ? "You" : "Friend");
            nameLabel.setForeground(clientColor);
            bubble.add(nameLabel);

            // Add the message to the bubble
            JLabel msgLabel = new JLabel(msg);
            bubble.add(msgLabel);

            // Add the bubble to the messagePane
            messagePane.insertComponent(bubble);
            try {
                doc.insertString(doc.getLength(), "\n", null); // Add a newline after each bubble
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatClient chatClient = new ChatClient();
            chatClient.setVisible(true);
        });
    }
}