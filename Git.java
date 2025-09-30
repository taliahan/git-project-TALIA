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
        createBlob(input);

        String content = Files.readString(input.toPath());
        String sha = hashSHA1(content);
        
        // making sure index file exists
        File index = new File("git", "index");
        if (!index.exists()) {
            index.createNewFile();
        }
        String add = sha + " " + input.getName();

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
            if (line.endsWith(" " + input.getName())) {
                // blob alr exists
                if (line.equals(add)) {
                    System.out.println("Blob already added to index");
                    return; 
                } else {
                // blob file is entered but the content has been altered so it has a new hash
                    lines.set(i, add); // update hash
                    update = true;
                    break;
                }
            }
        }

        // if the blob didnt laready exist
        if (!update) {
            lines.add(add); // new file
        }
        Files.write(index.toPath(), lines);
    }

    

}