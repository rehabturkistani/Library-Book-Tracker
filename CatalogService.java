import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

class CatalogService {
    /*
     * ExecutionStatistics instance to track the number of valid records processed,
     * search results found, books added, and errors encountered during the
     * execution of the program
     */
    private final ExecutionStatistics stats = new ExecutionStatistics();

    public void run(String[] args) {
        try {
            validateArguments(args);

            Path catalogPath = Path.of(args[0]);
            Path logPath = catalogPath.toAbsolutePath().getParent().resolve("errors.log");
            String operationArg = args[1];

            setupFiles(catalogPath);

            List<Book> catalog = new ArrayList<>();
            loadCatalog(catalogPath, logPath, catalog);

            executeOperation(operationArg, catalog, catalogPath, logPath);

            printStatistics();

        } catch (BookCatalogException e) {
            System.err.println("CRITICAL ERROR: " + e.getMessage());
        }
    }

    public void printHeader() {
        System.out.printf("%-30s %-20s %-15s %5s%n", "Title", "Author", "ISBN", "Copies");
        System.out.println("--------------------------------------------------------------------------------");
    }

    /*
     * validate command-line arguments and throw appropriate exceptions for missing
     * or invalid inputs
     * 
     * @param args the command-line arguments to validate
     * 
     * @throws InsufficientArgumentsException if fewer than 2 arguments are provided
     */
    private void validateArguments(String[] args)
            throws InsufficientArgumentsException, InvalidFileNameException {

        if (args.length < 2) {
            throw new InsufficientArgumentsException(
                    "At least two arguments required: <catalogFile.txt> <operation>");
        }

        if (!args[0].endsWith(".txt")) {
            throw new InvalidFileNameException(
                    "Catalog file name end with '.txt'.");
        }
    }

