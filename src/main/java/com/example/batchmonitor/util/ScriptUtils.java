package com.example.batchmonitor.util;

import com.example.batchmonitor.exception.BatchExecutionException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class ScriptUtils {

    public static List<String> buildCommand(String scriptPath, String parameters) {
        List<String> command = new ArrayList<>();

        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            throw new BatchExecutionException("Script file does not exist: " + scriptPath);
        }

        if (!scriptFile.canExecute()) {
            log.info("Script file is not executable, setting executable permission: {}", scriptPath);
            scriptFile.setExecutable(true);
        }

        // For shell scripts on Unix-like systems
        if (isUnixSystem() && scriptPath.endsWith(".sh")) {
            command.add("bash");
            command.add(scriptPath);
        } else if (isWindowsSystem() && (scriptPath.endsWith(".bat") || scriptPath.endsWith(".cmd"))) {
            // For batch/cmd scripts on Windows
            command.add("cmd.exe");
            command.add("/c");
            command.add(scriptPath);
        } else {
            // Direct execution for other executable files
            command.add(scriptPath);
        }

        // Add parameters if present
        if (parameters != null && !parameters.isBlank()) {
            command.addAll(Arrays.asList(parameters.split("\\s+")));
        }

        return command;
    }

    public static String readFileContent(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.readString(path);
        } catch (IOException e) {
            throw new BatchExecutionException("Error reading file: " + filePath, e);
        }
    }

    public static boolean isUnixSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("mac");
    }

    public static boolean isWindowsSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }
}
