package com.example.batchmonitor.util;

import com.example.batchmonitor.exception.BatchExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ScriptUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void testBuildCommand_UnixShellScript() throws IOException {
        // Skip test if not on Unix-like system
        if (!ScriptUtils.isUnixSystem()) {
            return;
        }

        // Create test script file
        Path scriptPath = tempDir.resolve("test.sh");
        Files.writeString(scriptPath, "#!/bin/bash\necho 'Hello'");
        Files.setPosixFilePermissions(scriptPath, java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));

        // Build command
        List<String> command = ScriptUtils.buildCommand(scriptPath.toString(), "--param value");

        // Verify command
        assertEquals(3, command.size());
        assertEquals("bash", command.get(0));
        assertEquals(scriptPath.toString(), command.get(1));
        assertEquals("--param value", command.get(2));
    }

    @Test
    void testBuildCommand_WindowsBatchScript() throws IOException {
        // Skip test if not on Windows system
        if (!ScriptUtils.isWindowsSystem()) {
            return;
        }

        // Create test batch file
        Path scriptPath = tempDir.resolve("test.bat");
        Files.writeString(scriptPath, "@echo Hello");

        // Build command
        List<String> command = ScriptUtils.buildCommand(scriptPath.toString(), "/param_value");

        // Verify command
        assertEquals(4, command.size());
        assertEquals("cmd.exe", command.get(0));
        assertEquals("/c", command.get(1));
        assertEquals(scriptPath.toString(), command.get(2));
        assertEquals("/param_value", command.get(3));
    }

    @Test
    void testBuildCommand_ExecutableFile() throws IOException {
        // Create test executable file
        Path scriptPath = tempDir.resolve("test.exe");
        Files.createFile(scriptPath);

        // Build command
        List<String> command = ScriptUtils.buildCommand(scriptPath.toString(), "param1 param2");

        // Verify command
        assertEquals(3, command.size());
        assertEquals(scriptPath.toString(), command.get(0));
        assertEquals("param1", command.get(1));
        assertEquals("param2", command.get(2));
    }

    @Test
    void testBuildCommand_ScriptNotFound() {
        // Try to build command for non-existent script
        Path nonExistentScript = tempDir.resolve("not-exists.sh");

        // Verify exception is thrown
        assertThrows(BatchExecutionException.class, () -> {
            ScriptUtils.buildCommand(nonExistentScript.toString(), "");
        });
    }

    @Test
    void testBuildCommand_NoParameters() throws IOException {
        // Create test script file
        Path scriptPath = tempDir.resolve("test");
        Files.createFile(scriptPath);

        // Build command with no parameters
        List<String> command = ScriptUtils.buildCommand(scriptPath.toString(), null);

        // Verify command
        assertEquals(1, command.size());
        assertEquals(scriptPath.toString(), command.get(0));

        // Test with empty parameters
        command = ScriptUtils.buildCommand(scriptPath.toString(), "");
        assertEquals(1, command.size());
        assertEquals(scriptPath.toString(), command.get(0));
    }

    @Test
    void testReadFileContent() throws IOException {
        // Create test file with content
        String content = "Test file content";
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, content.getBytes(StandardCharsets.UTF_8));

        // Read content
        String readContent = ScriptUtils.readFileContent(testFile.toString());

        // Verify content
        assertEquals(content, readContent);
    }

    @Test
    void testReadFileContent_FileNotFound() {
        // Try to read non-existent file
        Path nonExistentFile = tempDir.resolve("not-exists.txt");

        // Verify exception is thrown
        assertThrows(BatchExecutionException.class, () -> {
            ScriptUtils.readFileContent(nonExistentFile.toString());
        });
    }
}
