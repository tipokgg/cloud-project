import java.io.*;

public class ChunkFileMessage extends AbstractMessage {

    // класс который генерирует чанки
    private File senderFile;
    private File receiverFile; // сам файл
    private byte[] data; // буффер, куда будут записаны байты из файла
    private int position; // позиция, с которой будут читаться байты из файла

    public ChunkFileMessage() {
    }


    public ChunkFileMessage(File receiverFile, File senderFile, int position, int chunkSize) {
        // размер чанка
        data = new byte[chunkSize]; // создаем буффер нужного размера
        this.senderFile = senderFile;
        this.receiverFile = receiverFile;
        this.position = position;
        fillArray(this.position); // вызываем метод, который заполнит буффер
    }


    public void fillArray(int position) {
        // открыаем RandomAccessFile
        try (RandomAccessFile raf = new RandomAccessFile(senderFile, "r")) {
            // устаналивем указатель на нужную позицию
            raf.seek(position);
            // и читаем в буффер
            raf.read(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public File getSenderFile() {
        return senderFile;
    }

    public void setSenderFile(File senderFile) {
        this.senderFile = senderFile;
    }

    public File getReceiverFile() {
        return receiverFile;
    }

    public void setReceiverFile(File receiverFile) {
        this.receiverFile = receiverFile;
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
