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
 * Instances of the Agent class are the actual agents in the CyberSym simulation. 
 * Agents generate demands and aim to fulfill them in order to increase their lifespan. When their
 * {@code remainingTime} runs out, they are removed from the grid. 
 * In order to fulfill their demands, agents can make use of different actions like extracting 
 * Resources from their Sources and delivering Resources to other agents. Actions can be determined 
 * by different intelligence and planning settings
 * 
 * @author Janosch Haber, 10400192
 * @date 37.04.2015
 */
public class Agent {
	// Representation of the Environment (continuous + rasterized)
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	// Items held by the individual Agent instance
	private int remainingTime;
	private Hashtable<Character,LinkedList<Resource>> resourceInventory;
	private Hashtable<List<Character>,LinkedList<Product>> productInventory;
	
	// Information about the Agent's demands
	private LinkedList<Character> demand;
	private LinkedList<List<Character>> indirectPartDemand;
	private Hashtable<Character, Integer> indirectResourceDemand;
	
	// Information about Resources in the Agent's neighborhood
	private int nrAvailableSources;
	private Hashtable<Character, List<Source>> availableResources;
	private Set<Character> availableResourceTypes;
	
	// Information about other Agents in the neighborhood
	private int nrReachableAgents;
	private Set<Agent> reachableAgents;
	
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
		
		resourceInventory = new Hashtable<Character,LinkedList<Resource>>();
		productInventory = new Hashtable<List<Character>,LinkedList<Product>>();
		
