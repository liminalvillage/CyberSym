package cybersym1;

/**
 * <h1>Resource</h1>
 * 
 * Representation of a basic Resource that can be extracted, transported and combined to consumable 
 * Products. Every action completed with relation to a given Resource increases its value.
 * 
 * @author Janosch Haber, 10400192, University of Amsterdam (UvA)
 * @date 06-05-2015
 */
public class Resource {

	// The individual Resource's features
	private int value;
	private char resourceType;
	
	/**
	 * Resource Constructor
	 * @param value The amount of work already used in order to produce this Resource
	 * @param resourceType The {@code char} representation of the Resource type
	 */
	public Resource(int value, char resourceType) {
		this.value = value;
		this.resourceType = resourceType;
	}
	
	/**
	 * Increases a Resource's value by a given amount ({@code Integer})
	 * @param additionalCost The additional cost that is added as value to the individual Resource
	 */
	public void increaseValue(int additionalCost){
		value+=additionalCost;
	}
	
	/**
	 * Returns the current value of the individual Resource
	 * @return The current value of the individual Resource
	 */
	public int getValue(){
		return value;
	}
	
	/**
	 * Returns a Resource's type as a {@code char} representation
	 * @return A Resource's type as a {@code char} representation
	 */
	public char getResourceType() {
		return resourceType;
	}	
}
