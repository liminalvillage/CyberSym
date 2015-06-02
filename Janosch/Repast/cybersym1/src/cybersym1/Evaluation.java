package cybersym1;

public class Evaluation {
	
	private static int productionCost;
	private static int costRatio;
	private static int wishesFulfilled;
	
	public Evaluation() {
		productionCost = 0;
		costRatio = 0;
		wishesFulfilled = 0;
	}
	
	public static void increaseProduction(int cost, int ratio) {
		wishesFulfilled++;
		productionCost += cost;
		costRatio += ratio;
	}

	public static int getProductionCost() {
		return productionCost;
	}

	public static int getCostRatio() {
		return costRatio;
	}

	public static int getWishesFulfilled() {
		return wishesFulfilled;
	}

	public int getMeanCost() {
		double meanCost = (double) getProductionCost() / getWishesFulfilled();
		return (int) meanCost;
	}
	
	public int getMeanRatio() {
		double meanRatio = (double) getCostRatio() / getWishesFulfilled();
		return (int) meanRatio;
	}
}
