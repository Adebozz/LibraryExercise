package library;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.GregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import loans.LoanExtent;
import resources.Resource;
import resources.ResourceExtent;
import subscribers.Pensioner;
import subscribers.PremiumMember;
import subscribers.Student;
import subscribers.SubscriberExtent;

/**
 * Requirement 4: "Games may only be borrowed by Students."
 */
class GameEligibilityTest {
	private static final GregorianCalendar TODAY = new GregorianCalendar(2004, 0, 20);

	private LibraryServices library;
	private Resource game;

	@BeforeEach
	void setUp() {
		library = new LibraryServices(new ResourceExtent(), new SubscriberExtent(), new LoanExtent());
		game = new Resource("Game", "Halo", "X-Box");
		library.addResource(game);
	}

	@Test
	void studentCanBorrowGame() throws Exception {
		Student student = new Student("Zach", "De Montfort University");
		library.addStudent(student);

		library.loanResourceToSubscriber(game, student, TODAY);

		assertNotNull(game.getLoan());
	}

	@Test
	void pensionerCannotBorrowGame() {
		Pensioner pensioner = new Pensioner("Elsie");
		library.addPensioner(pensioner);

		assertThrows(Exception.class, () -> library.loanResourceToSubscriber(game, pensioner, TODAY));
		assertNull(game.getLoan(), "game must still be available");
	}

	@Test
	void premiumMemberCannotBorrowGame() {
		PremiumMember member = new PremiumMember("Nina", 19.99);
		library.addPremiumMember(member);

		assertThrows(Exception.class, () -> library.loanResourceToSubscriber(game, member, TODAY));
		assertNull(game.getLoan(), "game must still be available");
	}
}
