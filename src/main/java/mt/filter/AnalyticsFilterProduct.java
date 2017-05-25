package mt.filter;


public class AnalyticsFilterProduct {
	private int buyOrders = 0;
	private int sellOrders = 0;
	private long timer = 0;

	public int getBuyOrders() {
		return buyOrders;
	}

	public void setBuyOrders(int buyOrders) {
		this.buyOrders = buyOrders;
	}

	public int getSellOrders() {
		return sellOrders;
	}

	public void setSellOrders(int sellOrders) {
		this.sellOrders = sellOrders;
	}

	public void setTimer(long timer) {
		this.timer = timer;
	}

	/**
	* This method keeps track of buy/sell orders and prints to console every minute
	*/
	public void averageBuySellOrders(AnalyticsFilter analyticsFilter) {
		if (is1minuteLong(analyticsFilter)) {
			System.out.println(
					"Buy Orders per minute: " + buyOrders + " .\nSell Orders per minute: " + sellOrders + " .");
			timer = System.currentTimeMillis();
			buyOrders = 0;
			sellOrders = 0;
		}
	}

	public boolean is1minuteLong(AnalyticsFilter analyticsFilter) {
		return timer + 60000 <= analyticsFilter.getTime();
	}
}