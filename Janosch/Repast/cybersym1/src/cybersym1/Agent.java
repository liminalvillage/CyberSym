package cybersym1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;

/**
 * <h1>Agent</h1>
 * 
 * Instances of the Agent class represent the actual Agents of the CyberSym simulation. 
 * 
 * @author Janosch Haber, 10400192, University of Amsterdam (UvA)
 * @date 18-05-2015
 */
public class Agent {
	private static final boolean print = false;
	
	// Representation of the Environment (continuous + rasterized)
	private Grid<Object> grid;
	GridPoint pt;	
	
	// Items held by the individual Agent instance
	private Inventory inventory = new Inventory();
	
	private Hashtable<List<Character>, Integer> wishlist;
	Hashtable<List<Character>, Integer> partialDemand;
	private Hashtable<List<Character>, Request> internalJobList;
	private Hashtable<List<Character>, Hashtable<String,Request>> jobList;	
	
	private int hoursTillDeadline;
	private int workingHoursLeft;
	
	private boolean charged = false;
	private int wishesFulfilled = 0;
	private int deliveredItems = 0;
	private int extractedResources = 0;
	private int assembledProducts = 0;
	private int credit = 0;
	
	// Information about the neighborhood
	private Set<Agent> reachableAgents;
	private Hashtable<Character, LinkedList<Source>> reachableSources;
	
	private Hashtable<Character, Integer> availableResources;
	private Hashtable<List<Character>, Integer> requestRegister;
	private int totalResourceRequest = 0;
	private int totalProductRequest = 0;
	
	/**
	 * Agent constructor
	 * @param grid The rasterized environment representation
	 */
	public Agent(Grid<Object> grid) {
		this.grid = grid;
		wishlist = new Hashtable<List<Character>, Integer>();
		jobList = new Hashtable<List<Character>, Hashtable<String,Request>>();
		reachableAgents = new HashSet<Agent>();
		generateNewWish();
		workingHoursLeft = CybersymBuilder.getMaxWorkloadPerDay();	
		charged = false;
		wishesFulfilled = 0;
		deliveredItems = 0;
		extractedResources = 0;
		assembledProducts = 0;
		hoursTillDeadline = CybersymBuilder.getDeadlinePeriod();
	}
	
	/**
	 * Initializes the agent
	 */
	public void initialize() {
		pt = grid.getLocation(this);
	}
	
	/**
	 * Scheduled method to invoke the Agent's behavior. 
	 * Evaluates the environment, determines the individual agent's behavior and updates the 
	 */
	@ScheduledMethod(start=1, interval=1)
	public void step() {
		if(workingHoursLeft > 0) {
			AgentAction agentAction;
			if (print) System.out.println("----------------------------------------------------------------");	
			if (print) System.out.println("Working hours: " + workingHoursLeft + "/" + CybersymBuilder.getRequirementNewDemand());
			if (print) System.out.println("Agent wish(es): ");
			if (print) System.out.println(wishlist.keySet().toString());
			if (print) System.out.println("Agent Part Inventory is ");
			if (print) inventory.printProductInventory(); 
			if (print) System.out.println("Agent Resource Inventory is "); 
			if (print) inventory.printResourceInventory(); 
			if (print) System.out.println("Initializing step...");				
			initializeStep();			
			
			if (print) System.out.println("Determining best action...");	
			agentAction = determineAction();
			if (print) System.out.println("Best action is " + agentAction.getClass());	
			if (agentAction.getCost() <= workingHoursLeft) {
				if (print) System.out.println("Performing action...");	
				performAction(agentAction);
				if (print) System.out.println("Action cost " + agentAction.getCost() + " hour(s)." );
				workingHoursLeft -= agentAction.getCost();
			}
		}	
		if (credit > CybersymBuilder.getRequirementNewDemand()-1) {
			generateNewWish();
			credit -= CybersymBuilder.getRequirementNewDemand();
		}
		hoursTillDeadline--;
	}	
	
	public void initializeStep() {
		setReachableAgents();
		setReachableSources();
		generateExternalJobList();
		generateIntenralJobList();
		mergeJobLists();		
		evaluateRequests();
	}	
	
