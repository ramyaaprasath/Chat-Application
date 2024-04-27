# Chat Server
This is a simple chat server implemented in Java. It uses a variety of advanced Java topics including multithreading, networking, encryption, and Swing for the GUI.

## Features
**Multithreading:** The server can handle multiple clients simultaneously using a thread pool.

**Networking:** The server uses Java's Socket and ServerSocket classes to communicate with clients.

**Encryption:** Messages are encrypted using a combination of RSA and AES encryption. The RSA keys are used to securely exchange the AES keys, which are then used to encrypt the actual messages.

**GUI:** The server has a simple GUI built with Swing that displays log messages.

## Yet to Add
**User Authentication:** Currently, the server does not authenticate users. This could be added in the future.

**Persistent Message History:** The server does not currently store message history. This could be added using a database or a simple file-based storage system.

**Private Messaging:** The server currently broadcasts all messages to all clients. The ability for clients to send private messages to each other could be added.

## Advanced Java Topics Used
**Multithreading: **The server uses an ExecutorService to manage a pool of threads. Each client is handled by a separate thread.

**Networking:** The server uses the ServerSocket class to listen for incoming connections, and the Socket class to communicate with clients.

**Encryption:** The server uses the Cipher class from the javax.crypto package for encryption and decryption. It uses RSA encryption to securely exchange AES keys, and then uses AES encryption for the actual messages.

**Swing:** The server uses various Swing components (JFrame, JTextArea, JScrollPane, JMenuBar, JMenu, JMenuItem) to build the GUI.

## How to Run
Compile the Java files: javac *.java

Run the server: java ChatServer

Run the client(s): java ChatClient

Please note that you need to have the RSA key pair files (public_key.der and pkcs8_key) in the keypairs directory.

