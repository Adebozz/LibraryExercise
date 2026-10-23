package library;

import java.util.GregorianCalendar;

import resources.Resource;
import subscribers.Pensioner;
import subscribers.PremiumMember;
import subscribers.Student;

/**
 * The resources, subscribers and loans the library starts with. Previously this
 * lived in the constructors of the static *Extent singletons.
 *
 * Loans go through LibraryServices so the seed data obeys the same business
 * rules as any other loan. A seed loan that breaks a rule fails start-up
 * rather than being silently skipped.
 *
 * The original data loaned Halo (R4, a game) to Elsie (S1, a pensioner), which
 * breaks requirement 4. Assumed the data was wrong, not the rule, and moved
 * that loan to Zach (S2, a student).
 */
public final class SeedData {
	private SeedData() {
	}

	public static void load(LibraryServices library) throws Exception {
		library.addResource(new Resource("Book", "Lord of the Rings", "J.R.Tolkien"));
		library.addResource(new Resource("Book", "2001 Space Odessey", "Arthur C Clarke"));
		library.addResource(new Resource("Book", "How to win at bingo", "Two fat ladies"));
		library.addResource(new Resource("Book", "The Cold. Don't let it kill you.", "J.R.Hartley"));
		library.addResource(new Resource("Game", "Halo", "X-Box"));
		library.addResource(new Resource("Game", "Midtown madness", "PS2"));

		library.addPensioner(new Pensioner("Bill"));
		library.addPensioner(new Pensioner("Elsie"));
		library.addStudent(new Student("Zach", "De Montfort University"));
		library.addStudent(new Student("Amelia", "Christchurch College"));
		library.addPremiumMember(new PremiumMember("Nina", 19.99));

		library.loanResourceToSubscriber(library.findResource("R1"), library.findSubscriber("S3"), new GregorianCalendar(2004, 0, 20));
		library.loanResourceToSubscriber(library.findResource("R5"), library.findSubscriber("S3"), new GregorianCalendar(2004, 0, 1));
		library.loanResourceToSubscriber(library.findResource("R4"), library.findSubscriber("S2"), new GregorianCalendar(2004, 0, 1));
	}
}
