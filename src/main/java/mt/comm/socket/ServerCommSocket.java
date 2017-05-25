package mt.comm.socket;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import mt.Order;
import mt.comm.ClientSideMessage;
import mt.comm.ServerSideMessage;
import mt.comm.ServerSideMessage.Type;
import mt.comm.common.CommUtils;

/**
 * This class is responsible for handling sockets and streams between several
 * clients and server.
 *
 */
public class ServerCommSocket extends Thread {
	private ServerCommSocketProduct serverCommSocketProduct = new ServerCommSocketProduct();

	/**
	 * The TCP IP port used by server socket in order to receive connections.
	 */
	private static final int SERVER_SOCKET_TCP_IP_PORT = 9090;

	/**
	 * The server socket used to receive new client's connections.
	 */
	private ServerSocket serverSocket;

	/**
	 * Reference to the queue used to store messages received from clients.
	 */
	private BlockingQueue<ServerSideMessage> serverMessages;

	/**
	 * Constructor of the objects of this class.
	 * 
	 * @param serverMessages
	 *            Queue used to store messages received from clients.
	 * @throws IOException
	 *             If some error occurred while a server socket is being
	 *             created.
	 */
	public ServerCommSocket(final BlockingQueue<ServerSideMessage> serverMessages) throws IOException {
		System.out.println("ServerComm >> Trying to create a server socket...");
		this.serverSocket = new ServerSocket(SERVER_SOCKET_TCP_IP_PORT);
		System.out.println("ServerComm >> The server socket is available for accepting new connections");

		serverCommSocketProduct.setConnectedUsers(new HashMap<String, ObjectOutputStream>());
		this.serverMessages = serverMessages;
	}

	@Override
	public void run() {
		Socket clientSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		try {
			while (true) {
				// Creating the server socket in order to accept new client's
				// connections.
				clientSocket = serverSocket.accept();
				System.out.println("ServerComm >> A new client connection is received");

				// Creating streams IN and OUT for the new connection.
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				in = new ObjectInputStream(clientSocket.getInputStream());
				System.out.println("ServerComm >> The streams IN and OUT were created sucessfully");

				// Retrieving a message received from client, through IN stream.
				ServerSideMessage message = (ServerSideMessage) in.readObject();
				System.out.println("ServerComm >> Processing " + message);

				// Validating if a client (nickname) is already connected.
				String nickname = message.getSenderNickname();
				if (serverCommSocketProduct.getConnectedUsers().get(nickname) != null) {
					final String errorMsg = String.format(CommUtils.USER_IS_ALREADY_CONNECTED, nickname);
					System.out.println("ServerComm >> " + errorMsg);
					out.writeObject(CommUtils.createErrorMessage(errorMsg));
				} else {
					// Adding information regarding to the new connected user.
					serverCommSocketProduct.getConnectedUsers().put(nickname, out);

					// Adding connected message to the queue in order to inform
					// the server component.
					serverMessages.put(message);

					// Creating a new thread to deal with receiving server
					// messages sent by the clients.
					String serverThreadName = "ServerCommThread-" + nickname;
					ServerCommThread serverCommThread = new ServerCommThread(nickname, clientSocket, in, serverMessages, serverCommSocketProduct.getConnectedUsers());
					serverCommThread.setName(serverThreadName);
					serverCommThread.start();
					System.out.println(String.format("ServerComm >> Starting thread '%s' to handle messages received from client '%s'", serverThreadName, nickname));
				}
			}
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
			System.out.println("ServerComm >> An error has occurred due to : " + CommUtils.getCause(e));
		} finally {
			try {
				CommUtils.releaseResources(in, out, clientSocket, serverSocket);
				serverCommSocketProduct.getConnectedUsers().clear();
				serverCommSocketProduct.setConnectedUsers(null);
				serverMessages.clear();
				serverMessages = null;
			} catch (IOException e) {
				System.out.println("ServerComm >> An error has occurred while releasing resources due to : " + CommUtils.getCause(e));
			}
		}
	}

