package library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.GregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import audit.RecordingAuditLogger;
import loans.LoanExtent;
import resources.Resource;
import resources.ResourceExtent;
import subscribers.Pensioner;
import subscribers.PremiumMember;
import subscribers.SubscriberExtent;

class BorrowingLimitTest {
	private static final GregorianCalendar TODAY = new GregorianCalendar(2004, 0, 20);

	private LibraryServices library;

	@BeforeEach
	void setUp() {
		library = new LibraryServices(new ResourceExtent(), new SubscriberExtent(), new LoanExtent(), new RecordingAuditLogger());
	}

	@Test
	void pensionerCannotBorrowMoreThanThree() throws Exception {
		Pensioner pensioner = new Pensioner("Bill");
		library.addPensioner(pensioner);
		for (int i = 0; i < 3; i++) {
			library.loanResourceToSubscriber(newBook(), pensioner, TODAY);
		}

		Resource fourth = newBook();
		assertThrows(Exception.class, () -> library.loanResourceToSubscriber(fourth, pensioner, TODAY));
		assertEquals(3, pensioner.getLoans().size());
	}

	// Requirement 2: "Premium members can borrow up to 10 items"
	@Test
	void premiumMemberCannotBorrowMoreThanTen() throws Exception {
		PremiumMember member = new PremiumMember("Nina", 19.99);
		library.addPremiumMember(member);
		for (int i = 0; i < 10; i++) {
			library.loanResourceToSubscriber(newBook(), member, TODAY);
		}

		Resource eleventh = newBook();
		assertThrows(Exception.class, () -> library.loanResourceToSubscriber(eleventh, member, TODAY));
		assertEquals(10, member.getLoans().size());
	}

	private Resource newBook() {
		Resource book = new Resource("Book", "A book", "An author");
		library.addResource(book);
		return book;
	}
}
