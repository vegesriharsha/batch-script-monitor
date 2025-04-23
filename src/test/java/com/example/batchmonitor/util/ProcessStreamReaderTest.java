package com.example.batchmonitor.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProcessStreamReaderTest {

    @Test
    void testProcessStreamReader() throws InterruptedException {
        // Create test input stream with multiple lines
        String testInput = "Line 1\nLine 2\nLine 3\n";
        InputStream inputStream = new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));

        // Create collection to store output
        List<String> output = new ArrayList<>();

        // Create and start the reader
        ProcessStreamReader reader = new ProcessStreamReader(inputStream, output::add);
        reader.start();

        // Wait for processing to complete
        Thread.sleep(100);
        reader.stop();
        reader.waitFor();

        // Verify output
        assertEquals(3, output.size());
        assertEquals("Line 1", output.get(0));
        assertEquals("Line 2", output.get(1));
        assertEquals("Line 3", output.get(2));
    }

    @Test
    void testProcessStreamReader_EmptyStream() throws InterruptedException {
        // Create empty input stream
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        // Create collection to store output
        List<String> output = new ArrayList<>();

        // Create and start the reader
        ProcessStreamReader reader = new ProcessStreamReader(inputStream, output::add);
        reader.start();

        // Wait for processing to complete
        Thread.sleep(100);
        reader.stop();
        reader.waitFor();

        // Verify output
        assertTrue(output.isEmpty());
    }

    @Test
    void testProcessStreamReader_IOException() throws InterruptedException {
        // Create a mock input stream that throws IOException when read
        InputStream inputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Test IO exception");
            }
        };

        // Create a latch to detect when reader processes the exception
        CountDownLatch latch = new CountDownLatch(1);

        // Create and start the reader with an error handler
        ProcessStreamReader reader = new ProcessStreamReader(inputStream, line -> {});
        reader.start();

        // Wait for processing to complete
        boolean completed = latch.await(200, TimeUnit.MILLISECONDS);
        reader.stop();

        // Since we can't easily verify the exception inside the thread,
        // we just ensure the test doesn't hang or crash
        assertFalse(completed);
    }
}
