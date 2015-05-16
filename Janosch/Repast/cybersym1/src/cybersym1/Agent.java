package cybersym1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;

/**
 * <h1>Agent</h1>
 * 
 * Instances of the Agent class represent the actual Agents of the CyberSym simulation. 
 * Agents generate demands and aim to fulfill them in order to increase their lifespan. When their
 * {@code remainingTime} runs out, they are removed from the grid. 
 * In order to fulfill their demands, agents can make use of different actions like extracting 
 * Resources from their Sources, delivering Resources to other agents or combining Resources to
 * Products they can consume. Agents do not get a reward for completing these actions. 
 * At the current state, actions are determined by a hardcoded preference setting.
 * 
 * @author Janosch Haber, 10400192, University of Amsterdam (UvA)
 * @date 06-05-2015
 */
public class Agent {
	// Representation of the Environment (continuous + rasterized)
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	GridPoint pt;	
	// Items held by the individual Agent instance
	private int remainingTime;
	private Hashtable<Character,LinkedList<Resource>> resourceInventory;
	private Hashtable<List<Character>,LinkedList<Product>> productInventory;	
	// Information about the Agent's demands
	private LinkedList<Character> demand;
	private LinkedList<List<Character>> indirectPartDemand;
	private Hashtable<Character, Integer> indirectResourceDemand;
	private LinkedList<Character> resourceRequest;
	private Set<Character> availableResourceTypes;	
	// Information about other Agents in the neighborhood
	private Set<Agent> reachableAgents;
	private Hashtable<Character,LinkedList<Agent>> neighborResourceRequests;
	
	/**
	 * Agent constructor
	 * @param space The continuous environment representation
	 * @param grid The rasterized environment representation
	 * @param lifespan The initial lifespan of an individual agent
	 */
	public Agent(ContinuousSpace<Object> space, Grid<Object> grid, int lifespan) {
		this.space = space;
		this.grid = grid;
		this.remainingTime = lifespan;
		this.resourceRequest = new LinkedList<Character>();
		
		resourceInventory = new Hashtable<Character,LinkedList<Resource>>();
		productInventory = new Hashtable<List<Character>,LinkedList<Product>>();
		
		demand = CybersymBuilder.possibleProducts.get(RandomHelper.nextIntFromTo(0, 
				CybersymBuilder.nrPossibleProducts-1));
		generateIndirectDemand();
		//System.out.println("Agent Demand: " + getDemand());
	}
	
	/**
	 * Initializes the agent by evaluating its environment. (This cannot be done in the
	 * constructor as at that point in time not all agents in the environment are yet set)
	 */
	public void initialize() {
		pt = grid.getLocation(this);
		setReachableAgents();
	}
	
	/**
	 * Scheduled method to invoke the Agent's behavior. Evaluates the environment and
	 * determines the individual agent's behavior. After each tick, the {@code remainingTime} 
	 * of an agent is decreased by one. If it reaches zero, the Agent "dies" and is removed from the
	 * grid.
	 */
	@ScheduledMethod(start=1, interval=1)
	public void step () {	
		setReachableAgents();		
		//System.out.print("Agent Inventory: " + getNrResourcesInInventory() + " Resources and ");
		//System.out.println(getNrProductsInInventory() + " Products.");
		Hashtable<Character, LinkedList<Source>> availableResources = getAvailableResources(pt, true);
		setResourceRequest();
		//System.out.println("Agent needs " + resourceRequest.size() + " resources from other agents.");
		determineAction(availableResources);
		
		remainingTime--;
		if (remainingTime <= 0) {
			CybersymBuilder.removeInventoryFromRegister(this.resourceInventory);
			CybersymBuilder.removeArrayFromRegister(this.demand, "demand");
			@SuppressWarnings("unchecked")
			Context<Object> context = ContextUtils.getContext(this);
			context.remove(this);
			System.out.println("Agent died.");
		}
	}	

