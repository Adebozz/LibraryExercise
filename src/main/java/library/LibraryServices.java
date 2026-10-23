package library;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import audit.AuditLogger;

import loans.*;

import subscribers.*;
import resources.*;

/**
 * The single entry point to the library for all librarians.
 *
 * Every operation runs under one lock. A loan checks and changes several
 * objects (resource, subscriber, loan extent), so those steps must happen as
 * one atomic unit; thread-safe collections alone cannot give that. Operations
 * are in-memory and take microseconds, so one lock is ample for 50 librarians.
 *
 * Callers must go through this class: using the extents directly bypasses the lock.
 */
public class LibraryServices {
	private final ResourceExtent resources;
	private final SubscriberExtent subscribers;
	private final LoanExtent loans;
	private final AuditLogger audit;
	// Fair: waiting librarians are served in arrival order, so a busy caller
	// (e.g. frequent reports) cannot starve the others. Costs a little
	// throughput, which does not matter at this load.
	private final ReentrantLock lock = new ReentrantLock(true);

	public LibraryServices(ResourceExtent resources, SubscriberExtent subscribers, LoanExtent loans, AuditLogger audit) {
		this.resources = resources;
		this.subscribers = subscribers;
		this.loans = loans;
		this.audit = Objects.requireNonNull(audit, "audit");
	}
	public void addResource(Resource resource) {
		lock.lock();
		try {
			resources.createResource(resource);
		}
		finally {
			lock.unlock();
		}
	}
	public void addPensioner(Pensioner pensioner) {
		lock.lock();
		try {
			subscribers.createPensioner(pensioner);
		}
		finally {
			lock.unlock();
		}
	}
	public void addStudent(Student student) {
		lock.lock();
		try {
			subscribers.createStudent(student);
		}
		finally {
			lock.unlock();
		}
	}
	public void addPremiumMember(PremiumMember premiumMember) {
		lock.lock();
		try {
			subscribers.createPremiumMember(premiumMember);
		}
		finally {
			lock.unlock();
		}
	}
	public Resource findResource(String id) {
		lock.lock();
		try {
			return resources.findByPrimaryKey(id);
		}
		finally {
			lock.unlock();
		}
	}
	public Object findSubscriber(String id) {
		lock.lock();
		try {
			return subscribers.findByPrimaryKey(id);
		}
		finally {
			lock.unlock();
		}
	}

	public Loan loanResourceToSubscriber(Resource resource, Object subscriber, GregorianCalendar gregorianCalendar) throws Exception {
		lock.lock();
		try {
			return loans.createLoan(resource, subscriber, gregorianCalendar);
		}
		finally {
			lock.unlock();
		}
	}
	public void returnedResource(Loan loan, GregorianCalendar gregorianCalendar) {
		if ( loan == null ) {
			return;
		}
		lock.lock();
		try {
			loan.setReturnDate(gregorianCalendar);
		}
		finally {
			lock.unlock();
		}
	}

	/*
	 * Snapshots for reporting. Each is a copy taken under the lock, so a report
	 * sees a consistent state and can be written out (possibly slowly, to a
	 * file) without holding up librarians.
	 */
	public List resourcesSnapshot() {
		lock.lock();
		try {
			return new ArrayList(resources.getResources());
		}
		finally {
			lock.unlock();
		}
	}
	public List subscribersSnapshot() {
		lock.lock();
		try {
			return new ArrayList(subscribers.getSubscribers());
		}
		finally {
			lock.unlock();
		}
	}
	public List outstandingLoansSnapshot() {
		lock.lock();
		try {
			List outstanding = new ArrayList();
			for (Iterator iter = loans.getLoans().iterator(); iter.hasNext();) {
				Loan loan = (Loan) iter.next();
				if ( loan.getReturnDate() == null ) {
					outstanding.add(loan);
				}
			}
			return outstanding;
		}
		finally {
			lock.unlock();
		}
	}
}
