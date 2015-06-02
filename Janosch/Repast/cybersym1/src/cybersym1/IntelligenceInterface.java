package cybersym1;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class IntelligenceInterface {
	private static final boolean print = false;
	

	
	public static int rateAction(DeliverResource action, int typeRating) {
		//TODO: Hardcoded evaluation period
		Agent requester = action.getRequester();
		int charged = (requester.getCharged()) ? 0 : 1;	
		int timeRating = (int) ((1-(requester.getHoursTillDeadline() / 72.0)) * 100 * charged);
		
		double distance = Math.sqrt( (double) action.getWaitCounter()/action.getNrLinks()) * 3.725; 
		int distanceRating = (int) distance;
		int rating = ((distanceRating + typeRating) / 2) + timeRating + 1;
		
		if (print) System.out.println("Time rating = " + timeRating);
		if (print) System.out.println("Distance rating = " + distanceRating);
		if (print) System.out.println("Type Rating: " + typeRating);
		if (print) System.out.println("Total: " + rating);
		
		action.setRating(rating);
		return rating;
	}
	
	public static int rateAction(DeliverProduct action, int typeRating) {
		//TODO: Hardcoded evaluation period
		Agent requester = action.getRequester();
		int charged = (requester.getCharged()) ? 0 : 1;	
		int timeRating = (int) ((1-(requester.getHoursTillDeadline() / 72.0)) * 100 * charged);
		
		double distance = Math.sqrt( (double) action.getWaitCounter()/action.getNrLinks()) * 3.725; 
		int distanceRating = (int) distance;
		int rating = ((distanceRating + typeRating) / 2) + timeRating + action.getProduct().size();
		
		if (print) System.out.println("Time rating = " + timeRating);
		if (print) System.out.println("Distance rating = " + distanceRating);
		if (print) System.out.println("Type Rating: " + typeRating);
		if (print) System.out.println("Total: " + rating);
		
		action.setRating(rating);
		return rating;
	}
	
	public static int rateAction(ExtractResource action, Agent agent, int typeRating) {
		//TODO: Hardcoded evaluation period
		Agent requester = action.getRequester();
		int charged = (requester.getCharged()) ? 0 : 1;	
		int timeRating = (int) ((1-(requester.getHoursTillDeadline() / 72.0)) * 100 * charged);
		
		int availabilityRating = evaluateSourcesFor(action.getResource(), agent);		
		if (availabilityRating < 0 && action.getNrLinks() != 0) {
			availabilityRating = availabilityRating - (int)(-Math.sqrt(action.getWaitCounter()/action.getNrLinks()) * 7.5 + 100);
		}		
		int rating = (availabilityRating + typeRating) / 2 + timeRating;
		
		if (print) System.out.println("Time rating = " + timeRating);
		if (print) System.out.println("Availability rating = " + availabilityRating);
		if (print) System.out.println("Type Rating: " + typeRating);
		if (print) System.out.println("Total: " + rating);
		
		action.setRating(rating);
		return rating;
	}
	
	public static int rateAction(AssembleProduct action, Agent agent, int typeRating) {
		//TODO: Hardcoded evaluation period
		Agent requester = action.getRequester();
		int charged = (requester.getCharged()) ? 0 : 1;	
		int timeRating = (int) ((1-(requester.getHoursTillDeadline() / 72.0)) * 100 * charged);
		
		Agent receiver = action.getReceiver();
		int receiverRating = 0;
		if (receiver == agent) {
			List<Character> product = action.getProduct();
			for (List<Character> wish : agent.getWishList()) {
				if (Collections.indexOfSubList(wish,product) != -1) {
					receiverRating = (int) ( (double) wish.size() / product.size()) * 100;
				}
			}
		} else {
			receiverRating = (int) (Math.sqrt( (double) action.getWaitCounter()/action.getNrLinks()) * 3.725); 		
		}
		if (print) System.out.println("Receiver rating = " + receiverRating);
		
		int rating = (receiverRating + typeRating) / 2 + timeRating;
		if (print) System.out.println("Product Delivery Rating: " + rating);
			
		action.setRating(rating);
		return rating;
	}
	
	public static int evaluateSourcesFor(char resource, Agent agent) {
		int regeneration = 0;
		int extraction = 0;
		int quantity = 0;
		for (Source source : agent.getSourcesFor(resource)) {
			// TODO: Hardcoded interval
			double temp = (72.0 / source.getRegenerationPeriod()) - source.getNrExtractionsInPeriod();
			if (print) System.out.println("Temp reg for source is = " + (int) temp);
			regeneration += (int) temp;
			extraction += source.getNrExtractionsInPeriod();
			quantity += source.getQuantity();
		}
		double fraction = ((double) (regeneration - extraction) / (double) (quantity + extraction - regeneration)) * 100;
		if (print) System.out.println("Source Rating is: " + fraction);
		return (int) fraction; 
	}
	
	public static int evaluateSource(Source source) {
		double fraction = ((72.0 / source.getRegenerationPeriod()) - source.getNrExtractionsInPeriod()) /
					(source.getQuantity() + source.getNrExtractionsInPeriod() -	(72.0 / source.getRegenerationPeriod()));
		if (print) System.out.println("Source Rating is: " + fraction);
		return (int) fraction;
	}
	
	public static int getRatingForItemType(List<Character> itemType, Agent agent) {
		if (itemType.size() == 1) {
			double fraction =( (double) agent.getResourceRequestFor(itemType.get(0)) / 
					agent.getTotalResourceRequest()) * 100;
			//if (print) System.out.println("Type Rating is: " + fraction);
			return (int) fraction;
		} else {
			double fraction =( (double) agent.getProductRequestFor(itemType) / agent.getTotalProductRequest()) * 100;
			//if (print) System.out.println("Type Rating is: " + fraction);
			return (int) fraction;
		}
		
	}
	
	// TODO: Intelligence parameters for action selection. Now it is based on Action rating.
	public static AgentAction getBestAction(LinkedList<AgentAction> availableActions) {
		AgentAction bestAction = new Wait();
		for (AgentAction action : availableActions) {
			if (action.getRating() > bestAction.getRating()) bestAction = action;
		}
		return bestAction;
	}
	
	
	
	
}
