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


    // coutn slahes for which is deeper
    



    // // must generate a tree file with references to its files and subdirectories, create all necessary blob objects, and return the SHA-1 hash of the tree
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
            return new ArrayList<>();
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

        // sort by pathname, compares the two strings character by character using Unicode order
        // used https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/String.html#compareTo(java.lang.String)and https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Comparable.html for help
        workingList.sort((a,b) -> {
            // a: "blob <sha> <path>"
            int firstSpaceA  = a.indexOf(' ');
            int secondSpaceA = a.indexOf(' ', firstSpaceA + 1);
            String pathA     = a.substring(secondSpaceA + 1);

            int firstSpaceB  = b.indexOf(' ');
            int secondSpaceB = b.indexOf(' ', firstSpaceB + 1);
            String pathB     = b.substring(secondSpaceB + 1);

            return pathA.compareTo(pathB);
        });
      
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

    // counts slashes
    private static int depth(String dir) {
        int d = 0;
        for (int i = 0; i < dir.length(); i++) {
            if (dir.charAt(i) == '/') {
                d++;
            }
        }
        return d;
    }



    // helper: find the leaf-most directory (deepest dir with only immediate children, no immediate subdir trees)
    private static String findLeafMostDir(List<String> workingList) {
        
        // 
        List<String> dirs = new ArrayList<>();
        for (String line : workingList) {
            // geting the path of each blob in workinglist
            String[] p = line.split(" ", 3);
            String path = p[2];
            int i = path.lastIndexOf('/');

            // only proceed if path has a parent directory
            if (i > 0) {
                String dir = path.substring(0, i);
                if (!dirs.contains(dir)) {
                    dirs.add(dir);
                }
            }
        }
        // if there wasnt anything in the working list, so dirs is empty
        if (dirs.isEmpty()) {
            return null;
        }

        // deepest first by slash count
        dirs.sort((a, b) -> Integer.compare(depth(b), depth(a)));


        // choose first dir that has immediate children and no immediate "tree" children
        for (String d : dirs) {
            // only consider directories inside the one we are looking at
            String pref = d + "/";
            
            // to see if it has immediate children/dir
            boolean hasImmediate = false;
            boolean hasImmediateDir = false;

            for (String line : workingList) {
                String[] p = line.split(" ", 3);
                String path = p[2];
                // path isnt in the dir we are looking at
                if (!path.startsWith(pref)) {
                    continue;
                }
                String tail = path.substring(pref.length());
                if (tail.contains("/")) {
                    continue;
                }
                // not immediate child
                hasImmediate = true;
                if (p[0].equals("tree")) {
                    hasImmediateDir = true;
                    break;
                }
            }
            // found leafmost
            if (hasImmediate && !hasImmediateDir) {
                return d;
            }
        }
        return null;
    }

    // helper for updating the working list 
    // Replaces all entries directly under targetDir with: "tree <treeSHA> <targetDir>"
    private static void updateWorkingListWithTree(List<String> workingList, String targetDir, String treeSHA) {
        String prefix = targetDir + "/";
        List<String> next = new ArrayList<>();

        for (String line : workingList) {
            // keep entries NOT under targetDir/
            String path = line.split(" ", 3)[2];
            if (!path.startsWith(prefix)) next.add(line);
        }

        // add the collapsed tree entry for that directory
        next.add("tree " + treeSHA + " " + targetDir);

        // sort by path so subdirectories remain grouped
        next.sort((a,b) -> {
            // a: "blob <sha> <path>"
            int firstSpaceA  = a.indexOf(' ');
            int secondSpaceA = a.indexOf(' ', firstSpaceA + 1);
            String pathA     = a.substring(secondSpaceA + 1);

            int firstSpaceB  = b.indexOf(' ');
            int secondSpaceB = b.indexOf(' ', firstSpaceB + 1);
            String pathB     = b.substring(secondSpaceB + 1);

            return pathA.compareTo(pathB);
        });
      

        // in-place update
        workingList.clear();
        workingList.addAll(next);
    }




    // create the first leaf-most tree from the working list (step 2)
    public static void createFirstLeafTree(List<String> workingList) throws Exception {

        if (workingList == null || workingList.isEmpty()) {
            return;
        }


        // pick the deepest dir that has immediate children and no immediate subdir trees
        String targetDir = findLeafMostDir(workingList);
        if (targetDir == null) {
            System.out.println("No collapsible leaf directory found.");
            return;
        }
        collapseDirOnce(workingList, targetDir);

    }

    // Build a tree for dir from its *immediate* children in workingList, write it to git/objects/<sha>, then replace those children with: "tree <sha> <dir>"
    public static void collapseDirOnce(List<String> workingList, String dir) throws Exception {

        List<String> kids = immediateChildren(workingList, dir);
        if (kids.isEmpty()) {
            return;
        }
        String sha = writeTree(kids);
        updateWorkingListWithTree(workingList, dir, sha);
    }


    // grab immediate children under a dir from the working list
    private static List<String> immediateChildren(List<String> wl, String dir) {
        String prefix = dir + "/";
        List<String> children = new ArrayList<>();
        for (String line : wl) {
            String[] p = line.split(" ", 3); // [type, sha, path]
            String path = p[2];
            if (!path.startsWith(prefix)) {
                continue;
            }

            String tail = path.substring(prefix.length());
            // skip grandchildren
            if (tail.contains("/")) {
                continue;
            }

            // keep exact child lines, but rewrite path to the immediate child name
            children.add(p[0] + " " + p[1] + " " + tail); // "blob sha name" or "tree sha dirname"
        }
        return children;
    }
    
    // writes a tree object for these children and return its SHA
    private static String writeTree(List<String> children) throws Exception {
        // sort by child name
        children.sort((a, b) -> {
            int a1 = a.indexOf(' ');
            a1 = a.indexOf(' ', a1 + 1);
            int b1 = b.indexOf(' ');
            b1 = b.indexOf(' ', b1 + 1);
            String pa = a.substring(a1 + 1);
            String pb = b.substring(b1 + 1);
            return pa.compareTo(pb);
        });

        String content = String.join("\n", children);
        String sha = hashSHA1(content);
        File objs = new File("git", "objects");
        if (!objs.exists()) {
            objs.mkdirs();
        }
        File f = new File(objs, sha);
        if (!f.exists()) {
            Files.writeString(f.toPath(), content);
        }
        return sha;
    }


    // Build trees from the current index and return the ROOT tree SHA
    public static String buildTreesFromIndex() throws Exception {
        List<String> wl = createWorkingList();
        if (wl.isEmpty()) {
            return null;
        }

        // keep collapsing deepest leaf dirs until only one tree remains
        while (!(wl.size() == 1 && wl.get(0).startsWith("tree "))) {
            String leaf = findLeafMostDir(wl);
            if (leaf != null) {
                // collapse that leaf-most directory
                collapseDirOnce(wl, leaf);
                continue;
            }

            // If no leaf directory is found, you might only have top-level files left
            // Collapse the top-level (root) directory name inferred from paths
            // taking the parent of the first path if it has one otherwise synthesize a "(root)" tree from top-level blobs.

            String[] bsp = wl.get(0).split(" ", 3);
            String firstPath = bsp[2];
            int cut = firstPath.lastIndexOf('/');
            // if there is another /
            if (cut > 0) {
                String parent = firstPath.substring(0, cut); // e.g., "myProgram"
                collapseDirOnce(wl, parent);
            } else {
                // top-level files only -> make a root tree from them
                List<String> top = new ArrayList<>();
                for (String e : wl) {
                    String[] p = e.split(" ", 3);

                    // if not another subdirectory add it to root tree file
                    if (p[0].equals("blob")) {
                        top.add("blob " + p[1] + " " + p[2]);
                    }
                }

                // if there were blobs, get the sha and now update working list
                if (!top.isEmpty()) {
                    String sha = writeTree(top);
                    wl.clear();
                    wl.add("tree " + sha + " (root)");
                } else {
                    break; // nothing left to do
                }
            }
        }
        String[] dd = wl.get(0).split(" ", 3);

        System.out.println("--- WL after top-level collapse ---");
        for (String s : wl) {
            System.out.println(s);
        } 

        // returning root tree sha
        return dd[1];
    }




    }
