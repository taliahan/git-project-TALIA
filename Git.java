import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
        File objects = new File("git/objects");
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


    

}