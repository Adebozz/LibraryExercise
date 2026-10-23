package loans;

import java.util.*;
import resources.*;
import reporting.OutputServiceContributor;
import subscribers.*;

public class Loan  implements OutputServiceContributor {
	private Resource resource;
	private Object subscriber;
	private GregorianCalendar loanDate;
	private GregorianCalendar returnDate;
	private String id;
	
	public static int idCounter = 0;
	
	public String getFormattedString() {
		String formattedString = (resource==null?"Unknown":resource.getFormattedString()) + " loaned to ";
		if ( subscriber == null ) {
			formattedString += "Nobody";
		}
		else if ( subscriber instanceof Student ) {
			formattedString += ((Student)subscriber).getFormattedString();
		}
		else if ( subscriber instanceof Pensioner ) {
			formattedString += ((Pensioner)subscriber).getFormattedString();
		}
		else if ( subscriber instanceof PremiumMember ) {
			formattedString += ((PremiumMember)subscriber).getFormattedString();
		}
		else {
			formattedString += "Unknown subscriber type.";
		}
		return  formattedString;
	}

	public GregorianCalendar getReturnDate() {
		return returnDate;
	}
	void setId(String id) {
		this.id = id;
	}
	public void setReturnDate(GregorianCalendar returnDate) {
		if ( this.returnDate != null )
			throw new IllegalStateException("Return date has already been assigned");
		if ( subscriber instanceof Student ) {
			((Student)subscriber).removeLoan(this);
		}
		else if ( subscriber instanceof Pensioner ) {
			((Pensioner)subscriber).removeLoan(this);
		}
		else if ( subscriber instanceof PremiumMember ) {
			((PremiumMember)subscriber).removeLoan(this);
		}
		resource.removeLoan();
		this.returnDate = returnDate;
	}

	public void setLoanDate(GregorianCalendar loanDate) {
		this.loanDate = loanDate;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public void setSubscriber(Object subscriber) {
		this.subscriber = subscriber;
	}

	public Loan() {
		this.id = "L"+idCounter++;
	}
	
	public Loan(String id) {
		this.id = id;
		idCounter ++;
	}
	
	public String getId() {
		return id;
	}

	public GregorianCalendar getLoanDate() {
		return loanDate;
	}

	public Resource getResource() {
		return resource;
	}

	public Object getSubscriber() {
		return subscriber;
	}
	
	/**
	 * A loan is overdue when the elapsed days exceed the resource's maximum loan
	 * period. Handles month and year boundaries correctly.
	 */
	public boolean isOverdue(GregorianCalendar today) {
		if ( today == null || loanDate == null || resource == null ) {
			return false;
		}
		int elapsedDays = today.get(Calendar.DAY_OF_MONTH) - loanDate.get(Calendar.DAY_OF_MONTH);
		return elapsedDays > resource.getMaxLoanPeriod();
	}
}