    /*
     * setup the catalog and log files, creating them if they do not exist
     * 
     * @param path the path to the catalog file to set up
     * 
     */
    private void setupFiles(Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            System.err.println("Error creating files: " + e.getMessage());
            System.exit(1);
        }
    }

    /*
     * load the catalog from the specified file, parsing each line into a Book
     * object and handling any errors by logging them
     * 
     * @param catalogPath the path to the catalog file to load
     */
    private void loadCatalog(Path catalogPath, Path logPath, List<Book> catalog) {

        try (BufferedReader br = Files.newBufferedReader(catalogPath)) {
            String line;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                try {
                    Book book = parseAndValidateBook(line);
                    catalog.add(book);
                    stats.incrementValidRecords();

                } catch (BookCatalogException e) {
                    logError(logPath, line, e);
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading catalog: " + e.getMessage());
        }
    }

    /*
     * execute the specified operation (add new book, search by ISBN, or search by
     * title keyword) and handle any errors by logging them
     * 
     * @param operationArg the argument specifying the operation to execute
     * 
     * @param catalog the current list of books in the catalog
     * 
     * @param catalogPath the path to the catalog file for saving updates
     * 
     * @param logPath the path to the log file for logging errors
     */
    private void executeOperation(String operationArg, List<Book> catalog, Path catalogPath, Path logPath) {
        try {
            if (isNewBookRecord(operationArg)) {

                Book newBook = parseAndValidateBook(operationArg);

                for (Book b : catalog) {
                    if (b.getIsbn().equals(newBook.getIsbn())) {
                        throw new DuplicateISBNException("ISBN already exists in catalog: " + newBook.getIsbn());
                    }
                }

                catalog.add(newBook);
                Collections.sort(catalog);
                saveCatalog(catalogPath, catalog);

                printHeader();
                printBook(newBook);
                stats.incrementBooksAdded();

            } else if (operationArg.matches("\\d{13}")) {

                boolean found = false;
                for (Book b : catalog) {
                    if (b.getIsbn().equals(operationArg)) {
                        if (!found) {
                            printHeader();
                            found = true;
                        }
                        printBook(b);
                        stats.incrementSearchResults();
                    }
                }
                if (!found)
                    System.out.println("No book found with ISBN: " + operationArg);

            } else {

                boolean found = false;
                String keyword = operationArg.toLowerCase();

                for (Book b : catalog) {
                    if (b.getTitle().toLowerCase().contains(keyword)) {
                        if (!found) {
                            printHeader();
                            found = true;
                        }
                        printBook(b);
                        stats.incrementSearchResults();
                    }
                }
                if (!found)
                    System.out.println("No books found matching title: " + operationArg);
            }

        } catch (BookCatalogException e) {
            logError(logPath, operationArg, e);
            System.err.println("Operation Error: " + e.getMessage());
        }
    }

    /*
     * PARSING
     * parse a line of text into a Book object, validating the format and content
     * 
     * @param text the line of text to parse and validate
     * 
     * @return a Book object created from the parsed text
     */

    private Book parseAndValidateBook(String text)
            throws BookCatalogException {

        String[] parts = text.split(":", -1);

        if (parts.length != 4) {
            throw new MalformedBookEntryException("Invalid format. Expected Title:Author:ISBN:Copies");
        }

        String title = parts[0].trim();
        String author = parts[1].trim();
        String isbn = parts[2].trim();
        String copiesStr = parts[3].trim();

        if (title.isEmpty())
            throw new MalformedBookEntryException("Title cannot be empty");

        if (author.isEmpty())
            throw new MalformedBookEntryException("Author cannot be empty");

        if (!isbn.matches("\\d{13}"))
            throw new InvalidISBNException("ISBN must be exactly 13 digits");

        int copies;

        try {
            copies = Integer.parseInt(copiesStr);
            if (copies <= 0)
                throw new MalformedBookEntryException("Copies must be a positive integer");
        } catch (NumberFormatException e) {
            throw new MalformedBookEntryException("Copies must be a valid integer");
        }

        return new Book(title, author, isbn, copies);
    }

    /*
     * SAVE
     * determine if the operation argument is a new book record based on the
     * presence of colons
     * and the expected format of a book entry
     * 
     * @param operationArg the argument specifying the operation to execute
     * 
     * @return true if the argument is a new book record, false otherwise
     */
    private boolean isNewBookRecord(String operationArg) {
        return operationArg.contains(":");
    }

    private void saveCatalog(Path catalogPath, List<Book> catalog) {
        try (BufferedWriter bw = Files.newBufferedWriter(catalogPath)) {
            for (Book book : catalog) {
                bw.write(book.toFileString());
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving to catalog: " + e.getMessage());
        }
    }

    /*
     * log errors to the specified log file
     * param logPath the path to the log file to write to
     * param offendingText the text that caused the error to be logged
     * param e the exception that was thrown, containing details about the error
     */
    private void logError(Path logPath, String offendingText, Exception e) {

        stats.incrementErrorCount();

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String logEntry = String.format("[%s] INVALID LINE: \"%s\"%n%s: %s%n", timestamp, offendingText,
                e.getClass().getSimpleName(), e.getMessage());

        try (BufferedWriter bw = Files.newBufferedWriter(logPath, StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            bw.write(logEntry);
        } catch (IOException ioException) {
            System.err.println("Failed to write to error log: " + ioException.getMessage());
        }
    }

    /*
     * print the details of a book in a formatted manner
     */

    private void printBook(Book book) {

        System.out.printf("%-30s %-20s %-15s %5d%n",
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getCopies());
    }

    /*
     * using the ExecutionStatistics instance to print a summary of the execution
     * results
     */
    private void printStatistics() {
        System.out.println("\nExecution Statistics");
        System.out.println("Valid records processed : " + stats.getValidRecords());
        System.out.println("Search results          : " + stats.getSearchResults());
        System.out.println("Books added             : " + stats.getBooksAdded());
        System.out.println("Errors encountered      : " + stats.getErrorCount());
    }
}