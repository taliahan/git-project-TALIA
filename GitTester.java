import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class GitTester {
    public static void main(String[] args) throws Exception {
        Git.initializeRepo();
        // verifyRepo();
        // File git = new File("git");
        // cleanUp(git);

        // // ensuring robust functionality
        // for (int i = 0; i < 300; i++) {
        //     Git.initializeRepo();
        //     verifyRepo();
        //     cleanUp(git);
        // }

        // // testing sha1
        // String result = Git.hashSHA1("Hero pookie");
        // String expectation = "d187aa9d5bd8719783d54b081624b8d34f014104";
        // if (result.equals(expectation))  {
        //     System.out.println("hash method works");
        // }
        // else {
        //     System.out.println("hash method doesnt work");
        // }

        // // testing createBlob
        // File examp1 = new File("blob.txt");
        // Files.writeString(examp1.toPath(), "Hello world");

        // Git.initializeRepo(); 
        // Git.createBlob(examp1);
        // String hash = Git.hashSHA1("Hello world");
        // File blobFile = new File("git/objects", hash);

        // if (blobFile.exists()) {
        //     System.out.println("Blob created successfully: " + blobFile.getName());
        // } else {
        //     System.out.println("Blob creation failed.");
        // }

        // // stretch goal 3.1.1
        // resetRepo();
        // File test = new File("blobtest.txt");
        // Files.writeString(test.toPath(), "Stretch goal!");
        // Git.createBlob(test);

        // if (Git.verifyBlob("Stretch goal!")) {
        //     System.out.println("Blob verified successfully.");
        // } else {
        //     System.out.println("Blob not found.");
        // }

        // resetRepo();

        // // testing addToIndex
        // File testFile = new File("testIndex.txt");
        // Files.writeString(testFile.toPath(), "first version");

        // Git.initializeRepo();
        // Git.addToIndex(testFile);

        // // verify added
        // System.out.println("Index after first add: " + Files.readString(new File("git/index").toPath()));

        // // change file content
        // Files.writeString(testFile.toPath(), "second version");
        // Git.addToIndex(testFile);

        // // verify updated
        // System.out.println("Index after update: " + Files.readString(new File("git/index").toPath()));

        // // readd same content
        // Git.addToIndex(testFile); // should say "Blob already added to index"

        // // stretch goal 2.4.1
        // resetRepo();

        // testIndexWithFile("alphaaaaaa.txt", "ur not sigma");
        // testIndexWithFile("rad.txt", "ur not rad");
        // testIndexWithFile("gamma.txt", "kappa kappa gamma hahahhahehhehe");

        // // stretch goal 2.4.2
        // System.out.println("Running resetRepoState...");
        // resetRepoState();

        // // verify repo is clean
        // File objects = new File("git/objects");
        // File index = new File("git/index");

        // if (objects.exists() && objects.listFiles().length == 0 && index.exists() && index.length() == 0) {
        //     System.out.println("resetRepoState successfully cleaned repository.");
        // } else {
        //     System.out.println("resetRepoState did not fully clean repository.");
        // }

        // testing 3.1

        // makes sure When adding the same file from the same directory and SHA value, it should be ignored
        resetRepo();
        // Files.createDirectories(Path.of("dir1"));
        // Files.writeString(Path.of("dir1/Hello.txt"), "hi");
        // Git.createBlob(new File("dir1/Hello.txt"));
        // Git.addToIndex(new File("dir1/Hello.txt")); // adds
        // Git.addToIndex(new File("dir1/Hello.txt")); // should print “already staged...” and not change index
        // System.out.println(Files.readString(Path.of("git/index"))); // only 1 line

        // // now testing When adding identical files in different directories, each file will share the same SHA value but be distinguished by its unique path

        // Files.createDirectories(Path.of("dir2"));
        // Files.writeString(Path.of("dir2/Hello.txt"), "hi");
        // Git.addToIndex(new File("dir2/Hello.txt"));
        // System.out.println(Files.readString(Path.of("git/index"))); // 2 line

        // // now testing: When adding files with identical content but different file names, they are added to the index as separate entries
        // Files.writeString(Path.of("dir1/Hero.txt"), "hi");
        // Git.addToIndex(new File("dir1/Hero.txt"));
        // System.out.println(Files.readString(Path.of("git/index"))); // 3 lines all with same blob

        // // modify one of them
        // Files.writeString(Path.of("dir1/Hello.txt"), "changed");
        // Git.addToIndex(new File("dir1/Hello.txt"));

        // System.out.println(Files.readString(Path.of("git/index")));

        // // testing 3.2

        // creating a tree
        // Files.createDirectories(Path.of("project/docs"));
        // Files.createDirectories(Path.of("project/src/animals"));

        // Files.writeString(Path.of("project/README.md"), "readme");
        // Files.writeString(Path.of("project/docs/Hello.txt"), "hello");
        // Files.writeString(Path.of("project/docs/World.txt"), "world");
        // Files.writeString(Path.of("project/src/Cat.java"), "class Cat {}");
        // Files.writeString(Path.of("project/src/animals/Dog.java"), "class Dog {}");

        // // testing 3.3
        Files.createDirectories(Path.of("myProgram/docs"));
        Files.writeString(Path.of("myProgram/README.md"), "readme");
        Files.writeString(Path.of("myProgram/docs/Hello.txt"), "hello");
        Files.writeString(Path.of("myProgram/docs/World.txt"), "world");
        Git.addToIndex(new File("myProgram/README.md"));
        Git.addToIndex(new File("myProgram/docs/Hello.txt"));
        Git.addToIndex(new File("myProgram/docs/World.txt"));
        List<String> wl = Git.createWorkingList();
        // for (String line : wl) {
        //     System.out.println(line);
        // }
        // wl.forEach(System.out::println);
        // Git.createFirstLeafTree(wl);
        // System.out.println("--- WL after collapse ---");

        Git.buildTreesFromIndex();


        // // steps 2–7: create all tree levels
        // System.out.println("Creating trees from working list...");
        // Git.createFirstLeafTree(workingList);
        // System.out.println();

        // // final check
        // System.out.println("Final working list:");
        // for (String w : workingList) {
        //     System.out.println(w);
        // }

        // System.out.println("\nDone. Check git/objects for blob and tree files.");

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
