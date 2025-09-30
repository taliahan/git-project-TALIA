import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.List;

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
        else {
            System.out.println("hash method doesnt work");
        }
        
        // testing createBlob
        File examp1 = new File("blob.txt");
        Files.writeString(examp1.toPath(), "Hello world");

        Git.initializeRepo(); 
        Git.createBlob(examp1);
        String hash = Git.hashSHA1("Hello world");
        File blobFile = new File("git/objects", hash);

        if (blobFile.exists()) {
            System.out.println("Blob created successfully: " + blobFile.getName());
        } else {
            System.out.println("Blob creation failed.");
        }

        // stretch goal 3.1.1
        resetRepo();
        File test = new File("blobtest.txt");
        Files.writeString(test.toPath(), "Stretch goal!");
        Git.createBlob(test);

        if (Git.verifyBlob("Stretch goal!")) {
            System.out.println("Blob verified successfully.");
        } else {
            System.out.println("Blob not found.");
        }

        resetRepo();

        // testing addToIndex
        File testFile = new File("testIndex.txt");
        Files.writeString(testFile.toPath(), "first version");

        Git.initializeRepo();
        Git.addToIndex(testFile);

        // verify added
        System.out.println("Index after first add: " + Files.readString(new File("git/index").toPath()));

        // change file content
        Files.writeString(testFile.toPath(), "second version");
        Git.addToIndex(testFile);

        // verify updated
        System.out.println("Index after update: " + Files.readString(new File("git/index").toPath()));

        // readd same content
        Git.addToIndex(testFile); // should say "Blob already added to index"

        // stretch goal 4.1.1
        resetRepo();

        testIndexWithFile("alphaaaaaa.txt", "ur not sigma");
        testIndexWithFile("rad.txt", "ur not rad");
        testIndexWithFile("gamma.txt", "kappa kappa gamma hahahhahehhehe");
 


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


    
}
