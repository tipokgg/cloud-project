import java.io.*;

public class ChunkFileMessage extends AbstractMessage {

    // класс который генерирует чанки
    private File clientSideFile;
    private File severSideFile; // сам файл
    private byte[] data; // буффер, куда будут записаны байты из файла
    private int position; // позиция, с которой будут читаться байты из файла

    public ChunkFileMessage() {
    }


    public ChunkFileMessage(File severSideFile, File clientSideFile, int position, int chunkSize) {
        // размер чанка
        data = new byte[chunkSize]; // создаем буффер нужного размера
        this.clientSideFile = clientSideFile;
        this.severSideFile = severSideFile;
        this.position = position;
        fillArray(this.position); // вызываем метод, который заполнит буффер
    }


    public void fillArray(int position) {
        // открыаем RandomAccessFile
        try (RandomAccessFile raf = new RandomAccessFile(clientSideFile, "r")) {
            // устаналивем указатель на нужную позицию
            raf.seek(position);
            // и читаем в буффер
            raf.read(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public File getClientSideFile() {
        return clientSideFile;
    }

    public void setClientSideFile(File clientSideFile) {
        this.clientSideFile = clientSideFile;
    }

    public File getSeverSideFile() {
        return severSideFile;
    }

    public void setSeverSideFile(File severSideFile) {
        this.severSideFile = severSideFile;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