	/**
	 * Interface for determining an individual Agent's next action.
	 * At the current state, actions are determined by a hardcoded preference setting
	 * @param availableResources The list of Resources that are available in the Agent's 
	 * neighborhood and can be used to fulfill his demand
	 */
	public void determineAction(Hashtable<Character, LinkedList<Source>> availableResources) {
		//TODO: Interface for selection parameters / intelligence settings
		
		
		// OPTION 1: Consume a finished Product in order to increase the Agent's lifespan
		if (consumeProduct()) return;
		
		// OPTION 2: Extract a Resource from one of the sources in the neighborhood		
		if (!availableResources.isEmpty()) {
			//System.out.println("There are " + availableResources.size() + " needed resources available.");
			//System.out.println("Agent needs " + resourceRequest.size() + " resources from other agents.");
			Character extractedResource = extractResource(availableResources);
			if ( extractedResource != null) {
				// Update resource demand
				int oldAmount = indirectResourceDemand.get(extractedResource);
				if (oldAmount == 1) {
					indirectResourceDemand.remove(extractedResource);
				} else {
					indirectResourceDemand.put(extractedResource,oldAmount-1);
				}		
				return;
			}
		} 		
		
		// OPTION 3: Assemble a product from two basic Resources in the inventory
		List<Character> resourceSupply = new ArrayList<Character>(resourceInventory.keySet());	
		//System.out.println(resourceSupply.size() + " different Resource Types present.");
		LinkedList<List<Character>> possibleProducts =  generateCombinations(resourceSupply);
		//System.out.print("Agent can generate "); System.out.print(possibleProducts.size()); 
		//System.out.println(" possible partial products."); System.out.println(possibleProducts.toString());
		if (!possibleProducts.isEmpty()) {
			if (combineResources(possibleProducts))	return;
		}

		// OPTION 4: Add another basic Resource to a partial Product in the inventory
		LinkedList<List<Character>> productSupply = getProductList();
		possibleProducts = generateCombinations(resourceSupply, productSupply);
		//System.out.print("Agent can assemble "); System.out.print(possibleProducts.size()); 
		//System.out.println(" possible partial products."); System.out.println(possibleProducts.toString());		
		if (!possibleProducts.isEmpty()) {
			if (combinePartAndResource(possibleProducts)) return;
		}
		
		// OPTION 5: Combine two partial Products 
		HashMap<List<Character>, List<List<Character>>> possibleCombinations = 
				generateCombinations(productSupply);
		if (!possibleCombinations.isEmpty()) {
			if (combineParts(possibleCombinations)) return;
		}
		
		// OPTION 6: Deliver a requested Resource to another Agent in the neighborhood
		setNeighborResourceRequest();
		Set<Character> requestedResources = neighborResourceRequests.keySet();
		Hashtable<Character,LinkedList<Agent>> potentialReceivers = 
				new Hashtable<Character,LinkedList<Agent>>();
		for (char resource : requestedResources) {
			if (resourceInventory.containsKey(resource)) {
				potentialReceivers.put(resource, neighborResourceRequests.get(resource));
			}
		}	
		if (!potentialReceivers.isEmpty()) {
			List<Character> keySet = new ArrayList<Character>(potentialReceivers.keySet());
			char resourceType = keySet.get(RandomHelper.nextIntFromTo(0, keySet.size()-1));
			Resource resource = getFromInventory(resourceType);
			LinkedList<Agent> receivers = potentialReceivers.get(resourceType);
			Agent receiver = receivers.get(RandomHelper.nextIntFromTo(0,receivers.size()-1));
			if (deliverResource(resource, receiver)) return;
		}
				
		//OPTION 7: Extract a Resource requested by an Agent from the neighborhood
		availableResources = getAvailableResources(pt, false);
		//System.out.println("Agent can get " + availableResources.size() + " resources for other Agents.");
		if (!availableResources.isEmpty()) {
			//System.out.println("Extracting a resource for a neighbor.");
			if (extractResource(availableResources) != null) return;
		}	
	}
	
	// XXX ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %%
	
