package chat;
//some comment
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalDateTime;
import javax.crypto.SecretKey;
import java.security.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import encryption.Encryption;

public class ChatServer extends JFrame {

	private Key privateKey;
	private JTextArea messageArea;
	private static final int PORT = 9898;
	private static ArrayList<ClientHandler> clients = new ArrayList<>();
	private ExecutorService pool = Executors.newFixedThreadPool(8); 

	public ChatServer() {
		super("Server");
		buildServerUI();
		try {
			privateKey = Encryption.readPrivateKey("keypairs/pkcs8_key");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void buildServerUI() {
		// set menu
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenuItem clearItem = new JMenuItem("Clear");
		JMenuItem exitItem = new JMenuItem("Exit");
		clearItem.addActionListener((ActionEvent e) -> {
			messageArea.setText("");
		});
		exitItem.addActionListener((ActionEvent e) -> {
			System.exit(0);
		});
		fileMenu.add(clearItem);
		fileMenu.add(exitItem);
		menuBar.add(fileMenu);
		this.setJMenuBar(menuBar);
		
		this.setLayout(new BorderLayout());
		messageArea = new JTextArea();
		messageArea.setEditable(false);
		messageArea.setText("");
		this.add(new JScrollPane(messageArea), BorderLayout.CENTER);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(400, 300);
	}

	public void startServer() {
		Runnable serverTask = () -> {
			try (ServerSocket listener = new ServerSocket(PORT)) {
				logMessage("Chat server started : " + LocalDateTime.now());
				while (true) {
					pool.execute(new ClientHandler(listener.accept(), this));
				}
			} catch (IOException e) {
				logMessage("Chat server failed : " + e.getMessage());
			}
		};
		new Thread(serverTask).start();
	}

	private void logMessage(String msg) {
		SwingUtilities.invokeLater(() -> {
			messageArea.append(msg + "\n");
		});
	}

	private static class ClientHandler implements Runnable {
		private Socket socket;
		private ChatServer server;
		private DataInputStream input;
		private DataOutputStream output;
		private SecretKey AESKey;
		private int id;
		private static int nextId = 1;

		public ClientHandler(Socket socket, ChatServer server) {
			this.id = ClientHandler.nextId;
			ClientHandler.nextId++;
			this.socket = socket;
			this.server = server;
			try {
				input = new DataInputStream(socket.getInputStream());
				output = new DataOutputStream(socket.getOutputStream());
				server.logMessage("Starting thread for client " + id);
				server.logMessage("Client " + id + "'s host name is " + socket.getInetAddress().getHostName());
				server.logMessage("Client " + id + "'s IP address is " + socket.getInetAddress().getHostAddress());
			} catch (Exception e) {
				server.logMessage("Setup Error: " + e.getMessage());
			}
		}

		@Override
		public void run() {
			try {
				
				String clientMessage = input.readUTF();
				if ("HELLO".equals(clientMessage)) {
					output.writeUTF("Connected");
					output.flush();
					try {
						byte[] encryptedSeed = Base64.getDecoder().decode(input.readUTF());
						byte[] AESSeed = Encryption.pkDecrypt(server.privateKey, encryptedSeed);
						AESKey = (SecretKey) Encryption.generateAESKey(AESSeed);
					} catch (Exception e) {
						server.logMessage("Encryption Error: " + e.getMessage());
					}
					if (AESKey != null) {
						
						ChatServer.clients.add(this);
						while (true) {
							String encryptedMsg = input.readUTF();
							try {
								String msg = Encryption.decrypt(AESKey, encryptedMsg);
								for (ClientHandler client : ChatServer.clients) {
									if (client != this) {
										String toSend = this.id + ": " + msg;
										client.output.writeUTF(Encryption.encrypt(client.AESKey, toSend));
										client.output.flush();
									}
								}
							} catch (Exception e) {
								server.logMessage("Encryption Error: " + e.getMessage());
							}
						}
					}
				}
			} catch (IOException e) {
				ChatServer.clients.remove(this);
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					server.logMessage("Close the socket");
				}
			}
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			ChatServer chatServer = new ChatServer();
			chatServer.setVisible(true);
			chatServer.startServer();
		});
	}
	
}