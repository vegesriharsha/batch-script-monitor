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

/**
 * Utility class for script execution and file operations.
 * Provides platform-independent methods for executing scripts on different operating systems.
 */
@Slf4j
public class ScriptUtils {

    /**
     * Builds a command list to execute a script with the given parameters.
     * Handles platform-specific script execution (Windows vs Unix-like systems).
     *
     * @param scriptPath The path to the script file
     * @param parameters The command-line parameters to pass to the script
     * @return A list of command parts suitable for ProcessBuilder
     */
    public static List<String> buildCommand(String scriptPath, String parameters) {
        List<String> command = new ArrayList<>();

        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            throw new BatchExecutionException("Script file does not exist: " + scriptPath);
        }

        // Try to make the script executable if it's not already
        if (!scriptFile.canExecute()) {
            log.info("Script file is not executable, setting executable permission: {}", scriptPath);
            boolean success = scriptFile.setExecutable(true);
            if (!success) {
                log.warn("Failed to set executable permission for: {}", scriptPath);
                // Continue execution anyway, as some platforms don't require executable bit
            }
        }

        // Determine the appropriate command based on script type and platform
        if (isUnixSystem()) {
            handleUnixScript(scriptPath, command);
        } else if (isWindowsSystem()) {
            handleWindowsScript(scriptPath, command);
        } else {
            // For unknown systems, try direct execution
            log.warn("Unknown operating system, attempting direct execution of: {}", scriptPath);
            command.add(scriptPath);
        }

        // Add parameters if present
        if (parameters != null && !parameters.isBlank()) {
            command.addAll(Arrays.asList(parameters.split("\\s+")));
        }

        log.debug("Built command: {}", String.join(" ", command));
        return command;
    }

    /**
     * Handles script execution command building for Unix-like systems (Linux, macOS).
     *
     * @param scriptPath The path to the script
     * @param command The command list to populate
     */
    private static void handleUnixScript(String scriptPath, List<String> command) {
        String lowerPath = scriptPath.toLowerCase();

        if (lowerPath.endsWith(".sh") || !lowerPath.contains(".")) {
            // Shell script or no extension (likely a shell script)
            command.add("bash");
            command.add(scriptPath);
        } else if (lowerPath.endsWith(".py")) {
            // Python script
            command.add("python3");
            command.add(scriptPath);
        } else if (lowerPath.endsWith(".pl")) {
            // Perl script
            command.add("perl");
            command.add(scriptPath);
        } else if (lowerPath.endsWith(".rb")) {
            // Ruby script
            command.add("ruby");
            command.add(scriptPath);
        } else {
            // Other executable file
            command.add(scriptPath);
        }
    }

    /**
     * Handles script execution command building for Windows systems.
     *
     * @param scriptPath The path to the script
     * @param command The command list to populate
     */
    private static void handleWindowsScript(String scriptPath, List<String> command) {
        String lowerPath = scriptPath.toLowerCase();

        if (lowerPath.endsWith(".bat") || lowerPath.endsWith(".cmd")) {
            // Batch scripts
            command.add("cmd.exe");
            command.add("/c");
            command.add(scriptPath);
        } else if (lowerPath.endsWith(".ps1")) {
            // PowerShell scripts
            command.add("powershell.exe");
            command.add("-ExecutionPolicy");
            command.add("Bypass");
            command.add("-File");
            command.add(scriptPath);
        } else if (lowerPath.endsWith(".py")) {
            // Python scripts
            command.add("python");
            command.add(scriptPath);
        } else if (lowerPath.endsWith(".sh")) {
            // Shell scripts - try WSL if available
            if (isWslAvailable()) {
                command.add("wsl");
                command.add("bash");
                command.add(scriptPath);
            } else {
                // Try Git Bash if WSL isn't available
                String gitBashPath = findGitBash();
                if (gitBashPath != null) {
                    command.add(gitBashPath);
                    command.add("-c");
                    command.add(scriptPath);
                } else {
                    // No suitable shell found, warn and try direct execution
                    log.warn("No suitable shell found for .sh file on Windows. Direct execution will likely fail: {}", scriptPath);
                    command.add(scriptPath);
                }
            }
        } else {
            // Other executable file
            command.add(scriptPath);
        }
    }

    /**
     * Attempts to find Git Bash on a Windows system.
     *
     * @return Path to Git Bash or null if not found
     */
    private static String findGitBash() {
        String[] possiblePaths = {
                "C:\\Program Files\\Git\\bin\\bash.exe",
                "C:\\Program Files (x86)\\Git\\bin\\bash.exe"
        };

        for (String path : possiblePaths) {
            if (new File(path).exists()) {
                return path;
            }
        }
        return null;
    }

    /**
     * Reads the content of a file and returns it as a string.
     *
     * @param filePath Path to the file to read
     * @return The file content as a string
     */
    public static String readFileContent(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new BatchExecutionException("File does not exist: " + filePath);
            }
            return Files.readString(path);
        } catch (IOException e) {
            throw new BatchExecutionException("Error reading file: " + filePath, e);
        }
    }

    /**
     * Determines if the current operating system is a Unix-like system.
     *
     * @return true if on a Unix-like system (Linux, macOS, etc.), false otherwise
     */
    public static boolean isUnixSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("mac");
    }

    /**
     * Determines if the current operating system is Windows.
     *
     * @return true if on Windows, false otherwise
     */
    public static boolean isWindowsSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    /**
     * Checks if Windows Subsystem for Linux (WSL) is available.
     * This is a simple check that attempts to run the WSL command.
     *
     * @return true if WSL is available, false otherwise
     */
    public static boolean isWslAvailable() {
        try {
            Process process = new ProcessBuilder("wsl", "--version").start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.debug("WSL is not available: {}", e.getMessage());
            return false;
        }
    }
}
