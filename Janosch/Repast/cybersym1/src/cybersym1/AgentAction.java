package cybersym1;

import java.util.List;

public abstract class AgentAction {
	protected int cost;
	protected int rating;
	protected Request request;
	
	public int getRating() {
		return rating;
	}
	
	public int getCost() {
		return cost;
	}
	
	public Agent getReceiver() {
		return request.getLastLink();
	}
	
	public Agent getRequester() {
		return request.getRequester();
	}
	
	public int getNrLinks() {
		return request.getNrLinks();
	}
	
	public int getWaitCounter() {
		return request.getWaitCounter();
	}
	
	public void setRating(int rating) {
		this.rating = rating;
	}
	
	public void setCost(int cost) {
		this.cost = cost;
	}
}

class Wait extends AgentAction {
	
	Wait() {
		rating = -1;
		cost = 0;
	}
}

class FulfillWish extends AgentAction {
	private List<Character> wish;
	
	FulfillWish(List<Character> wish) {
		this.wish = wish;
		cost = 0;		
	}
	
	public List<Character> getWish() {
		return wish;
	}
}

abstract class AssembleProduct extends AgentAction {
	protected List<Character> product;
	
	public List<Character> getProduct() {
		return product;
	}
}

class AssembleProductR2 extends AssembleProduct {
	private char resource1;
	private char resource2;
	
	AssembleProductR2(char resource1, char resource2, Request request) {
		this.resource1 = resource1;
		this.resource2 = resource2;
		this.request = request;
		this.product = request.getRequest();
		cost = 1;
	}
	
	public char getFirstResource() {
		return resource1;
	}
	
	public char getSecondResource() {
		return resource2;
	}
}

class AssembleProductPR extends AssembleProduct {
	private List<Character> part;
	private char resource;
	private boolean front;
	
	AssembleProductPR(List<Character> part, char resource, boolean front, Request request) {
		this.part = part;
		this.resource = resource;
		this.front = front;
		this.request = request;
		this.product = request.getRequest();
		cost = 1;
	}
	
	public List<Character> getPart() {
		return part;
	}

	public char getResource() {
		return resource;
	}
	
	public boolean getPosition() {
		return front;
	}
}

class AssembleProductP2 extends AssembleProduct {
	private List<Character> part1;
	private List<Character> part2;
	
	AssembleProductP2(List<Character> part1, List<Character> part2, Request request) {
		this.part1 = part1;
		this.part2 = part2;
		this.request = request;
		this.product = request.getRequest();
		cost = 1;
	}
	
	public List<Character> getFirstPart() {
		return part1;
	}

	public List<Character> getSecondPart() {
		return part2;
	}
}

abstract class Delivery extends AgentAction {
	protected Agent sender;
	protected Agent receiver;
	
	public Agent getSender() {
		return sender;
	}
	
	public Agent getReceiver() {
		return receiver;
	}
}

class DeliverResource extends Delivery {
	private char resource;
	
	DeliverResource(Agent sender, char resource, Request request) {
		this.sender = sender;
		this.resource = resource;
		this.request = request;
		this.receiver = request.getLastLink();
		cost = 1;
	}
	
	public char getResource() {
		return resource;
	}
}

class DeliverProduct extends Delivery {
	private List<Character> product;
	
	DeliverProduct(Agent sender, List<Character> product, Request request) {
		this.sender = sender;			
		this.product = product;
		this.request = request;
		this.receiver = request.getLastLink();
		this.cost = 1;
	}
	
	public List<Character> getProduct() {
		return product;
	}
}

class ExtractResource extends Delivery {
	private char resource;
	private Source source;
	
	ExtractResource(Agent sender, Source source, Request request) {
		this.sender = sender;
		this.source = source;
		this.resource = source.getResourceType();
		this.request = request;
		this.receiver = request.getLastLink();
		this.cost = 1;
	}
	
	public char getResource() {
		return resource;
	}
	
	public Source getSource(){
		return source;
	}
}






