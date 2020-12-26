public class RequestFileDeleteMessage extends AbstractMessage {

    private String fileName;

    public RequestFileDeleteMessage() {
    }

    public RequestFileDeleteMessage(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