	/**
	 * Attempts to extract a demanded resource from the Sources in the environment. If it is 
	 * successful, a Resource object is created and added to the agent's resource inventory. The 
	 * cost of extracting the resource is subtracted from the agent's remaining lifespan.
	 * 
	 * @param availableResources A Hashtable with Resource demands and the available Sources thereof
	 * @return true if the resource could be extracted, false otherwise
	 */
	private Character extractResource(Hashtable<Character, LinkedList<Source>> availableResources) {
		//TODO! Chooses Resource and Source randomly. Implement interface to determine choice.
		List<Character> keySet = new ArrayList<Character>(availableResources.keySet());
		char resource = keySet.get(RandomHelper.nextIntFromTo(0,keySet.size()-1));
		LinkedList<Source> availableSources = availableResources.get(resource);
		Source source = availableSources.get(
				RandomHelper.nextIntFromTo(0,availableSources.size()-1));

		NdPoint spacePtSource = space.getLocation(source);
		NdPoint spacePtAgent = space.getLocation(this);
		Character extractedResourceType = null;
			
		int extractingCost = calculateDistance((int)spacePtSource.getX(), 
				(int)spacePtSource.getY(),(int)spacePtAgent.getX(), (int)spacePtAgent.getY());
		if (remainingTime >= extractingCost) {
			Resource extractedResource = source.extractResource();
			extractedResource.increaseValue(extractingCost);
			remainingTime-=extractingCost;				
			addToInventory(extractedResource);		
			extractedResourceType = extractedResource.getResourceType();
			
			//System.out.println("Extracted resource " + extractedResource.getResourceType());	
			return extractedResourceType;
		} 		
		System.out.println("ERROR: COULD NOT EXTRCT RESOURCE!");
		return extractedResourceType;
	}
	
	/**
	 * Attempts to combine two of the basic Resources that an Agent holds to create an intermediate
	 * Product part.
	 * 
	 * @param possibleProducts A {@code LinkedList<List<Character>>} representation of possible
	 * products (all permutations of size > 1).
	 * @return True if the Resources could be combined, false otherwise
	 */
	public boolean combineResources(LinkedList<List<Character>> possibleProducts) {
		
		// TODO: Random Selection. Implement interface to determine choice.
		List<Character> chosenPart = possibleProducts.get(RandomHelper.nextIntFromTo(0, 
				possibleProducts.size()-1));
		
		char resourceType1 = chosenPart.get(0);
		char resourceType2 = chosenPart.get(1);
		Resource resource1 = null, resource2 = null;
		
		if (isInInventory(resourceType1) && isInInventory(resourceType2)) {
			resource1 = getFromInventory(resourceType1);
			resource2 = getFromInventory(resourceType2);
			
			if (resource1 !=null && resource2 !=null) {
				Product newPart = new Product(resource1, resource2);
				addToInventory(newPart);
				remainingTime-= newPart.getInitialAssemblyCost();
				
				//System.out.println("Assembled product " + newPart.getProduct().toString());
				return true;
			}
		} 
		System.out.println("ERROR: COULD NOT COMBINE RESOURCES!");
		return false;
	}	
	
	/**
	 * Attempts to combine a Product part and a basic Resource from the inventory of an Agent.
	 * The Resource is destroyed in the process and the value of the product is increased.
	 *  
	 * @param possibleProducts A {@code LinkedList<List<Character>>} representation of possible
	 * products (all permutations of size > 1).
	 * @return True if the Product and Resource could be combined, false otherwise
	 */
	public boolean combinePartAndResource(LinkedList<List<Character>> possibleProducts) {
		Product product = null;
		Resource resourceSupply = null;
		boolean front = false;	
		
		//TODO: Random Selection. Implement interface to determine choice.
		List<Character> chosenPart = possibleProducts.get(RandomHelper.nextIntFromTo(0, 
				possibleProducts.size()-1));	
		
		// CASE 1: Resource needs to be added to the front of the product
		char resource = chosenPart.get(0);
		List<Character> part = chosenPart.subList(1, chosenPart.size());	
		if (isInInventory(part) && isInInventory(resource)) {
			//System.out.println("Add resource to front");
			front = true;	
			product = getFromInventory(part);
			resourceSupply = getFromInventory(resource);
		} else {
			// CASE 2: Resource needs to be added to the back of the product
			part = chosenPart.subList(0, chosenPart.size()-1);
			resource = chosenPart.get(chosenPart.size()-1);
			if (isInInventory(part) && isInInventory(resource)) {
				//System.out.println("Add resource to back");
				front = false;	
				product = getFromInventory(part);
				resourceSupply = getFromInventory(resource);
			}
		}

		if (resourceSupply != null && product != null) {
			remainingTime-= product.addResource(resourceSupply, front);
			addToInventory(product);
			//System.out.println("Added Resource to Product part.");
			return true;			
		} 
		System.out.println("ERROR: COULD NOT COMBINE PRODUCT AND RESOURCE!");
		return false;
	}
	
