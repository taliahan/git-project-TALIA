import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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

    

}