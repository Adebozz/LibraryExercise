# Trace Library Exercise

## Background

The library system records and monitors resources that are loaned to subscribers of the library service. You have inherited this codebase and are asked to improve it. It has not been reviewed recently, and the code may contain defects and the written requirements may be incomplete.

You are not expected to make every change you could think of. We are more interested in what you choose to prioritise, and why, than in how much you change. Choosing deliberately not to change something is a valid outcome.

## Getting started

The project is an old-style Java project with package folders at the repository root. `LibrarySimulator` (package `library`) is the entry point and demonstrates current behaviour.

Anyone on the team should be able to build, test and run your version with a single command on a clean machine, with no IDE. How you achieve that is up to you.

## Requirements

1. There are three subscriber types: Pensioners, Students and Premium members.
2. Premium members can borrow up to 10 items, pay a monthly subscription, and are entitled to priority reservation of new resources.
3. There are two resource types: Books and Games.
4. Games may only be borrowed by Students.
5. A resource can only be loaned to one subscriber at a time.
6. Games incur a surcharge on return, based on platform and loan period.

If you make assumptions about any of the above, record them. If you would normally ask a clarifying question, write down the question and the answer you assumed. You may send us questions; we may answer them with "use your judgement".

The system starts with pre-loaded resources, subscribers and loans.

## Tasks

1. Make this codebase safe and sensible to run in production, and be able to show how you know. Explain what you changed and why, and what you chose not to change.
2. The `FileOutputService` implementation does not currently work. Repair it and run it alongside the existing `ConsoleOutputService`.
3. Tests: add the tests that give you the most confidence in this system, and explain why you chose those scenarios. Your tests should give confidence that the system behaves correctly under its operating conditions (see below).

## Integration

### Platform events

The library system is to be integrated into Trace's internal platform, which uses event-driven communication between services. Describe which events the system should publish, the payloads, and how you would keep things consistent if delivery fails. Your reasoning matters more than any single answer.

### Audit

Trace's compliance team requires that all loan transactions are recorded in TraceAudit, including rejected attempts, using:

```java
AuditLogger.log(String category, String action, String detail)
```

Do not couple the core domain classes directly to infrastructure.

## Operational context

The system will be used by 50 concurrent branch librarians.

## Deliverables

- Your code, build and tests, with a commit history that shows how you worked.
- `DECISIONS.md`, no more than one page, covering:
  - problems you found and fixed
  - problems you found and deliberately left, and why
  - assumptions made and questions you would have asked
  - what you would do differently if rebuilding from scratch
  - how you used AI assistants, if at all
- Be prepared to walk us through your work live and make a small change to it.

## Use of AI assistants

You may use AI tools. You must disclose how you used them in `DECISIONS.md`. You are responsible for everything you submit and will be asked to explain and change it.