	/**
	 * Adds a second partial Product to the back of an existing product.
	 * 
	 * @param possibleCombinations A {@code HashMap<List<Character>, List<List<Character>>>} holding 
	 * a list representation of the possible products as Key and a list of necessary product parts 
	 * as value
	 * @return True if the parts could be combined, false otherwise
	 */
	public boolean combineParts(HashMap<List<Character>, List<List<Character>>> 
				possibleCombinations) {
		
		//TODO: Random Selection. Implement interface to determine choice.		
		List<List<Character>> keySet = 
				new ArrayList<List<Character>>(possibleCombinations.keySet());
		List<Character> randomKey = keySet.get(RandomHelper.nextIntFromTo(0,keySet.size()-1));		
		
		List<List<Character>> ingredients = possibleCombinations.get(randomKey);
		Product product = getFromInventory(ingredients.get(0));
		Product part2 = getFromInventory(ingredients.get(1));
		
		if (product != null && part2 != null) {
			product.addProduct(part2);
			addToInventory(product);	
			return true;
		}
		System.out.println("ERROR: COULD NOT COMBINE PRODUCTS!");
		return false;		
	}
	
	/**
	 * Attempts to fulfill an Agent's demand by consuming a product that matches its demand. If
	 * the agent holds a matching product, its lifespan is increased by a factor of its size, the 
	 * product is destroyed and a new demand is generated.
	 * 
	 * @return True if the agent has a demanded product and can consume it, false otherwise.
	 */
	public boolean consumeProduct() {
		if (isInInventory(demand)) {
			
			// Consume the product to fulfill the demand (destroys it)
			getFromInventory(demand);
			remainingTime+=demand.size()*100;		
			System.out.println("Demand fulfilled!");
			CybersymBuilder.removeArrayFromRegister(demand, "demand");
			// Generate a new random demand
			demand = CybersymBuilder.possibleProducts.get(RandomHelper.nextIntFromTo(0, 
					CybersymBuilder.nrPossibleProducts-1));
			//System.out.println("New Agent Demand: " + getDemand());
			generateIndirectDemand();	
			return true;
		} else return false;
	}
	
	/**
	 * Attempts to deliver a resource requested by an other Agent in the neighborhood
	 * @param resource The requested Resource that can be supplied by this Agent
	 * @param receiver The agent who requested the Resource
	 */
	public boolean deliverResource(Resource resource, Agent receiver) {
		NdPoint spacePtReceiver = space.getLocation(receiver);
		NdPoint spacePtAgent = space.getLocation(this);
			
		int deliveryCost = calculateDistance((int)spacePtReceiver.getX(), 
				(int)spacePtReceiver.getY(),(int)spacePtAgent.getX(), (int)spacePtAgent.getY());
		if (remainingTime >= deliveryCost) {
			resource.increaseValue(deliveryCost);
			receiver.addToInventory(resource);
			remainingTime-= deliveryCost;
			//System.out.println("Delivered a Resource.");
			return true;
		} else return false;
	}
	
	// XXX GETTER FUNTIONS %% GETTER FUNTIONS %% GETTER FUNTIONS %% GETTER FUNTIONS %% GETTER FUNTIONS %% GETTER FUNTIONS %% GETTER FUNTIONS %%		
	
