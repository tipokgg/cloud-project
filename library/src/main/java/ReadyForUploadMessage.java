import java.io.File;

public class ReadyForUploadMessage extends AbstractMessage {

    // подтверждение о готовности стороны принять файл

    private File file;

    public ReadyForUploadMessage() {
    }

    public ReadyForUploadMessage(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
