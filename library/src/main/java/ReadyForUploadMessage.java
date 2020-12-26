import java.io.File;

public class ReadyForUploadMessage extends AbstractMessage {

    // подтверждение о готовности стороны принять файл

    private File serverSideFile;
    private File clientSideFile;

    public ReadyForUploadMessage() {
    }


    public ReadyForUploadMessage(File clientSideFile, File serverSideFile) {
        this.clientSideFile = clientSideFile;
        this.serverSideFile = serverSideFile;
    }

    public File getClientSideFile() {
        return clientSideFile;
    }

    public void setClientSideFile(File clientSideFile) {
        this.clientSideFile = clientSideFile;
    }

    public File getServerSideFile() {
        return serverSideFile;
    }

    public void setServerSideFile(File serverSideFile) {
        this.serverSideFile = serverSideFile;
    }
}
