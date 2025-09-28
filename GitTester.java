import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class GitTester {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        Git.initializeRepo();
        verifyRepo();
        File git = new File("git");
        cleanUp(git);

        // ensuring robust functionality
        for (int i = 0; i < 300; i++) {
            Git.initializeRepo();
            verifyRepo();
            cleanUp(git);
        }

        // testing sha1
        String result = Git.hashSHA1("Hero pookie");
        String expectation = "d187aa9d5bd8719783d54b081624b8d34f014104";
        if (result.equals(expectation))  {
            System.out.println("hash method works");
        }
        else{
            System.out.println("hash method doesnt work");
        }

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
        for (File subFile : directory.listFiles()) {
            if (subFile.isDirectory()) {
                cleanUp(subFile);
            }
            subFile.delete();
        }
        directory.delete();
    }


    
}
