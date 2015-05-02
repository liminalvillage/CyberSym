package cybersym1;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
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
 * The CybersymBuilder that initiates the Repast simulation, reads in the parameters and invokes
 * the environment (Context) and agents (Agents and Resources)
 * @author Janosch
 *
 */
public class CybersymBuilder implements ContextBuilder<Object> {
	
	public static int gridWidth;
	public static int gridHeight;
	public static HashMap<Integer,LinkedList<Character>> possibleProducts;	
	public static int nrPossibleProducts;

	@SuppressWarnings("rawtypes")
	@Override
	public Context build(Context<Object> context) {
		
		context.setId("cybersym1");
				
		Parameters params = RunEnvironment.getInstance().getParameters();
		gridWidth = (Integer)params.getValue("grid_width");
		gridHeight = (Integer)params.getValue("grid_height");
		String fileName = params.getValueAsString("file_name");
		readFile(fileName);		
		
		//NetworkBuilder<Object>netBuilder = new NetworkBuilder<Object>
		//	("miningNetwork", context, true);
		//netBuilder.buildNetwork();		
		ContinuousSpaceFactory spaceFactory=
				ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space =
				spaceFactory.createContinuousSpace("space", context, 
				new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), gridWidth, gridHeight);
		
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object>grid = gridFactory.createGrid("grid", context, 
				new GridBuilderParameters<Object>(new WrapAroundBorders (),
				new SimpleGridAdder<Object>(), true, gridWidth, gridHeight));		
		
		int AgentCount = (Integer)params.getValue("agent_count");
		int minLifespan = (Integer)params.getValue("min_lifespan");
		int maxLifespan = (Integer)params.getValue("max_lifespan");
		for (int i = 0; i < AgentCount; i++) {
			int lifespan = RandomHelper.nextIntFromTo(minLifespan, maxLifespan);
			context.add(new Agent(space, grid, lifespan));
		}
		
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
		
		for (Object obj : context) {
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj, (int)pt.getX(), (int)pt.getY());
		}
		
		for (Object obj : context) {
			if (obj.getClass() == Agent.class) {
				((Agent) obj).initialize();
			}
		}
				
		return context ;
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
			/*
			System.out.print("Created ");
			System.out.print(nrPossibleProducts);
			System.out.println(" possible Products.");
			System.out.println(possibleProducts.toString());
			*/
		}
		catch(FileNotFoundException ex) {
			System.out.println("File not found!");
		}
		catch(IOException ex) {
			System.out.println("FileReader error!");
		}
	}
}



