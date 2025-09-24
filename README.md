# Enterprise-Grade Java TCP Chat Server

This project is an advanced, multithreaded TCP chat server built in Java. It is designed using a modern, service-oriented architecture to demonstrate key software engineering principles such as **Separation of Concerns**, **Concurrency Management**, and **Protocol-Based Communication**.

The server is capable of handling multiple simultaneous clients, managing different chat rooms, and facilitating direct peer-to-peer file transfers, all through a sophisticated dual-port communication system.

---

## 🌟 Key Features

* **Service-Oriented Architecture:** The application logic is decoupled into distinct services (`LoginService`, `ChatroomManager`, `DataTransferManager`), making the system modular, scalable, and easy to maintain.
* **Efficient Concurrency with a Thread Pool:** A `ThreadExecutorService` manages a fixed pool of threads based on available CPU cores, ensuring the server can handle numerous clients without succumbing to the high overhead of thread-per-client models.
* **Dual-Port Communication System:** The server intelligently separates traffic types for maximum efficiency:
    * **Control Port (5010):** Handles lightweight, command-based traffic like logins, chat messages, and transfer negotiations.
    * **Data Port (5011):** A dedicated, high-throughput channel used exclusively for streaming large files between clients.
* **Stateful Session and Chat Room Management:** The server maintains the state of authenticated users and chat room memberships, providing features like message history and broadcasting to specific groups.
* **Secure and Negotiated File Transfers:** A robust, multi-step handshake protocol allows users to request, accept, or reject file transfers before any data is sent, ensuring security and consent.

---

## 🎓 Why This Project is a Good Learning Resource

This project serves as a practical example of several important computer science and software engineering concepts:

* **Object-Oriented Design:** Demonstrates encapsulation, inheritance, and polymorphism through its class structure.
* **Concurrency and Multithreading:** Shows how to build a responsive, non-blocking server that can handle many tasks at once.
* **Network Programming:** Provides a deep look into `java.net` sockets, streams, and client-server communication.
* **Design Patterns:** Implicitly uses patterns like **Singleton** (for manager classes), **Factory** (thread creation), and **Observer** (broadcasting messages).
* **Protocol Design:** Illustrates the importance of creating a strict, well-documented communication protocol for distributed systems.

---

## 🏗️ System Architecture Deep Dive

The server is built on a layered architecture where each component has a single, well-defined responsibility.



### Core Components

* `Server.java` **(The Network Hub)** 🗼
    * This is the application's entry point. It acts as the central network hub, creating two `ServerSocket` instances on two separate threads—one for the control port and one for the data port. Its sole responsibility is to listen for and accept new socket connections, after which it immediately delegates all further interaction to other components.

