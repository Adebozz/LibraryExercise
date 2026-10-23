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

	/**
	 * Every attempt is audited, accepted or rejected. Audit is fail-closed: if
	 * an accepted loan cannot be audited it is cancelled and the caller gets an
	 * exception. All of this happens under the lock, so no one sees a loan that
	 * is later cancelled.
	 */
	public Loan loanResourceToSubscriber(Resource resource, Object subscriber, GregorianCalendar gregorianCalendar) throws Exception {
		lock.lock();
		try {
			Loan loan;
			try {
				loan = loans.createLoan(resource, subscriber, gregorianCalendar);
			}
			catch (Exception rejection) {
				auditRejection(rejection, "LOAN_REJECTED",
						"resourceId=" + resourceId(resource) + " subscriberId=" + subscriberId(subscriber) + " date=" + format(gregorianCalendar));
				throw rejection;
			}
			try {
				audit.log(AUDIT_CATEGORY, "LOAN_CREATED", describe(loan, gregorianCalendar));
			}
			catch (RuntimeException auditFailure) {
				loans.cancelLoan(loan);
				throw new IllegalStateException("Loan cancelled because it could not be audited", auditFailure);
			}
			return loan;
		}
		finally {
			lock.unlock();
		}
	}
	/**
	 * The return is checked and audited before it is applied, so if the audit
	 * fails there is nothing to undo.
	 */
	public void returnedResource(Loan loan, GregorianCalendar gregorianCalendar) {
		if ( loan == null ) {
			return;
		}
		lock.lock();
		try {
			if ( loan.getReturnDate() != null ) {
				IllegalStateException rejection = new IllegalStateException("Return date has already been assigned");
				auditRejection(rejection, "RETURN_REJECTED", describe(loan, gregorianCalendar));
				throw rejection;
			}
			audit.log(AUDIT_CATEGORY, "LOAN_RETURNED", describe(loan, gregorianCalendar));
			loan.setReturnDate(gregorianCalendar);
		}
		finally {
			lock.unlock();
		}
	}

	/*
	 * Audit details use ids only, never names: the audit store keeps entries
	 * for a long time, so it should hold as little personal data as possible.
	 */
	private static final String AUDIT_CATEGORY = "LOAN";

	/**
	 * Records a refused attempt. If that audit fails too, the refusal still
	 * reaches the caller, with the audit failure attached to it.
	 */
	private void auditRejection(Exception rejection, String action, String detail) {
		try {
			audit.log(AUDIT_CATEGORY, action, detail + " reason=\"" + String.valueOf(rejection.getMessage()).replace('"', '\'') + "\"");
		}
		catch (RuntimeException auditFailure) {
			rejection.addSuppressed(auditFailure);
		}
	}
	private static String describe(Loan loan, GregorianCalendar date) {
		return "loanId=" + loan.getId() + " resourceId=" + resourceId(loan.getResource())
				+ " subscriberId=" + subscriberId(loan.getSubscriber()) + " date=" + format(date);
	}
	private static String resourceId(Resource resource) {
		return resource == null ? null : resource.getId();
	}
	private static String subscriberId(Object subscriber) {
		if ( subscriber instanceof Student ) {
			return ((Student)subscriber).getId();
		}
		if ( subscriber instanceof Pensioner ) {
			return ((Pensioner)subscriber).getId();
		}
		if ( subscriber instanceof PremiumMember ) {
			return ((PremiumMember)subscriber).getId();
		}
		return null;
	}
	private static String format(GregorianCalendar date) {
		return date == null ? null : date.toZonedDateTime().toLocalDate().toString();
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
