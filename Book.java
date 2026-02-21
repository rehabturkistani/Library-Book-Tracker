class Book implements Comparable<Book> {

    private final String title;
    private final String author;
    private final String isbn;
    private final int copies;

    /**
     * Constructor for Book class to create object
     * 
     * @param title
     * @param author
     * @param isbn
     * @param copies
     */
    public Book(String title, String author, String isbn, int copies) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.copies = copies;
    }

    /**
     * Getters for Book class to access private attributes
     * 
     * @return title, author, isbn, copies
     */

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public int getCopies() {
        return copies;
    }

    /**
     * Override compareTo method to sort books by title in alphabetical order,
     * ignoring case sensitivity
     * 
     * @param other the other book to compare to
     */
    @Override
    public int compareTo(Book other) {
        if (this.title == null || other.title == null)
            return 0;
        return this.title.compareToIgnoreCase(other.title);
    }

    /**
     * Override toString method to return a string representation of the book
     * 
     * @return string representation of the book
     */

    public String toFileString() {
        return String.format("%s:%s:%s:%d", title, author, isbn, copies);
    }
}