package library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.GregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import audit.RecordingAuditLogger;
import audit.RecordingAuditLogger.Entry;
import loans.Loan;
import loans.LoanExtent;
import resources.Resource;
import resources.ResourceExtent;
import subscribers.Pensioner;
import subscribers.Student;
import subscribers.SubscriberExtent;

/**
 * Compliance: every loan transaction is recorded in TraceAudit, including
 * rejected attempts. If the audit cannot be written, the transaction does not
 * happen (fail-closed).
 */
class AuditTest {
	private static final GregorianCalendar LOAN_DATE = new GregorianCalendar(2004, 0, 20);
	private static final GregorianCalendar RETURN_DATE = new GregorianCalendar(2004, 1, 3);

	private RecordingAuditLogger audit;
	private LibraryServices library;
	private Resource book;
	private Pensioner pensioner;

	@BeforeEach
	void setUp() {
		audit = new RecordingAuditLogger();
		library = new LibraryServices(new ResourceExtent(), new SubscriberExtent(), new LoanExtent(), audit);
		book = addBook(); // R0
		pensioner = new Pensioner("Bill");
		library.addPensioner(pensioner); // S0
	}

	@Test
	void acceptedLoanIsAudited() throws Exception {
		library.loanResourceToSubscriber(book, pensioner, LOAN_DATE);

		assertEquals(new Entry("LOAN", "LOAN_CREATED", "loanId=L0 resourceId=R0 subscriberId=S0 date=2004-01-20"), audit.last());
	}

	@Test
	void loanRejectedAtBorrowingLimitIsAudited() throws Exception {
		for (int i = 0; i < 3; i++) {
			library.loanResourceToSubscriber(addBook(), pensioner, LOAN_DATE);
		}
		Resource fourth = addBook(); // R4

		assertThrows(Exception.class, () -> library.loanResourceToSubscriber(fourth, pensioner, LOAN_DATE));

		assertEquals(new Entry("LOAN", "LOAN_REJECTED",
				"resourceId=R4 subscriberId=S0 date=2004-01-20 reason=\"Bill has reached their borrowing limit.\""), audit.last());
	}

	@Test
	void loanRejectedBecauseResourceIsOnLoanIsAudited() throws Exception {
		library.loanResourceToSubscriber(book, pensioner, LOAN_DATE);
		Pensioner other = new Pensioner("Elsie");
		library.addPensioner(other); // S1

		assertThrows(Exception.class, () -> library.loanResourceToSubscriber(book, other, LOAN_DATE));

		Entry entry = audit.last();
		assertEquals("LOAN_REJECTED", entry.action());
		assertTrue(entry.detail().startsWith("resourceId=R0 subscriberId=S1 date=2004-01-20 reason=\"This resource is already on loan"), entry.detail());
	}

	@Test
	void gameRefusedToPensionerIsAudited() {
		Resource game = new Resource("Game", "Halo", "X-Box");
		library.addResource(game); // R1

		assertThrows(Exception.class, () -> library.loanResourceToSubscriber(game, pensioner, LOAN_DATE));

		assertEquals(new Entry("LOAN", "LOAN_REJECTED",
				"resourceId=R1 subscriberId=S0 date=2004-01-20 reason=\"Games may only be borrowed by students.\""), audit.last());
	}

	@Test
	void returnIsAudited() throws Exception {
		Loan loan = library.loanResourceToSubscriber(book, pensioner, LOAN_DATE);

		library.returnedResource(loan, RETURN_DATE);

		assertEquals(new Entry("LOAN", "LOAN_RETURNED", "loanId=L0 resourceId=R0 subscriberId=S0 date=2004-02-03 surcharge=0.0"), audit.last());
	}

	@Test
	void lateGameReturnIncursPlatformSurchargeAndIsAudited() throws Exception {
		Resource game = new Resource("Game", "Halo", "X-Box"); // 7 day loan period, 1.0 surcharge
		library.addResource(game); // R1
		Student student = new Student("Zach", "De Montfort University");
		library.addStudent(student); // S1
		Loan loan = library.loanResourceToSubscriber(game, student, LOAN_DATE);
		GregorianCalendar lateReturn = new GregorianCalendar(2004, 0, 28); // 8 days later, overdue by 1

		double surcharge = library.returnedResource(loan, lateReturn);

		assertEquals(1.0, surcharge, "overdue X-Box game incurs its platform surcharge");
		assertEquals(new Entry("LOAN", "LOAN_RETURNED", "loanId=L0 resourceId=R1 subscriberId=S1 date=2004-01-28 surcharge=1.0"), audit.last());
	}

	@Test
	void secondReturnIsRejectedAndAudited() throws Exception {
		Loan loan = library.loanResourceToSubscriber(book, pensioner, LOAN_DATE);
		library.returnedResource(loan, RETURN_DATE);

		assertThrows(IllegalStateException.class, () -> library.returnedResource(loan, RETURN_DATE));

		assertEquals(new Entry("LOAN", "RETURN_REJECTED",
				"loanId=L0 resourceId=R0 subscriberId=S0 date=2004-02-03 reason=\"Return date has already been assigned\""), audit.last());
	}

	@Test
	void loanIsCancelledWhenAuditFails() {
		audit.startFailing();

		assertThrows(Exception.class, () -> library.loanResourceToSubscriber(book, pensioner, LOAN_DATE));

		assertNull(book.getLoan(), "book must still be available");
		assertEquals(0, pensioner.getLoans().size(), "pensioner must not hold the loan");
		assertTrue(library.outstandingLoansSnapshot().isEmpty(), "no loan may be stored");
	}

	@Test
	void returnIsBlockedWhenAuditFails() throws Exception {
		Loan loan = library.loanResourceToSubscriber(book, pensioner, LOAN_DATE);
		audit.startFailing();

		assertThrows(Exception.class, () -> library.returnedResource(loan, RETURN_DATE));

		assertNull(loan.getReturnDate(), "loan must still be open");
		assertSame(loan, book.getLoan(), "book must still be on loan");
		assertEquals(1, pensioner.getLoans().size());
	}

	@Test
	void rejectionIsStillReportedWhenAuditFails() {
		Resource game = new Resource("Game", "Halo", "X-Box");
		library.addResource(game);
		audit.startFailing();

		Exception rejection = assertThrows(Exception.class, () -> library.loanResourceToSubscriber(game, pensioner, LOAN_DATE));

		assertEquals("Games may only be borrowed by students.", rejection.getMessage(), "the caller still learns why the loan was refused");
		assertEquals(1, rejection.getSuppressed().length, "the audit failure is attached, not lost");
	}

	@Test
	void studentLoanOfGameIsAudited() throws Exception {
		Resource game = new Resource("Game", "Halo", "X-Box");
		library.addResource(game); // R1
		Student student = new Student("Zach", "De Montfort University");
		library.addStudent(student); // S1

		library.loanResourceToSubscriber(game, student, LOAN_DATE);

		assertEquals(new Entry("LOAN", "LOAN_CREATED", "loanId=L0 resourceId=R1 subscriberId=S1 date=2004-01-20"), audit.last());
	}

	private Resource addBook() {
		Resource resource = new Resource("Book", "A book", "An author");
		library.addResource(resource);
		return resource;
	}
}
