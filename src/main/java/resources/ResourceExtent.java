/*
 * Created on 02-Feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package resources;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ResourceExtent {
	private final Map resources = new Hashtable();
	private final AtomicInteger idCounter = new AtomicInteger();
	
	public ResourceExtent() {
	}
	public void createResource(Resource resource) {
		resource.setId(getNextId());
		resources.put(resource.getId(), resource);
	}
	public Resource findByPrimaryKey(String id) {
		return (Resource)resources.get(id);
	}
	public Collection getResources() {
		return resources.values();
	}
	public String getNextId() {
		return "R" + idCounter.getAndIncrement();
	}
}
