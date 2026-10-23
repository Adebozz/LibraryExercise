/*
 * Created on 02-Feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package loans;

import java.util.*;

import resources.*;
import subscribers.*;

public class LoanExtent {
	private final Map loans = new Hashtable();
	private int idCounter = 0;
	
	public LoanExtent() {
	}
	public Loan createLoan(Resource resource, Object subscriber, GregorianCalendar gregorianCalendar) throws Exception {
		if ( resource == null ) {
			throw new NullPointerException("Resource parameter can not be null");
		}
		if ( subscriber == null ) {
			throw new NullPointerException("Subscriber parameter can not be null");
		}
		Loan loan = new Loan();
		resource.setLoan(loan);
		if ( subscriber instanceof Student ) {
			((Student)subscriber).addLoan(loan);
		}
		else if ( subscriber instanceof Pensioner ) {
			((Pensioner)subscriber).addLoan(loan);
		}
		else if ( subscriber instanceof PremiumMember ) {
			((PremiumMember)subscriber).addLoan(loan);
		}
		loan.setResource(resource);
		loan.setSubscriber(subscriber);
		loan.setLoanDate(gregorianCalendar);
		loan.setId(getNextId());
		loans.put(loan.getId(), loan);
		return loan;
	}
	public Loan findByPrimaryKey(String id) {
		return (Loan)loans.get(id);
	}
	public Collection getLoans() {
		return loans.values();
	}
	public String getNextId() {
		return "L" + (idCounter++);
	}
}
