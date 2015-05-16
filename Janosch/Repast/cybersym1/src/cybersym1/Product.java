package cybersym1;

import java.util.LinkedList;

/**
 * <h1>Product</h1>
 * 
 * Representation of a compound product that was assembled from at least two separate parts.
 * Products can be extended, transported and possibly consumed (if fulfilling a demand).
 * 
 * @author Janosch Haber, 10400192, University of Amsterdam (UvA)
 * @date 06-05-2015
 */
public class Product {

	// A Product's features
	private LinkedList<Character> product;
	private int initialAssemblyCost;
	private int value;
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
		initialAssemblyCost = product.size();
		value = resource1.getValue() + resource2.getValue() + initialAssemblyCost;
		size = product.size();
		resource1 = null;
		resource2 = null;
	}
		
	/**
	 * Adds a basic Resource to an existing Product. The Resource is destroyed in the process.
	 * @param resource A resource to be added to the existing Product
	 * @param front True if the Resource must be appended to the front of the Product. False if 
	 * it should be appended to the end
	 * @return The {@code int} cost of adding the resource to the Product (Which is 1)
	 */
	public int addResource(Resource resource, boolean front) {		
		if (front) {
			LinkedList<Character> newProduct = new LinkedList<Character>();
			newProduct.add(resource.getResourceType());
			newProduct.addAll(product);
			product = newProduct;						
		} else {
			product.add(resource.getResourceType());
		}
		value+= resource.getValue() + 1;
		size+= 1;
		resource = null;		
		return 1;
	}
	
	/**
	 * Combines two products. The send one is destroyed in the process.
	 * @param part2	A second product that is to be added to the called one
	 *
	 * @return The {@code int} cost of adding the resource to the Product (The size of the part to be added)
	 */
	public int addProduct(Product part2) {	
		product.addAll(part2.getProduct());		
		size+= part2.getSize();
		value+= part2.getValue() + part2.getSize();
		part2 = null;
		return 1;
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
	
	/**
	 * Returns the instance's value (total production cost) 
	 * @return The {@code int} instance's value (total production cost)
	 */
	public int getValue() {
		return value;
	}	
	
	/**
	 * Returns the initial assembly cost
	 * @return The {@code int} initial assembly cost
	 * @return
	 */
	public int getInitialAssemblyCost() {
		return initialAssemblyCost;
	}
}