	//TODO Hardcoded days
	@ScheduledMethod(start=24, interval=24)
	public void newDay() {
		//System.out.println("A new day! Let's get some work done.");
		workingHoursLeft = CybersymBuilder.getMaxWorkloadPerDay(); 
	}
	
	//TODO Hardcoded evaluation interval
	@ScheduledMethod(start=144, interval=72)
	public void judgementDay() {	
		if (!charged) {
			@SuppressWarnings("unchecked")
			Context<Object> context = ContextUtils.getContext(this);
			context.remove(this);
			System.out.println("Agent could not recharge during last period. Removed from grid.");
		} else {
			charged = false;
			System.out.println("Agent has had " + wishesFulfilled + " wished fulfilled.");
			wishesFulfilled = 0;
			deliveredItems = 0;
			extractedResources = 0;
			hoursTillDeadline = CybersymBuilder.getDeadlinePeriod();
		}
	}
		
	/**
	 * Sets an Agent's field {@code reachableAgents} to a set of other Agents that are in the
	 * Agent's neighborhood   
	 */
	public void setReachableAgents() {	
		GridCellNgh<Agent> nghCreatorAgents = new GridCellNgh<Agent>(grid,pt,Agent.class,
				CybersymBuilder.getAgentScopeAgents(),CybersymBuilder.getAgentScopeAgents());
		List<GridCell<Agent>> closeAgents = nghCreatorAgents.getNeighborhood(true);
		reachableAgents = new HashSet<Agent>();
		for (GridCell<Agent>cell : closeAgents) {
			for (Agent agent : cell.items()) {
				reachableAgents.add(agent);
			}
		}		
	}
	
	public void setReachableSources() {
		reachableSources = new Hashtable<Character, LinkedList<Source>>();
		GridCellNgh<Source> nghCreatorSources = new GridCellNgh<Source>(grid,pt,Source.class,
				CybersymBuilder.getAgentScopeSources(),CybersymBuilder.getAgentScopeSources());
		List<GridCell<Source>> neighborhoodSources = nghCreatorSources.getNeighborhood(true);		
	
		for (GridCell<Source> cell : neighborhoodSources) {
			for (Source source : cell.items()) {
				char resource = source.getResourceType();
				if (reachableSources.containsKey(resource)) {
					LinkedList<Source> sources = reachableSources.get(resource);
					sources.add(source);
					reachableSources.put(resource, sources);
				} else {
					LinkedList<Source> sources = new LinkedList<Source>(); 
					sources.add(source);
					reachableSources.put(resource, sources);
				}
			}
		}	
		availableResources = new Hashtable<Character, Integer>();
		Set<Character> resourceTypes = reachableSources.keySet();
		for (Character resource : resourceTypes) {
			LinkedList<Source> sources = reachableSources.get(resource);
			int totalChange = 0;
			for (Source source : sources) {
				totalChange -= source.getNrExtractionsInPeriod();
				totalChange += (int) ((double) CybersymBuilder.getDeadlinePeriod()/source.getRegenerationPeriod());
			}
			availableResources.put(resource, totalChange);
		}
	}	

	public void generateExternalJobList() {
		jobList = new Hashtable<List<Character>, Hashtable<String,Request>>();
		for (Agent neighbor : reachableAgents) {
			Hashtable<List<Character>, Hashtable<String,Request>> neighborJobList = 
					neighbor.getJobList();
			
			Set<List<Character>> jobListkeySet = neighborJobList.keySet();
		    Iterator<List<Character>> it1 = jobListkeySet.iterator();		    
		    while (it1.hasNext()) {
		    	List<Character> key = it1.next();
		    	Hashtable<String,Request> neighborJobs = neighborJobList.get(key);
		    	Hashtable<String,Request> storedJobs;
		    	if (jobList.containsKey(key)) storedJobs = jobList.get(key);
		    	else storedJobs = new Hashtable<String,Request>();
		    		
		    	Set<String> jobsKeySet = neighborJobs.keySet();
			    Iterator<String> it2 = jobsKeySet.iterator();			    
			    while (it2.hasNext()) {
			    	String ID = it2.next();
			    	Request neighborRequest = neighborJobs.get(ID);
			    	if (validateRequest(neighborRequest)) {
			    		Request newJob = new Request(neighborRequest, neighbor);				    					
						if (storedJobs.containsKey(ID)) {
							Request job = storedJobs.get(ID);
							if (newJob.getNrLinks() < job.getNrLinks()) {
								storedJobs.put(ID, newJob);
							}
						} else storedJobs.put(ID, newJob);
			    	}		
			    }		
			    if (!storedJobs.isEmpty()) jobList.put(key, storedJobs);
		    }
		}
	}
	
