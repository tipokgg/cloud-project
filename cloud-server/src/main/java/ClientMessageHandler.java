import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClientMessageHandler extends SimpleChannelInboundHandler<AbstractMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(ClientMessageHandler.class);
    private static final ArrayList<String> testUsers = new ArrayList<>(Arrays.asList("antiv18", "user1"));
    private static final ConcurrentHashMap<ChannelHandlerContext, AuthorizedClient> users = new ConcurrentHashMap();


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractMessage msg) throws Exception {

        if (msg instanceof RequestAuthToServerMessage) { // авторизация
            String userName = ((RequestAuthToServerMessage) msg).getUserName();
            if (testUsers.contains(userName)) {
                users.put(ctx, new AuthorizedClient(userName));
                ctx.writeAndFlush(new FileListMessage(getUserFiles(userName)));
            } else {
                ctx.writeAndFlush(new FailAuthMessage(userName)); // если ошибка авторизации
            }
        } else if (msg instanceof InitFileTransferMessage) { //пришёл запрос на upload от клиента
            InitFileTransferMessage init = (InitFileTransferMessage) msg;
            ctx.writeAndFlush(new ReadyForUploadMessage(init.getFile(), new File(users.get(ctx).getCurrentDir() + init.getFileName())));
        } else if (msg instanceof ChunkFileMessage) { // пришёл чанк от клиента
            ChunkFileMessage chunk = (ChunkFileMessage) msg;

            try (RandomAccessFile raf = new RandomAccessFile(chunk.getSeverSideFile(), "rw")) {
                raf.seek(chunk.getPosition());
                raf.write(chunk.getData());
            }

        } else if (msg instanceof RequestFileDownloadMessage) { // если  пришёл запрос на загрузку с сервера
            String fileName = ((RequestFileDownloadMessage) msg).getFileName();
            File serverSideFile = new File(users.get(ctx).getCurrentDir() + fileName);

            try (InputStream is = Files.newInputStream(Paths.get(serverSideFile.toURI()))) {
                String md5 = DigestUtils.md5Hex(is);
                System.out.println("checksum: " + md5);
            }

            System.out.println("Длина файла в байтах - " + serverSideFile.length());
            long countOfChunks = serverSideFile.length()/1024 + 1;
            System.out.println("Будет передано чанков - " + countOfChunks);


            new Thread(() -> {
                // устанавливаем начальную позичию, откуда читать файл
                int currentPosition = 0;
                // в цикле начинаем генерировать чанки, которые являются сериализованными объектами и отправлять на сервер
                // в чанках есть буффер с данными, вычитанными из файла, что это за файл, какой размер чанка и из какой позиции читались байты
                for (int i = 0; i < countOfChunks; i++) {
                    if (i != countOfChunks -1) {
                        ctx.writeAndFlush(new ChunkFileMessage(serverSideFile, currentPosition, 1024));
                        ctx.flush();
                        currentPosition += 1024; // после итерации увеличиваем позицию откуда читать байты на размер чанка
                        // в последней итерации создаем чанк, равный количеству байт, который осталось считать из файла до конца,
                        // чтобы не делать полный чанк длиной 1024 байта
                    } else {
                        ctx.writeAndFlush(new ChunkFileMessage(serverSideFile, currentPosition,(int) serverSideFile.length() - currentPosition));
                        ctx.writeAndFlush(new UpdateClientSideMessage()); // чтоьбы клиент обновил список файлов у себя
                        ctx.flush();
                    }
                }

            }).start();

        } else if (msg instanceof FileListRequestMessage) {

            Path clientDir = Paths.get(users.get(ctx).getCurrentDir());
            List<String> clientFiles;

            clientFiles = Files.list(clientDir)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());

            ctx.writeAndFlush(new FileListMessage(clientFiles));

            LOG.info("Sent list of files for: " + users.get(ctx).getUserName());
        } else if (msg instanceof RequestFileDeleteMessage) {

            String fileName = ((RequestFileDeleteMessage) msg).getFileName();
            Files.deleteIfExists(Paths.get("cloud-server/files/user1/" + fileName));

            String userName = "user1";
            ctx.writeAndFlush(new FileListMessage(getUserFiles(userName)));

        }

        else LOG.error("ERROR");

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOG.info("Client connected!");
    }


    private List<String> getUserFiles(String userName) throws IOException {
        Path clientDir = Paths.get("cloud-server/files/" + userName);
        return  Files.list(clientDir)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
    }



}
