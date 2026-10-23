package library;

import java.util.GregorianCalendar;

import reporting.ConsoleOutputService;
import resources.*;
import subscribers.*;

public class LibrarySimulator {
	LibraryServices library;
	LibraryReportingService reporting;
	public LibrarySimulator() {
		library = new LibraryServices();
		reporting = new LibraryReportingService();
		reporting.addOutputService(new ConsoleOutputService());
	}
	
	public static void main(String[] args) {
		LibrarySimulator librarySimulator = new LibrarySimulator();
		librarySimulator.runSimulator();
	}
	
	public void runReports() {
		reporting.outputResourcesReport();
		reporting.outputSubscribersReport();
		reporting.outputOutstandingLoansReport();
	}
	public void runSimulator() {
		reporting.outputOutstandingLoansReport();
		Resource resource = ResourceExtent.INSTANCE.findByPrimaryKey("R1");
		Object subscriber = SubscriberExtent.INSTANCE.findByPrimaryKey("S1");
		try {
			library.loanResourceToSubscriber(resource, subscriber, new GregorianCalendar(2004, 00, 20));
		}
		catch (Exception e) {
			if ( e.getMessage().startsWith("This resource is already on loan") ) {
				System.out.println("Error - Resource can not be loaned");
			}
			else if ( e.getMessage().endsWith("has reached their borrowing limit.") ) {
				System.out.println("Error - Max Limit");
			}
			else {
				System.out.println(e.getMessage());
			}
		}
		
		reporting.outputOutstandingLoansReport();
		library.returnedResource(resource.getLoan(), new GregorianCalendar(2004, 00, 20));
		
		reporting.outputOutstandingLoansReport();
		try {
			library.loanResourceToSubscriber(resource, subscriber, new GregorianCalendar(2004, 00, 20));
		} 
		catch (Exception e) {
			if ( e.getMessage().startsWith("This resource is already on loan") ) {
				System.out.println("Error - Resource can not be loaned");
			}
			else if ( e.getMessage().endsWith("has reached their borrowing limit.") ) {
				System.out.println("Error - Max Limit");
			}
			else {
				System.out.println(e.getMessage());
			}
		} 
		reporting.outputOutstandingLoansReport();
	}
}
