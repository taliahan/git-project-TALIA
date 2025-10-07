import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.nio.file.Path;


public class Git {
    public static void main(String[] args) throws IOException {
        
    }

    public static void initializeRepo() throws IOException {
        File git = new File("git");
        File objects = new File(git, "objects");
        File index = new File(git, "index");
        File head = new File(git, "HEAD");
        if (objects.exists() && index.exists() && head.exists() && git.exists()) {
            System.out.println("Git Repository Already Exists");
            return;
        }

        if (!git.exists()) {
            git.mkdir();
        }

        if (!objects.exists()) {
            objects.mkdir();
        }

        if (!index.exists()) {
            index.createNewFile();
        }
        if (!head.exists()) {
            head.createNewFile();
        }

        System.out.println("Git Repository Created");
        return;
    }
    

    // got help from https://www.geeksforgeeks.org/java/sha-1-hash-in-java/
    public static String hashSHA1(String content) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        // getting raw hash result in a byte array

        byte[] digest = sha1.digest(content.getBytes());
        BigInteger hashValue = new BigInteger(1, digest);
        String hashString = hashValue.toString(16);

        // padding result to 40 char bc 160-bit hash = 40 characters but biginteger sometimes drops leading 0s
        while (hashString.length() < 40) {
            hashString = "0" + hashString;
        }