	public void addPartialDemand(List<Character> key) {
		if (partialDemand.containsKey(key)) {
			int counter = partialDemand.get(key);
			partialDemand.put(key, counter++);
		} else partialDemand.put(key, 1);
	}
	
	public void removePartialDemand(List<Character> key) {
		if (partialDemand.containsKey(key)) {
			int counter = partialDemand.get(key);
			if (counter > 1) partialDemand.put(key, counter--);
			else partialDemand.remove(key);
		}
	}	
	
	public void generateIntenralJobList() {
		partialDemand = new Hashtable<List<Character>, Integer>();
		
		for (List<Character> wish : wishlist.keySet()) {			
			List<Character> part;  
		    for (int start=0; start < wish.size(); start++) {
		        for (int end=1; end <= wish.size()-start; end++) {
		        	part = wish.subList(start, start+end);
		        	addPartialDemand(part);
		        }	         
		    }
		}
		for (List<Character> itemInInventory : inventory.getProductList()) {
			List<Character> part;  
			for (int start=0; start < itemInInventory.size(); start++) {
		        for (int end=1; end <= itemInInventory.size()-start; end++) {
		        	part = itemInInventory.subList(start, start+end);
		        	removePartialDemand(part);
		        }
		    }
		}
		for (char itemInInventory : inventory.getResourceList()) {
			List<Character> part = Arrays.asList(itemInInventory);
			removePartialDemand(part);
		}
			
		internalJobList = new Hashtable<List<Character>, Request>();
		for (List<Character> demand : partialDemand.keySet()) {
			// ONE Request for each jobType! Necessary for tracing of Requests.
			Request newJob = new Request(demand, this);
			internalJobList.put(demand, newJob);		
		}
	}
	
	public void mergeJobLists() {
		for (List<Character> key : internalJobList.keySet()) {
			Hashtable<String,Request> storedJobs;
			if (jobList.containsKey(key)) storedJobs = jobList.get(key);
	    	else storedJobs = new Hashtable<String,Request>();
			Request newJob = internalJobList.get(key);
			storedJobs.put(newJob.getID(), newJob);
			jobList.put(key, storedJobs);
		}
	}
	
	public boolean validateRequest(Request job) {
		Agent requester = job.getRequester();
		if (requester.hasRequest(job)) {
			return true;
		} else {
			// if (print) System.out.println("Request is no longer active. Removing it from jobList.");
			return false;
		}
	}
	
	public boolean hasRequest(Request request) {
		if (internalJobList.containsKey(request.getRequest())) return true;
		else return false;
	}
	
	public void evaluateRequests() {
		requestRegister = new Hashtable<List<Character>, Integer>();
		totalResourceRequest = 0;
		totalProductRequest = 0;
		Set<List<Character>> jobListkeySet = jobList.keySet();
	    Iterator<List<Character>> it = jobListkeySet.iterator();		    
	    while (it.hasNext()) {
	    	List<Character> key = it.next();
	    	int counter = jobList.get(key).size();
	    	requestRegister.put(key, counter);
	    	if (key.size() == 1) {
	    		totalResourceRequest += counter;
	    	} else {
	    		totalProductRequest += counter;
	    	}
	    }
	}
	
	public boolean jobListContains (Request request) {
		if (jobList.containsKey(request.getRequest())) {
			Hashtable<String,Request> jobs = jobList.get(request.getRequest());
			String ID = request.getID();
			if (jobs.containsKey(ID)) return true;		
		} 
		return false;		
	}
	
	public int getJobListSize() {
		Set<List<Character>> keySet = jobList.keySet();
	    Iterator<List<Character>> it = keySet.iterator();
	    int counter = 0;
	    while (it.hasNext()) {
	    	counter += jobList.get(it.next()).size();
	    }
	    return counter;
	}
		
