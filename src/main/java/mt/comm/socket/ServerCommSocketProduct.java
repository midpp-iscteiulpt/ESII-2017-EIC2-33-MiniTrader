package mt.comm.socket;


import java.util.Map;
import java.io.ObjectOutputStream;
import mt.Order;
import mt.comm.ClientSideMessage;
import mt.comm.common.CommUtils;
import java.io.IOException;

public class ServerCommSocketProduct implements Cloneable {
	private Map<String, ObjectOutputStream> connectedUsers;

	public Map<String, ObjectOutputStream> getConnectedUsers() {
		return connectedUsers;
	}

	public void setConnectedUsers(Map<String, ObjectOutputStream> connectedUsers) {
		this.connectedUsers = connectedUsers;
	}

	/**
	* This method is used to send an order to a certain client.
	* @param receiversNickname Client's nickname.
	* @param order The order to be sent to the client.
	*/
	public void sendOrder(String receiversNickname, Order order) {
		ObjectOutputStream out = connectedUsers.get(receiversNickname);
		if (out != null) {
			try {
				ClientSideMessage message = CommUtils.createOrderMessage(order);
				out.reset();
				out.writeObject(message);
				System.out
						.println(String.format("ServerComm >> Sending %s to client '%s'", message, receiversNickname));
			} catch (IOException e) {
				System.out.println("ServerComm >> An error has thrown while sending a client message due to : "
						+ CommUtils.getCause(e));
			}
		}
	}

	/**
	* This method is used to send an error message to a certain client.
	* @param toNickname Client's nickname.
	* @param error The error message to be sent.
	*/
	public void sendError(String toNickname, String error) {
		ObjectOutputStream out = connectedUsers.get(toNickname);
		if (out != null) {
			try {
				ClientSideMessage message = CommUtils.createErrorMessage(error);
				out.writeObject(message);
				System.out.println(String.format("ServerComm >> Sending %s to '%s'", message, toNickname));
			} catch (IOException e) {
				System.out.println("ServerComm >> An error has thrown while sending an error client message due to: "
						+ CommUtils.getCause(e));
			}
		}
	}

	/**
	* This method is used to validate if a certain client is still connected.
	* @param nickname The client's nickname used to validate if he's connected.
	* @return  True if client is connected, otherwise false.
	*/
	public boolean isClientConnected(String nickname) {
		return connectedUsers.get(nickname) != null;
	}

	/**
	* This method is used to disconnect a client.
	* @param nickname The client's nickname to be disconnected.
	*/
	public void disconnectClient(String nickname) {
		ObjectOutputStream out = connectedUsers.remove(nickname);
		if (out != null) {
			try {
				CommUtils.releaseResources(out);
				System.out.println(
						String.format("ServerComm >> The client '%s' has been disconnected by server", nickname));
			} catch (IOException e) {
				System.out.println(
						String.format("ServerComm >> An error has thrown while disconnecting client '%s' due to: %s",
								nickname, CommUtils.getCause(e)));
			}
		}
	}

	public Object clone() throws CloneNotSupportedException {
		return (ServerCommSocketProduct) super.clone();
	}
}