        return hashString;
    } 


    public static void createBlob(File input) throws IOException, NoSuchAlgorithmException {
        // makes sure there is an objects directory too
        File objects = new File("git", "objects");
        if (!objects.exists()) {
            objects.mkdirs();
        }

        String content = Files.readString(input.toPath());
        String hash = hashSHA1(content);
        File blobFile = new File("git/objects", hash);
        if (blobFile.exists()) {
            System.out.println("Blob already exists: " + hash);
            return;
        }

        Files.writeString(blobFile.toPath(), content);
    }
    
    public static boolean verifyBlob(String content) throws NoSuchAlgorithmException {
        String hash = Git.hashSHA1(content);
        File blobFile = new File("git/objects", hash);
        return blobFile.exists();
    }



    public static void addToIndex(File input) throws NoSuchAlgorithmException, IOException {
        String content = Files.readString(input.toPath());
        String sha = hashSHA1(content);
        
        // making sure index file exists
        File index = new File("git", "index");
        if (!index.exists()) {
            index.createNewFile();
        }

        // to understand how to get the relative path, looked at: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Path.html#relativize(java.nio.file.Path), https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/File.html#getPath()
        // create path object to represent current working directory
        Path current = Path.of("").toAbsolutePath();
        // convert the file being staged into an absolute path
        Path absolute = input.toPath().toAbsolutePath();
        // to describe the file’s location relative to working directory
        String relativePath = current.relativize(absolute).toString();

        String add = sha + " " + relativePath;

        // reading all the lines
        List<String> lines = new ArrayList<>();
        if (index.length() > 0) {
            lines = Files.readAllLines(index.toPath());
        }
        
        boolean update = false;

        // checking through each line of index to see if the blob alr exists
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // if filename already exists in index
            if (line.endsWith(" " + relativePath)) {

                // blob alr exists
                if (line.equals(add)) {
                    System.out.println("File already staged with same content");
                    return; 
                } else {
                // blob file is entered but the content has been altered so it has a new hash
                    lines.set(i, add); // update hash
                    update = true;
                    System.out.println("File already staged but with different content; updated the index");
                    break;
                }
            }
        }

        // if the blob didnt laready exist
        if (!update) {
            lines.add(add); // new file
            System.out.println("The file was added to the index.");
        }
        Files.write(index.toPath(), lines);
    }


    // must generate a tree file with references to its files and subdirectories, create all necessary blob objects, and return the SHA-1 hash of the tree
    public static String createTree(String directoryPath) throws NoSuchAlgorithmException, IOException {

        // validating existence of directory we r building a tree for
        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("not a directory");
        }
            
        // arraylist of strings to represent each line we r going to add to the tree
        ArrayList<String> entries = new ArrayList<>();
        // getting all files and subdirectories in this directory in the form of an array list of files
        File[] subs = dir.listFiles();

        // first lets handle files
        for (File fil : subs) {
            if (fil.isFile()) {
                // make sure a blob object exists
                createBlob(fil);

                // getting the files sha so we can reference in the tree
                String content = Files.readString(fil.toPath());
                String sha = hashSHA1(content);

                // add the line to the arraylist of entries (format is blob, <SHA1>, pathname)
                entries.add("blob " + sha + " " + fil.getName());
            }
        }

        // now lets handle subdirectories
        for (File sub : subs) {
            if (sub.isDirectory()) {
                // create tree for the subfolder
                String subTreeSHA = createTree(sub.getPath());
                entries.add("tree " + subTreeSHA + " " + sub.getName());

            }
        }
        
        // combining the arraylist of strings (entries) into one string that is separated by new lines
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            str.append(entries.get(i));
            if (i < entries.size() - 1) {
                str.append("\n");
            }
        }
        String treeContent = str.toString();

        // generating sha1 hash of entire tree content
        String treeSHA = hashSHA1(treeContent);


        // write the tree file into git/objects/<treeSHA>
        // making sure objects directory exists first
        File objects = new File("git", "objects");
        if (!objects.exists()) {
            objects.mkdirs();
        }

        File treeFile = new File(objects, treeSHA);
        if (!treeFile.exists()) {
            Files.writeString(treeFile.toPath(), treeContent);
        }
        return treeSHA;
    }

    // creating a working list from the index file and sorting it by path
    public static List<String> createWorkingList() throws IOException {

        // verifying the index file exists
        File index = new File("git", "index");
        if (!index.exists()) {
            throw new FileNotFoundException("Index file not found.");
        }

        // read all index lines
        List<String> lines = Files.readAllLines(index.toPath());
        List<String> workingList = new ArrayList<>();

        // putting "blob " at the beginning of every line and store in working list
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                workingList.add("blob " + line);
            }
        }

        // sort by pathname
        // used https://docs.oracle.com/javase/8/docs/api/java/util/Collections.html#sort-java.util.List-java.util.Comparator- and https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html for help
        Collections.sort(workingList, Git::comparePaths);
      

        // writing to a new file for debugging purposes (making sure the method is working as intended)
        File workingListFile = new File("git", "workingList.txt");
        Files.write(workingListFile.toPath(), workingList);

        System.out.println("Working list created and sorted:");
        for (String w : workingList) System.out.println(w);

        return workingList;
    }


    // helper that compares two working list entries alphabetically by file path
    public static int comparePaths(String a, String b) {
        String[] partsA = a.split(" ", 3);
        String[] partsB = b.split(" ", 3);
        String pathA = partsA[2];
        String pathB = partsB[2];
        return pathA.compareTo(pathB);
    }


    // create the first leaf-most tree from the working list (step 2)
    public static void createFirstLeafTree(List<String> workingList) throws IOException, NoSuchAlgorithmException {

        if (workingList.isEmpty()) {
            return;
        }

        // using the first path to find which directory we’re in
        String[] first = workingList.get(0).split(" ", 3);
        String firstPath = first[2];
        int lastSlash = firstPath.lastIndexOf('/');
        if (lastSlash == -1) {
            System.out.println("No subdirectories found.");
            return;
        }
        String targetDir = firstPath.substring(0, lastSlash);

        // collecting all entries that belong to that directory
        List<String> entries = new ArrayList<>();
        for (String line : workingList) {
            String[] parts = line.split(" ", 3);
            String path = parts[2];
            if (path.startsWith(targetDir + "/")) {
                String filename = path.substring(targetDir.length() + 1);
                // parts[1] is the sha
                entries.add("blob " + parts[1] + " " + filename);
            }
        }

        // building the tree file content
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            sb.append(entries.get(i));
            if (i < entries.size() - 1) {
                sb.append("\n");
            }
        }
        String treeContent = sb.toString();

        // hasing it and saving the tree object
        String treeSHA = hashSHA1(treeContent);
        File objects = new File("git", "objects");
        if (!objects.exists()) {
            objects.mkdirs();
        }
        File treeFile = new File(objects, treeSHA);
        if (!treeFile.exists()) {
            Files.writeString(treeFile.toPath(), treeContent);
        }

        System.out.println("Created tree for " + targetDir + ": " + treeSHA);
        System.out.println("Tree file contents:\n" + treeContent);

        // step 3

        String prefix = targetDir + "/";
        // new list that will become the updated working list
        List<String> newList = new ArrayList<>();

        // keep only the lines that are not inside this directory
        for (String line : workingList) {
            // break the line into 3 pieces: type, SHA, and path
            String[] parts = line.split(" ", 3);
            // take the 3rd piece, which is the file path  
            String path = parts[2];

            // skip files we just turned into a tree
            if (!path.startsWith(prefix)) {
                newList.add(line);
            }
        }

        // add one new line representing this entire directory as a tree
        newList.add("tree " + treeSHA + " " + targetDir);

        // sort the list again alphabetically by path 
        Collections.sort(newList, Git::comparePaths);

        // replace the old working list with the new updated one
        workingList.clear();
        workingList.addAll(newList);

        // print the updated working list for confirmation
        System.out.println("\nUpdated working list:");
        for (String w : workingList)
            System.out.println(w);

        // step 4 baby

        // the next directory to turn into a tree is one level above the last one; take the first path from the updated working list and get its parent folder
        String[] next = workingList.get(0).split(" ", 3);
        String nextPath = next[2];
        int slashIndex = nextPath.lastIndexOf('/');
        if (slashIndex == -1) {
            System.out.println("No higher-level directory to collapse yet.");
            return;
        }
        String parentDir = nextPath.substring(0, slashIndex);

        // collect all entries that belong to this folder (everything that starts with "myProgram/")
        List<String> parentEntries = new ArrayList<>();
        for (String line : workingList) {
            String[] parts = line.split(" ", 3);
            String path = parts[2];
            if (path.startsWith(parentDir + "/")) {
                String fileName = path.substring(parentDir.length() + 1); // drop the folder prefix
                parentEntries.add(parts[0] + " " + parts[1] + " " + fileName);
            }
        }

        // build the tree text (the contents that go inside the new tree file)
        StringBuilder sbb = new StringBuilder();
        for (int i = 0; i < parentEntries.size(); i++) {
            sbb.append(parentEntries.get(i));
            if (i < parentEntries.size() - 1) {
                sbb.append("\n");
            }
        }
        String parentTreeContent = sbb.toString();

        // hash the new trees text and save it in git/objects
        String parentTreeSHA = hashSHA1(parentTreeContent);
        File objectsDir = new File("git", "objects");
        if (!objectsDir.exists()) {
            objectsDir.mkdirs();
        }
        ;
        File parentTreeFile = new File(objectsDir, parentTreeSHA);
        if (!parentTreeFile.exists()) {
            Files.writeString(parentTreeFile.toPath(), parentTreeContent);
        }

        // print what we created
        System.out.println("Created tree for " + parentDir + ": " + parentTreeSHA);
        System.out.println("Tree file contents: " + parentTreeContent);

        // step 5 WOOO

        // collapse working list completely by clearing everything except the top-level tree
        workingList.clear();
        workingList.add("tree " + parentTreeSHA + " " + parentDir);

        // show the final, collapsed working list
        System.out.println("Working list fully collapsed:");
        for (String w : workingList) {
            System.out.println(w);
        }

        String rootSHA = null;


        // step 6: creating the final root tree
        // The only remaining entry is the top directory line: tree <SHA> <dirname>
        if (workingList.size() == 1 && workingList.get(0).startsWith("tree ")) {
            // use that single line as the root tree’s content
            String rootContent = workingList.get(0);

            // hash it to get the root tree’s SHA
            rootSHA = hashSHA1(rootContent);

            // write the root tree object to git/objects/<rootSHA>
            File objectsDir2 = new File("git", "objects");
            if (!objectsDir2.exists()) {
                objectsDir.mkdirs();
            }

            File rootFile = new File(objectsDir2, rootSHA);
            if (!rootFile.exists()) {
                Files.writeString(rootFile.toPath(), rootContent);
            }

            // print confirmation
            System.out.println("Created root tree: " + rootSHA);
            System.out.println("Root tree file contents:\n" + rootContent);

        } else {
            System.out.println("Cannot create root tree... working list not fully collapsed yet.");
        }
    
        // step 7... LFG
        if (rootSHA != null) {
            // clear out anything old and replace with the root tree entry
            workingList.clear();
            workingList.add("tree " + rootSHA + " (root)");
            
            System.out.println("Final working list now contains the root tree entry:");
            for (String w : workingList) {
                System.out.println(w);
            }

        System.out.println("\nYou can now trace every SHA in git/objects to rebuild the directory structure!");

    }


    }

        



}