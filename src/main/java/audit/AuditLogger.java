package audit;

public interface AuditLogger {
	public void log(String category, String action, String detail);
}
