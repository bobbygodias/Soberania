package org.soberania.control;

import android.system.Os;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class ShellUserService extends IControlService.Stub {

    private static final int MAX_CAPTURE = 128 * 1024;

    public ShellUserService() {
    }

    @Override
    public void destroy() {
        System.exit(0);
    }

    @Override
    public int uid() {
        return Os.getuid();
    }

    @Override
    public String exec(String command) {
        if (command == null || command.isBlank()) {
            return "Comando vazio.";
        }

        Process process = null;
        try {
            process = new ProcessBuilder("/system/bin/sh", "-c", command)
                    .redirectErrorStream(true)
                    .start();

            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            Process finalProcess = process;
            Thread drain = new Thread(() -> {
                try (InputStream in = finalProcess.getInputStream()) {
                    byte[] buffer = new byte[4096];
                    int totalStored = 0;
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        if (totalStored < MAX_CAPTURE) {
                            int store = Math.min(read, MAX_CAPTURE - totalStored);
                            capture.write(buffer, 0, store);
                            totalStored += store;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }, "soberania-shell-drain");
            drain.start();

            boolean finished = process.waitFor(20, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            }
            drain.join(2500);

            String output = capture.toString(StandardCharsets.UTF_8);
            if (capture.size() >= MAX_CAPTURE) {
                output += "\n[saída truncada em 128 KiB]";
            }
            output += finished
                    ? "\n\n[exit=" + process.exitValue() + ", uid=" + Os.getuid() + "]"
                    : "\n\n[TIMEOUT após 20s, processo encerrado, uid=" + Os.getuid() + "]";

            return output.trim();
        } catch (Throwable t) {
            if (process != null) {
                try {
                    process.destroyForcibly();
                } catch (Throwable ignored) {
                }
            }
            return t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }
}
