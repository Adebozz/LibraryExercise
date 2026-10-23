package loans;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;

import resources.Resource;

/**
 * Books have a 21 day maximum loan period: day 21 is on time, day 22 is overdue.
 */
class LoanOverdueTest {

	@Test
	void bookReturnedOnDay21IsNotOverdue() {
		Loan loan = bookLoanedOn(date(2004, Calendar.JANUARY, 1));
		assertFalse(loan.isOverdue(date(2004, Calendar.JANUARY, 22)));
	}

	@Test
	void bookOnDay22IsOverdue() {
		Loan loan = bookLoanedOn(date(2004, Calendar.JANUARY, 1));
		assertTrue(loan.isOverdue(date(2004, Calendar.JANUARY, 23)));
	}

	@Test
	void overdueAcrossMonthBoundary() {
		Loan loan = bookLoanedOn(date(2004, Calendar.JANUARY, 31));
		// 30 days later
		assertTrue(loan.isOverdue(date(2004, Calendar.MARCH, 1)));
	}

	@Test
	void overdueAcrossYearBoundary() {
		Loan loan = bookLoanedOn(date(2003, Calendar.DECEMBER, 20));
		// 26 days later
		assertTrue(loan.isOverdue(date(2004, Calendar.JANUARY, 15)));
	}

	@Test
	void notOverdueAcrossMonthBoundary() {
		Loan loan = bookLoanedOn(date(2004, Calendar.JANUARY, 25));
		// 11 days later
		assertFalse(loan.isOverdue(date(2004, Calendar.FEBRUARY, 5)));
	}

	private static Loan bookLoanedOn(GregorianCalendar loanDate) {
		Loan loan = new Loan();
		loan.setResource(new Resource("Book", "A book", "An author"));
		loan.setLoanDate(loanDate);
		return loan;
	}

	private static GregorianCalendar date(int year, int month, int day) {
		return new GregorianCalendar(year, month, day);
	}
}
