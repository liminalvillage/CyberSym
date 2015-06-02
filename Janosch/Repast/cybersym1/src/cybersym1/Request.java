package cybersym1;

import java.util.List;

import repast.simphony.essentials.RepastEssentials;

public class Request {
	private String ID;
	private List<Character> request;
	private Agent requester;
	private int createdAt;
	private boolean isResource;
	private Agent lastLink;
	private int nrLinks;
	
	Request(List<Character> request, Agent requester) {
		this.request = request;
		this.requester = requester;
		if (request.size() > 1) this.isResource = false;
			else this.isResource = true;		
		this.lastLink = requester;
		this.nrLinks = 0;
		this.ID = requester.toString() + "R:" + request.toString();
		this.createdAt = (int) RepastEssentials.GetTickCount();
	}
	
	Request(Request neighborRequest, Agent neighbor) {
		this.request = neighborRequest.getRequest();
		this.requester = neighborRequest.getRequester();
		this.createdAt = neighborRequest.getCreatedAt();
		if (neighborRequest.isResource) this.isResource = true;
			else this.isResource = false;
		this.nrLinks = neighborRequest.nrLinks+1;
		this.lastLink = neighbor;
		this.ID = neighborRequest.getID();
	}
	
	public Agent getLastLink() {
		return lastLink;
	}
	
	public int getNrLinks() {
		return nrLinks;
	}
	
	public boolean isResource() {
		return isResource;
	}

	public List<Character> getRequest(){
		return request;
	}
	
	public Agent getRequester() {
		return requester;
	}
	
	public int getCreatedAt() {
		return createdAt;
	}
	
	public String getID() {
		return ID;
	}
	
	public int getWaitCounter() {
		return (int) RepastEssentials.GetTickCount() - createdAt;
	}
}
