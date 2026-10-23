package library;
import java.util.*;

import loans.*;

import subscribers.*;
import resources.*;

public class LibraryServices {
	private final ResourceExtent resources;
	private final SubscriberExtent subscribers;
	private final LoanExtent loans;
	
	public LibraryServices(ResourceExtent resources, SubscriberExtent subscribers, LoanExtent loans) {
		this.resources = resources;
		this.subscribers = subscribers;
		this.loans = loans;
	}
	public void addResource(Resource resource) {
		resources.createResource(resource);
	}
	public void addPensioner(Pensioner pensioner) {
		subscribers.createPensioner(pensioner);
	}
	public void addStudent(Student student) {
		subscribers.createStudent(student);
	}
	public void addPremiumMember(PremiumMember premiumMember) {
		subscribers.createPremiumMember(premiumMember);
	}
	public Resource findResource(String id) {
		return resources.findByPrimaryKey(id);
	}
	public Object findSubscriber(String id) {
		return subscribers.findByPrimaryKey(id);
	}
	
	public Loan loanResourceToSubscriber(Resource resource, Object subscriber, GregorianCalendar gregorianCalendar) throws Exception {
		return loans.createLoan(resource, subscriber, gregorianCalendar);
	}
	public void returnedResource(Loan loan, GregorianCalendar gregorianCalendar) {
		if ( loan != null ) {
			loan.setReturnDate(gregorianCalendar);
		}
	}

}
