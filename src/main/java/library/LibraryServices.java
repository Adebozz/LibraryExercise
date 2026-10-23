package library;
import java.util.*;

import loans.*;

import subscribers.*;
import resources.*;

public class LibraryServices {
	
	public LibraryServices() {
	}
	public void addResource(Resource resource) {
		ResourceExtent.INSTANCE.createResource(resource);
	}
	public void addPensioner(Pensioner pensioner) {
		SubscriberExtent.INSTANCE.createPensioner(pensioner);
	}
	public void addStudent(Student student) {
		SubscriberExtent.INSTANCE.createStudent(student);
	}
	public void addPremiumMember(PremiumMember premiumMember) {
		SubscriberExtent.INSTANCE.createPremiumMember(premiumMember);
	}
	
	public void loanResourceToSubscriber(Resource resource, Object subscriber, GregorianCalendar gregorianCalendar) throws Exception {
		LoanExtent.INSTANCE.createLoan(resource, subscriber, gregorianCalendar);
	}
	public void returnedResource(Loan loan, GregorianCalendar gregorianCalendar) {
		if ( loan != null ) {
			loan.setReturnDate(gregorianCalendar);
		}
	}

}
