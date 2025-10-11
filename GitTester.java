import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class GitTester {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        resetRepo();
        Git.stage(new File(".gitignore"));
        Git.stage(new File("README.md"));
        Git.stage(new File("Git.java"));
        Git.stage(new File("GitTester.java"));
        Git.stage(new File("GitWrapper.java"));

        String commit = Git.commit("Shimon", "test commit");        
        Git.checkout(commit);
    }
    
    public static void verifyRepo() {
        File git = new File("git");
        File objects = new File(git, "objects");
        File index = new File(git, "index");
        File head = new File(git, "HEAD");

        if (git.exists() && objects.exists() && index.exists() && head.exists() && git.isDirectory() && objects.isDirectory() && index.isFile() && head.isFile()) {
            System.out.println("All files and directories exist.");
            return;
        }

        // if not all files/directories exist, see which one doesn't and make sure they are correctly files or directories

        if (git.exists() && git.isDirectory()) {
            System.out.println("Git directory exists");
        } else {
            System.out.println("Failed to make git directory");
        }

        if (objects.exists() && objects.isDirectory()) {
            System.out.println("Objects directory exists");
        } else {
            System.out.println("Failed to make objects directory");
        }

        if (index.exists() && index.isFile()) {
            System.out.println("Index file exists");
        } else {
            System.out.println("Failed to make index file");
        }

        if (head.exists() && head.isFile()) {
            System.out.println("HEAD file exists");
        } else {
            System.out.println("Failed to make head file");
        }
    }


    // https://www.geeksforgeeks.org/java/java-program-to-delete-a-directory/  
    // https://stackoverflow.com/questions/20281835/how-to-delete-a-folder-with-files-using-java
    // got help here for deleting directories
    public static void cleanUp(File directory) {
        Git.cleanUp(directory);
    }


    public static void resetRepo() throws IOException {
        File git = new File("git");
        if (git.exists()) {
            cleanUp(git);
        }
        Git.initializeRepo();
    }

    // adds an entry of a sample txt file into the index file and then verifies that the index entry matches the actual file after being added
    public static void testIndexWithFile(String filename, String content) throws IOException, NoSuchAlgorithmException {
        // write sample content into file
        File file = new File(filename);
        Files.writeString(file.toPath(), content);

        // add file to index (also creates blob)
        Git.addToIndex(file);

        // get expected hash
        String expectedHash = Git.hashSHA1(content);

        // read index and verify that file entry was added to it
        List<String> lines = Files.readAllLines(new File("git/index").toPath());
        boolean found = false;
        for (String line : lines) {
            if (line.equals(expectedHash + " " + file.getName())) {
                found = true;
                break;
            }
        }

        // verify blob exists
        File blobFile = new File("git/objects", expectedHash);

        if (found && blobFile.exists()) {
            System.out.println("Verified index + blob for " + file.getName());
        } else {
            System.out.println("Verification failed for " + file.getName());
        }
    }

    // cleans the git/index file // deletes all non-Java test files in the working directory 
    
    public static void resetRepoState() throws IOException {
        File git = new File("git"); 
        
        // deletes all blobs inside git/objects 
        File objects = new File(git, "objects"); 
        if (objects.exists() && objects.isDirectory()) { 
            for (File blob : objects.listFiles()) { 
                blob.delete(); } 
        } 

        // resets the index file to empty 
        File index = new File(git, "index"); 
        if (index.exists()) { 
            Files.writeString(index.toPath(), ""); 
            // overwrite with empty content 
        } 
            
        // delete all non-Java test files in working directory 
        File hehehhe = new File(".");
        for (File hahha : hehehhe.listFiles()) {
            if (hahha.isFile() && !hahha.getName().equals("README.md") 
    && !hahha.getName().endsWith(".java") && !hahha.getName().startsWith(".")) {
                hahha.delete();
            }
        }
        System.out.println("Repository state has been reset."); }
}
