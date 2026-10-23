/*
 * Created on 02-Feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package library;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import reporting.OutputService;

/**
 * @author Simon
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LibraryReportingService {
	private final LibraryServices library;
	// Output services may be added while a report is running on another thread
	private final Collection outputServices = new CopyOnWriteArrayList();
	
	public LibraryReportingService(LibraryServices library) {
		this.library = library;
	}
	public void outputSubscribersReport() {
		Collection subscribers = library.subscribersSnapshot();
		OutputService outputService;
		for (Iterator iter = outputServices.iterator(); iter.hasNext();) {
			outputService = (OutputService)iter.next();
			try {
				outputService.outputExtent("Subscribers", subscribers);
			}
			catch (Exception e) {
				System.out.println("Exception while outputing subscriber to service " + outputService.getName() + " - Original message: "+e.getMessage());
			}
		}
	}
	
	/**
	 * Generates a report on resources using the registered output services
	 *
	 */
	public void outputResourcesReport() {
		Collection resources = library.resourcesSnapshot();
		OutputService outputService;
		for (Iterator iter = outputServices.iterator(); iter.hasNext();) {
			outputService = (OutputService)iter.next();
			try {
				outputService.outputExtent("Resources", resources);
			}
			catch (Exception e) {
				System.out.println("Exception while outputing resource to service " + outputService.getName() + " - Original message: "+e.getMessage());
			}
		}
	}
	
	public void outputOutstandingLoansReport() {
		Collection outstandingLoans = library.outstandingLoansSnapshot();
		OutputService outputService;
		for (Iterator iter = outputServices.iterator(); iter.hasNext();) {
			outputService = (OutputService)iter.next();
			try {
				outputService.outputExtent("Loans", outstandingLoans);
			}
			catch (Exception e) {
				System.out.println("Exception while outputing loan to service " + outputService.getName() + " - Original message: "+e.getMessage());
			}
		}
	}
	public void addOutputService(OutputService outputService) {
		outputServices.add(outputService);
	}
	
	public void removeOutputService(OutputService outputService) {
		outputServices.remove(outputService);
	}
	

}
