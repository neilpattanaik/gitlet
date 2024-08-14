package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Repository.*;

/**
 * Represents a gitlet commit object.
 */
public class Commit implements Serializable {

    /** The message of this Commit. */
    protected String message;
    /** Stores mappings of file names to SHA hashes. */
    protected HashMap<String, String> blobMap;
    /** Stores the SHA hash of the parent commit. */
    protected String parentHashcode;
    /** Stores the commit timestamp. */
    protected Date timestamp;
    /** Date formatter used for String Representation and logging. */
    protected static final SimpleDateFormat FORMATTER = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");

    /**
     * Retrieves the parent commit of this commit.
     *
     * @return the parent commit, or null if there is no parent.
     */
    public Commit getParentCommit() {
        if (parentHashcode == null) {
            return null;
        }
        return getCommit(parentHashcode);
    }

    /**
     * Returns a string representation of the commit.
     *
     * @param hash the hash of the commit.
     * @return the string representation of the commit.
     */
    public String toString(String hash) {
        return "===\n"
                + "commit " + hash + "\n"
                + "Date: " + FORMATTER.format(this.getTimestamp()) + "\n"
                + this.getMessage() + "\n";
    }

    /**
     * Creates a new Commit object with a message and timestamp.
     * Iterates through the staging area and adds or replaces files in the blobMap.
     *
     * @param message the commit message.
     * @param parentHashcode the hash code of the parent commit.
     * @param map the blob map containing file-to-hash mappings.
     */
    public Commit(String message, String parentHashcode, HashMap<String, String> map) {
        this.blobMap = map;
        this.timestamp = new Date();
        this.message = message;
        this.parentHashcode = parentHashcode;
    }

    /**
     * Creates a new Commit object with a message and timestamp,
     * initializing the blobMap from the head commit.
     *
     * @param message the commit message.
     * @param parentHashcode the hash code of the parent commit.
     */
    public Commit(String message, String parentHashcode) {
        blobMap = Repository.getHeadCommit().blobMap;
        timestamp = new Date();

        StagingArea iterStagingArea = new StagingArea();
        for (String[] stageOperation : iterStagingArea) {
            if (stageOperation[2].equals("add")) {
                this.blobMap.put(stageOperation[0], stageOperation[1]);
            } else {
                this.blobMap.remove(stageOperation[0]);
            }
        }
        this.message = message;
        this.parentHashcode = parentHashcode;
    }

    /**
     * Returns the timestamp of this commit.
     *
     * @return the timestamp of this commit.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the hash of the parent commit.
     *
     * @return the hash of the parent commit.
     */
    public String getParentHash() {
        return this.parentHashcode;
    }

    /**
     * Returns the message of this commit.
     *
     * @return the message of this commit.
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Checks if this commit is tracking the specified file.
     *
     * @param filename the name of the file to check.
     * @return true if the file is tracked by this commit, false otherwise.
     */
    public boolean isTracking(String filename) {
        return blobMap.containsKey(filename);
    }

    /**
     * Retrieves the hash of a file's blob in this commit.
     *
     * @param filename the name of the file.
     * @return the hash of the file's blob, or null if the file is not tracked.
     */
    public String getBlobHash(String filename) {
        return blobMap.get(filename);
    }

    /**
     * Retrieves the blob map of this commit.
     *
     * @return the blob map of this commit.
     */
    public HashMap<String, String> getBlobMap() {
        return blobMap;
    }

    /**
     * Traverses back to the root, returning a set of parent commit hashes including itself.
     *
     * @param hash the hash of the starting commit.
     * @return a set of parent commit hashes.
     */
    public static Set<String> pathToRoot(String hash) {
        HashSet<String> hashes = new HashSet<>();
        Commit currentCommit;

        while (hash != null) {
            hashes.add(hash);
            currentCommit = getCommit(hash);
            hash = currentCommit.getParentHash();
            if (currentCommit instanceof MergeCommit) {
                hashes.addAll(pathToRoot(((MergeCommit) currentCommit).getSecondaryParentHashcode()));
            }
        }

        return hashes;
    }

    /**
     * Finds the latest common ancestor of two commits using BFS.
     *
     * @param commitOneHash the hash of the first commit.
     * @param commitTwoHash the hash of the second commit.
     * @return the hash of the latest common ancestor, or null if not found.
     */
    public static String getRecentAncestor(String commitOneHash, String commitTwoHash) {
        Set<String> commitOneAncestors = Commit.pathToRoot(commitOneHash);

        Queue<String> visitingQueue = new LinkedList<>();
        visitingQueue.add(commitTwoHash);
        String currentHash;
        Commit currentCommit;

        while (!visitingQueue.isEmpty()) {
            currentHash = visitingQueue.poll();
            if (commitOneAncestors.contains(currentHash)) {
                return currentHash;
            }

            currentCommit = getCommit(currentHash);
            visitingQueue.add(currentCommit.getParentHash());

            if (currentCommit instanceof MergeCommit) {
                visitingQueue.add(((MergeCommit) currentCommit).getSecondaryParentHashcode());
            }
        }

        return null; // Never Reached
    }

    /**
     * Returns a set of all files tracked by this commit.
     *
     * @return a set of all files tracked by this commit.
     */
    public Set<String> getAllTrackedFiles() {
        return blobMap.keySet();
    }
}