	/**
	 * Returns a {@code Product} from the inventory that matches the List representation in query.
	 * The Product is removed from the inventory through this process.
	 * 
	 * @param query	A {@code List} representation of the Product to be fetched from supply
	 * @return A {@code Product} from the inventory that matches the List representation in query.
	 */
	public Product getFromInventory(List<Character> query) {
		if (productInventory.containsKey(query)) {
			LinkedList<Product> supply = productInventory.remove(query);
			if (!supply.isEmpty()) {
				Product product = supply.removeFirst();
				if (!supply.isEmpty()) {
					productInventory.put(query,supply);	
				}
				return product;
			}
		} 
		return null;
	}
	
	/**
	 * Returns a {@code Resource} from the inventory that matches the char representation in query.
	 * The Resource is removed from the inventory through this process.
	 * 
	 * @param query	A {@code char} representation of the Resource to be fetched from supply
	 * @return A {@code Resource} from the inventory that matches the char representation in query.
	 */
	public Resource getFromInventory(char query) {
		if (resourceInventory.containsKey(query)) {
			LinkedList<Resource> supply = resourceInventory.remove(query);
			if (!supply.isEmpty()) {
				Resource resource = supply.removeFirst();
				if (!supply.isEmpty()) {
					resourceInventory.put(query,supply);
				}
				CybersymBuilder.removeFromRegister(query, "supply");
				return resource;
			}
		} 
		return null;
	}	
	
	/**
	 * Returns the amount of basic Resources in the {@code resourceInventory}
	 * @return The amount of basic Resources in the {@code resourceInventory}
	 */
	public int getNrResourcesInInventory() {
		int counter = 0;
		Set<Character> keys = resourceInventory.keySet();
		for(char key: keys){
			counter+= resourceInventory.get(key).size();
        }
		return counter;
	}
	
	/**
	 * Returns the amount of Products in the {@code productInventory}
	 * @return The amount of Products in the {@code productInventory}
	 */
	public int getNrProductsInInventory() {
		int counter = 0;
		Set<List<Character>> keys = productInventory.keySet();
		for(List<Character> key: keys){
			counter+= productInventory.get(key).size();
        }
		return counter;
	}
	
	/**
	 * Returns the {@code remainingTime} of an individual Agent
	 * @return The {@code remainingTime} of an individual Agent
	 */
	public int getRemainingTime() {
		return this.remainingTime;
	}
	
	/**
	 * Returns a {@code LinkedList} representation of an Agent's product inventory
	 * @return A {@code LinkedList} representation of an Agent's product inventory
	 */
	public LinkedList<List<Character>> getProductList() {
		return new LinkedList<List<Character>>(productInventory.keySet());	
	}
	
	/**
	 * Returns the set of Resource Types available to the Agent through Sources in his neighborhood
	 * @return The set of Resource Types available to the Agent through Sources in his neighborhood
	 */
	public Set<Character> getAvailableResourceTypes() {
		return availableResourceTypes;
	}
	
	/**
	 * Returns a list of the Resources that an individual Agents requests from other Agents in his 
	 * neighborhood since he cannot extract it himself
	 * @return  A list of the Resources that an individual Agents requests from other Agents
	 */
	public LinkedList<Character> getResourceRequest() {
		return resourceRequest;
	}
	
	// XXX HELPER FUNTIONS %% HELPER FUNTIONS %% HELPER FUNTIONS %% HELPER FUNTIONS %% HELPER FUNTIONS %% HELPER FUNTIONS %% HELPER FUNTIONS %%
	
	/**
	 * Checks the Agent's demand against the available Sources and generates a request list for all
	 * Resources he cannot extract himself;
	 */
	public void setResourceRequest() {
		resourceRequest = new LinkedList<Character>();
		
		List<Character> demand = new ArrayList<Character>(indirectResourceDemand.keySet());	
		List<Character> avResOwnDemand = new ArrayList<Character>(getAvailableResourceTypes());
		for (char item : demand) {
			if(!avResOwnDemand.contains(item)) {
				int amount = indirectResourceDemand.get(item);
				for (int i=0; i<amount; i++) {
					resourceRequest.add(item);
					CybersymBuilder.addToRegister(item, "request");
				}
			}
		}		
	}
	
