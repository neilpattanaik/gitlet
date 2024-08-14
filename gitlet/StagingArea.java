package gitlet;

import java.io.*;
import java.util.Iterator;

import static gitlet.Repository.*;
import static gitlet.Utils.*;

/**
 * Represents the staging area for files to be committed in the gitlet version-control system.
 */
public class StagingArea implements Iterable<String[]> {
    /** The index containing the staging area **/
    private static final File INDEX_DIR = join(GITLET_DIR, "index");

    /**
     * Gets the existing map storage or creates a new one if necessary.
     *
     * @return the current StageMapMemory object.
     */
    public static StageMapMemory getMapStorage() {
        if (INDEX_DIR.isFile()) {
            return Utils.readObject(INDEX_DIR, StageMapMemory.class);
        } else {
            StageMapMemory mapStorage = new StageMapMemory();
            Utils.writeObject(INDEX_DIR, mapStorage);
            return mapStorage;
        }
    }

    /**
     * Stages a file to be added in the next commit. Assumes the file is valid.
     * Throws an exception if the filename is invalid or if the file is not tracked.
     *
     * @param filename the name of the file to stage.
     */
    public static void stageFile(String filename) {
        StageMapMemory mapStorage = getMapStorage();
        // Check if file is staged for removal, and remove it from the removalMap if so
        if (mapStorage.removeFromRemovalStageMap(filename) != null) {
            Utils.writeObject(INDEX_DIR, mapStorage);
            return;
        }
        // Verify file exists
        File file = join(CWD, filename);
        if (!file.exists()) {
            throw new GitletException("File does not exist.");
        }

        // Build Blob Object with the file's contents (does not override any non-identical versions of the file)
        String blobHash = createBlob(file);

        // Add to stageMap only if the file does not match the existing version in the most recent commit
        String existingBlobHash = getHeadCommit().getBlobHash(filename);
        if (existingBlobHash == null || !existingBlobHash.equals(blobHash)) {
            // Associate filename with blobHash in the index file
            mapStorage.putIntoAdditionStageMap(filename, blobHash);
            Utils.writeObject(INDEX_DIR, mapStorage);
        }
    }

    /**
     * Checks if there are any staged changes.
     *
     * @return true if there are staged changes, false otherwise.
     */
    public static Boolean hasStagedChanges() {
        StageMapMemory mapStorage = getMapStorage();
        return !(mapStorage.removalStageMapIsEmpty() && mapStorage.additionStageMapIsEmpty());
    }

    /**
     * Removes a file, staging it for removal if it is tracked, or unstaging it if it is staged for addition.
     *
     * @param filename the name of the file to remove.
     */
    public static void removeFile(String filename) {
        // Check if file is currently staged for addition or not tracked in previous commit
        StageMapMemory mapStorage = getMapStorage();
        String existingBlobHash = getHeadCommit().getBlobHash(filename);
        String additionBlobHash = mapStorage.removeFromAdditionStageMap(filename);
        if (existingBlobHash == null && additionBlobHash == null) {
            throw new GitletException("No reason to remove the file.");
        }
        // Just unstage if addition and wasn't in previous commit
        if (existingBlobHash == null) {
            // Return after unstaging for addition
            Utils.writeObject(INDEX_DIR, mapStorage);
            return;
        }

        // Remove file if it hasn't already been deleted
        File file = join(CWD, filename);
        restrictedDelete(file);

        // Stage file for removal
        mapStorage.putIntoRemovalStageMap(filename, existingBlobHash);
        Utils.writeObject(INDEX_DIR, mapStorage);
    }

    /**
     * Clears the staging list.
     */
    public static void clearIndex() {
        StageMapMemory mapStorage = getMapStorage();
        mapStorage.clearMaps();
        Utils.writeObject(INDEX_DIR, mapStorage);
    }

    /**
     * Unstages a file, if it is staged for removal or addition. Otherwise, does nothing.
     *
     * @param filename the name of the file to unstage.
     */
    public static void unStage(String filename) {
        StageMapMemory mapStorage = getMapStorage();
        mapStorage.removeFromAdditionStageMap(filename);
        mapStorage.removeFromRemovalStageMap(filename);
    }

    /**
     * Returns an iterator over the staged files.
     *
     * @return an iterator over the staged files, providing 3-element arrays [filename, hash, "add" or "rm"].
     */
    @Override
    public Iterator<String[]> iterator() {
        return new StagedFileIterator();
    }

    /**
     * Iterator providing 3-element arrays [filename, hash, "add" or "rm"].
     */
    private static class StagedFileIterator implements Iterator<String[]> {
        StageMapMemory mapStorage = getMapStorage();
        private final Iterator<String> addKeySetIter = mapStorage.getAdditionStageMapKeys().iterator();
        private final Iterator<String> rmKeySetIter = mapStorage.getRemovalStageMapKeys().iterator();

        /**
         * Checks if there are more elements in the iteration.
         *
         * @return true if there are more elements, false otherwise.
         */
        @Override
        public boolean hasNext() {
            return (addKeySetIter.hasNext() || rmKeySetIter.hasNext());
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element, a 3-element array [filename, hash, "add" or "rm"].
         */
        @Override
        public String[] next() {
            String filename;
            if (addKeySetIter.hasNext()) {
                filename = addKeySetIter.next();
                return new String[]{filename, mapStorage.getFromAdditionStageMap(filename), "add"};
            } else {
                filename = rmKeySetIter.next();
                return new String[]{filename, mapStorage.getFromRemovalStageMap(filename), "rm"};
            }
        }
    }
}
