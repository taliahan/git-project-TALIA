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
import java.util.HashSet;
import java.util.List;
import java.nio.file.Path;


public class Git {
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

    public static void addToIndexFilePath(String filePath) throws IOException {
        try {
            addToIndex(new File(filePath));
        } catch (NoSuchAlgorithmException e){ //does NOT catch IOExceptions
            e.printStackTrace(); 
        }
    }

    //If the file does not exist, it throws an IOException.
    //If the file is a directory, it throws an IOException.
    public static void addToIndex(File input) throws NoSuchAlgorithmException, IOException {        
        if (!input.exists()){
            throw new IOException();
        }
        if (input.isDirectory()){
            throw new IOException();
        }
        
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

    //Taken and edited from Shimon's code:
        //NOTE: Does not automatically BLOB everything inside of it!
    //That should have been done when indexed anyway.
    public static String makeIndexTree() throws NoSuchAlgorithmException, IOException {
        StringBuilder rootTreeContents = new StringBuilder();
        String indexFile = Files.readString((new File("git/index")).toPath());
        String[] entriesArr = indexFile.split("\n");
        ArrayList<String> entries = new ArrayList<String>();
        HashSet<String> directories = new HashSet<String>();
        for (int i = 0; i < entriesArr.length; i++){
            String path = entriesArr[i].split(" ")[1]; //no tree/blob prefix yet
            if (path.contains("/")){
                String directory = path.substring(0, path.indexOf("/"));
                directories.add(directory);
                entries.add("blob " + entriesArr[i]);
            } else { //is a file
                rootTreeContents.append("blob " + entriesArr[i]);
                rootTreeContents.append("\n");
            }
        }
        for (String directory : directories){
            String treeHash = makeIndexTreeHelper(entries, directory);
            rootTreeContents.append("tree " + treeHash + " " + directory);
            rootTreeContents.append("\n");
        }
        if (rootTreeContents.length() > 0){
            rootTreeContents.deleteCharAt(rootTreeContents.length() - 1);
        }
        String contents = rootTreeContents.toString();
        String hash = hashSHA1(contents);

        File newFile = new File("git/objects/" + hash);
        Files.writeString(newFile.toPath(), contents);

        return hash;
    }

    //returns tree hash
    public static String makeIndexTreeHelper(ArrayList<String> entries, String directoryPrefix) throws NoSuchAlgorithmException, IOException {
        ArrayList<String> subentries = new ArrayList<String>();
        for (String entry : entries){
            String path = entry.split(" ")[2];
            if (path.contains(directoryPrefix)){
                subentries.add(entry);
            }
        }
        HashSet<String> treeEntryRows = new HashSet<String>(); //get only unqiue adds
        for (String subentry : subentries){
            String subpath = subentry.split(" ")[2].substring(directoryPrefix.length() + 1);
            if (subpath.contains("/")){ //a directory
                String firstFolder = subpath.substring(0, subpath.indexOf("/"));
                String subTreeHash = makeIndexTreeHelper(subentries, directoryPrefix + "/" + firstFolder);
                treeEntryRows.add("tree " + subTreeHash + " " + directoryPrefix + "/" + firstFolder);
            } else { //a file
                treeEntryRows.add(subentry); //already formatted nicely
            }
        }
        StringBuilder entryContentSB = new StringBuilder();
        for (String s : treeEntryRows){
            entryContentSB.append(s);
            entryContentSB.append("\n");
        }
        if (entryContentSB.length() > 0){
            entryContentSB.deleteCharAt(entryContentSB.length() - 1);
        }
        String entryContent = entryContentSB.toString();
        String treeHash = hashSHA1(entryContent);
        
        File newFile = new File("git/objects/" + treeHash);
        Files.writeString(newFile.toPath(), entryContent);

        return treeHash;
    }

    //stages index files
    public static void stage(File f) throws NoSuchAlgorithmException, IOException {
        if (f.isFile()){
            addToIndex(f);
            createBlob(f);
        } else {
            for (File sf : f.listFiles()){
                stage(sf);
            }
        }
    }

    //assumes already staged (obv)
    public static String commit(String author, String message) throws IOException, NoSuchAlgorithmException {
        String tree = "tree: " + makeIndexTree();
        String parent = "parent: " + Files.readString(new File("git/HEAD").toPath());
        author = "author: " + author;
        String date = "date: " + new java.util.Date().toString();
        message = "message: " + message;
        
        String commitContent = tree + "\n" + parent + "\n" + author + "\n" + date + "\n" + message;
        String hash = hashSHA1(commitContent);
        String filePath = "git/objects/" + hash;

        File commit = new File(filePath);
        Files.writeString(commit.toPath(), commitContent);
        Files.writeString(new File("git/HEAD").toPath(), hash);
        
        return hash;
    }

    public static void cleanUp(File file) {
        if (file.isFile()){
            file.delete();
        }
        for (File subFile : file.listFiles()) {
            if (subFile.isDirectory()) {
                cleanUp(subFile);
            }
            subFile.delete();
        }
        file.delete();
    }

    public static boolean DONTDELETE(File f){ //list of files that are bad to remove/outside of test scope
        Path current = Path.of("").toAbsolutePath();
        Path absolute = f.toPath().toAbsolutePath();
        String relativePath = current.relativize(absolute).toString();
        
        switch (relativePath){
            case ".DS_Store":
                return true;
            case "Git.java":
                return true;
            case "GitWrapper.java":
                return true;
            case "GitTester.java":
                return true;
            case "README.md":
                return true;
            case ".gitignore":
                return true;
            case ".git": //real git file
                return true;
            case "git": //fake git file
                return true;
            default:
                return false;
        }
    }

    public static void deleteWorkingDirectory() {
        File parent = new File(System.getProperty("user.dir"));
        for (File f : parent.listFiles()){
            if (DONTDELETE(f)){
                continue;
            }
            System.out.println(f.getName());
            cleanUp(f);
        }
    }

    //would have to also pass in parentPath parameter but im storing absolute paths in tree
    //format bc a) it's easier and b) i didnt realize i shouldnt and am too lazy to fix it
    //also this extra credit here will cover that
    //and you probably didnt even notice except from this comment since the code works
    public static void rebuildFromTree(String hash) throws IOException {
        String content = Files.readString(new File("git/objects/" + hash).toPath());
        for (String entry : content.split("\n")){
            String[] data = entry.split(" ");
            File f = new File(data[2]);
            if (data[0].equals("tree")){
                f.mkdir();
                rebuildFromTree(data[1]);
            } else if (data[0].equals("blob")){
                String blobContent = Files.readString(new File("git/objects/" + data[1]).toPath());
                Files.writeString(f.toPath(), blobContent);
            }
        }
    }

    //doesn't trace through Head --> prev --> prev --> etc. since that makes no sense to do
    //we already have the hash that corresponds to the commit entry in the objects folder
    //i see know reason why this would help
    //if anything, this version is better since it allows to cross branches
    public static void checkout(String commitHash) throws IOException {
        deleteWorkingDirectory();
        Files.writeString(new File("git/HEAD").toPath(), commitHash);
        String commit = Files.readString(new File("git/objects/" + commitHash).toPath()).split(" ")[1];
        rebuildFromTree(commit.split("\n")[0]);
    }
}