* `ServerHandler.java` **(The Client's Diplomat)** 🤵
    * Each client connecting to the **control port** is assigned a dedicated `ServerHandler` instance running in the thread pool. This class is the command router for a single client. It reads the raw string data sent by the client, parses it to identify a command (e.g., `LOGIN`, `SEND_MSG`, `WANT_TO_SEND_FILE`), and then invokes the appropriate service to handle the logic. It maintains the client's authenticated state and current chat room.

### The Service Layer

* `LoginService.java` **(The Bouncer)** 🔑
    * This service is the authority on user identity and sessions. It manages user creation, validates login attempts, and tracks which users are currently active and connected. It prevents the same user from logging in multiple times, ensuring session integrity.

* `ClientManager.java` **(The Switchboard Operator)** ☎️
    * This static class acts as a central registry for all active client control sockets. It holds a `ConcurrentHashMap` mapping a unique client ID to its socket connection. This allows any part of the server to send a message to a specific client (`unibroadcastMessage`) or to all clients (`broadcastMessage`) without needing direct access to the socket objects.

* `ChatroomManager.java` **(The Architect)** 🗺️
    * This service manages the lifecycle of chat rooms. It creates a set of default rooms on server startup and provides methods for users to list available rooms and join them. It also tracks the membership of each room, which is crucial for targeted message broadcasting.

* `DataTransferManager.java` **(The Logistics Coordinator)** 🚚
    * This is the core of the file transfer system. It manages the pairing of sender and receiver sockets on the **data port**. When a file transfer is accepted, both clients are instructed to connect to the data port. This manager waits for both connections, pairs them using a unique transfer ID, and then initiates a `relayFileStream` that pipes the bytes directly from the sender's `InputStream` to the receiver's `OutputStream`.

---

## ⚙️ How It Works: A Step-by-Step Flow

Understanding the flow of data is key to understanding the design.

### Flow 1: A User Sends a Chat Message

1.  **Client:** The user types a message in `ClientUI`. The client formats this into a protocol string: `SEND_MSG::...::Hello World`.
2.  **Server (Control Port):** The `Server` accepts the connection and the client's dedicated `ServerHandler` reads the string.
3.  **Command Routing:** The `ServerHandler` splits the string, identifies the `SEND_MSG` command, and calls its internal `handleTextMessage` method.
4.  **Message Parsing:** The `MessageProtocol` class is used to parse the string into a structured `MessageModel` object. This object is saved to the `Datahandler` (in-memory history).
5.  **Broadcasting:** The `ServerHandler` gets the user's current chat room from the `ChatroomManager`. It then iterates through the members of that room.
6.  **Targeted Delivery:** For each member, it retrieves their unique client ID and uses `ClientManager.unibroadcastMessage()` to send the formatted chat message directly to their socket.

### Flow 2: User A Sends a File to User B

1.  **Initiation (User A):** User A uses the `ClientUI` to send a request: `WANT_TO_SEND_FILE::UserB::document.pdf::102400`.
2.  **Mediation (Server):** User A's `ServerHandler` receives the request. It validates that User B is online using the `LoginService`.
3.  **Forwarding Request:** The server generates a unique `transferId` and forwards a notification to User B's `ClientHandler`: `INCOMING_FILE::UserA::document.pdf::102400::[transferId]`.
4.  **Acceptance (User B):** User B's `ClientUI` prompts them to accept. If they type "ja", the client sends `ACCEPT_FILE::[transferId]` back to the server.
5.  **Orchestration (DataTransferManager):**
    * User B's `ServerHandler` receives the acceptance and calls the `DataTransferManager`.
    * The `DataTransferManager` registers the pending transfer.
    * The server sends two separate commands back to the clients:
        * To User A: `START_FILE_TRANSFER::5011::[transferId]`
        * To User B: `PROCEED_WITH_DOWNLOAD::5011::[transferId]`
6.  **Data Connection:** Both `ClientUI` instances receive their respective commands and **independently connect to the server's data port (5011)**. They each send the unique `transferId` as the first piece of data.
7.  **Relaying:** The `DataTransferManager`, which has been waiting for two connections with the same ID, receives them, pairs their sockets, and starts the `relayFileStream` method. This method reads bytes from User A's socket and writes them directly to User B's socket until the specified file size is reached. The server acts as a pass-through, never saving the file to its own disk.
8.  **Completion:** Once the relay is finished, both sockets on the data port are closed, and the control connection remains open for further commands.

---

## 📜 Communication Protocol

The server operates on a strict, delimiter-based protocol. All messages sent to the control port must follow these formats.

**Delimiter:** `::`

### Core Commands

* `LOGIN::[username]`
* `LOGOUT`
* `LIST_ROOMS`
* `JOIN_ROOM::[chatroomId]`
* `SEND_MSG::[messageId]::[timestamp]::[type]::[sender]::[chatroomId]::[content]`

### File Transfer Commands

* `WANT_TO_SEND_FILE::[recipientUsername]::[filename]::[fileSize]`
* `ACCEPT_FILE::[transferId]`
* `REJECT_FILE::[transferId]`

---

## 🚀 Getting Started

### Prerequisites

* Java Development Kit (JDK) 11 or newer.

### Running the Server

1.  **Clone the Repository:**
    ```sh
    git clone [https://github.com/gabrielrx31/ChatServer.git](https://github.com/gabrielrx31/ChatServer.git)
    ```
2.  **Navigate to the Source Directory:**
    ```sh
    cd ChatServer/src/main/java/
    ```
3.  **Compile All Server and Common Files:**
    This command ensures all necessary packages are compiled.
    ```sh
    javac server/core/*.java Services/*.java common/models/*.java common/protocols/*.java models/*.java
    ```
4.  **Run the Server:**
    Execute the main `Server` class. It will automatically start listeners on ports 5010 (control) and 5011 (data).
    ```sh
    java server.core.Server
    ```

### Running the Client

1.  **Compile the Client:**
    In a **new terminal**, navigate to the same `src/main/java/` directory.
    ```sh
    javac client/ClientUI.java
    ```
2.  **Run the Client:**
    ```sh
    java client.ClientUI
    ```
    You can run multiple instances of the client to simulate a multi-user environment. Follow the on-screen prompts to log in and interact with the server.

---

## 💡 Future Improvements

* **Persistent Storage:** Replace the in-memory `Datahandler` with a database (like SQLite or PostgreSQL) to persist users, chat rooms, and message history.
* **Private Messaging:** Implement a `PRIVATE_MSG` command to allow one-to-one communication.
* **Encryption:** Use `SSLServerSocket` and `SSLSocket` to encrypt all communication between the client and server.
* **GUI Client:** Build a graphical user interface using a framework like JavaFX or Swing for a more user-friendly experience.