	public AgentAction determineAction() {		
		// OPTION 1: The Agent's inventory contains one or more products that fulfill its wishes. 
		// The Agent can consume this products to recharge. Recharging is always free.
		ArrayList<FulfillWish> consumableProducts = getConsumableProducts();
		if (consumableProducts.size() > 0) return consumableProducts.get(0);
		
		// OPTION 2: The Agent investigates internal and external job requests to find the best 
		// available option given its situation
		LinkedList<AgentAction> bestActions = new LinkedList<AgentAction>();	
		bestActions.add(new Wait());
		if (print) System.out.println("Evaluating " + jobList.keySet().size() + " requests.");
		for(List<Character> itemType : jobList.keySet()) {
			int typeRating = IntelligenceInterface.getRatingForItemType(itemType, this);
			AgentAction bestActionForJobType = determineBestJob(itemType, typeRating);
			if (!(bestActionForJobType instanceof Wait)) bestActions.add(bestActionForJobType);
		}	
		if (print) System.out.println("Agent can choose from " + bestActions.size() + " possible actions");
		return IntelligenceInterface.getBestAction(bestActions);	
	}
	
	public AgentAction determineBestJob(List<Character> itemType, int typeRating) {
		//if (print) System.out.println("Checking requests for " + itemType);
		AgentAction bestAction = new Wait();
		
		Hashtable<String, Request> possibleJobs = jobList.get(itemType);		
		//System.out.println(possibleJobs.values().size() + " requests for this type. Finding best...");
		for (Request job : possibleJobs.values()) {						
			//OPTION 1: A neighbor requests a Resource 
			if (job.getRequester() != this && job.isResource()){
				char resource = job.getRequest().get(0);
				//OPTION 1A: The requested Resource is in the Agent's inventory		
				if (inventory.contains(resource)) {
					DeliverResource action = new DeliverResource(this, resource, job);					
					int score = IntelligenceInterface.rateAction(action, typeRating);
					if (print) System.out.println("1A Delivery of " + resource + ". Rating: " + score);
					if (action.getRating() > bestAction.getRating()) bestAction = action;
				//OPTION 1B: The requested Resource is available in the Agent's neighborhood
				} else if (availableResources.containsKey(resource)) {					
					Source source = getBestSource(resource);
					ExtractResource action = new ExtractResource(this, source, job);
					int score = IntelligenceInterface.rateAction(action, this, typeRating);
					if (print) System.out.println("1B Extraction of " + resource + " for neighbor. Rating: " + score);
					if (action.getRating() > bestAction.getRating()) bestAction = action;
				}
				
			//OPTION 2: A neighbor requests a Product
			} else if (job.getRequester() != this && !job.isResource()) {
				List<Character> product = job.getRequest();
				//OPTION 2A: The requested Product is in the Agent's inventory		
				if (inventory.contains(product)) {					
					DeliverProduct action = new DeliverProduct(this, product, job);
					int score = IntelligenceInterface.rateAction(action, typeRating);
					if (print) System.out.println("2A: Delivery of " + product.toString() + ". Rating: " + score);
					if (action.getRating() > bestAction.getRating()) bestAction = action;
				//OPTION 2B: The requested Product can be assembled from parts in the Agent's inventory		
				} else {
					List<AssembleProduct> combinations = findPossibleCombinations(job);					
					for (AssembleProduct action : combinations) {
						int score = IntelligenceInterface.rateAction(action, this, typeRating);
						if (print) System.out.println("2B: Assembly of " + product.toString() + " for neighbor. Rating: " + score);	
						if (action.getRating() > bestAction.getRating()) bestAction = action;
					}					
				}				
			//OPTION 3: The Agent requests a Resource that is available in its neighborhood
			} else if (job.getRequester() == this && job.isResource() 
						&& availableResources.containsKey(job.getRequest().get(0))){
				
				Source source = getBestSource(job.getRequest().get(0));
				ExtractResource action = new ExtractResource(this, source, job);
				int score = IntelligenceInterface.rateAction(action, this, typeRating);
				if (print) System.out.println("3: Extraction of " + job.getRequest().get(0) + ". Rating: " + score);
				if (action.getRating() > bestAction.getRating()) bestAction = action;
				
			//OPTION 4: The Agent requests a Product that can be assembled from parts in the inventory
			} else if (job.getRequester() == this && !job.isResource()) {
				List<AssembleProduct> combinations = findPossibleCombinations(job);
				//else System.out.println("OPTION 5: No action possible for this request.");
				for (AssembleProduct action : combinations) {
					int score = IntelligenceInterface.rateAction(action, this, typeRating);
					if (print && combinations.size() > 0) System.out.println("4: Assembly of " + action.getProduct().toString() + ". Rating: " + score);
					if (action.getRating() > bestAction.getRating()) bestAction = action;
				}
			} //else if (print) System.out.println("OPTION 5: No action possible for this request.");
		}		
		return bestAction;
	}

