import java.nio.file.Path;
import java.util.List;  
/**
 * UPDATED: Task to analyze and execute operations in a separate thread.
 */
public class OperationAnalyzerTask implements Runnable {
private final CatalogService service;
    private final String arg;
    private final Path path;
    private final Path log;
    private final List<Book> catalog;

     OperationAnalyzerTask(CatalogService service, String arg, Path path, Path log, List<Book> catalog) {
        this.service = service;
        this.arg = arg;
        this.path = path;
        this.log = log;
        this.catalog = catalog;
    }

    @Override
    public void run() {
service.executeOperation(arg, catalog, path, log);    }
}

