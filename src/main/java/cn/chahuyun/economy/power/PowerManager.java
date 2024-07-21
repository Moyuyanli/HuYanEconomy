package cn.chahuyun.economy.power;

import cn.hutool.core.util.ClassUtil;
import kotlin.coroutines.EmptyCoroutineContext;
import net.mamoe.mirai.event.ConcurrencyKind;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.ListeningStatus;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 权限注解管理
 *
 * @author Moyuyanli
 * @Date 2023/1/2 20:16
 */
public class PowerManager {

    private static final Map<String, Object> map = new HashMap<>();


    public static void init(EventChannel<Event> eventEventChannel) {
        Set<Class<?>> classes = ClassUtil.scanPackage("cn.chahuyun.economy.manager");
        for (Class<?> aClass : classes) {
            Object newInstance = null;
            try {
                newInstance = aClass.getConstructor().newInstance();
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException e) {
                e.printStackTrace();
            }
            Object finalNewInstance = newInstance;
            Arrays.stream(aClass.getMethods()).filter(it -> {
                Annotation[] declaredAnnotations = it.getDeclaredAnnotations();
                boolean isPower = false;
                for (Annotation declaredAnnotation : declaredAnnotations) {
                    if (declaredAnnotation instanceof Power) {
                        isPower = true;
                        break;
                    }
                }
                return isPower;
            }).forEach(it -> execute(finalNewInstance, it, eventEventChannel.filterIsInstance(MessageEvent.class)));
        }
    }

    private static void execute(Object bean, Method method, @NotNull EventChannel<MessageEvent> channel) {
        Power annotation = method.getAnnotation(Power.class);
        channel.filter(event -> {
            MessageChain message = event.getMessage();
            String code = message.serializeToMiraiCode();


            MatchingMenu matching = annotation.matching();
            String[] text = annotation.text();

            String[] permissions = annotation.permissions();

            boolean quit = false;

            if (matching == MatchingMenu.TEXT) {
                for (String s : text) {
                    if (code.equals(s)) {
                        quit = true;
                        break;
                    }
                }
            } else {
                quit = Pattern.matches(text[0], code);
            }

            for (String permission : permissions) {
                if (permission.equals("null")) {
                    quit = true;
                    break;
                }
            }

            return quit;
        }).subscribe(MessageEvent.class,
                EmptyCoroutineContext.INSTANCE,
                ConcurrencyKind.LOCKED,
                annotation.priority(),
                event -> {
                    try {
                        method.invoke(bean, event);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    return ListeningStatus.LISTENING;
                });
    }

}
