package gitlet;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

import static gitlet.Utils.*;


/**
 *
 * The Repository class manages major Gitlet operations, including initialization,
 * branching, commits, merges, and status reporting. This class interacts with the file system
 * to track changes, manage branches, and handle the staging area. It maintains references
 * to various directories and files used in the version control system, such as the current
 * working directory, the .gitlet directory, objects, commits, and branches.
 * <p>
 * Main functionalities:
 * </p>
 * <ul>
 *   <li>Initialization of the Gitlet system</li>
 *   <li>Branch creation and deletion</li>
 *   <li>Commit management, including saving and retrieving commits</li>
 *   <li>File tracking and blob creation</li>
 *   <li>Merging branches and resolving conflicts</li>
 *   <li>Displaying the status of the working directory and staging area</li>
 * </ul>
 * The class ensures that operations are performed safely, avoiding conflicts and untracked file
 * overwrites, and provides error handling for invalid operations.
 * <p>
 * List of instance variables:
 * </p>
 * <ul>
 *   <li>{@code CWD} - The current working directory.</li>
 *   <li>{@code GITLET_DIR} - The .gitlet directory.</li>
 *   <li>{@code OBJECTS_DIR} - The objects directory which stores trees, blobs, and commits.</li>
 *   <li>{@code COMMITS_DIR} - The commits directory which stores commit objects.</li>
 *   <li>{@code BRANCH_POINTERS} - The branches directory which stores branch pointers.</li>
 *   <li>{@code INDEX_DIR} - The index containing the staging area.</li>
 *   <li>{@code HEAD} - The head pointer indicating the current branch.</li>
 * </ul>
 */
public class Repository {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The objects directory which stores trees, blobs, and commits. */
    private static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    /** The commits directory which stores commit objects. */
    private static final File COMMITS_DIR = join(OBJECTS_DIR, "commits");
    /** The branches directory which stores branch pointers. */
    private static final File BRANCH_POINTERS = join(GITLET_DIR, "branch_pointers");
    /** The head pointer indicating the current branch. */
    private static final File HEAD = join(GITLET_DIR, "head");

    /**
     * Initializes a new Gitlet version-control system in the current directory.
     * Creates the necessary directories and initial setup.
     * Will error if the .gitlet folder already exists in the CWD.
     */
    static void init() {

        // Create .gitlet directory, if it doesn't exist. Throws an error if it exists
        if (!GITLET_DIR.mkdir()) {
            throw new GitletException("A Gitlet version-control system already exists in the current directory.");
        }

        // Structure .gitlet folder
        OBJECTS_DIR.mkdir();
        BRANCH_POINTERS.mkdir();
        COMMITS_DIR.mkdir();

        // If HEAD pointer is null, assumes we haven't yet made the initial commit
        if (!isHeadValid()) {
            // Creates main branch and initial commit. Sets HEAD pointer.
            createBranch("main");
            pointHeadToBranch("main");
        }
    }


    /**
     * Checks if the Gitlet version-control system has already been initialized in the current directory.
     *
     * @return true if Gitlet is initialized, false otherwise.
     */
    static boolean isInitialized() {
        return GITLET_DIR.exists();
    }

    /**
     * Retrieves the name of the current head branch.
     *
     * @return the name of the current head branch.
     */
    static String getHead() {
        return readContentsAsString(HEAD);
    }


    /**
     * Retrieves the commit object associated with the head of the specified branch.
     *
     * @param branch the name of the branch to get the head commit from.
     * @return the commit object at the head of the specified branch.
     */
    static Commit getHeadCommit(String branch) {
        return getCommit(readContentsAsString(join(BRANCH_POINTERS, branch)));
    }


    /**
     * Serializes a commit object and stores it in the object store.
     *
     * @param commit the file to create a blob from.
     * @return the hashcode of the serialized commit.
     */
    static String saveCommit(Commit commit) {
        byte[] serializedCommit = serialize(commit);
        String hash = sha1(serializedCommit);
        writeContents(join(COMMITS_DIR, hash), serializedCommit);
        return hash;
    }

    /**
     * Gets pointer of branch.
     *
     * @param branch the branch to return the pointer of.
     * @return the file containing branch.
     */
    static File getBranchPointer(String branch) {
        return join(BRANCH_POINTERS, branch);
    }


    /**
     * Creates a new blob object from the given file and stores it in the object store.
     *
     * @param file the file to create a blob from.
     * @return the hashcode of the newly created blob.
     */
    static String createBlob(File file) {
        // Read file contents and store as byte array
        byte[] blob = readContents(file);
        // Get hashKey for blob
        String blobHash = sha1(blob);
        Utils.writeContents(join(OBJECTS_DIR, blobHash), blob);
        return blobHash;
    }


