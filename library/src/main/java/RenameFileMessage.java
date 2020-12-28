public class RenameFileMessage extends AbstractMessage {

    private String currentFileName;
    private String newFileName;

    public RenameFileMessage() {
    }

    public RenameFileMessage(String currentFileName, String newFileName) {
        this.currentFileName = currentFileName;
        this.newFileName = newFileName;
    }

    public String getCurrentFileName() {
        return currentFileName;
    }

    public void setCurrentFileName(String currentFileName) {
        this.currentFileName = currentFileName;
    }

    public String getNewFileName() {
        return newFileName;
    }

    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }
}
