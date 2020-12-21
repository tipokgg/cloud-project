import java.io.File;

public class ReadyForUploadMessage extends AbstractMessage {

    // пока бесполезный класс
    // подтверждение о готовности стороны принять файл

    File file;

    public ReadyForUploadMessage() {
    }

    public ReadyForUploadMessage(File file) {
        this.file = file;
    }


}
