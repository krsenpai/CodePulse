package com.codepulse.service;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DockerExecutionService {

    private static final int MAX_OUTPUT_BYTES = 1024 * 1024;

    private ProcessBuilder createDockerProcess(
            Path tempDirectory,
            String containerName,
            String... command
    ) {
        ProcessBuilder processBuilder = new ProcessBuilder();

        processBuilder.command(
                "docker",
                "run",
                "--rm",
                "--name", containerName,
                "--memory", "256m",
                "--cpus", "0.5",
                "--pids-limit", "64",
                "--network", "none",
                "--read-only",
                "--cap-drop", "ALL",
                "--security-opt", "no-new-privileges",
                "-v",
                tempDirectory.toAbsolutePath() + ":/app",
                "codepulse-java-runner"
        );

        processBuilder.command().addAll(
                java.util.Arrays.asList(command)
        );

        return processBuilder;
    }

    private void cleanupContainer(String containerName) {

        try {
            Process cleanupProcess =
                    new ProcessBuilder(
                            "docker",
                            "rm",
                            "-f",
                            containerName
                    ).start();

            cleanupProcess.waitFor(
                    5,
                    TimeUnit.SECONDS
            );

        } catch (Exception ignored) {
        }
    }

    public DockerResult execute(String code, String input) {

        Path tempDirectory = null;

        String compileContainerName =
                "codepulse-compile-" + UUID.randomUUID();

        String runContainerName =
                "codepulse-run-" + UUID.randomUUID();

        try {

            tempDirectory =
                    Files.createTempDirectory("codepulse-docker-");

            Path javaFile =
                    tempDirectory.resolve("Main.java");

            Files.writeString(javaFile, code);

            /*
             * STEP 1: COMPILE
             */

            ProcessBuilder compiler =
                    createDockerProcess(
                            tempDirectory,
                            compileContainerName,
                            "javac",
                            "Main.java"
                    );

            compiler.directory(tempDirectory.toFile());

            Process compileProcess =
                    compiler.start();

            OutputReader compileError =
                    new OutputReader(
                            compileProcess.getErrorStream()
                    );

            Thread compileErrorThread =
                    new Thread(compileError);

            compileErrorThread.start();

            boolean compileFinished =
                    compileProcess.waitFor(
                            5,
                            TimeUnit.SECONDS
                    );

            if (!compileFinished) {

                cleanupContainer(
                        compileContainerName
                );

                compileProcess.destroyForcibly();

                return new DockerResult(
                        "TIMEOUT",
                        "Compilation exceeded the 5-second limit."
                );
            }

            compileErrorThread.join();

            if (compileError.isLimitExceeded()) {

                cleanupContainer(
                        compileContainerName
                );

                return new DockerResult(
                        "OUTPUT_LIMIT_EXCEEDED",
                        "Compilation produced too much output."
                );
            }

            int compileExitCode =
                    compileProcess.exitValue();

            if (compileExitCode != 0) {

                return new DockerResult(
                        "COMPILE_ERROR",
                        compileError.getOutput()
                );
            }

            /*
             * STEP 2: RUN
             */

            ProcessBuilder runner =
                    createDockerProcess(
                            tempDirectory,
                            runContainerName,
                            "java",
                            "Main"
                    );

            runner.directory(tempDirectory.toFile());

            Process runProcess =
                    runner.start();

            // Send program input
            if (input != null && !input.isEmpty()) {

                runProcess
                        .getOutputStream()
                        .write(
                                input.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );
            }

            runProcess
                    .getOutputStream()
                    .close();

            OutputReader outputReader =
                    new OutputReader(
                            runProcess.getInputStream()
                    );

            OutputReader errorReader =
                    new OutputReader(
                            runProcess.getErrorStream()
                    );

            Thread outputThread =
                    new Thread(outputReader);

            Thread errorThread =
                    new Thread(errorReader);

            outputThread.start();
            errorThread.start();

            long startTime =
                    System.currentTimeMillis();

            boolean runFinished = false;

            while (!runFinished) {

                if (outputReader.isLimitExceeded()
                        || errorReader.isLimitExceeded()) {

                    cleanupContainer(
                            runContainerName
                    );

                    runProcess.destroyForcibly();

                    return new DockerResult(
                            "OUTPUT_LIMIT_EXCEEDED",
                            "Program produced too much output."
                    );
                }

                if (!runProcess.isAlive()) {
                    runFinished = true;
                    break;
                }

                if (System.currentTimeMillis()
                        - startTime >= 5000) {

                    cleanupContainer(
                            runContainerName
                    );

                    runProcess.destroyForcibly();

                    return new DockerResult(
                            "TIMEOUT",
                            "Program exceeded the 5-second limit."
                    );
                }

                Thread.sleep(50);
            }

            outputThread.join();
            errorThread.join();

            if (outputReader.isLimitExceeded()
                    || errorReader.isLimitExceeded()) {

                return new DockerResult(
                        "OUTPUT_LIMIT_EXCEEDED",
                        "Program produced too much output."
                );
            }

            String output =
                    outputReader.getOutput();

            String runtimeError =
                    errorReader.getOutput();

            int runExitCode =
                    runProcess.exitValue();

            if (runExitCode != 0) {

                return new DockerResult(
                        "RUNTIME_ERROR",
                        runtimeError
                );
            }

            return new DockerResult(
                    "SUCCESS",
                    output
            );

        } catch (Exception e) {

            return new DockerResult(
                    "DOCKER_ERROR",
                    e.getMessage()
            );

        } finally {

            if (tempDirectory != null) {

                try {

                    Files.walk(tempDirectory)
                            .sorted(Comparator.reverseOrder())
                            .forEach(path -> {

                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                }

                            });

                } catch (IOException ignored) {
                }
            }
        }
    }

    /*
     * Continuously reads Docker output.
     */
    private static class OutputReader
            implements Runnable {

        private final InputStream inputStream;

        private final ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        private final AtomicBoolean limitExceeded =
                new AtomicBoolean(false);

        OutputReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {

            byte[] buffer = new byte[8192];

            try {

                int bytesRead;

                while ((bytesRead =
                        inputStream.read(buffer)) != -1) {

                    if (output.size() + bytesRead
                            > MAX_OUTPUT_BYTES) {

                        limitExceeded.set(true);
                        return;
                    }

                    output.write(
                            buffer,
                            0,
                            bytesRead
                    );
                }

            } catch (IOException ignored) {
            }
        }

        boolean isLimitExceeded() {
            return limitExceeded.get();
        }

        String getOutput() {

            return output.toString(
                    StandardCharsets.UTF_8
            );
        }
    }

    public static class DockerResult {

        private final String status;
        private final String output;

        public DockerResult(
                String status,
                String output
        ) {
            this.status = status;
            this.output = output;
        }

        public String getStatus() {
            return status;
        }

        public String getOutput() {
            return output;
        }
    }
}