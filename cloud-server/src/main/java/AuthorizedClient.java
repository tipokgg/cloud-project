import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AuthorizedClient {

    private String userName;
    private String currentDir;

    public AuthorizedClient(String userName) {
        this.userName = userName;
        setCurrentDir("cloud-server/files/" + userName + "/");
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
        Path path = Paths.get(currentDir);
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.currentDir = currentDir;
    }


}
