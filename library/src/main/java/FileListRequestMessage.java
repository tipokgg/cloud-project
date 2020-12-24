public class FileListRequestMessage extends AbstractMessage{

    private String username;

    public FileListRequestMessage() {
    }

    public FileListRequestMessage(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
