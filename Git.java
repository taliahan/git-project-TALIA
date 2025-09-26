import java.io.File;
import java.io.IOException;

public class Git {
    public static void main(String[] args) throws IOException {
        
    }

    public static void initializeRepo() throws IOException{
        File git = new File("git");
        File objects = new File(git, "objects");
        File index = new File(git, "index");
        File head = new File(git, "HEAD");
        if (objects.exists() && index.exists() && head.exists() && git.exists()) {
            System.out.println("Git Repository Already Exists");
            return;
            }

        if (!git.exists()) {
            git.mkdir();}

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
    

}