	/**
	 * This method is used to send an order to a certain client.
	 * 
	 * @param receiversNickname
	 *            Client's nickname.
	 * @param order
	 *            The order to be sent to the client.
	 */
	public void sendOrder(String receiversNickname, Order order) {
		serverCommSocketProduct.sendOrder(receiversNickname, order);
	}

	/**
	 * This method is used to send an error message to a certain client.
	 * 
	 * @param toNickname
	 *            Client's nickname.
	 * @param error
	 *            The error message to be sent.
	 */
	public void sendError(String toNickname, String error) {
		serverCommSocketProduct.sendError(toNickname, error);
	}

	/**
	 * This method is used to validate if a certain client is still connected.
	 * 
	 * @param nickname
	 *            The client's nickname used to validate if he's connected.
	 * @return True if client is connected, otherwise false.
	 */
	public boolean isClientConnected(String nickname) {
		return serverCommSocketProduct.isClientConnected(nickname);
	}

	/**
	 * This method is used to disconnect a client.
	 * 
	 * @param nickname
	 *            The client's nickname to be disconnected.
	 */
	public void disconnectClient(String nickname) {
		serverCommSocketProduct.disconnectClient(nickname);
	}

	public Object clone() throws java.lang.CloneNotSupportedException {
		ServerCommSocket clone = (ServerCommSocket) super.clone();
		clone.serverCommSocketProduct = (ServerCommSocketProduct) this.serverCommSocketProduct.clone();
		return clone;
	}
}

/**
 * This class implements the thread that is instantiated for each client
 * connected to the ServerComm, in order to handle with the messages received
 * from the client part.
 *
 */
class ServerCommThread extends Thread {
	/**
	 * The client's nickname associated to this thread.
	 */
	private String nickname;

	/**
	 * The input stream used to receive messages from client part.
	 */
	private ObjectInputStream in;

	/**
	 * The client socket.
	 */
	private Socket clientSocket;

	/**
	 * The reference to the mapping between nicknames and output streams.
	 */
	private Map<String, ObjectOutputStream> connectedUsers;

	/**
	 * Reference to the queue used to store messages received from clients.
	 */
	private BlockingQueue<ServerSideMessage> serverMessages;

	public ServerCommThread(String nickname, Socket clientSocket, ObjectInputStream in, final BlockingQueue<ServerSideMessage> serverMessages,
			final Map<String, ObjectOutputStream> connectedUsers) {
		this.nickname = nickname;
		this.clientSocket = clientSocket;
		this.in = in;
		this.connectedUsers = connectedUsers;
		this.serverMessages = serverMessages;
	}

	@Override
	public void run() {
		try {
			while (true) {
				if (connectedUsers.get(nickname) != null) {
					ServerSideMessage message = (ServerSideMessage) in.readObject();
					serverMessages.put(message);
					System.out.println(String.format("ServerComm >> Processing %s from client '%s'", message, nickname));
					if (Type.DISCONNECTED.equals(message.getType())) {
						String senderNickname = message.getSenderNickname();
						ObjectOutputStream out = connectedUsers.remove(senderNickname);
						out.close();
						System.out.println(String.format("ServerComm >> Client '%s' is disconnected from server", senderNickname));
						break;
					}
				}
			}
		} catch (EOFException | SocketException e) {
			System.out.println(String.format("ServerComm >> The socket for client '%s' has been closed", nickname));
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
			System.out.println("ServerComm >> An error has occurred due to :" + CommUtils.getCause(e));
		} finally {
			try {
				CommUtils.releaseResources(in, clientSocket);
				connectedUsers = null;
				serverMessages = null;
			} catch (IOException e) {
				System.out.println("ServerComm >> An error has occurred while releasing resources due to :" + CommUtils.getCause(e));
			}
		}
	}
}
