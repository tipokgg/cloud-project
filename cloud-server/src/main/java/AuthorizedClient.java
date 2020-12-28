import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AuthorizedClient {

    private String userName;
    private String currentDir;
    private String rootDir;

    public AuthorizedClient(String userName) {
        this.userName = userName;
        setRootDir("cloud-server/files/" + userName + "/");
        this.currentDir = this.rootDir;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCurrentDir() {
        return currentDir;
    }

    public void setCurrentDir(String currentDir) {
        this.currentDir = currentDir;
    }

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        Path path = Paths.get(rootDir);
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.rootDir = rootDir;
    }
}
