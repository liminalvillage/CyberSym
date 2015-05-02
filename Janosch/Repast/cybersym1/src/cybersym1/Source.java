package cybersym1;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;

/**
 * <h1>Source</h1>
 * Instances of the Source class represent sources of basic resources in the environment. 
 * Sources have a limited capacity and may or may not regenerate. They produce a single resource which can be extracted 
 * by Agents in the Source's neighborhood. If the capacity of a Source is depleted, the Source is exhausted and cannot
 * regenerate any longer. It then is removed from the map. 
 * 
 * @author Janosch Haber, 10400192
 * @date 37.04.2015
 */
public class Source {
	// Representation of the Environment (continuous + rasterized)
	
	@SuppressWarnings("unused")
	private ContinuousSpace<Object> space;
	@SuppressWarnings("unused")
	private Grid<Object> grid;
	
	// Features of the Source
	private int quantity;
	private int regenerationPeriod;
	private int currentRegenration = 0;
	private char resource;

	/**
	 * Source Constructor
	 * @param space	The continuous environment representation
	 * @param grid The rasterized environment representation
	 * @param quantity The individual Source's current contingent of resources
	 * @param regenerationPeriod The individual Source's regeneration period
	 * @param resource The resource that can be extracted from this source
	 */
	public Source(ContinuousSpace<Object> space, Grid<Object> grid, int quantity, int regenerationPeriod, char resource) {
		this.space = space ;
		this.grid = grid ;
		this.quantity = quantity;
		this.regenerationPeriod = regenerationPeriod;
		this.resource = resource;
	}
	
	/**
	 * Scheduled method to invoke the Source's behavior. 
	 * After each tick, the time unit is added to the current regeneration counter. 
	 * If it reaches the Source's {@code regenerationPerio}, the Source is regenerated.
	 */
	@ScheduledMethod(start=1, interval=1)
	public void step() {
		this.currentRegenration++;
		if (this.currentRegenration == this.regenerationPeriod) {
			regenerate();
		}
	}	
	
	/**
	 * Regenerates the Source's resources by one unit
	 */
	public void regenerate() {
		this.quantity++; 
		this.currentRegenration = 0;		
	}
	
	/**
	 * Returns the resource that can be extracted from this Source
	 * @return The {@code char} resource that can be extracted from this Source
	 */
	public char getResourceType() {
		return this.resource;
	}
	
	/**
	 * Returns the current individual Source's contingent
	 * @return The current individual Source's contingent in number of units (int)
	 */
	public int getQuantity() {
		return this.quantity;
	}
	
	/**
	 * Extracts one resource unit from the Source. By doing so, a Resource instance is created and passed 
	 * to the mining Agent. If the source is exhausted, it is removed from the environment.
	 * @return The extracted Resource instance
	 */
	public Resource extractResource() {
		Resource extractedResource = new Resource(0, getResourceType());
		this.quantity--;
		if (quantity == 0) {
			@SuppressWarnings("unchecked")
			Context<Object> context = ContextUtils.getContext(this);
			context.remove(this);
		}
		return extractedResource;
	}
	
	/**
	 * Returns the Source's resource type and quantity for the visualization
	 * @return String
	 */
	public String getLabel(){
		String result = String.valueOf(this.resource);
		result+=" - ";
		result+=String.valueOf(this.getQuantity());
		return result;
	}
}
