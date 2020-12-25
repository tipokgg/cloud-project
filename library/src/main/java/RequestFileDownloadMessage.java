public class RequestFileDownloadMessage extends AbstractMessage {

    private String fileName;
    private String userName;

    public RequestFileDownloadMessage() {
    }

    public RequestFileDownloadMessage(String fileName, String userName) {
        this.fileName = fileName;
        this.userName = userName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