	public List<AssembleProduct> findPossibleCombinations(Request request) {
		List<AssembleProduct> possibleCombinations = new ArrayList<AssembleProduct>();
		List<Character> product = request.getRequest();
		//if (print) System.out.println("Product is " + product.toString());
		for (int i=1; i < product.size(); i++) {
			boolean p1r = false, p2r = false;
			List<Character> part1 = product.subList(0, i);
			//if (print) System.out.println("Part1 is " + part1.toString());
			if (inventory.contains(part1)) {
				List<Character> part2 = product.subList(i, product.size());
				//if (print) System.out.println("Part2 is " + part2.toString());
				if (inventory.contains(part2)) {
					if (!part1.equals(part2) || inventory.getNrOf(part1) > 1) {
						//if (print) System.out.println("Both are in inventory!");
						if (part1.size() == 1) p1r = true;
						if (part2.size() == 1) p2r = true;
						
						if (p1r && p2r) {
							//if (print) System.out.println("Request for " + product.toString() + 
							//		" can be fulfilled through combination of " + 
							//		part1.toString() + " + " + part2.toString());
							possibleCombinations.add(new AssembleProductR2(
									part1.get(0), part2.get(0), request));
						} else if (p1r && !p2r) {
							//if (print) System.out.println("Request for " + product.toString() + 
							//		" can be fulfilled through combination of " + 
							//		part1.toString() + " + " + part2.toString());
							possibleCombinations.add(new AssembleProductPR(
									part2, part1.get(0), true, request));
						} else if (!p1r && p2r) {
							//if (print) System.out.println("Request for " + product.toString() + 
							//		" can be fulfilled through combination of " + 
							//		part1.toString() + " + " + part2.toString());
							possibleCombinations.add(new AssembleProductPR(
									part1, part2.get(0), false, request));
						} else if (!p1r && !p2r) {
							//if (print) System.out.println("Request for " + product.toString() + 
							//		" can be fulfilled through combination of " + 
							//		part1.toString() + " + " + part2.toString());
							possibleCombinations.add(new AssembleProductP2(
									part1, part2, request));
						}						
					}
				}
	        }	         
	    }
		return possibleCombinations;
	}
	
	public Source getBestSource(char resource) {
		Source bestSource = null;
		int bestScore = -1;
		LinkedList<Source> availableSources = reachableSources.get(resource);
		for (Source availableSource : availableSources) {
			int currentScore = IntelligenceInterface.evaluateSource(availableSource);
			if (bestSource == null || currentScore > bestScore) {
				bestSource = availableSource;
				bestScore = currentScore;
			}
		}
		return bestSource;
	}
	

		
	// XXX ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %%
	
	public int performAction(AgentAction action) {
		if (action instanceof AssembleProductR2) return performAction((AssembleProductR2) action);
		if (action instanceof AssembleProductPR) return performAction((AssembleProductPR) action);
		if (action instanceof AssembleProductP2) return performAction((AssembleProductP2) action);
		if (action instanceof DeliverResource) return performAction((DeliverResource) action);
		if (action instanceof DeliverProduct) return performAction((DeliverProduct) action);
		if (action instanceof ExtractResource) return performAction((ExtractResource) action);
		if (action instanceof FulfillWish) return performAction((FulfillWish) action);
		if (action instanceof Wait) return performAction((Wait) action);
		return -1;		
	}
	
	public int performAction(AssembleProductR2 action) {
		char resourceType1 = action.getFirstResource();
		char resourceType2 = action.getSecondResource();	
		Resource resource1 = inventory.get(resourceType1);
		Resource resource2 = inventory.get(resourceType2);
		
		if (resource1 == null || resource2 == null) {
			System.out.println("ERROR: COULD NOT COMBINE RESOURCES!");
			return -1;
		}
		
		Product newPart = new Product(resource1, resource2);
		inventory.add(newPart);
		assembledProducts++;
		credit++;
		return action.getCost();
	} 
	
