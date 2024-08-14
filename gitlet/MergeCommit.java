package gitlet;

/**
 * Represents a gitlet merge commit object, which has two parent commits.
 */
public class MergeCommit extends Commit {
    /** Stores the hash of the second parent. */
    private final String secondaryParentHashcode;

    /**
     * Creates a new MergeCommit object with a message, primary parent hash, and secondary parent hash.
     *
     * @param message the commit message.
     * @param parentHashcode the hash code of the primary parent commit.
     * @param secondaryParentHashcode the hash code of the secondary parent commit.
     */
    public MergeCommit(String message,
                       String parentHashcode,
                       String secondaryParentHashcode) {
        super(message, parentHashcode);
        this.secondaryParentHashcode = secondaryParentHashcode;
    }

    /**
     * Retrieves the hash of the secondary parent commit.
     *
     * @return the hash of the secondary parent commit.
     */
    public String getSecondaryParentHashcode() {
        return this.secondaryParentHashcode;
    }

    /**
     * Returns a string representation of the merge commit.
     *
     * @param hash the hash of the commit.
     * @return the string representation of the merge commit.
     */
    @Override
    public String toString(String hash) {
        return "===\n"
                + "commit " + hash + "\n"
                + "Merge: " + this.getParentHash().substring(0, 7) + " "
                + this.getSecondaryParentHashcode().substring(0, 7) + "\n"
                + "Date: " + FORMATTER.format(this.getTimestamp()) + "\n"
                + this.getMessage() + " \n";
    }
}
