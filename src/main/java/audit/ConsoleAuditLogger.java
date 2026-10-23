package audit;

/**
 * Stand-in adapter for TraceAudit, used by the simulator. A production adapter
 * would write to a durable outbox that is forwarded to TraceAudit.
 */
public class ConsoleAuditLogger implements AuditLogger {
	public void log(String category, String action, String detail) {
		System.out.println("AUDIT " + category + " " + action + " " + detail);
	}
}
