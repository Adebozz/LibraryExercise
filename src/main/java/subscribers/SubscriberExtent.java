/*
 * Created on 02-Feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package subscribers;

import java.util.*;

public class SubscriberExtent {
	static Map subscribers = new Hashtable();
	static int idCounter = 0;
	public static SubscriberExtent INSTANCE = new SubscriberExtent();
	
	private SubscriberExtent() {
		// Initialise and register library subscribers
		createPensioner(new Pensioner("Bill"));
		createPensioner(new Pensioner("Elsie"));
		createStudent(new Student("Zach", "De Montfort University"));
		createStudent(new Student("Amelia", "Christchurch College"));
		createPremiumMember(new PremiumMember("Nina", 19.99));
	}
	public void createPensioner(Pensioner pensioner) {
		pensioner.setId(getNextId());
		subscribers.put(pensioner.getId(), pensioner);
	}
	public void createStudent(Student student) {
		student.setId(getNextId());
		subscribers.put(student.getId(), student);
	}
	public void createPremiumMember(PremiumMember premiumMember) {
		premiumMember.setId(getNextId());
		subscribers.put(premiumMember.getId(), premiumMember);
	}
	public static String getNextId() {
		return "S" + (idCounter++);
	}
	public Object findByPrimaryKey(String id) {
		return subscribers.get(id);
	}
	public Collection getSubscribers() {
		return subscribers.values();
	}
}
