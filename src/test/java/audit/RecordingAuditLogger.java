package audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test adapter that keeps every entry in memory, and can be switched to fail
 * so tests can check what happens when TraceAudit is unavailable.
 */
public class RecordingAuditLogger implements AuditLogger {
	public record Entry(String category, String action, String detail) {
	}

	private final List<Entry> entries = Collections.synchronizedList(new ArrayList<>());
	private volatile boolean failing;

	public void log(String category, String action, String detail) {
		if (failing) {
			throw new IllegalStateException("TraceAudit unavailable");
		}
		entries.add(new Entry(category, action, detail));
	}

	public void startFailing() {
		failing = true;
	}

	public List<Entry> entries() {
		synchronized (entries) {
			return new ArrayList<>(entries);
		}
	}

	public Entry last() {
		List<Entry> all = entries();
		return all.isEmpty() ? null : all.get(all.size() - 1);
	}

	public long count(String action) {
		return entries().stream().filter(entry -> entry.action().equals(action)).count();
	}
}