    /**
     * Creates a new branch with the given name, pointing to the current head commit.
     * If the current HEAD is null, will assume that this is the initial commit.
     * Calling createBranch NOT update the current head/branch, even if it creates the initial commit.
     *
     * @param branch the name of the new branch.
     */
    static void createBranch(String branch) {

        File reference = getBranchPointer(branch);

        if (reference.exists()) {
            throw new GitletException("A branch with that name already exists.");
        }

        // If the head is null, create an initial commit.
        String branchHeadHashcode;
        if (isHeadValid()) {
            branchHeadHashcode = getHeadCommitHashcode();
        } else {
            branchHeadHashcode = saveCommit(new Commit("initial commit", null, new HashMap<String, String>()));
        }

        writeContents(reference, branchHeadHashcode);
    }


    /**
     * Updates the head reference to point to the specified branch.
     *
     * @param newBranch the name of the branch to set as the new head.
     */
    static void pointHeadToBranch(String newBranch) {
        writeContents(HEAD, newBranch);
    }


    /**
     * Deletes the specified branch from the list of branches.
     * Errors if branch is the current branch or branch does not exist.
     *
     * @param branch the name of the branch to be deleted.
     */
    static void deleteBranch(String branch) {
        verifyBranchExists(branch, "A branch with that name does not exist.");
        if (branch.equals(getHead())) {
            throw new GitletException("Cannot remove the current branch.");
        }

        getBranchPointer(branch).delete();
    }


    /**
     * Switches to the specified branch, updating the working directory to match the state of that branch.
     *
     * @param newBranch the name of the branch to switch to.
     */
    static void switchBranch(String newBranch) {
        verifyBranchExists(newBranch, "No such branch exists.");
        if (checkBranchIsCurrentBranch(newBranch)) {
            throw new GitletException("No need to switch to the current branch.");
        }

        Commit currentBranchHead = getHeadCommit();
        Commit newBranchHead = getHeadCommit(newBranch);

        // Verifies there are no untracked files that would be overwritten
        verifyUntrackedFiles(currentBranchHead, newBranchHead);

        // Delete any tracked files not tracked in the new branch
        deleteTrackedFiles(currentBranchHead, newBranchHead);

        // Write all the files tracked in the new commit to CWD
        restoreAllTrackedFiles(newBranchHead);

        pointHeadToBranch(newBranch);
        StagingArea.clearIndex();
    }


    /**
     * Ensures that switching branches or resetting commits will not overwrite untracked files in the working directory.
     *
     * @param oldCommit the current commit before the switch/reset.
     * @param newCommit the commit to switch/reset to.
     */
    static void verifyUntrackedFiles(Commit oldCommit, Commit newCommit) {
        LinkedList<String> untrackedFiles = new LinkedList<String>(plainFilenamesIn(CWD));
        untrackedFiles.removeAll(oldCommit.getAllTrackedFiles());

        Set<String> newCommitTrackedFiles = newCommit.getAllTrackedFiles();

        if (!Collections.disjoint(untrackedFiles, newCommitTrackedFiles)) {
            throw new GitletException("There is an untracked file in the way; delete it, or add and commit it first.");
        }
    }


    /**
     * Deletes files tracked by the old commit but not by the new commit from the working directory.
     *
     * @param oldCommit the old commit.
     * @param newCommit the new commit.
     */
    static void deleteTrackedFiles(Commit oldCommit, Commit newCommit) {
        Set<String> filesToDelete = oldCommit.getAllTrackedFiles();
        filesToDelete.removeAll(newCommit.getAllTrackedFiles());
        for (String filename : filesToDelete) {
            restrictedDelete(join(CWD, filename));
        }
    }


    /**
     * Restores all files tracked by the specified commit to the working directory.
     *
     * @param commit the commit whose tracked files will be restored.
     */
    static void restoreAllTrackedFiles(Commit commit) {
        Set<String> trackedFiles = commit.getAllTrackedFiles();

        for (String filename : trackedFiles) {
            restore(filename, commit);
        }
    }


    /**
     * Checks if the head pointer exists.
     *
     * @return true if the head pointer exists, false otherwise.
     */
    private static Boolean isHeadValid() {
        if (!HEAD.isFile()) {
            return false;
        }

        return join(COMMITS_DIR, getHeadCommitHashcode()).isFile();
    }


