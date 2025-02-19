package cn.chahuyun.economy.manager;


import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.plugin.PluginManager;
import cn.chahuyun.economy.sign.SignEvent;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.economy.utils.ImageUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.mockito.Mockito.*;

public class SignManagerTest {

    @Mock
    private GroupMessageEvent event;
    @Mock
    private User user;
    @Mock
    private Contact subject;
    @Mock
    private MessageChain message;
    @Mock
    private UserInfo userInfo;
    @Mock
    private SignEvent signEvent;
    @Mock
    private MessageChainBuilder messages;
    @Mock
    private BufferedImage userInfoImageBase;
    @Mock
    private Graphics2D graphics;

    @InjectMocks
    private SignManager signManager;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(event.getSender()).thenReturn(user);
        when(event.getSubject()).thenReturn(subject);
        when(event.getMessage()).thenReturn(message);
        when(UserManager.getUserInfo(user)).thenReturn(userInfo);
        when(userInfo.sign()).thenReturn(true);
        when(signEvent.getEventReply()).thenReturn(messages);
        when(messages.size()).thenReturn(2);
        when(userInfoImageBase.createGraphics()).thenReturn(graphics);
        when(ImageUtil.getG2d(userInfoImageBase)).thenReturn(graphics);
        when(UserManager.getUserInfoImageBase(userInfo)).thenReturn(userInfoImageBase);
    }

    @Test
    public void sign_UserAlreadySignedIn_ShouldReturnMessage() {
        when(userInfo.sign()).thenReturn(false);

        signManager.sign(event);

        verify(subject, times(1)).sendMessage(any(MessageChain.class));
    }

    @Test
    public void sign_SuccessfulSign_ShouldUpdateUserInfoAndSendImage() throws IOException {
        when(userInfo.sign()).thenReturn(true);
        when(EconomyUtil.plusMoneyToUser(any(User.class), anyDouble())).thenReturn(true);
        when(signEvent.getGold()).thenReturn(10.0);
        when(signEvent.getReply()).thenReturn(message);
        when(userInfo.getOldSignNumber()).thenReturn(0);

        signManager.sign(event);

        verify(subject, times(1)).sendMessage(any(MessageChain.class));
        verify(graphics, times(1)).dispose();
    }

    @Test
    public void sign_SignFailed_ShouldReturnErrorMessage() throws IOException {
        when(userInfo.sign()).thenReturn(true);
        when(EconomyUtil.plusMoneyToUser(any(User.class), anyDouble())).thenReturn(false);

        signManager.sign(event);

        verify(subject, times(1)).sendMessage("签到失败!");
    }

    @Test
    public void sign_EventReplySizeNotTwo_ShouldSendEventReply() throws IOException {
        when(userInfo.sign()).thenReturn(true);
        when(EconomyUtil.plusMoneyToUser(any(User.class), anyDouble())).thenReturn(true);
        when(messages.size()).thenReturn(3);

        signManager.sign(event);

        verify(subject, times(1)).sendMessage(any(MessageChain.class));
    }

    @Test
    public void sendSignImage_CustomImage_ShouldDrawString() throws IOException {
        when(PluginManager.isCustomImage).thenReturn(true);

        signManager.sendSignImage(userInfo, subject, messages.build());

        verify(graphics, times(1)).drawString(anyString(), anyInt(), anyInt());
    }

    @Test
    public void sendSignImage_NonCustomImage_ShouldDrawString() throws IOException {
        when(PluginManager.isCustomImage).thenReturn(false);

        signManager.sendSignImage(userInfo, subject, messages.build());

        verify(graphics, times(1)).drawString(anyString(), anyInt(), anyInt());
    }
}
