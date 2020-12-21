import java.io.*;

public class ChunkFileMessage extends AbstractMessage {

    // класс который генерирует чанки

    File file; // сам файл
    int chunkSize; // размер чанка
    String nameOfChunkedFile; // пока не испольуется
    byte[] data; // буффер, куда будут записаны байты из файла
    int position; // позиция, с которой будут читаться байты из файла

    public ChunkFileMessage() {
    }

    //
    public ChunkFileMessage(File file, int position, long chunkSize) {
        System.out.println("Устанавливаю длину чанка - " + chunkSize);
        this.chunkSize = (int) chunkSize; // заполняем длину чанка
        System.out.println("Теперь длина чанка - " + this.chunkSize);
        data = new byte[this.chunkSize]; // создаем буффер нужного размера
        this.file = file;
        this.position = position;
        fillArray(this.position); // вызываем метод, который заполнит буффер
    }



    public void fillArray(int position) {
        // открыаем RandomAccessFile
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            System.out.println("Заполняю буффер байтами с позиции - " + position);
            System.out.println("Длина буффера - " + data.length);
            // устаналивем указатель на нужную позицию
            raf.seek(position);
            // и читаем в буффер
            raf.read(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