    /**
     * Retrieves the commit object at the head of the current branch.
     *
     * @return the commit object at the head of the current branch.
     */
    public static Commit getHeadCommit() {
        String commitHash = readContentsAsString(join(BRANCH_POINTERS, getHead()));
        return getCommit(commitHash);
    }


    /**
     * Retrieves the hash key of the current head commit.
     *
     * @return the hash key of the current head commit.
     */
    public static String getHeadCommitHashcode() {
        return readContentsAsString(getBranchPointer(getHead()));
    }


    /**
     * Updates the head reference pointer to point to the specified commit hash.
     *
     * @param newHashcode the new commit hash to set the head reference to.
     */
    public static void updateBranchPointer(String newHashcode) {
        writeContents(getBranchPointer(getHead()), newHashcode);
    }


    /**
     * Retrieves the commit object associated with the given hash.
     *
     * @param hashcode the hash of the commit to retrieve.
     * @return the commit object associated with the given hash.
     */
    public static Commit getCommit(String hashcode) {
        File commitFile = join(COMMITS_DIR, hashcode);
        if (!commitFile.isFile()) {
            return null;
        }
        return readObject(commitFile, Commit.class);
    }


    /**
     * Verifies that branch exists, and throws GitletException if not.
     *
     * @param branch the name of the branch to check.
     */
    public static void verifyBranchExists(String branch, String errorMessage) {
        if (!join(BRANCH_POINTERS, branch).exists()) {
            throw new GitletException(errorMessage);
        }
    }


    /**
     * Returns true if and only if the given branch is the current branch. Returns false otherwise.
     *
     * @param branch the name of the branch to check.
     * @return true if the branch is current branch, false otherwise.
     */
    public static boolean checkBranchIsCurrentBranch(String branch) {
        return getHead().equals(branch);
    }


    /**
     * Retrieves a map of unstaged modified files in the current working directory.
     *
     * @param filesInCWD the list of files in the current working directory.
     * @return a map of file names to their modification statuses.
     */
    public static Map<String, String> getUnstagedModifiedFiles(List<String> filesInCWD) {
        Map<String, String> modifiedFiles = new HashMap<>();
        Commit headCommit = getHeadCommit();

        Map<String, String> blobMap = headCommit.getBlobMap();
        StageMapMemory stageMap = StagingArea.getMapStorage();

        for (String filename : filesInCWD) {
            String currentFileHash = sha1(readContents(join(CWD, filename)));

            if (blobMap.containsKey(filename)) {
                String trackedFileHash = blobMap.get(filename);

                if (!currentFileHash.equals(trackedFileHash) && !stageMap.additionStageMapContains(filename)) {
                    modifiedFiles.put(filename, "modified");
                }
            }

            if (stageMap.additionStageMapContains(filename)) {
                String stagedFileHash = stageMap.getFromAdditionStageMap(filename);

                if (!currentFileHash.equals(stagedFileHash)) {
                    modifiedFiles.put(filename, "modified");
                }
            }
        }

        for (String trackedFile : blobMap.keySet()) {

            if (!join(CWD, trackedFile).exists() && !stageMap.removalStageMapContains(trackedFile)) {
                modifiedFiles.put(trackedFile, "deleted");
            }
        }

        return modifiedFiles;
    }