	/**
	 * Evaluates the resource demand of Agents in the neighborhood. Sets the Agents field 
	 * {@code neighborResourceRequests} with a Hashtable containing the Resource {@code char} as key
	 * and a linked list of Agents requesting that Resource.
	 */
	public void setNeighborResourceRequest() {
		neighborResourceRequests = new Hashtable<Character,LinkedList<Agent>>();
		for (Agent agent : reachableAgents) {
			LinkedList<Character> request = agent.getResourceRequest();
			for (char resource : request) {
				if (neighborResourceRequests.containsKey(resource)) {
					 LinkedList<Agent> callers = neighborResourceRequests.get(resource);
					 callers.add(agent);
					 neighborResourceRequests.put(resource, callers);
				} else {
					LinkedList<Agent> callers = new LinkedList<Agent>();
					callers.add(agent);			
					neighborResourceRequests.put(resource, callers);
				}
			}
		}
	}	
	
	/**
	 * Adds a Resource to the {@code resourceInventory}
	 * @param newResource A new Resource to be added to the inventory.
	 */
	public void addToInventory(Resource newResource) {
		char key = newResource.getResourceType();
		if (resourceInventory.containsKey(key)) {
			LinkedList<Resource> supply = resourceInventory.get(key);
			supply.add(newResource);
			resourceInventory.put(key,supply);
			CybersymBuilder.addToRegister(key, "supply");
		} else {
			LinkedList<Resource> newSupply = new LinkedList<Resource>();
			newSupply.add(newResource);
			resourceInventory.put(key,newSupply);
			CybersymBuilder.addToRegister(key, "supply");
		}
	}
	
	/**
	 * Adds a Product to the {@code productInventory}
	 * @param newProduct A new Product to be added to the inventory.
	 */
	public void addToInventory(Product newProduct) {
		List<Character> key = newProduct.getProduct();
		if (productInventory.containsKey(key)) {
			LinkedList<Product> supply = productInventory.get(key);
			supply.add(newProduct);
			productInventory.put(key,supply);
		} else {
			LinkedList<Product> newSupply = new LinkedList<Product>();
			newSupply.add(newProduct);
			productInventory.put(key,newSupply);
		}
	}
	
	/**
	 * Returns true if a Resource represented as {@code char} is in the {@code resourceInventory}
	 * @param query A Resource represented as {@code char} 
	 * @return True if a Resource represented as {@code char} is in the {@code resourceInventory}
	 */
	public boolean isInInventory(char query) {
		return resourceInventory.containsKey(query);
	}
	
	/**
	 * Returns true if a Product represented as {@code List} is in the {@code productInventory}
	 * @param query A Product represented as {@code List}
	 * @return True if a Product represented as {@code List} is in the {@code productInventory}
	 */
	public boolean isInInventory(List<Character> query) {
		return productInventory.containsKey(query);
	}
	
	/**
	 * Creates a {@code LinkedList<List<Character>>} of the possible combinations of basic resources
	 * that can be used in the demanded product.
	 *  
	 * @param inventory A list of the {@code char} representation of the available Resources
	 * @return A {@code LinkedList<List<Character>>} of possible combinations of basic resources
	 */
	public LinkedList<List<Character>> generateCombinations(List<Character> inventory) {
		LinkedList<List<Character>> possibleProducts = new LinkedList<List<Character>>();
		
		for (int i=0; i < inventory.size(); i++) {
			for (int j=0; j < inventory.size(); j++) {
				if (i != j) {
					List<Character> currentPair = new ArrayList<Character>();
					currentPair.add(inventory.get(i));
					currentPair.add(inventory.get(j));
					if (demand.equals(currentPair) || indirectPartDemand.contains(currentPair) ) {
						possibleProducts.add(currentPair);
					}
				}
			}
		}		
		return possibleProducts;
	}
	