		demand = CybersymBuilder.possibleProducts.get(RandomHelper.nextIntFromTo(0, 
				CybersymBuilder.nrPossibleProducts-1));
		generateIndirectDemand();
		System.out.println("Agent Demand: " + getDemand());
	}
	
	/**
	 * Initializes the agent by evaluating its environment. This cannot be done in the
	 * constructor as at that point in time not all agents in the environment are yet set.
	 */
	public void initialize() {
		GridPoint pt = grid.getLocation(this);
		setAvailableResources(pt);
		setReachableAgents(pt);
	}
	
	/**
	 * Scheduled method to invoke the Agent's behavior. Evaluates the environment and
	 * determines the individual agent's behavior. After each tick, the {@code remainingTime} 
	 * of an agent is decreased by one
	 */
	@ScheduledMethod(start=1, interval=1)
	public void step () {	
		GridPoint pt = grid.getLocation(this);
		setAvailableResources(pt);
		setReachableAgents(pt);
		
		System.out.print("Agent Inventory: " + getNrResourcesInInventory() + " Resources and ");
		System.out.println(getNrProductsInInventory() + " Products.");
		determineAction();
		
		remainingTime--;
		if (remainingTime <= 0) {
			@SuppressWarnings("unchecked")
			Context<Object> context = ContextUtils.getContext(this);
			context.remove(this);
		}
	}	
	
	/**
	 * Interface for determining an individual Agent's next action
	 */
	public void determineAction() {
		//TODO: Interface for selection parameters / intelligence settings
		
		// OPTION 1: Extract a Resource from one of the sources in the neighborhood
		List<Character> resourceDemand = new ArrayList<Character>(indirectResourceDemand.keySet());
		if (!resourceDemand.isEmpty()) {
			extractResource(resourceDemand.get(RandomHelper.nextIntFromTo(0, 
					resourceDemand.size()-1)));
			return;
		} 		
		
		// OPTION 2: Assemble a product from two basic Resources in the inventory
		List<Character> resourceSupply = new ArrayList<Character>(resourceInventory.keySet());	
		//System.out.println(resourceSupply.size() + " different Resource Types present.");
		LinkedList<List<Character>> possibleProducts =  generateCombinations(resourceSupply);
		//System.out.print("Agent can generate "); System.out.print(possibleProducts.size()); 
		//System.out.println(" possible partial products."); System.out.println(possibleProducts.toString());
		if (!possibleProducts.isEmpty()) {
			combineResources(possibleProducts);
			return;
		}

		// OPTION 3: Add another basic Resource to a partial Product in the inventory
		LinkedList<List<Character>> productSupply = getProductList();
		possibleProducts = generateCombinations(resourceSupply, productSupply);
		//System.out.print("Agent can assemble "); System.out.print(possibleProducts.size()); 
		//System.out.println(" possible partial products."); System.out.println(possibleProducts.toString());		
		if (!possibleProducts.isEmpty()) {
			combinePartAndResource(possibleProducts);
			return;
		}
		
		// OPTION 4: Combine two partial Products TODO
		HashMap<List<Character>, List<List<Character>>> possibleCombinations = generateCombinations(productSupply);
		System.out.print("Agent can assemble "); System.out.print(possibleCombinations.size()); 
		System.out.println(" possible partial products."); System.out.println(possibleCombinations.toString());	
		if (!possibleCombinations.isEmpty()) {
			combineParts(possibleCombinations);
			return;
		}
		
		// OPTION 5: Deliver a Resource to another Agent in the neighborhood TODO
		
		// OPTION 6: Consume a finished Product in order to increase the Agent's lifespan
		if (consumeProduct()) {
			return;
		}
	}
	
	// ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %% ACTIONS %%
	
	/**
	 * Attempts to extract a demanded resource from the Sources in the environment. If it is 
	 * successful, a Resource object is created and added to the agent's resource inventory. The 
	 * cost of extracting the resource is subtracted from the agent's remaining lifespan.
	 * 
	 * @param demand A {@code char} representation of the demanded resource
	 * @return true if the resource could be extracted, false otherwise
	 */
	private boolean extractResource(char demand) {
		//TODO! Chooses Source randomly. Implement interface to determine choice.
		
		List<Source> availableSources = availableResources.get(demand);
		if (availableSources != null) {
			int max = availableSources.size()-1;
			Source source = availableSources.get(RandomHelper.nextIntFromTo(0, max));
			NdPoint spacePtSource = space.getLocation(source);
			NdPoint spacePtAgent = space.getLocation(this);
			
			int extractingCost = calculateDistance((int)spacePtSource.getX(), 
					(int)spacePtSource.getY(),(int)spacePtAgent.getX(), (int)spacePtAgent.getY());
			if (remainingTime >= extractingCost) {
				Resource extractedResource = source.extractResource();
				extractedResource.increaseValue(extractingCost);
				remainingTime-=extractingCost;				
				addToInventory(extractedResource);		
				
				System.out.print("Mined resource ");				
				System.out.println(extractedResource.getResourceType());	

				// TODO Move out of here ?!
				// Update resource demand
				int oldAmount = indirectResourceDemand.get(extractedResource.getResourceType());
				if (oldAmount == 1) {
					indirectResourceDemand.remove(extractedResource.getResourceType());
				} else {
					indirectResourceDemand.put(extractedResource.getResourceType(),oldAmount-1);
				}		
				return true;
			} 
		} 
		System.out.println("ERROR: COULD NOT EXTRCT RESOURCE!");
		return false;
	}
	
	/**
	 * Attempts to combine two of the basic Resources that an Angent holds to create an intermediate
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
				
				System.out.print("Assembled product ");
				System.out.println(newPart.getProduct().toString());
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
			System.out.println("Add resource to front");
			front = true;	
			product = getFromInventory(part);
			resourceSupply = getFromInventory(resource);
		} else {
			// CASE 2: Resource needs to be added to the back of the product
			part = chosenPart.subList(0, chosenPart.size()-1);
			resource = chosenPart.get(chosenPart.size()-1);
			if (isInInventory(part) && isInInventory(resource)) {
				System.out.println("Add resource to back");
				front = false;	
				product = getFromInventory(part);
				resourceSupply = getFromInventory(resource);
			}
		}

		if (resourceSupply != null && product != null) {
			remainingTime-= product.addResource(resourceSupply, front);
			addToInventory(product);
			System.out.println("Added Resource to Product part.");
			return true;			
		} 
		System.out.println("ERROR: COULD NOT COMBINE PRODUCT AND RESOURCE!");
		return false;
	}
	
	/**
	 * Adds a second part to the back of an existing product.
	 * @param possibleCombinations A {@code HashMap<List<Character>, List<List<Character>>>} holding 
	 * a list representation of the possible products as Key and a list of necessary product parts 
	 * as value
	 * @return True if the parts could be combined, false otherwise
	 */
	public boolean combineParts(HashMap<List<Character>, List<List<Character>>> possibleCombinations) {
		
		//TODO: Random Selection. Implement interface to determine choice.		
		List<List<Character>> keySet = new ArrayList<List<Character>>(possibleCombinations.keySet());
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
			remainingTime+=demand.size()*10;		
			System.out.println("Demand fulfilled !");
			
			// Generate a new random demand
			demand = CybersymBuilder.possibleProducts.get(RandomHelper.nextIntFromTo(0, 
					CybersymBuilder.nrPossibleProducts-1));
			System.out.println("New Agent Demand: " + getDemand());
			generateIndirectDemand();	
			return true;
		} else return false;
	}
	
	/**
	 * Attempts to deliver a resource demanded by an other Agent in the environment to it.
	 * @param resourceToBring The demanded resources that can be supplied by this Agent
	 * @param receiver The agent who demanded for a resource
	 */
	public void deliverResource(Resource resourceToBring, Agent receiver) {
		//TODO! Stub.
	}
	
	// GETTER FUNTIONS %% GETTER FUNTIONS %% GETTER FUNTIONS %% GETTER FUNTIONS %% GETTER FUNTIONS %% GETTER FUNTIONS %% GETTER FUNTIONS %%
	
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
	 * Returns a {@code Set<Character>} of the different Resource types that are 
	 * available to an Agent through the Sources in its environment
	 * @return	A {@code Set<Character>} of the different available Resource types
	 */
	public Set<Character>getAvailableResourceTypes() {	
		return availableResources.keySet();	
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
	
	// HELPER FUNTIONS %% HELPER FUNTIONS %% HELPER FUNTIONS %% HELPER FUNTIONS %% HELPER FUNTIONS %% HELPER FUNTIONS %% HELPER FUNTIONS %%
	
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
		} else {
			LinkedList<Resource> newSupply = new LinkedList<Resource>();
			newSupply.add(newResource);
			resourceInventory.put(key,newSupply);
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
	 * @return
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
	 * Returns a {@code Set} of other Agents that are in the Agent's neighborhood   
	 * @param pt The location of the Agent whose neighborhood is evaluated
	 * @return A {@code Set<Agent>} containing all the Agents in the neighborhood
	 */
	public void setReachableAgents(GridPoint pt) {
	
		GridCellNgh<Agent> nghCreatorAgents = new GridCellNgh<Agent>(grid,pt,Agent.class,10,10);
		List<GridCell<Agent>> closeAgents = nghCreatorAgents.getNeighborhood(true);
		reachableAgents = new HashSet<Agent>();
		nrReachableAgents = 0;
		for (GridCell<Agent>cell : closeAgents) {
			nrReachableAgents+=cell.size();
			for (Agent agent : cell.items()) {
				reachableAgents.add(agent);
			}
		}		
	}
	
	/**
	 * Sets the individual Agent's available Resources (from Sources only)
	 * @param pt The location of the Agent whose environment is evaluated
	 */
	public void setAvailableResources(GridPoint pt) {				
		GridCellNgh<Source> nghCreatorSources = new GridCellNgh<Source>(grid,pt,Source.class,10,10);
		List<GridCell<Source>> closeSources = nghCreatorSources.getNeighborhood(true);
		availableResources = new Hashtable<Character, List<Source>>();
		nrAvailableSources = 0; 		
		for (GridCell<Source> cell : closeSources) {
			nrAvailableSources+=cell.size();
			for (Source source : cell.items()) {
				char resource = source.getResourceType();
				
				if (availableResources.containsKey(resource)) {
					List<Source> sources = availableResources.get(resource);
					sources.add(source);
					availableResources.put(resource, sources);
				} else {
					List<Source> sources = new ArrayList<Source>(); 
					sources.add(source);
					availableResources.put(resource, sources);
				}
			}
		}
		availableResourceTypes = getAvailableResourceTypes();
	}	
	
	/**
	 * Generates the indirect demand of an Agent given its Product demand. Indirect demand is 
	 * divided in indirect demand for other Product parts and indirect demand for (minable) basic
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
	        		} else {
	        			int oldCount = indirectResourceDemand.get(resource);
	        			indirectResourceDemand.put(resource, oldCount+1);
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
	
	// VISUALIZATION TOOLS %% VISUALIZATION TOOLS %% VISUALIZATION TOOLS %% VISUALIZATION TOOLS %% VISUALIZATION TOOLS %% VISUALIZATION TOOLS %%
	
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
		//TODO! Make interface to alter options.
		String label = "T:";
		label+= String.valueOf(this.remainingTime);
		
		label+= " D:";
		label+= getDemand();
		
		if (availableResourceTypes != null) {
			label+= " R:";
			label+= String.valueOf(this.nrAvailableSources);
		}
		
		label+= " A:";
		label+= String.valueOf(this.nrReachableAgents);
		return label;
	}
}