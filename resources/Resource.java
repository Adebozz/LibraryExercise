package resources;

import reporting.OutputServiceContributor;
import loans.*;

public class Resource  implements OutputServiceContributor {
	public String id;
	public String title;
	public String gameType;
	public String type;
	public String author;
	
	public Loan loan;
	
	public Resource(String type, String title, String extra){
		this.type = type;
		this.title = title;
		if ( type.equals("Book")) {
			this.author = extra;
		}
		else {
			this.gameType = extra;
		}
	}
	public Resource(String type, String id, String title, String extra){
		this.type = type;
		this.id = id;
		this.title = title;
		if ( type.equals("Book")) {
			this.author = extra;
		}
		else {
			this.gameType = extra;
		}
	}
	void setId(String id) {
		this.id = id;
	}
	public String getId() {
		return id;
	}
	public String getTitle() {
		return title;
	}
	/**
	 * Records the loan against this resource. Games may be loaned to Students
	 * and Premium members; books may be loaned to any subscriber.
	 */
	public void setLoan(Loan loan) throws Exception {
		if ( this.loan != null ) {
			throw new Exception("This resource is already on loan: "+this.loan.getFormattedString());
		}
		this.loan = loan;
	}
	public void removeLoan() {
		loan = null;
	}
	public Loan getLoan() {
		return loan;
	}

	public int getMaxLoanPeriod() {
		if ( type.equals("Book") ) {
			return 21;
		}
		if ( gameType.equals("X-Box") ) {
			return 7;
		}
		else if ( gameType.equals("PS2") ) {
			return 7;
		}
		else if ( gameType.equals("Amstrad") ) {
			return 14;
		}
		else {
			return 28;
		}
	}
	public double getSurcharge() {
		if ( type.equals("Book") ) {
			return 0;
		}
		if ( gameType.equals("X-Box") ) {
			return 1.0;
		}
		else if ( gameType.equals("PS2") ) {
			return 1.5;
		}
		else if ( gameType.equals("Amstrad") ) {
			return .5;
		}
		else {
			return 0;
		}
	}
	public String getFormattedString() {
		if ( type.equals("Book") )
			return "Book - " + title + " by " + author;
		else
			return "Game - " + title + " for the " + gameType;
	}
}
