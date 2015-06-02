package cybersym1;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.grid.Grid;

/**
 * <h1>Source</h1>
 * 
 * Instances of the Source class represent physical Sources of basic Resources in the environment. 
 * Sources produce a single resource type which can be extracted by Agents in the Source's 
 * neighborhood. A Source has a given quantity of Resources in stock. This quantity regenerates over 
 * time. 
 * 
 * @author Janosch Haber, 10400192, University of Amsterdam (UvA)
 * @date 29-05-2015
 */
public class Source {
	// Representation of the Environment (continuous + rasterized)	
	public Grid<Object> grid;
	
	// Features of the Source
	private int quantity;
	private int regenerationPeriod;
	private int currentRegenration = 0;
	private int extractionsInPeriod = 0;
	private char resource;

	/**
	 * Source Constructor
	 * @param grid The rasterized environment representation
	 * @param quantity The Source's initial contingent of resources
	 * @param regenerationPeriod The Source's regeneration period
	 * @param resource The resource that can be extracted from this Source
	 */
	public Source(Grid<Object> grid, int quantity, int regenerationPeriod, char resource) {
		this.grid = grid ;		
		this.regenerationPeriod = regenerationPeriod;
		this.quantity = quantity;
		this.resource = resource;
	}
	
	/**
	 * Scheduled method to invoke the Source's behavior. 
	 * After each tick, a time unit is added to the current regeneration counter. If it reaches the 
	 * Source's {@code regenerationPeriod}, the Source is regenerated.
	 */
	@ScheduledMethod(start=1, interval=1)
	public void step() {
		this.currentRegenration++;
		if (this.currentRegenration == this.regenerationPeriod) {
			regenerate();
		}
	}	
	
	/**
	 * Scheduled method to reset the extraction counter after each evaluation period
	 * TODO: Hardcoded interval
	 */
	@ScheduledMethod(start=1, interval=72)
	public void reset() {
		extractionsInPeriod = 0;
	}
	
	/**
	 * Regenerates the Source's resources by one unit
	 */
	public void regenerate() {
		this.quantity++; 
		this.currentRegenration = 0;		
	}
	
	/**
	 * Extracts one resource unit from the Source. By doing so, a Resource instance is created and 
	 * passed to the mining Agent. If the source is non-regenerative and the last Resource item is
	 * extracted from it, the Source is exhausted and it is removed from the environment.
	 * @return The extracted Resource instance
	 */
	public Resource extractResource() {
		if (quantity != 0) {
			Resource extractedResource = new Resource(0, this.getResourceType());
			this.quantity--;
			this.extractionsInPeriod++;
			return extractedResource;
		}
		return null;		
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
	 * @return The current individual Source's contingent in number of units ({@code Integer})
	 */
	public int getQuantity() {
		return this.quantity;
	}	
	
	/**
	 * Returns the regeneration period of the Source
	 * @return The regeneration period of the Source
	 */
	public int getRegenerationPeriod() {
		return regenerationPeriod;
	}
	
	/**
	 * Retruns the number of extractions during the current evaluation period
	 * @return The number of extractions during the current evaluation period
	 */
	public int getNrExtractionsInPeriod() {
		return extractionsInPeriod;
	}
	
	/**
	 * Returns the Source's Resource type and quantity for the visualization
	 * @return The Source's Resource type and quantity for the visualization (String)
	 */
	public String getLabel(){
		String label = String.valueOf(this.resource) + " - " + String.valueOf(this.getQuantity());
		return label;
	}
}
