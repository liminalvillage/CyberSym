package cybersym1;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;

/**
 * <h1>CybersymBuilder</h1>
 * 
 * The CybersymBuilder is Repast's central tool to control the simulation framework. It is used to
 * initiate the simulation, instantiate all Agents (Agents and Resources) and place them in the
 * environment (Context). Here it is also used to provide a static interface to evaluate the system
 * performance
 * 
 * @author Janosch Haber, 10400192, University of Amsterdam (UvA)
 * @date 06-05-2015
 *
 */
public class CybersymBuilder implements ContextBuilder<Object> {
	
	// The dimensions of the simulation environment. Passed from the simulation GUI
	public static int gridWidth;
	public static int gridHeight;	
	// A list of all the products that may be demanded by the Agents in the simulation
	public static HashMap<Integer,LinkedList<Character>> possibleProducts;	
	public static int nrPossibleProducts;
	// Agent settings passed from the simulation GUI
	public static int agentScopeSources;
	public static int agentScopeAgents;
	// Records of system behavior for statistical system analysis and evaluation 
	public static Hashtable<Character,Integer> letterDemandRegister;
	public static Hashtable<Character,Integer> letterSupplyRegister;
	public static Hashtable<Character,Integer> letterRequestRegister;
	public static Hashtable<Character,Integer> letterDepletionRegister;
	/// Additional graph TODO: Needs to be integrated in the Repast simulation environment
	public static Histogram resourceStatistics;

