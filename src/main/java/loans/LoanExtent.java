/*
 * Created on 02-Feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package loans;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import resources.*;
import subscribers.*;

public class LoanExtent {
	private final Map loans = new Hashtable();
	private final AtomicInteger idCounter = new AtomicInteger();
	
	public LoanExtent() {
	}
	public Loan createLoan(Resource resource, Object subscriber, GregorianCalendar gregorianCalendar) throws Exception {
		if ( resource == null ) {
			throw new NullPointerException("Resource parameter can not be null");
		}
		if ( subscriber == null ) {
			throw new NullPointerException("Subscriber parameter can not be null");
		}
		// Requirement 4: games may only be borrowed by Students. Checked before
		// any state changes, so there is nothing to undo.
		if ( resource.isGame() && !(subscriber instanceof Student) ) {
			throw new Exception("Games may only be borrowed by students.");
		}
		Loan loan = new Loan();
		// The loan must be complete before anything stores it: subscribers key
		// their loans by id, so the id cannot change after addLoan.
		loan.setId(getNextId());
		loan.setResource(resource);
		loan.setSubscriber(subscriber);
		loan.setLoanDate(gregorianCalendar);

		resource.setLoan(loan);
		try {
			if ( subscriber instanceof Student ) {
				((Student)subscriber).addLoan(loan);
			}
			else if ( subscriber instanceof Pensioner ) {
				((Pensioner)subscriber).addLoan(loan);
			}
			else if ( subscriber instanceof PremiumMember ) {
				((PremiumMember)subscriber).addLoan(loan);
			}
		}
		catch (Exception e) {
			// The subscriber rejected the loan, so release the resource again:
			// a rejected loan must leave no trace.
			resource.removeLoan();
			throw e;
		}
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
		return "L" + idCounter.getAndIncrement();
	}
}
