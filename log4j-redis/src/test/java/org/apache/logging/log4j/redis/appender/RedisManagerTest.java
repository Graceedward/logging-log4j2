package org.apache.logging.log4j.redis.appender;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class RedisManagerTest {

    JedisPool mockJedisPool;
    Jedis mockJedis;
    RedisManager manager;

    String[] KEYS = {"abc", "def"};
    private String HOST = "localhost";
    private int PORT = 6379;

    @Before
    public void setUp() throws Exception {
        initMocks();
        manager = new ManagerTestRedisAppenderBuilder()
                .setHost(HOST)
                .setPort(PORT)
                .setKeys(KEYS)
                .getRedisManager();
        manager.startup();
    }

    private void initMocks() {
        mockJedisPool = Mockito.mock(JedisPool.class);
        mockJedis = Mockito.mock(Jedis.class);
        when(mockJedisPool.getResource()).thenReturn(mockJedis);
        when(mockJedis.rpush(anyString(), anyString())).thenReturn(1L);
    }

    @Test
    public void verifySendsBytesToAllKeys() {
        manager.send("value");
        Mockito.verify(mockJedis, Mockito.times(KEYS.length)).rpush(anyString(), anyString());
        Mockito.verify(mockJedisPool).getResource();
    }

    @Test
    public void verifyReleasePoolResources() {
        manager.stop(100, TimeUnit.HOURS);
        Mockito.verify(mockJedisPool).destroy();
    }

    private class TestRedisManager extends RedisManager {

        TestRedisManager(LoggerContext loggerContext, String name, String[] keys, String host, int port, Charset charset) {
            super(loggerContext, name, keys, host, port, null, null);
        }

        @Override
        JedisPool createPool(String host, int port, SslConfiguration ssl) {
            return mockJedisPool;
        }
    }

    private class ManagerTestRedisAppenderBuilder extends RedisAppender.Builder<ManagerTestRedisAppenderBuilder> {
        @Override
        RedisManager getRedisManager() {
            return new TestRedisManager(
                    LoggerContext.getContext(),
                    getName(),
                    getKeys(),
                    getHost(),
                    getPort(),
                    getCharset()
            );
        }
    }
}
