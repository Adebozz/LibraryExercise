# Platform Events

The brief asks for a description, not an implementation — this records the reasoning the team would review, not working code. The audit requirement names a concrete interface to implement; this one does not, and guessing at a message-bus client would add infrastructure the exercise didn't ask for.

## Which events, and why

Only state changes other services plausibly need to react to. I'm proposing three, and explicitly rejecting a fourth.

| Event | Published when | Why another service needs it |
|---|---|---|
| `library.loan.created.v1` | A loan is accepted | A notifications service can confirm the loan to the subscriber; a catalogue/availability service can mark the resource unavailable without polling the library service. |
| `library.loan.returned.v1` | A resource is returned | Availability service can mark the resource free again. Carries the overdue/surcharge facts a billing service would need to charge a game-return fee (requirement 6) once that exists. |
| `library.subscriber.enrolled.v1` | A **Premium member** is added | Requirement 2 says Premium members pay a monthly subscription — a billing service has to find out they exist. Not raised for Pensioners/Students, who have no billing relationship. |

**Considered and rejected:** `library.loan.rejected`. Rejections are already compliance-logged via `AuditLogger` (requirement: audit includes rejected attempts). A second channel for the same fact would mean two systems of record for "why was this rejected" that can drift. If a consumer genuinely needs rejection data (e.g. an abuse-detection service watching repeated borrowing-limit hits), I'd want that requirement stated explicitly rather than assume it — this is exactly the kind of thing I'd ask the compliance/platform team rather than guess.

## Payloads

Every event carries an `eventId` (for consumer-side dedup — see below), `occurredAt`, and only ids, never subscriber names, matching the same minimise-personal-data reasoning as the audit entries.

```json
// library.loan.created.v1
{
  "eventId": "9f2c...",
  "occurredAt": "2004-01-20T00:00:00Z",
  "loanId": "L7",
  "resourceId": "R1",
  "resourceType": "Game",
  "subscriberId": "S2",
  "subscriberType": "Student",
  "loanDate": "2004-01-20",
  "dueDate": "2004-01-27"
}

// library.loan.returned.v1
{
  "eventId": "4a1e...",
  "occurredAt": "2004-02-03T00:00:00Z",
  "loanId": "L7",
  "resourceId": "R1",
  "subscriberId": "S2",
  "returnDate": "2004-02-03",
  "overdue": true,
  "daysOverdue": 7,
  "surcharge": 1.0
}

// library.subscriber.enrolled.v1
{
  "eventId": "7bd0...",
  "occurredAt": "2004-01-05T00:00:00Z",
  "subscriberId": "S4",
  "subscriberType": "PremiumMember",
  "monthlySubscription": 19.99
}
```

`dueDate`/`overdue`/`daysOverdue`/`surcharge` are derived from `Resource.getMaxLoanPeriod()`/`getSurcharge()` and `Loan.isOverdue()`, which already exist — the event is reporting facts the domain already computes, not inventing new ones. The `.v1` suffix is there from day one so a breaking payload change ships as `.v2` instead of mutating a type consumers already depend on.

## Keeping things consistent if delivery fails

The risky moment is the gap between "the loan was accepted" and "the event made it to the broker" — a crash or a down broker in between must not mean the fact is lost, and must not mean the librarian's operation fails because a downstream system is unavailable.

**Outbox, not direct publish.** `LibraryServices` would not call a message-bus client inline. It would append the event to a durable outbox — in the same atomic unit as the state change — under the same lock used today for the audit write (see `LibraryServices.loanResourceToSubscriber`). A separate relay reads the outbox and publishes to the broker, retrying with backoff until it gets an ack, then marks the row delivered. This is the same shape as a DB-outbox-plus-relay; in this exercise there's no database, so today that "outbox" would be an in-memory append-only queue drained by a background publisher — the design is what matters, the storage is swappable later.

**At-least-once, so consumers must dedupe.** The relay can crash after the broker acks but before marking the row delivered, and will then redeliver. Consumers key on `eventId` and discard duplicates, rather than the system trying to guarantee exactly-once delivery, which isn't achievable across a network boundary.

**Ordering is per-aggregate, not global.** A consumer must not process `loan.returned` before `loan.created` for the same loan, even under redelivery or retry. Partition/order by `loanId` (or `resourceId`), not by a single global sequence — global ordering isn't needed and would make the relay a bottleneck.

**Event publishing fails open; audit fails closed — deliberately asymmetric.** If the audit write fails, the loan is cancelled (see `DECISIONS.md`) because compliance requires the record to exist before the transaction stands. If the event broker is unreachable, the loan is **not** rolled back — the outbox row just waits for the relay to retry. Blocking a librarian's desk because a notifications queue is down is a worse availability trade than a few minutes of lag for other services. This is also why events live in a separate step from the audit write rather than reusing its fail-closed path.

**Don't let a stuck outbox go unnoticed.** Alert on the age of the oldest undelivered row, not just on relay errors — a relay that's silently stopped polling looks fine on error-rate dashboards but is still losing freshness.

## What I'd ask the platform team

- Is there an existing outbox/relay pattern or broker client teams are expected to use, or is this greenfield for this service?
- Does any consumer actually need `loan.rejected`, or is the audit log sufficient for that case?
- Expected consumer count/fan-out — it affects whether topic-per-event-type or a single stream with type discrimination is the better default here.
