package library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import audit.RecordingAuditLogger;
import loans.Loan;
import loans.LoanExtent;
import reporting.OutputService;
import resources.Resource;
import resources.ResourceExtent;
import subscribers.Pensioner;
import subscribers.Student;
import subscribers.SubscriberExtent;

/**
 * The library is used by 50 concurrent branch librarians. Each test releases 50
 * threads at the same instant through a start gate so their operations really
 * collide, and repeats the scenario because a race only shows when the timing
 * lines up. A pass is evidence, not proof, that there is no race.
 */
class ConcurrencyTest {
	private static final int LIBRARIANS = 50;
	private static final int ROUNDS = 200;
	private static final GregorianCalendar TODAY = new GregorianCalendar(2004, 0, 20);

	private ResourceExtent resources;
	private SubscriberExtent subscribers;
	private LoanExtent loans;
	private RecordingAuditLogger audit;
	private LibraryServices library;

	@BeforeEach
	void setUp() {
		newLibrary();
	}

	// Requirement 5: a resource can only be loaned to one subscriber at a time
	@Test
	void sameGameLoanedToFiftyStudentsAtOnceSucceedsExactlyOnce() throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			newLibrary();
			Resource game = addGame();
			List<Student> students = new ArrayList<>();
			List<Callable<Void>> attempts = new ArrayList<>();
			for (int i = 0; i < LIBRARIANS; i++) {
				Student student = new Student("Student " + i, "A college");
				library.addStudent(student);
				students.add(student);
				attempts.add(() -> {
					library.loanResourceToSubscriber(game, student, TODAY);
					return null;
				});
			}

			int successes = runAtTheSameTime(attempts);

