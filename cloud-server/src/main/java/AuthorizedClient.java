public class AuthorizedClient {

    private String userName;
    private String rootDir;
    private String currentDir;

    public AuthorizedClient(String userName) {
        this.userName = userName;
        this.rootDir = "cloud-server/files/" + userName + "/";
        this.currentDir = "cloud-server/files/" + userName + "/";
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    public String getCurrentDir() {
        return currentDir;
    }

    public void setCurrentDir(String currentDir) {
        this.currentDir = currentDir;
    }


}
