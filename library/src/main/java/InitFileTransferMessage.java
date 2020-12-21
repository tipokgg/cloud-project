import java.io.File;

public class InitFileTransferMessage extends AbstractMessage {

    // класс содержит информацию о файле, его имя и размер
    // объект этого класса посылается при инициализации передачи файла

    private File file;
    private String fileName;
    private long fileSize;

    public InitFileTransferMessage() {
    }

    public InitFileTransferMessage(File file) {
        this.file = file;
        this.fileName = file.getName();
        this.fileSize = file.length();
    }

    @Override
    public String toString() {
        return  "information about file - " +
                "file name: " + fileName +
                "file size: " + fileSize + " bytes";
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
