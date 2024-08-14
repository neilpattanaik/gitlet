package gitlet;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeMap;

/**
 * Storage object for the additionStageMap and removalStageMap objects.
 * Workaround for Java type-checking when reading hashmaps from memory.
 */
public class StageMapMemory implements Serializable {
    private TreeMap<String, String> additionStageMap;
    private TreeMap<String, String> removalStageMap;

    /**
     * Initializes a new StageMapMemory object with empty addition and removal stage maps.
     */
    public StageMapMemory() {
        this.additionStageMap = new TreeMap<>();
        this.removalStageMap = new TreeMap<>();
    }

    /**
     * Clears the StageMapMemory object.
     */
    public void clearStageMapMemory() {
        clearMaps();
    }

    /**
     * Removes a file from the removal stage map.
     *
     * @param filename the name of the file to remove.
     * @return the hash of the removed file, or null if the file was not in the map.
     */
    public String removeFromRemovalStageMap(String filename) {
        return removalStageMap.remove(filename);
    }

    /**
     * Checks if the removal stage map is empty.
     *
     * @return true if the removal stage map is empty, false otherwise.
     */
    public boolean removalStageMapIsEmpty() {
        return removalStageMap.isEmpty();
    }

    /**
     * Checks if the addition stage map is empty.
     *
     * @return true if the addition stage map is empty, false otherwise.
     */
    public boolean additionStageMapIsEmpty() {
        return additionStageMap.isEmpty();
    }

    /**
     * Checks if a file is in the addition stage map.
     *
     * @param filename the name of the file to check.
     * @return true if the file is in the addition stage map, false otherwise.
     */
    public boolean additionStageMapContains(String filename) {
        return additionStageMap.containsKey(filename);
    }

    /**
     * Checks if a file is in the removal stage map.
     *
     * @param filename the name of the file to check.
     * @return true if the file is in the removal stage map, false otherwise.
     */
    public boolean removalStageMapContains(String filename) {
        return removalStageMap.containsKey(filename);
    }

    /**
     * Retrieves the hash of a file from the addition stage map.
     *
     * @param filename the name of the file to retrieve.
     * @return the hash of the file, or null if the file is not in the map.
     */
    public String getFromAdditionStageMap(String filename) {
        return additionStageMap.get(filename);
    }

    /**
     * Retrieves the hash of a file from the removal stage map.
     *
     * @param filename the name of the file to retrieve.
     * @return the hash of the file, or null if the file is not in the map.
     */
    public String getFromRemovalStageMap(String filename) {
        return removalStageMap.get(filename);
    }

    /**
     * Removes a file from the addition stage map.
     *
     * @param filename the name of the file to remove.
     * @return the hash of the removed file, or null if the file was not in the map.
     */
    public String removeFromAdditionStageMap(String filename) {
        return additionStageMap.remove(filename);
    }

    /**
     * Retrieves the set of keys (filenames) in the addition stage map.
     *
     * @return the set of keys in the addition stage map.
     */
    public Set<String> getAdditionStageMapKeys() {
        return additionStageMap.keySet();
    }

    /**
     * Retrieves the set of keys (filenames) in the removal stage map.
     *
     * @return the set of keys in the removal stage map.
     */
    public Set<String> getRemovalStageMapKeys() {
        return removalStageMap.keySet();
    }

    /**
     * Clears both the addition and removal stage maps.
     */
    public void clearMaps() {
        additionStageMap.clear();
        removalStageMap.clear();
    }

    /**
     * Adds a file and its hash to the addition stage map.
     *
     * @param filename the name of the file to add.
     * @param hash the hash of the file.
     */
    public void putIntoAdditionStageMap(String filename, String hash) {
        additionStageMap.put(filename, hash);
    }

    /**
     * Adds a file and its hash to the removal stage map.
     *
     * @param filename the name of the file to add.
     * @param hash the hash of the file.
     */
    public void putIntoRemovalStageMap(String filename, String hash) {
        removalStageMap.put(filename, hash);
    }
}
