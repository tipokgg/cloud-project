import java.io.Serializable;


// абстрактый класс для реализации сериализации, от него наследуются все остальные в модуле library
// так как доступ к информации о сериализованных объектах должен быть как у сервера, так и у клиента, решил собрать их
// в мотдельном модуле library, и подключить модуль как зависимость к клиенту и к серверу
public abstract class AbstractMessage implements Serializable {
}
