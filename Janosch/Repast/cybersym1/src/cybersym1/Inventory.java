package cybersym1;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Inventory {
	public Hashtable<Character,LinkedList<Resource>> resourceInventory;			
	public Hashtable<List<Character>,LinkedList<Product>> productInventory;
	
	public Inventory() {
		resourceInventory = new Hashtable<Character,LinkedList<Resource>>();
		productInventory = new Hashtable<List<Character>,LinkedList<Product>>();	
	}
	
	/**
	 * Returns a {@code Product} from the inventory that matches the List representation in query.
	 * The Product is removed from the inventory through this process.
	 * 
	 * @param query	A {@code List} representation of the Product to be fetched from supply
	 * @return A {@code Product} from the inventory that matches the List representation in query.
	 */
	public Product get(List<Character> query) {
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
	public Resource get(char query) {
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
	public int getNrResources() {
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
	public int getNrProducts() {
		int counter = 0;
		Set<List<Character>> keys = productInventory.keySet();
		for(List<Character> key: keys){
			counter+= productInventory.get(key).size();
        }
		return counter;
	}
	
	/**
	 * Adds a Resource to the {@code resourceInventory}
	 * @param newResource A new Resource to be added to the inventory.
	 */
	public void add(Resource newResource) {
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
	 * Adds a Resource to the {@code resourceInventory}
	 * @param newResource A new Resource to be added to the inventory.
	 */
	public void addAll(char key, LinkedList<Resource> newResources) {
		if (resourceInventory.containsKey(key)) {
			LinkedList<Resource> supply = resourceInventory.get(key);
			supply.addAll(newResources);
			resourceInventory.put(key,supply);
		} else {
			LinkedList<Resource> newSupply = new LinkedList<Resource>();
			newSupply.addAll(newResources);
			resourceInventory.put(key,newSupply);		
		}
	}
	
	/**
	 * Adds a Product to the {@code productInventory}
	 * @param newProduct A new Product to be added to the inventory.
	 */
	public void add(Product newProduct) {
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
	
	public void addAll(List<Character> key, LinkedList<Product> newProducts) {
		if (productInventory.containsKey(key)) {
			LinkedList<Product> supply = productInventory.get(key);
			supply.addAll(newProducts);
			productInventory.put(key,supply);
		} else {
			LinkedList<Product> newSupply = new LinkedList<Product>();
			newSupply.addAll(newProducts);
			productInventory.put(key,newSupply);
		}
	}
	
	public boolean contains (char resource) {
		return resourceInventory.containsKey(resource);
	}
	
	public boolean contains (List<Character> product) {
		if (product.size() == 1) return resourceInventory.containsKey(product.get(0));
		else return productInventory.containsKey(product);
	}
	
	public int getNrOf(char resource) {
		if (resourceInventory.containsKey(resource)) return resourceInventory.get(resource).size();
		else return 0;
	}
	
	public int getNrOf(List<Character> product) {
		if (productInventory.containsKey(product)) return productInventory.get(product).size();
		else return 0;
	}
	
	/**
	 * Returns a {@code LinkedList} representation of an Agent's product inventory
	 * @return A {@code LinkedList} representation of an Agent's product inventory
	 */
	public LinkedList<List<Character>> getProductList() {
		return new LinkedList<List<Character>>(productInventory.keySet());	
	}
	
	public LinkedList<Character> getResourceList() {
		return new LinkedList<Character>(resourceInventory.keySet());
	}
	
	public void printResourceInventory() {
		System.out.println(resourceInventory.toString());
	}
	
	public void printProductInventory() {
		System.out.println(productInventory.toString());
	}
}