	public int performAction(AssembleProductPR action) {
		List<Character> part = action.getPart();
		char resourceType = action.getResource();			
		Resource resource = inventory.get(resourceType);
		Product product = inventory.get(part);	
		
		if (product == null || resource == null) {
			System.out.println("ERROR: COULD NOT COMBINE PRODUCT PART AND RESOURCE!");
			return -1;
		}
		
		boolean front = action.getPosition();		
		product.addResource(resource, front);
		inventory.add(product);
		assembledProducts++;
		credit++;
		return action.getCost();
	} 
	
	public int performAction(AssembleProductP2 action) {
		List<Character> part1Type = action.getFirstPart();
		List<Character> part2Type = action.getSecondPart();			
		Product product = inventory.get(part1Type);
		Product part2 = inventory.get(part2Type);	
		
		if (product == null || part2 == null) {
			System.out.println("ERROR: COULD NOT COMBINE PRODUCTS!");
			return -1;
		}
		
		product.addProduct(part2);
		inventory.add(product);	
		assembledProducts++;
		credit++;
		return action.getCost();

	}
	
	public int performAction(DeliverResource action) {
		char resourceType = action.getResource();
		Agent receiver = action.getReceiver();		
	    Resource resource = inventory.get(resourceType);
	    
	    if (resource == null || receiver == null) {
			System.out.println("ERROR: COULD NOT DELIVER RESOURCE!");
			return -1;
		}
	    
	    resource.increaseCost(action.getCost());
		receiver.addToInventory(resource);
		if (print) System.out.println("Delivered a Resource.");
		deliveredItems++;		
		credit++;
		return action.getCost();
	}
	
	public int performAction(DeliverProduct action) {
		List<Character> productType = action.getProduct();
		Agent receiver = action.getReceiver();		
		Product product = inventory.get(productType);	
		
		if (product == null || receiver == null) {
			System.out.println("ERROR: COULD NOT DELIVER PRODUCT!");
			return -1;
		}
		 
		product.increaseCost(action.getCost());
		receiver.addToInventory(product);
		if (print) System.out.println("Delivered a product.");
		deliveredItems++;
		credit++;
		return action.getCost();
	}
	
	public int performAction(ExtractResource action) {
		Source source = action.getSource();		
		Resource extractedResource = source.extractResource();	
		
		if (source == null || extractedResource == null) {
			System.out.println("ERROR: COULD NOT DELIVER PRODUCT!");
			return -1;
		}
		
		int extractionCost = action.getCost();
		extractedResource.increaseCost(extractionCost);	
		inventory.add(extractedResource);
		extractedResources++;		
		credit++;
		return extractionCost;	
	}
	
	public int performAction(FulfillWish action) {
		Product consumable = inventory.get(action.getWish());
		
		if (consumable == null) {
			System.out.println("ERROR: COULD NOT CONSUME PRODUCT!");
			return -1;
		}
		
		charged = true;
		wishesFulfilled += 1;
		int cost = consumable.getCost();
		int ratio = getCostRatio(consumable.getProduct(), cost);
		Evaluation.increaseProduction(cost, ratio);
		System.out.println("Wish for " + consumable.getProduct().toString() + " fulfilled. Cost " + 
				 cost + ", ratio of " + ratio + "%.");
		wishlist.remove(action.getWish());
		return action.getCost();
	}
	
	public int performAction(Wait action) {
		return action.getCost();
	}
	
	public void addToInventory(Product newProduct) {
		inventory.add(newProduct);
	}
	
	public void addToInventory(Resource newResource) {
		inventory.add(newResource);
	}
	
	public void generateNewWish() {		
		// TODO: Check list type ?!
		ArrayList<Character> newWish;
		do {
			 newWish = CybersymBuilder.possibleProducts.get(RandomHelper.nextIntFromTo(0, 
					 CybersymBuilder.getNrPossibleProducts()-1));
		} while (wishlist.containsKey(newWish));
		wishlist.put(newWish,0);	
	}
	
	// XXX GETTER FUNCTIONS %% GETTER FUNCTIONS %% GETTER FUNCTIONS %% GETTER FUNCTIONS %% GETTER FUNCTIONS %% GETTER FUNCTIONS %% GETTER FUNCTIONS %%		
	
