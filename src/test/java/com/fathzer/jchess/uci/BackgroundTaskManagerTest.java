package com.fathzer.jchess.uci;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.*;

import org.junit.jupiter.api.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

class BackgroundTaskManagerTest {

    private BackgroundTaskManager manager;

    @BeforeEach
    void setUp() {
        manager = new BackgroundTaskManager();
    }

    @AfterEach
    void tearDown() {
        manager.close();
    }

    @Test
    void testDoBackgroundRunsTask() throws InterruptedException {
        AtomicBoolean ran = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        BackgroundTaskManager.Task task = new BackgroundTaskManager.Task(
                () -> { ran.set(true); latch.countDown(); },
                () -> {}, e -> fail("Should not throw"));
        assertTrue(manager.doBackground(task));
        assertTrue(latch.await(1, java.util.concurrent.TimeUnit.SECONDS));
        assertTrue(ran.get());
    }

    @Test
    void testDoBackgroundRejectsIfAlreadyRunning() {
        CountDownLatch latch = new CountDownLatch(1);
        BackgroundTaskManager.Task firstTask = new BackgroundTaskManager.Task(latch::await, () -> {}, e -> {});
        BackgroundTaskManager.Task secondTask = new BackgroundTaskManager.Task(() -> {}, () -> {}, e -> {});
        assertTrue(manager.doBackground(firstTask));
        assertFalse(manager.doBackground(secondTask));
        latch.countDown(); // Let firstTask finish
    }

    @Test
    void testExceptionInTaskCallsLoggerAndStops() {
        AtomicReference<Exception> logged = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Exception testEx = new Exception("test");
        BackgroundTaskManager.Task task = new BackgroundTaskManager.Task(
                () -> { throw testEx; },
                latch::countDown, // stop() should be called after exception
                logged::set
        );
        assertTrue(manager.doBackground(task));
        // Give some time for the task to run and logger to be called
        await().atMost(1000, TimeUnit.MILLISECONDS).until(() -> logged.get()!=null);
        assertEquals(testEx, logged.get());
        assertEquals(0, latch.getCount());
    }

    @Test
    void testStopRunsStopTask() {
        AtomicBoolean stopped = new AtomicBoolean(false);
        AtomicBoolean wasStopped = new AtomicBoolean(false);
        AtomicReference<Throwable> exception = new AtomicReference<>();
        BackgroundTaskManager.Task task = new BackgroundTaskManager.Task(
                () -> {
            		await().atMost(1000, TimeUnit.MILLISECONDS).pollInterval(10, TimeUnit.MILLISECONDS).until(stopped::get);
            		wasStopped.set(stopped.get());
                	},
                () -> { stopped.set(true); }, exception::set);
        assertTrue(manager.doBackground(task));
        assertTrue(manager.stop());
        await().atMost(200, TimeUnit.MILLISECONDS).until(wasStopped::get);
        assertTrue(stopped.get());
        assertNull(exception.get());
    }

    @Test
    void testStopReturnsFalseIfNoTask() {
        assertFalse(manager.stop());
    }

    @Test
    void testStopTaskThrowsCallsLogger() {
        AtomicReference<Exception> logged = new AtomicReference<>();

        BackgroundTaskManager.Task task = new BackgroundTaskManager.Task(
                () -> { await().atMost(1000, TimeUnit.MILLISECONDS).pollInterval(10, TimeUnit.MILLISECONDS).until(() -> logged.get()!=null); },
                () -> { throw new RuntimeException("stop failed"); },
                logged::set
        );

        assertTrue(manager.doBackground(task));
        assertTrue(manager.stop());
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> logged.get()!=null);
        assertEquals("stop failed", logged.get().getMessage());
    }

    @Test
    void testCloseShutsDownExecutor() {
        manager.close();
        // No direct way to check, but subsequent close() should not throw
        assertDoesNotThrow(() -> manager.close());
    }
}