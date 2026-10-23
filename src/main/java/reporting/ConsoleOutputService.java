package reporting;

import java.util.*;

public class ConsoleOutputService implements OutputService {
	public ConsoleOutputService() {
	}
	
	public void outputExtent(String title, Collection subscribers) throws Exception {
		System.out.println("\n"+title+"\n-----------");
		OutputServiceContributor subscriber;
		for (Iterator iter = subscribers.iterator(); iter.hasNext();) {
			subscriber = (OutputServiceContributor)iter.next();
			System.out.println(subscriber.getFormattedString());
		}
	}
	
	public String getName() {
		return "Console output service";
	}
}
