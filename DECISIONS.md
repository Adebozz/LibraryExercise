# Decisions

## Problems found and fixed

- **Borrowing limit was off-by-one.** `addLoan` on all three subscriber types rejected only once the *projected* count exceeded the limit (`> max`), so a Premium member could hold 11 items instead of 10. Changed to check the *current* count against the limit (`>= max`) before adding.
- **Rejected loans left the resource stuck on loan.** `LoanExtent.createLoan` called `resource.setLoan(loan)` before checking the subscriber's limit; if the subscriber rejected the loan, the resource was still marked on-loan with no way to free it. The resource is now released if the subscriber rejects.
- **Loan id was assigned after the loan was stored**, so a loan could be put into a subscriber's map keyed by an id it didn't have yet. Id is now assigned before the loan is handed to anything.
- **Games could be loaned to non-students.** Nothing enforced requirement 4. The seed data itself violated it (Halo loaned to a Pensioner). Added the check in `LoanExtent.createLoan`; reassigned the seed loan to a Student (see Assumptions).
- **Overdue calculation used day-of-month, not elapsed days**, so a loan taken out on the 28th and checked on the 2nd of the next month looked "not overdue" by date-of-month arithmetic. Replaced with real calendar-day differences (`ChronoUnit.DAYS`).
- **No concurrency control.** The services were effectively static/singleton `*Extent` classes with no locking; 50 concurrent librarians could race on id assignment, double-loan a resource, or corrupt a report mid-read. Refactored to instance-based `LibraryServices` with constructor-injected extents, serialised all read/write operations behind one fair `ReentrantLock`, made id generation atomic, and made reports operate on a snapshot copy taken under the lock so a slow report write can't block other librarians.
- **`FileOutputService` didn't work**: it opened the file once in the constructor (truncating it) and reopened it in `outputExtent` (truncating again), so only the last report survived, the writer was never closed, and it wasn't thread-safe. Rewritten to open/write/close per call in append mode, synchronized, alongside `ConsoleOutputService`.
- **No audit trail.** Compliance requires every loan transaction, accepted or rejected, logged via `AuditLogger`. Added an `AuditLogger` interface (kept out of the domain classes — `LibraryServices` is the only caller) with a `ConsoleAuditLogger` stand-in for the real TraceAudit adapter. Audit is fail-closed: if logging an accepted loan fails, the loan is cancelled (`LoanExtent.cancelLoan` undoes it completely) and the caller gets an exception with the audit failure attached as a suppressed exception, never silently swallowed.
- **README's "Getting started" was stale and didn't satisfy the brief's own requirement.** It still described "package folders at the repository root," three commits after sources moved to the standard Maven layout, and never stated the single build/test/run command the brief explicitly asks for. Updated it to name the three commands directly and note the layout move.
- **Game surcharge on return (requirement 6) was dormant and incomplete.** `Resource.getSurcharge()` existed but was never called, and even if it had been, it only varied by platform, not by loan period as the requirement says. Added `Loan.surchargeDue(returnDate)`: the platform sets the *rate* (unchanged), loan period decides *whether it applies at all* — the rate is charged once a return is overdue, and not otherwise, rather than inventing a per-day multiplier nothing asked for. `LibraryServices.returnedResource` now returns that amount and records it in the audit entry, since there's still no billing system to hand it to — see `PLATFORM-EVENTS.md`, where `loan.returned.v1` already carried this same figure.

## Problems found and deliberately left

- **Premium member priority reservation is not implemented.** "Priority reservation of new resources" has no existing reservation concept to extend, and the brief doesn't specify what priority means operationally (first refusal? a holds queue? notified before the catalogue is public?). Unlike the surcharge, there's no existing method or field to complete — building one means designing a reservation subsystem from nothing on a guess. Flagged rather than invented.
- **Raw `Exception` as the rejection type** (borrowing limit, game eligibility, double loan) is kept as-is. Callers currently distinguish cases by parsing `getMessage()` (see `LibrarySimulator`), which is fragile, but replacing it with typed exceptions touches every call site and test for a cosmetic win; not worth the diff size for this exercise.
- **`Loan`'s unused static `idCounter`** still increments on every `new Loan()` even though `LoanExtent` immediately overwrites the id with its own atomic counter. It's dead weight, not a bug (the value is always discarded), so I left it rather than touching `Loan`'s constructors for no behavioural change.

## Assumptions and questions

- Assumed the **seed data was wrong, not the rule**, for the Halo/Pensioner loan — moved it to a Student. (Question I'd have asked: should invalid seed data fail startup loudly, as it now does, or be skipped with a warning?)
- Assumed **audit detail strings should carry ids only, never names** — TraceAudit is long-retention, so minimising personal data in it seemed the safer default. (Would confirm with compliance.)
- Assumed a **fair lock serialising all operations is acceptable at this scale** (50 librarians, in-memory, microsecond operations) rather than finer-grained locking. Would ask if this is expected to scale beyond in-process use.
- **`Student.getMaxBorrowing()`'s Javadoc disagreed with its own code** — the comment said "up to 5 resources," the method returns `6`. Pensioner's equivalent method has no such comment, so this reads as a stale leftover, not a deliberate value — but nothing in the written requirements states a student limit at all, so there's no way to tell from the brief which number was actually intended. Left the code's existing behaviour (`6`) unchanged rather than guessing, and reworded the comment so it states the discrepancy instead of asserting either number is correct. This is a question for the business, not something I should resolve by editing a comment to agree with the code.

## What I'd do differently from scratch

- Give subscriber types a common interface instead of three parallel classes with identical `addLoan`/`removeLoan` shapes and `instanceof` chains scattered across `Loan`, `LoanExtent`, and `LibraryServices`.
- Use typed, checked exceptions per rejection reason instead of `Exception` + message-sniffing.
- Keep `*Extent` classes as pure in-memory stores from day one with no static state, so the concurrency work in this exercise wouldn't have been a refactor.

## Use of AI assistants

Used Claude Code throughout: analysing the baseline for defects, implementing each fix and its tests commit-by-commit, and drafting this document. All output was reviewed and tested (`./mvnw test`, 36/36 passing) before being committed.
