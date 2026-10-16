package reporting;

import java.io.FileWriter;
import java.util.Collection;
import java.util.Iterator;

public class FileOutputService implements OutputService {
	private FileWriter writer;
	private String path;
	
	public FileOutputService(String path) throws Exception {
		this.path = path;
		this.writer = new FileWriter(path);
	}
	
	public void outputExtent(String title, Collection subscribers) throws Exception {
		writer = new FileWriter(path);
		writer.write("\n" + title + "\n-----------\n");
		OutputServiceContributor subscriber;
		for (Iterator iter = subscribers.iterator(); iter.hasNext();) {
			subscriber = (OutputServiceContributor)iter.next();
			writer.write(subscriber.getFormattedString());
			writer.write("\n");
		}
		writer.flush();
	}
	
	public String getName() {
		return "File output service";
	}
}
