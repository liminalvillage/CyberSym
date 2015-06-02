package cybersym1;

import java.util.LinkedList;

public abstract class Item {
	protected int cost;
	
	/**
	 * Returns the current cost of the individual Item
	 * @return The current cost of the individual Item
	 */
	public int getCost(){
		return cost;
	}
	
	/**
	 * Increases an Item's cost by a given amount ({@code Integer})
	 * @param additionalCost The additional cost that is added to the Item
	 */
	public void increaseCost(int additionalCost){
		cost += additionalCost;
	}
}

class Resource extends Item{
	private char resourceType;	
	
	/**
	 * Resource Constructor
	 * @param cost The amount of work already used in order to produce this Resource
	 * @param resourceType The {@code char} representation of the Resource type
	 */
	public Resource(int cost, char resourceType) {
		this.cost = cost;
		this.resourceType = resourceType;
	}	
	
	/**
	 * Returns a Resource's type as a {@code char} representation
	 * @return A Resource's type as a {@code char} representation
	 */
	public char getResourceType() {
		return resourceType;
	}	
}

class Product extends Item{
	private LinkedList<Character> product;
	private int size;
	
	/**
	 * Product Constructor for initial assembly of two Resources. The resources are destroyed in the process 
	 * @param resource1 A first Resource	
	 * @param resource2 A second Resource. It is appended to the first
	 */
	public Product(Resource resource1, Resource resource2) {
		product = new LinkedList<Character>();
		product.add(resource1.getResourceType());
		product.add(resource2.getResourceType());
		cost = resource1.getCost() + resource2.getCost() + 1;
		size = product.size();
		resource1 = null;
		resource2 = null;
	}
		
	/**
	 * Adds a basic Resource to an existing Product. The Resource is destroyed in the process.
	 * @param resource A resource to be added to the existing Product
	 * @param front True if the Resource must be appended to the front of the Product. False if 
	 * it should be appended to the end
	 */
	public void addResource(Resource resource, boolean front) {		
		if (front) {
			LinkedList<Character> newProduct = new LinkedList<Character>();
			newProduct.add(resource.getResourceType());
			newProduct.addAll(product);
			product = newProduct;						
		} else {
			product.add(resource.getResourceType());
		}
		cost+= resource.getCost() + 1;
		size+= 1;
		resource = null;		
	}
		
	/**
	 * Combines two products. The send one is destroyed in the process.
	 * @param part2	A second product that is to be added to the called one
	 */
	public void addProduct(Product part2) {	
		product.addAll(part2.getProduct());		
		size+= part2.getSize();
		cost+= part2.getCost() + 1;
		part2 = null;
	}
	
	/**
	 * Returns the instance's {@code inkedList<Character>} Product representation 
	 * @return The instance's {@code inkedList<Character>} Product representation 
	 */
	public LinkedList<Character> getProduct() {
		return product;
	}
	
	/**
	 * Returns the instance's Product size 
	 * @return The {@code int} Product size
	 */
	public int getSize() {
		return size;
	}
}