	/**
	 * Creates a {@code LinkedList<List<Character>>} of the possible combinations of basic resources
	 * and available partial Products that can be used in the demanded product.
	 * 
	 * @param inventory A list of the {@code char} representation of the available Resources
	 * @param parts A list of lists of the {@code char} representation of available partial Products
	 * @return A list of the possible combinations of basic resources and available partial Products 
	 * that can be used in the demanded product
	 */
	public LinkedList<List<Character>> generateCombinations(List<Character> inventory, 
			LinkedList<List<Character>> parts) {
		LinkedList<List<Character>> possibleProducts = new LinkedList<List<Character>>();
		
		List<Character> currentPair = new ArrayList<Character>();	
		for (int i=0; i < inventory.size(); i++) {
			for (List<Character> part : parts) {
				currentPair = new ArrayList<Character>();				
				currentPair.add(inventory.get(i));
				currentPair.addAll(part);				
				if (demand.equals(currentPair) || indirectPartDemand.contains(currentPair) ) {
					possibleProducts.add(currentPair);
				}				
				currentPair = new ArrayList<Character>();
				currentPair.addAll(part);	
				currentPair.add(inventory.get(i));			
				if (demand.equals(currentPair) || indirectPartDemand.contains(currentPair) ) {
					possibleProducts.add(currentPair);
				}
			}
		}
		return possibleProducts;
	}
	
	/**
	 * Creates a {@code HashMap<List<Character>, List<List<Character>>>} of the possible 
	 * combinations of Product parts. Key is a list representation of the final product, Values is
	 * a list of the partial products that can be used to assemble it
	 *  
	 * @param productSupply A {@code LinkedList<List<Character>> productSupply} representation of 
	 * the Agent's product inventory
	 * @return A {@code HashMap<List<Character>, List<List<Character>>>} of the possible 
	 * combinations of Product parts
	 */
	public HashMap<List<Character>, List<List<Character>>> generateCombinations(
			LinkedList<List<Character>> inventory) {
		
		HashMap<List<Character>, List<List<Character>>> possibleProducts = 
				new HashMap<List<Character>, List<List<Character>>>();
				
		List<Character> currentPair;	
		for (int i=0; i < inventory.size(); i++) {
			for (int j=0; j < inventory.size(); j++) {
				if (i != j) {
					currentPair = new ArrayList<Character>();					
					currentPair.addAll(inventory.get(i));
					currentPair.addAll(inventory.get(j));
					if (demand.equals(currentPair) || indirectPartDemand.contains(currentPair) ) {
						List<List<Character>> ingredients = new ArrayList<List<Character>>();
						ingredients.add(inventory.get(i));
						ingredients.add(inventory.get(j));
						possibleProducts.put(currentPair, ingredients);
					}
				}
			}
		}
		return possibleProducts;
	}
	
	/**
	 * Sets an Agent's field {@code reachableAgents} to a set of other Agents that are in the
	 * Agent's neighborhood   
	 */
	public void setReachableAgents() {	
		GridCellNgh<Agent> nghCreatorAgents = new GridCellNgh<Agent>(grid,pt,Agent.class,
				CybersymBuilder.agentScopeAgents,CybersymBuilder.agentScopeAgents);
		List<GridCell<Agent>> closeAgents = nghCreatorAgents.getNeighborhood(true);
		reachableAgents = new HashSet<Agent>();
		for (GridCell<Agent>cell : closeAgents) {
			for (Agent agent : cell.items()) {
				reachableAgents.add(agent);
			}
		}		
	}
	
