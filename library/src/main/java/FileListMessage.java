import java.util.List;

public class FileListMessage extends AbstractMessage {

    private List<String> filesFromServer;

    public FileListMessage() {
    }

    public FileListMessage(List<String> filesFromServer) {
        this.filesFromServer = filesFromServer;
    }

    public List<String> getFilesFromServer() {
        return filesFromServer;
    }

    public void setFilesFromServer(List<String> filesFromServer) {
        this.filesFromServer = filesFromServer;
    }
}