	public int getCostRatio(List<Character> product, int cost) {
		double ratio = (cost / ((double) (2*product.size() -1))) * 100;
		return (int) ratio;
	}
	
	public Set<List<Character>> getWishList() {
		return wishlist.keySet();
	}
	
	public List<Source> getSourcesFor(char resource) {
		return reachableSources.get(resource);
	}
	
	public ArrayList<FulfillWish> getConsumableProducts() {
		ArrayList<FulfillWish> consumableProducts = new ArrayList<FulfillWish>();
		for (List<Character> wish : wishlist.keySet()) {
			if (inventory.contains(wish)) {
				consumableProducts.add(new FulfillWish(wish));
			}
		}		
		return consumableProducts;
	}
	
	public int getSourceStatus(char resource) {
		int regeneration = 0;
		int extraction = 0;
		int currentQuantity = 0;
		if (reachableSources.containsKey(resource)) {
			LinkedList<Source> sources = reachableSources.get(resource);			
			for (Source source : sources) {
				currentQuantity += source.getQuantity();
				regeneration += (int) (72 / source.getRegenerationPeriod());
				extraction += source.getNrExtractionsInPeriod();
			}
		}
		int status = (regeneration - extraction) / (currentQuantity + extraction - regeneration) * 100;
		return status;
	}
	/**
	 * Returns the set of Resource Types available to the Agent through Sources in his neighborhood
	 * @return The set of Resource Types available to the Agent through Sources in his neighborhood
	 */
	public Set<Character> getAvailableResourceTypes() {
		return availableResources.keySet();
	}
		
	public int getNrWishesFulfilled() {
		return wishesFulfilled;
	}
	
	public Hashtable<List<Character>, Hashtable<String,Request>> getJobList() {
		return jobList;
	}
	
	/**
	 * Returns the number of items (Resources and Product parts) that this agent delivered to its
	 * neighbors during the current production period
	 * @return The number of items (Resources and Product parts) that this agent delivered to its
	 * neighbors during the current production period
	 */
	public int getNrDeliveredItems() {
		return deliveredItems;
	}	
	
	public int getNrExtractedResources() {
		return extractedResources;
	}
	
	public int getNrAssembledProducts() {
		return assembledProducts;
	}
	
	public int getCentralityRating() {
		double rating = ((double) reachableAgents.size() / CybersymBuilder.getAgentCount()) * 100;
		return (int) rating;
	}
	
	public int getTotalNrOfActions() {
		return deliveredItems + extractedResources + assembledProducts;
	}
	
	public int getHoursTillDeadline() {
		return hoursTillDeadline;
	}
	
	public boolean getCharged() {
		return charged;
	}
	
	public int getResourceRequestFor(char resource) {
		//TODO: Check List type?!
		List<Character> key = new ArrayList<Character>();
		key.add(resource);
		if (requestRegister.containsKey(key)) {
			return requestRegister.get(key);
		} else return 0;
	}
	
	public int getProductRequestFor(List<Character> key) {
		if (requestRegister.containsKey(key)) {
			return requestRegister.get(key);
		} else return 0;
	}
	
	public int getTotalResourceRequest() {
		return totalResourceRequest;
	}
	
	public int getTotalProductRequest() {
		return totalProductRequest;
	}
	
	

	// XXX VISUALIZATION TOOLS %% VISUALIZATION TOOLS %% VISUALIZATION TOOLS %% VISUALIZATION TOOLS %% VISUALIZATION TOOLS %% VISUALIZATION TOOLS %%
	
	/**
	 * Returns a String to be printed on the Agent's label in the simulation visualization
	 * @return a String to be printed on the Agent's label in the simulation visualization
	 */
	public String getLabel(){
		String label = "C: " + getCentralityRating();
		int totalActions = getTotalNrOfActions();
		double extractions = ((double) extractedResources / totalActions) * 100;
		double deliveries = ((double) deliveredItems / totalActions) * 100;
		double assemblies = ((double) assembledProducts / totalActions) * 100;
		label += "% E: " + ((int) extractions);
		label += "% D: " + ((int) deliveries);
		label += "% A: " + ((int) assemblies) + "%";
		return label;		
	}
}