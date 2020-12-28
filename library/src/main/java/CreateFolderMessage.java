public class CreateFolderMessage extends AbstractMessage {

    private String folderName;

    public CreateFolderMessage() {
    }

    public CreateFolderMessage(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
}
