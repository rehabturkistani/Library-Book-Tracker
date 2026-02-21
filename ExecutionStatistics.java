class ExecutionStatistics {

    private int validRecords;
    private int searchResults;
    private int booksAdded;
    private int errorCount;

    public void incrementValidRecords() {
        validRecords++;
    }

    public void incrementSearchResults() {
        searchResults++;
    }

    public void incrementBooksAdded() {
        booksAdded++;
    }

    public void incrementErrorCount() {
        errorCount++;
    }

    public int getValidRecords() {
        return validRecords;
    }

    public int getSearchResults() {
        return searchResults;
    }

    public int getBooksAdded() {
        return booksAdded;
    }

    public int getErrorCount() {
        return errorCount;
    }
}
