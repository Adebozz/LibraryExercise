package library;

import java.util.GregorianCalendar;

import audit.ConsoleAuditLogger;
import loans.LoanExtent;
import reporting.ConsoleOutputService;
import reporting.FileOutputService;
import resources.*;
import subscribers.*;

public class LibrarySimulator {
	static final String REPORT_FILE = "library-report.txt";
	LibraryServices library;
	LibraryReportingService reporting;
	public LibrarySimulator() throws Exception {
		ResourceExtent resources = new ResourceExtent();
		SubscriberExtent subscribers = new SubscriberExtent();
		LoanExtent loans = new LoanExtent();
		library = new LibraryServices(resources, subscribers, loans, new ConsoleAuditLogger());
		reporting = new LibraryReportingService(library);
		reporting.addOutputService(new ConsoleOutputService());
		reporting.addOutputService(new FileOutputService(REPORT_FILE));
		SeedData.load(library);
	}
	
	public static void main(String[] args) throws Exception {
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
		Resource resource = library.findResource("R1");
		Object subscriber = library.findSubscriber("S1");
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
