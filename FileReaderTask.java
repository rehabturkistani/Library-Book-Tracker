import java.nio.file.Path;
import java.util.List;
/**
 * UPDATED: Task to read catalog file in a separate thread.
 */
public class FileReaderTask implements Runnable {
    private final CatalogService service;
    private final Path path;
    private final Path log;
    private final List<Book> catalog;
    

     FileReaderTask(CatalogService service, Path path, Path log, List<Book> catalog) {
        this.service = service;
        this.path = path;
        this.log = log;
        this.catalog = catalog;
    }

    @Override
    public void run() {
            service.loadCatalog(path, log, catalog);
        }
}
