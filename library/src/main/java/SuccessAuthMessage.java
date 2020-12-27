import java.util.List;

public class SuccessAuthMessage extends AbstractMessage {

    private List<String> filesFromServer;

    public SuccessAuthMessage() {
    }

    public SuccessAuthMessage(List<String> filesFromServer) {
        this.filesFromServer = filesFromServer;
    }

    public List<String> getFilesFromServer() {
        return filesFromServer;
    }

    public void setFilesFromServer(List<String> filesFromServer) {
        this.filesFromServer = filesFromServer;
    }

}
