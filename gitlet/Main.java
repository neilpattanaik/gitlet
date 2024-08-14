package gitlet;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 * Usage: java gitlet.Main ARGS, where ARGS contains <COMMAND> <OPERAND1> <OPERAND2> ...
 * Handles various Gitlet commands and dispatches them to the appropriate methods.
 *
 */
public class Main {

    public static void main(String[] args) {
        try {

            if (args.length == 0) {
                throw new GitletException("Please enter a command.");
            }

            String command = args[0];

            if (!command.equals("init") && !Repository.isInitialized()) {
                throw new GitletException("Not in an initialized Gitlet directory.");
            }
            switch (command) {
                case "init":
                    checkArgs(args, 1);
                    Repository.init();
                    break;
                case "add":
                    checkArgs(args, 2);
                    StagingArea.stageFile(args[1]);
                    break;
                case "commit":
                    checkArgs(args, 2);
                    Repository.newCommit(args[1]);
                    break;
                case "rm":
                    checkArgs(args, 2);
                    StagingArea.removeFile(args[1]);
                    break;
                case "log":
                    checkArgs(args, 1);
                    Repository.log();
                    break;
                case "global-log":
                    checkArgs(args, 1);
                    Repository.globalLog();
                    break;
                case "find":
                    checkArgs(args, 2);
                    Repository.find(args[1]);
                    break;
                case "status":
                    checkArgs(args, 1);
                    Repository.status();
                    break;
                case "restore":
                    if (args.length == 3) {
                        if (!args[1].equals("--")) {
                            throw new GitletException("Incorrect operands.");
                        }
                        Repository.restore(args[2]);
                    } else {
                        if (args.length != 4 || !args[2].equals("--")) {
                            throw new GitletException("Incorrect operands.");
                        }
                        Repository.restore(args[3], args[1]);
                    }
                    break;
                case "branch":
                    checkArgs(args, 2);
                    Repository.createBranch(args[1]);
                    break;
                case "switch":
                    checkArgs(args, 2);
                    Repository.switchBranch(args[1]);
                    break;
                case "rm-branch":
                    checkArgs(args, 2);
                    Repository.deleteBranch(args[1]);
                    break;
                case "reset":
                    checkArgs(args, 2);
                    Repository.reset(args[1]);
                    break;
                case "merge":
                    checkArgs(args, 2);
                    Repository.merge(args[1]);
                    break;
                default:
                    throw new GitletException("No command with that name exists.");
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /**
     * Checks if the number of arguments is correct for the given command.
     *
     * @param args the command-line arguments.
     * @param expected the expected number of arguments.
     * @throws GitletException if the number of arguments is incorrect.
     */
    private static void checkArgs(String[] args, int expected) {
        if (args.length != expected) {
            throw new GitletException("Incorrect operands.");
        }
    }
}
