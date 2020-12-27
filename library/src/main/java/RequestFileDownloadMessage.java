import java.io.File;

public class RequestFileDownloadMessage extends AbstractMessage {

    private String fileName;
    private File clientSideFile;

    public RequestFileDownloadMessage() {
    }

    public RequestFileDownloadMessage(String fileName, File clientSideFile) {
        this.fileName = fileName;
        this.clientSideFile = clientSideFile;
    }

    public File getClientSideFile() {
        return clientSideFile;
    }

    public void setClientSideFile(File clientSideFile) {
        this.clientSideFile = clientSideFile;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }


}
