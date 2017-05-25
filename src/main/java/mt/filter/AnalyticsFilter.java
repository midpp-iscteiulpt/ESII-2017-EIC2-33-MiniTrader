package mt.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mt.Order;
import mt.comm.ServerComm;
import mt.comm.ServerSideMessage;
import mt.comm.ServerSideMessage.Type;

public class AnalyticsFilter implements ServerComm {

	private AnalyticsFilterProduct analyticsFilterProduct = new AnalyticsFilterProduct();
	private ServerComm decoratedServerComm;
	private long testingVariable = 0;
	private ArrayList<ClientMessagesTimer> userMessage;
	private HashMap<String, List<Long>> map = new HashMap<>();

	public AnalyticsFilter(ServerComm serverCommToDecorate) {
		decoratedServerComm = serverCommToDecorate;
		analyticsFilterProduct.setTimer(System.currentTimeMillis());
		userMessage = new ArrayList<>();
	}

	@Override
	public void start() {
		decoratedServerComm.start();
	}

	@Override
	public ServerSideMessage getNextMessage() {
		ServerSideMessage message = decoratedServerComm.getNextMessage();
		if (message != null) {
			if (message.getType() == Type.NEW_ORDER) {
				if (message.getOrder().isBuyOrder())
					analyticsFilterProduct.setBuyOrders(analyticsFilterProduct.getBuyOrders() + 1);
				else if (message.getOrder().isSellOrder())
					analyticsFilterProduct.setSellOrders(analyticsFilterProduct.getSellOrders() + 1);
				long time = System.currentTimeMillis();
				String nickname = message.getSenderNickname();
				if (!map.containsKey(nickname)) {
					List<Long> list = new ArrayList<>();
					list.add(time);
					map.put(nickname, list);
				} else {
					List<Long> list1 = map.get(nickname);
					if (list1.size() == 10) {
						if (System.currentTimeMillis() - list1.get(0) <= 30000) {
							if (clientIsConnected(nickname)) {
								disconnectClient(nickname);
								map.remove(nickname);

								// SF-Ticket 1
								userMessage.add(new ClientMessagesTimer(nickname, System.currentTimeMillis()));

								//SF - Ticket 3
								return null;
							}
						} else {
							list1.add(time);
							list1.remove(0);
							map.put(nickname, list1);
						}
					} else {
						list1.add(time);
						map.put(nickname, list1);
					}
				}
			}

			// Apenas dar connect apos 30 segundos
			if (message.getType() == Type.CONNECTED) {
				if (checkDisconectedTimer(message.getSenderNickname()) == false) {
					// SF - Ticket 2
					decoratedServerComm.sendError(message.getSenderNickname(), "ConnectTimerLimit");
					decoratedServerComm.disconnectClient(message.getSenderNickname());
				}
			}
		}
		analyticsFilterProduct.averageBuySellOrders(this);
		return message;
	}

	/**
	 * 
	 * This method keeps track of buy/sell orders and prints to console every
	 * minute
	 * 
	 */
	public void averageBuySellOrders() {
		analyticsFilterProduct.averageBuySellOrders(this);
	}

	public boolean checkDisconectedTimer(String name) {
		for (int i = 0; i < userMessage.size(); i++) {
			if (name.equals(userMessage.get(i).getUsername())) {
				if (System.currentTimeMillis() - userMessage.get(i).getTime() < 30000) {
					return false;
				} else {

					userMessage.remove(i);
					return true;
				}

			}
		}

		return true;
	}

	public void setVariableTesting(long value) {
		testingVariable = value;
	}

	public long getTime() {
		return System.currentTimeMillis() + testingVariable;
	}

	@Override
	public boolean hasNextMessage() {
		return decoratedServerComm.hasNextMessage();
	}

	@Override
	public void sendOrder(String receiversNickname, Order order) {
		decoratedServerComm.sendOrder(receiversNickname, order);
	}

	@Override
	public void sendError(String toNickname, String error) {
		decoratedServerComm.sendError(toNickname, error);
	}

	@Override
	public boolean clientIsConnected(String nickname) {
		return decoratedServerComm.clientIsConnected(nickname);
	}

	@Override
	public void disconnectClient(String nickname) {
		decoratedServerComm.disconnectClient(nickname);
	}
}