			assertEquals(1, successes, "round " + round + ": exactly one loan may succeed");
			int loansHeldByStudents = students.stream().mapToInt(s -> s.getLoans().size()).sum();
			assertEquals(1, loansHeldByStudents, "round " + round + ": exactly one student holds the game");
			Student holder = (Student) game.getLoan().getSubscriber();
			assertTrue(holder.getLoans().contains(game.getLoan()), "round " + round + ": the game's loan belongs to its holder");
			assertEquals(1, audit.count("LOAN_CREATED"), "round " + round + ": one accepted loan audited");
			assertEquals(LIBRARIANS - 1, audit.count("LOAN_REJECTED"), "round " + round + ": every refused attempt audited");
		}
	}

	@Test
	void fiftyLoansToOnePensionerAtOnceStopAtTheLimit() throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			newLibrary();
			Pensioner pensioner = new Pensioner("Bill");
			library.addPensioner(pensioner);
			List<Resource> books = new ArrayList<>();
			List<Callable<Void>> attempts = new ArrayList<>();
			for (int i = 0; i < LIBRARIANS; i++) {
				Resource book = addBook();
				books.add(book);
				attempts.add(() -> {
					library.loanResourceToSubscriber(book, pensioner, TODAY);
					return null;
				});
			}

			int successes = runAtTheSameTime(attempts);

			assertEquals(3, successes, "round " + round + ": a pensioner may hold 3 loans");
			assertEquals(3, pensioner.getLoans().size(), "round " + round);
			long booksMarkedOnLoan = books.stream().filter(book -> book.getLoan() != null).count();
			assertEquals(3, booksMarkedOnLoan, "round " + round + ": every book on loan belongs to an accepted loan");
		}
	}

	@Test
	void concurrentLoansAllGetUniqueIdsAndNoneAreLost() throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			newLibrary();
			List<Callable<Void>> attempts = new ArrayList<>();
			for (int i = 0; i < LIBRARIANS; i++) {
				Resource book = addBook();
				Student student = new Student("Student " + i, "A college");
				library.addStudent(student);
				attempts.add(() -> {
					library.loanResourceToSubscriber(book, student, TODAY);
					return null;
				});
			}

			int successes = runAtTheSameTime(attempts);

			assertEquals(LIBRARIANS, successes, "round " + round + ": every loan is valid");
			Set<String> ids = new HashSet<>();
			for (Object loan : loans.getLoans()) {
				ids.add(((Loan) loan).getId());
			}
			assertEquals(LIBRARIANS, ids.size(), "round " + round + ": every accepted loan is stored under its own id");
		}
	}

	@Test
	void concurrentlyAddedResourcesAllGetUniqueIds() throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			newLibrary();
			List<Callable<Void>> additions = new ArrayList<>();
			for (int i = 0; i < LIBRARIANS; i++) {
				Resource book = new Resource("Book", "Book " + i, "An author");
				additions.add(() -> {
					library.addResource(book);
					return null;
				});
			}

			runAtTheSameTime(additions);

			assertEquals(LIBRARIANS, resources.getResources().size(), "round " + round + ": no resource overwritten");
		}
	}

	@Test
	void reportsRunWhileLoansAreBeingMade() throws Exception {
		Queue<Throwable> reportFailures = new ConcurrentLinkedQueue<>();
		LibraryReportingService reporting = new LibraryReportingService(library);
		reporting.addOutputService(new IteratingOutputService(reportFailures));
		AtomicBoolean loansFinished = new AtomicBoolean(false);

		Thread reporter = new Thread(() -> {
			while (!loansFinished.get()) {
				try {
					reporting.outputOutstandingLoansReport();
					reporting.outputResourcesReport();
				}
				catch (Throwable t) {
					reportFailures.add(t);
				}
			}
		});
		reporter.start();
		try {
			for (int round = 0; round < ROUNDS; round++) {
				List<Callable<Void>> attempts = new ArrayList<>();
				for (int i = 0; i < LIBRARIANS; i++) {
					Student student = new Student("Student " + round + "-" + i, "A college");
					library.addStudent(student);
					attempts.add(() -> {
						library.loanResourceToSubscriber(addBook(), student, TODAY);
						return null;
					});
				}
				runAtTheSameTime(attempts);
			}
		}
		finally {
			loansFinished.set(true);
			reporter.join();
		}

		assertTrue(reportFailures.isEmpty(), "reports failed while loans were made: " + reportFailures);
	}

	private void newLibrary() {
		resources = new ResourceExtent();
		subscribers = new SubscriberExtent();
		loans = new LoanExtent();
		audit = new RecordingAuditLogger();
		library = new LibraryServices(resources, subscribers, loans, audit);
	}

	private Resource addBook() {
		Resource book = new Resource("Book", "A book", "An author");
		library.addResource(book);
		return book;
	}

	private Resource addGame() {
		Resource game = new Resource("Game", "Halo", "X-Box");
		library.addResource(game);
		return game;
	}

	/**
	 * Runs every task on its own thread, released together by a start gate.
	 * Returns how many completed without throwing; a thrown exception counts as
	 * a rejected attempt, and the assertions on state catch anything worse.
	 */
	private static int runAtTheSameTime(List<Callable<Void>> tasks) throws InterruptedException {
		ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
		CountDownLatch ready = new CountDownLatch(tasks.size());
		CountDownLatch startGate = new CountDownLatch(1);
		AtomicInteger successes = new AtomicInteger();
		for (Callable<Void> task : tasks) {
			pool.submit(() -> {
				ready.countDown();
				startGate.await();
				try {
					task.call();
					successes.incrementAndGet();
				}
				catch (Exception rejected) {
					// counted by its absence from successes
				}
				return null;
			});
		}
		ready.await();
		startGate.countDown();
		pool.shutdown();
		assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "librarian threads did not finish");
		return successes.get();
	}

	/**
	 * Walks the collection it is given, as a real output service would, and
	 * records any failure. LibraryReportingService swallows exceptions from
	 * output services, so without this a failed report would go unnoticed.
	 */
	private static final class IteratingOutputService implements OutputService {
		private final Queue<Throwable> failures;

		IteratingOutputService(Queue<Throwable> failures) {
			this.failures = failures;
		}

		public void outputExtent(String title, Collection items) {
			try {
				for (Iterator iter = items.iterator(); iter.hasNext();) {
					iter.next();
				}
			}
			catch (Throwable t) {
				failures.add(t);
			}
		}

		public String getName() {
			return "Iterating test output service";
		}
	}
}
