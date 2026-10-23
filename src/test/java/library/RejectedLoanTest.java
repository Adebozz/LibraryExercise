package library;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import audit.RecordingAuditLogger;
import loans.LoanExtent;
import resources.Resource;
import resources.ResourceExtent;
import subscribers.Pensioner;
import subscribers.SubscriberExtent;

/**
 * A rejected loan must not change any state. This is checked as an invariant
 * rather than against the exact limit, so it stays independent of the
 * borrowing-limit tests.
 */
class RejectedLoanTest {
	private static final GregorianCalendar TODAY = new GregorianCalendar(2004, 0, 20);

	private LibraryServices library;

	@BeforeEach
	void setUp() {
		library = new LibraryServices(new ResourceExtent(), new SubscriberExtent(), new LoanExtent(), new RecordingAuditLogger());
	}

	@Test
	void rejectedLoanLeavesResourceAvailable() {
		Pensioner pensioner = new Pensioner("Bill");
		library.addPensioner(pensioner);
		List<Resource> books = new ArrayList<>();

		// More attempts than any subscriber limit allows, so some must be rejected
		for (int i = 0; i < 6; i++) {
			Resource book = new Resource("Book", "Book " + i, "An author");
			library.addResource(book);
			books.add(book);
			try {
				library.loanResourceToSubscriber(book, pensioner, TODAY);
			}
			catch (Exception expected) {
				// rejected attempts are what this test is about
			}
		}

		long booksMarkedOnLoan = books.stream().filter(book -> book.getLoan() != null).count();
		assertEquals(pensioner.getLoans().size(), booksMarkedOnLoan,
				"every book marked on loan must belong to an accepted loan");
	}
}