    /**
     * Displays the status of the working directory and staging area.
     */
    public static void status() {
        System.out.println("=== Branches ===");
        List<String> branches = plainFilenamesIn(BRANCH_POINTERS);
        branches.sort(String::compareTo);
        for (String branch : branches) {
            if (branch.equals(getHead())) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        List<String> stagedFiles = new ArrayList<>();
        StageMapMemory stageMap = StagingArea.getMapStorage();
        stagedFiles.addAll(stageMap.getAdditionStageMapKeys());
        stagedFiles.sort(String::compareTo);
        for (String file : stagedFiles) {
            System.out.println(file);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        List<String> removedFiles = new ArrayList<>();
        removedFiles.addAll(stageMap.getRemovalStageMapKeys());
        removedFiles.sort(String::compareTo);
        for (String file : removedFiles) {
            System.out.println(file);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        Map<String, String> modifiedFiles = getUnstagedModifiedFiles(plainFilenamesIn(CWD));
        List<String> sortedModifiedFiles = new ArrayList<>(modifiedFiles.keySet());
        sortedModifiedFiles.sort(String::compareTo);
        for (String file : sortedModifiedFiles) {
            System.out.println(file + " (" + modifiedFiles.get(file) + ")");
        }
        System.out.println();

        System.out.println("=== Untracked Files ===");
        List<String> untrackedFiles = new ArrayList<>();
        for (String file : plainFilenamesIn(CWD)) {
            if (!stageMap.additionStageMapContains(file) && !isTracked(file)) {
                untrackedFiles.add(file);
            }
        }
        untrackedFiles.sort(String::compareTo);
        for (String file : untrackedFiles) {
            System.out.println(file);
        }
        System.out.println();
    }


    /**
     * Creates a new commit with the specified message, including all staged changes.
     *
     * @param message the commit message.
     */
    public static void newCommit(String message) {
        if (!StagingArea.hasStagedChanges()) {
            throw new GitletException("No changes added to the commit.");
        } else if (message.isBlank()) {
            throw new GitletException("Please enter a commit message.");
        }

        Commit newCommit = new Commit(message, getHeadCommitHashcode());
        String hashcode = Repository.saveCommit(newCommit);
        updateBranchPointer(hashcode);

        StagingArea.clearIndex();
    }


    /**
     * Resets the current branch to the specified commit ID.
     * Updates the working directory to match the state of the specified commit.
     *
     * @param commitID the (potentially truncated) commit ID to reset to.
     */
    public static void reset(String commitID) {
        Commit headCommit = getHeadCommit();
        String fullCommitID = getFullCommitID(commitID);
        Commit revertCommit = getCommit(fullCommitID);

        verifyUntrackedFiles(headCommit, revertCommit);
        deleteTrackedFiles(headCommit, revertCommit);
        restoreAllTrackedFiles(revertCommit);

        updateBranchPointer(fullCommitID);
        StagingArea.clearIndex();
    }


    /**
     * Displays the log of commits for the current branch, starting from the current head.
     */
    public static void log() {
        Commit currentCommit = getHeadCommit();
        String currentHash = getHeadCommitHashcode();

        while (currentCommit != null) {
            System.out.println(currentCommit.toString(currentHash));
            currentHash = currentCommit.getParentHash();
            currentCommit = currentCommit.getParentCommit();
        }
    }


    /**
     * Displays the global log of all commits in the repository.
     */
    public static void globalLog() {
        List<String> commitHashes = plainFilenamesIn(COMMITS_DIR);
        for (String commitHash : commitHashes) {
            System.out.println(getCommit(commitHash).toString(commitHash));
        }
    }


    /**
     * Finds and displays all commits that have the specified commit message.
     *
     * @param targetMessage the commit message to search for.
     */
    public static void find(String targetMessage) {
        boolean foundMatch = false;
        List<String> hashes = plainFilenamesIn(COMMITS_DIR);

        for (String hash : hashes) {
            if (getCommit(hash).getMessage().equals(targetMessage)) {
                System.out.println(hash);
                foundMatch = true;
            }
        }

        if (!foundMatch) {
            throw new GitletException("Found no commit with that message.");
        }
    }


    /**
     * Checks if a file is tracked by the current commit.
     *
     * @param filename the name of the file to check.
     * @return true if the file is tracked, false otherwise.
     */
    private static boolean isTracked(String filename) {
        Commit headCommit = getHeadCommit();
        return headCommit.getBlobMap().containsKey(filename);
    }


    /**
     * Restores the specified file to its state in the given commit.
     *
     * @param filename the name of the file to restore.
     * @param commit the commit to restore the file from.
     */
    public static void restore(String filename, Commit commit) {
        if (!commit.isTracking(filename)) {
            throw new GitletException("File does not exist in that commit.");
        }
        writeContents(join(CWD, filename), readContents(join(OBJECTS_DIR, commit.getBlobHash(filename))));
        StagingArea.unStage(filename);
    }


    /**
     * Restores the specified file to its state in the given commit.
     * Also, stages the file for addition so that it is saved in the following commit.
     *
     * @param filename the name of the file to restore.
     * @param commit the commit to restore the file from.
     */
    public static void restoreAndStage(String filename, Commit commit) {
        if (!commit.isTracking(filename)) {
            throw new GitletException("File does not exist in that commit.");
        }
        writeContents(join(CWD, filename), readContents(join(OBJECTS_DIR, commit.getBlobHash(filename))));
        StagingArea.stageFile(filename);
    }


    /**
     * Restores the specified file to its state in the current head commit.
     *
     * @param filename the name of the file to restore.
     */
    public static void restore(String filename) {
        restore(filename, getHeadCommit());
    }


    /**
     * Restores the specified file to its state in the given commit ID.
     *
     * @param filename the name of the file to restore.
     * @param commitID the commit ID to restore the file from.
     */
    public static void restore(String filename, String commitID) {
        restore(filename, getCommit(getFullCommitID(commitID)));
    }


    /**
     * Retrieves the full commit ID corresponding to the abbreviated ID.
     *
     * @param abbreviatedID the abbreviated commit ID.
     * @return the full commit ID.
     */
    public static String getFullCommitID(String abbreviatedID) {
        List<String> hashes = plainFilenamesIn(COMMITS_DIR);
        for (String hash : hashes) {
            if (hash.contains(abbreviatedID)) {
                return hash;
            }
        }

        throw new GitletException("No commit with that id exists.");
    }


    /**
     * Merges files from the given branch into the current branch.
     *
     * @param branchName the name of the branch to merge from.
     */
    public static void merge(String branchName) {
        if (StagingArea.hasStagedChanges()) {
            throw new GitletException("You have uncommitted changes.");
        }
        if (getHead().equals(branchName)) {
            throw new GitletException("Cannot merge a branch with itself.");
        }
        verifyBranchExists(branchName, "A branch with that name does not exist.");

        Commit currentCommit = getHeadCommit();
        Commit givenCommit = getHeadCommit(branchName);
        String currentCommitHash = getHeadCommitHashcode();
        String givenCommitHash = sha1(serialize(givenCommit));
        verifyUntrackedFiles(currentCommit, givenCommit);

        String splitPointHash = Commit.getRecentAncestor(currentCommitHash, givenCommitHash);
        Commit splitPoint = getCommit(splitPointHash);

        if (splitPointHash.equals(givenCommitHash)) {
            throw new GitletException("Given branch is an ancestor of the current branch.");
        }

        if (splitPointHash.equals(currentCommitHash)) {
            switchBranch(branchName);
            throw new GitletException("Current branch fast-forwarded.");
        }

        boolean conflict = false;
        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(currentCommit.getAllTrackedFiles());
        allFiles.addAll(givenCommit.getAllTrackedFiles());
        allFiles.addAll(splitPoint.getAllTrackedFiles());

        for (String file : allFiles) {
            String splitHash = splitPoint.getBlobHash(file);
            String currentHash = currentCommit.getBlobHash(file);
            String givenHash = givenCommit.getBlobHash(file);

            // Present in all commits
            if (splitHash != null && currentHash != null && givenHash != null) {
                // Modified in given branch, but unchanged in current branch
                if (splitHash.equals(currentHash) && !splitHash.equals(givenHash)) {
                    restoreAndStage(file, givenCommit);
                    continue;
                }
            }

            // modified in different ways
            if (!StringUtils.equals(splitHash, currentHash)
                    && !StringUtils.equals(splitHash, givenHash)
                    && !StringUtils.equals(currentHash, givenHash)) {
                resolveConflict(file, currentCommit, givenCommit);
                conflict = true;
                continue;
            }

            // Not in ancestor commit
            if (splitHash == null) {

                // Only in current branch, so we don't change anything
                if (givenHash == null) {
                    continue;
                }

                // Only in given branch, so we need to stage for addition
                restoreAndStage(file, givenCommit);
                continue;
            }

            // File IS in ancestor.
            // Unmodified in current branch, removed in given branch. Thus, remove the file.
            if (splitHash.equals(currentHash) && givenHash == null) {
                StagingArea.removeFile(file);
            }
        }

        Commit newCommit = new MergeCommit("Merged " + branchName + " into " + getHead() + ".",
                currentCommitHash, givenCommitHash);
        updateBranchPointer(saveCommit(newCommit));
        StagingArea.clearIndex();

        if (conflict) {
            throw new GitletException("Encountered a merge conflict.");
        }
    }


    /**
     * Resolves conflicts by creating a conflict file and staging it.
     *
     * @param file the filename with conflict.
     * @param currentCommit the current branch commit.
     * @param givenCommit the given branch commit.
     */
    private static void resolveConflict(String file, Commit currentCommit, Commit givenCommit) {
        String currentContent = "";
        String givenContent = "";
        if (currentCommit.isTracking(file)) {
            currentContent = readContentsAsString(join(OBJECTS_DIR, currentCommit.getBlobHash(file)));
        }
        if (givenCommit.isTracking(file)) {
            givenContent = readContentsAsString(join(OBJECTS_DIR, givenCommit.getBlobHash(file)));
        }
        String conflictContent = "<<<<<<< HEAD\n" + currentContent + "=======\n" + givenContent + ">>>>>>>\n";
        writeContents(join(CWD, file), conflictContent);
        StagingArea.stageFile(file);
    }
}
