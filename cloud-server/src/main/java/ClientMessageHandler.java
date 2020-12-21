import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.RandomAccessFile;
import java.util.Arrays;

public class ClientMessageHandler extends SimpleChannelInboundHandler<AbstractMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(ClientMessageHandler.class);


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractMessage msg) throws Exception {

        // проверяем что за объект пришёл
        if (msg instanceof InitFileTransferMessage) {
            LOG.info("Received: " + msg);
            // если пришёл запрос на передачу файла от клиента, отправяем сер. объект о готвности приёма
            ctx.writeAndFlush(new ReadyForUploadMessage(((InitFileTransferMessage) msg).getFile()));
            // если это чанк с данными
        } else if (msg instanceof ChunkFileMessage) {
            // то сначала запишем в переменнуб
            ChunkFileMessage chunkFileMessage = (ChunkFileMessage) msg;
            // выводы в консоль для проверок, что происходит
            System.out.println("Получен чанк длиной - " + chunkFileMessage.data.length);
            System.out.println("Начало записи в файл с позиции - " + chunkFileMessage.position);
            System.out.println(Arrays.toString(chunkFileMessage.data));
            String filePath = "/home/tipokgg/Desktop/discord-0.0.10.deb"; // хардкод. куда пишем файл
            // записываем через RandomAccessFile, предвариьельно выбрав позицию, в какое место файла писать
            // информацию о позиции получаем из сериализованного объекта ChunkFileMessage
            try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
                raf.seek(chunkFileMessage.position);
                raf.write(chunkFileMessage.data);
            }

        }

        else LOG.error("ERROR");

    }


}