	/**
	 * Initiates the entire simulation
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Context build(Context<Object> context) {
		
		// Instantiate the evaluation records
		letterDemandRegister = new Hashtable<Character,Integer>();
		letterSupplyRegister = new Hashtable<Character,Integer>();
		letterRequestRegister = new Hashtable<Character,Integer>();
		letterDepletionRegister = new Hashtable<Character,Integer>();	
		
		// Create a new Context (simulation environment)
		context.setId("cybersym1");		
		
		// Get the first set of user settings from the simulation GUI		
		Parameters params = RunEnvironment.getInstance().getParameters();
		gridWidth = (Integer)params.getValue("grid_width");
		gridHeight = (Integer)params.getValue("grid_height");
		String fileName = params.getValueAsString("file_name");
		readFile(fileName);		
		
		//TODO: Enable the miningNetwork
		//NetworkBuilder<Object>netBuilder = new NetworkBuilder<Object>
		//	("miningNetwork", context, true);
		//netBuilder.buildNetwork();
		
		// Generate a continuous environment representation space
		ContinuousSpaceFactory spaceFactory=
				ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space =
				spaceFactory.createContinuousSpace("space", context, 
				new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), gridWidth, gridHeight);
		
		// Generate a rasterized environment representation grid
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object>grid = gridFactory.createGrid("grid", context, 
				new GridBuilderParameters<Object>(new WrapAroundBorders (),
				new SimpleGridAdder<Object>(), true, gridWidth, gridHeight));		
		
		// Invoke the Agents
		agentScopeSources = (Integer)params.getValue("scope_sources");
		agentScopeAgents = (Integer)params.getValue("scope_agents");
		int AgentCount = (Integer)params.getValue("agent_count");
		int minLifespan = (Integer)params.getValue("min_lifespan");
		int maxLifespan = (Integer)params.getValue("max_lifespan");
		for (int i = 0; i < AgentCount; i++) {
			int lifespan = RandomHelper.nextIntFromTo(minLifespan, maxLifespan);
			context.add(new Agent(space, grid, lifespan));
		}		
		// Invoke the Sources
		int SourceCount = (Integer)params.getValue("source_count");
		int minQuantity = (Integer)params.getValue("min_quantity");
		int maxQuantity = (Integer)params.getValue("max_quantity");
		int minRegeneration = (Integer)params.getValue("min_regeneration");
		int maxRegeneration = (Integer)params.getValue("max_regeneration");
		for (int i = 0; i < SourceCount; i++) {
			int initialQuantity = RandomHelper.nextIntFromTo(minQuantity, maxQuantity);
			int regenerationPeriod = RandomHelper.nextIntFromTo(minRegeneration, maxRegeneration);
			char resource = (char)RandomHelper.nextIntFromTo(65, 90);
			context.add(new Source(space, grid, initialQuantity, regenerationPeriod, resource));
		}		
		// "Physically" place all objects in the environment representation 
		for (Object obj : context) {
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj, (int)pt.getX(), (int)pt.getY());
		}		
		// Initialize the Agents
		for (Object obj : context) {
			if (obj.getClass() == Agent.class) {
				((Agent) obj).initialize();
			}
		}		
		
		// Invoke the additional graph window
		if (params.getValueAsString("show_histogram").equals("yes")) {
			resourceStatistics = new Histogram();
			context.add(resourceStatistics);
		}
		return context;
	}
	
	/**
	 * Reads a comma separated list of possible products (All capitalized)
	 * @param fileName The path to the comma separated list of possible products to be used
	 * for the simulation.
	 */
	public void readFile(String fileName) {
		//TODO: Make more robust
		possibleProducts = new HashMap<Integer,LinkedList<Character>>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			LinkedList<Character> product = new LinkedList<Character>();
			int input;
			int counter = 0;
			while((input = reader.read()) != -1){
				if((char)input != ',') {
					product.add((char)input);
				} else {
					possibleProducts.put(counter,product);
					counter++;
					product = new LinkedList<Character>();
				}
			}
			possibleProducts.put(counter++,product);
			reader.close();
			nrPossibleProducts = possibleProducts.size();
		}
		catch(FileNotFoundException ex) {
			System.out.println("File not found!");
		}
		catch(IOException ex) {
			System.out.println("FileReader error!");
		}
	}
		
	/**
	 * Increases the count for a given Resource in a selected statistical register
	 * @param letter The {@code char} representation of the Resource to be added 
	 * @param registerName The String name of the register (demand, supply, request or deplete)
	 */
	public static void addToRegister(char letter, String registerName) {
		Hashtable<Character,Integer> register = selectRegister(registerName);	
		if (register != null) {
			if (register.containsKey(letter)) {
				int count = register.get(letter);
				int newCount = count+1;
				register.put(letter, newCount);
			} else {
				register.put(letter, 1);
			}
			writeToRegister(registerName, register);			
		} 
	}
	
	/**
	 * Decreases the count for a given Resource in a selected statistical register
	 * @param letter The {@code char} representation of the Resource to be removed
	 * @param registerName The String name of the register (demand, supply, request or deplete)
	 */
	public static void removeFromRegister(char letter, String registerName) {		
		Hashtable<Character,Integer> register = selectRegister(registerName);
		if (register != null) {
			if (register.containsKey(letter)) {
				int count = register.get(letter);				
				if (count > 1) {
					int newCount = count-1;
					register.put(letter, newCount);
				} else {
					register.remove(letter);
				}
			} 
			writeToRegister(registerName, register);
		} 
	}
	
	/**
	 * Decreases the count for a given array of Resources in a selected statistical register
	 * @param array A {@code LinkedList} of letters that are to be removed from the given register
	 * @param registerName The register Name from which the letters should be removed
	 */
	public static void removeArrayFromRegister(LinkedList<Character> array, String registerName) {
		Hashtable<Character,Integer> register = selectRegister(registerName);
		for (char letter : array) {
			if (register != null) {
				if (register.containsKey(letter)) {
					int count = register.get(letter);				
					if (count > 1) {
						int newCount = count-1;
						register.put(letter, newCount);
					} else {
						register.remove(letter);
					}
				}
				writeToRegister(registerName, register);
			} 
		}
	}
	
	/**
	 * Decreases the register count for all Resources in a given Agent's inventory 
	 * @param inventory The Agent's inventory 
	 */
	public static void removeInventoryFromRegister(Hashtable<Character, 
			LinkedList<Resource>> inventory) {		
		
		for (Enumeration<LinkedList<Resource>> resources = inventory.elements(); 
				resources.hasMoreElements();) {
			LinkedList<Resource> list = resources.nextElement();
			char type = list.getFirst().getResourceType();
			int number = list.size();
			for (int i=0; i<number; i++) {
				removeFromRegister(type, "supply");
			}	
		}		
	}
	
	/**
	 * Returns the register that will be altered
	 * @param registerName The {@code String} name of the register to be returned
	 * @return A working copy of the register 
	 */
	private static Hashtable<Character,Integer> selectRegister(String registerName){
		Hashtable<Character,Integer> register = null;
		switch(registerName) {
			case "demand": register = letterDemandRegister; break;
			case "supply": register = letterSupplyRegister;	break;
			case "request": register = letterRequestRegister; break;
			case "deplete": register = letterDepletionRegister;	break;
		}
		return register;
	}
	
	/**
	 * Writes back an altered register to the static CybersymBuilder's fields
	 * @param registerName The {@code String} name of altered register to be written back 
	 * @param register The changed working copy of the register 
	 */
	private static void writeToRegister(String registerName, Hashtable<Character,Integer> register){
		switch(registerName) {
			case "demand": letterDemandRegister = register;	break;
			case "supply": letterSupplyRegister = register;	break;
			case "request": letterRequestRegister = register; break;
			case "deplete": letterDepletionRegister = register;	break;
		}			
	}
}



