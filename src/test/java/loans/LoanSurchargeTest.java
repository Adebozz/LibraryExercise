package loans;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;

import resources.Resource;

/**
 * Requirement 6: games incur a surcharge on return, based on platform (the
 * rate) and loan period (whether it applies at all - only once overdue).
 */
class LoanSurchargeTest {

	@Test
	void bookIsNeverSurchargedEvenWhenVeryLate() {
		Loan loan = loanedOn("Book", "A book", "An author", date(2004, Calendar.JANUARY, 1));
		assertEquals(0.0, loan.surchargeDue(date(2004, Calendar.MARCH, 1)));
	}

	@Test
	void gameReturnedOnTimeIsNotSurcharged() {
		Loan loan = loanedOn("Game", "Halo", "X-Box", date(2004, Calendar.JANUARY, 1));
		assertEquals(0.0, loan.surchargeDue(date(2004, Calendar.JANUARY, 8)), "day 7 is on time for X-Box");
	}

	@Test
	void overdueXboxGameIsSurchargedAtItsPlatformRate() {
		Loan loan = loanedOn("Game", "Halo", "X-Box", date(2004, Calendar.JANUARY, 1));
		assertEquals(1.0, loan.surchargeDue(date(2004, Calendar.JANUARY, 9)), "day 8 is overdue for X-Box");
	}

	@Test
	void overduePs2GameIsSurchargedAtItsPlatformRate() {
		Loan loan = loanedOn("Game", "Midtown madness", "PS2", date(2004, Calendar.JANUARY, 1));
		assertEquals(1.5, loan.surchargeDue(date(2004, Calendar.JANUARY, 9)), "day 8 is overdue for PS2");
	}

	@Test
	void overdueAmstradGameIsSurchargedAtItsPlatformRate() {
		Loan loan = loanedOn("Game", "An old game", "Amstrad", date(2004, Calendar.JANUARY, 1));
		assertEquals(0.5, loan.surchargeDue(date(2004, Calendar.JANUARY, 16)), "day 15 is overdue for Amstrad (14 day period)");
	}

	private static Loan loanedOn(String type, String title, String extra, GregorianCalendar loanDate) {
		Loan loan = new Loan();
		loan.setResource(new Resource(type, title, extra));
		loan.setLoanDate(loanDate);
		return loan;
	}

	private static GregorianCalendar date(int year, int month, int day) {
		return new GregorianCalendar(year, month, day);
	}
}