	/**
	 * Either checks the individual Agent's indirect Resource demand against the Sources that are 
	 * available (if {@code self} is true) or checks whether the agent can extract Resources 
	 * requested by neighboring Agents.
	 * 
	 * @param pt The location of the Agent whose environment is evaluated
	 * @param self True if the list should contain Resources demand by the Agent himself, false if
	 * the Agent should check for Resources demanded by neighboring Agents.
	 * @return A {@code Hashtable<Character, List<Source>>} of the available Resources and their 
	 * respective Sources.
	 */
	public Hashtable<Character, LinkedList<Source>> getAvailableResources(GridPoint pt, boolean self) {	
		Hashtable<Character, LinkedList<Source>> availableResources = 
				new Hashtable<Character, LinkedList<Source>>();
		GridCellNgh<Source> nghCreatorSources = new GridCellNgh<Source>(grid,pt,Source.class,
				CybersymBuilder.agentScopeSources,CybersymBuilder.agentScopeSources);
		List<GridCell<Source>> closeSources = nghCreatorSources.getNeighborhood(true);		
	
		for (GridCell<Source> cell : closeSources) {
			for (Source source : cell.items()) {
				char resource = source.getResourceType();
				if (self && indirectResourceDemand.containsKey(resource) || 
						!self && neighborResourceRequests.containsKey(resource)) { 			
					if (availableResources.containsKey(resource)) {
						LinkedList<Source> sources = availableResources.get(resource);
						sources.add(source);
						availableResources.put(resource, sources);
					} else {
						LinkedList<Source> sources = new LinkedList<Source>(); 
						sources.add(source);
						availableResources.put(resource, sources);
					}
				}
			}
		}
		if (self) {
			availableResourceTypes = availableResources.keySet(); 
		}
		return availableResources;
	}	
	
	/**
	 * Generates the indirect demand of an Agent given its Product demand. Indirect demand is 
	 * divided in indirect demand for other Product parts and indirect demand for basic
	 * Resources.
	 */
	public void generateIndirectDemand() {
		indirectResourceDemand = new Hashtable<Character, Integer>();
		indirectPartDemand = new LinkedList<List<Character>>();
		
		List<Character> sub;
	    int i, c;
	    int length = demand.size();   
	 
	    for( c = 0 ; c < length ; c++ )
	    {
	        for( i = 1 ; i <= length - c ; i++ )
	        {
	        	sub = demand.subList(c, c+i);
	        	if (sub.size() > 1) {
	        		indirectPartDemand.add(sub);
	        	} else {
	        		char resource = sub.get(0);
	        		if (indirectResourceDemand.get(resource) == null) {
	        			indirectResourceDemand.put(resource, 1);
	        			CybersymBuilder.addToRegister(resource, "demand");
	        		} else {
	        			int oldCount = indirectResourceDemand.get(resource);
	        			indirectResourceDemand.put(resource, oldCount+1);
	        			CybersymBuilder.addToRegister(resource, "demand");
	        	    }
	            }	         
	        }
	    }
	}
	
	/**
	 * Calculates the distance between two continuous space locations (coordinates) on the torus
	 * @param p1X The x coordinate of the first point
	 * @param p1Y The y coordinate of the first point
	 * @param p2X The x coordinate of the second point
	 * @param p2Y The y coordinate of the second point
	 * @return The pythagorean distance cast as {@code int}
	 */
	public int calculateDistance(int p1X, int p1Y, int p2X, int p2Y){
		return (int)Math.sqrt( Math.min( Math.pow((p2X - p1X), 2), 
				Math.pow(( CybersymBuilder.gridWidth - p2X - p1X), 
				2)) + Math.min( Math.pow((p2Y - p1Y), 2), 
				Math.pow(( CybersymBuilder.gridHeight - p2Y - p1Y), 2)));
	}
	
	// XXX VISUALIZATION TOOLS %% VISUALIZATION TOOLS %% VISUALIZATION TOOLS %% VISUALIZATION TOOLS %% VISUALIZATION TOOLS %% VISUALIZATION TOOLS %%
	
	/**
	 * Returns a {@code String} representation of the agent's demand 
	 * @return A {@code String} representation of the agent's demand 
	 */
	public String getDemand() {
		String demandString = "";
		for (char item : demand) {
			demandString+= item;			
		}
		return demandString;
	}
	
	/**
	 * Returns a String to be printed on the Agent's label in the simulation visualization
	 * @return a String to be printed on the Agent's label in the simulation visualization
	 */
	public String getLabel(){
		String label = "T:" + String.valueOf(this.remainingTime) + " D:" + getDemand();
		return label;
	}
}