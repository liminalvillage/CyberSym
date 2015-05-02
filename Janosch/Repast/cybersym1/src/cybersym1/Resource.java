package cybersym1;

/**
 * <h1>Resource</h1>
 * Representation of a basic Resource that can be extracted, transported and combined to consumable Products.
 * 
 * @author Janosch Haber, 10400192
 * @date 37.04.2015
 */
public class Resource {

	private int value;
	private char resourceType;
	
	public Resource(int value, char resourceType) {
		this.value = value;
		this.resourceType = resourceType;
	}
	
	public int getValue(){
		return value;
	}
	
	public char getResourceType() {
		return resourceType;
	}
	
	public void increaseValue(int additionalCost){
		value+=additionalCost;
	}
}
