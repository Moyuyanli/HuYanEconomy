package cn.chahuyun;

import cn.chahuyun.entity.GoldEconomyCurrency;
import kotlin.coroutines.CoroutineContext;
import kotlin.properties.ReadWriteProperty;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.SimpleListenerHost;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.cssxsh.mirai.economy.service.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 经济的服务实现
 *
 * @author Moyuyanli
 * @date 2022/11/9 14:44
 */
public class HuYanEconomyServer extends SimpleListenerHost implements IEconomyService {




    @NotNull
    @Override
    public EconomyAccount account(@NotNull String s, @Nullable String s1) throws UnsupportedOperationException, NoSuchElementException {
        return null;
    }

    @NotNull
    @Override
    public GroupEconomyAccount account(@NotNull Group group) throws UnsupportedOperationException, NoSuchElementException {
        return null;
    }

    @NotNull
    @Override
    public UserEconomyAccount account(@NotNull User user) throws UnsupportedOperationException, NoSuchElementException {
        return null;
    }

    @NotNull
    @Override
    public GroupEconomyContext context(@NotNull Group group) {
        return null;
    }

    @NotNull
    @Override
    public String getId() {
        return null;
    }


    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws Exception {

    }

    @NotNull
    @Override
    public BotEconomyContext context(@NotNull Bot bot) {
        return null;
    }

    @NotNull
    @Override
    public GlobalEconomyContext global() {
        return null;
    }

    @NotNull
    @Override
    public Map<String, EconomyCurrency> getBasket() {
        return null;
    }

    @Override
    public void register(@NotNull EconomyCurrency economyCurrency, boolean b) throws UnsupportedOperationException {

    }

    @Override
    public void reload(@NotNull Path path) throws IOException {

    }
}
