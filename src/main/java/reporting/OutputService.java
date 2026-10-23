package reporting;

import java.util.*;

public interface OutputService {
	public void outputExtent(String title, Collection subscribers) throws Exception;
	public String getName();
}
