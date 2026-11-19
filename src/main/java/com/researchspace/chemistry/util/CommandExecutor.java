package com.researchspace.chemistry.util;

import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CommandExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutor.class);

  // Time allowed to finish reading output once the process has exited
  private static final Duration OUTPUT_READ_TIMEOUT = Duration.ofSeconds(30);

  // Time allowed for a forcibly killed process to be reaped
  private static final Duration KILL_TIMEOUT = Duration.ofSeconds(5);

  // One thread per command, so a hung command can't stop other commands' output being read
  private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

  private final Duration defaultTimeout;

  public CommandExecutor(@Value("${openbabel.command.timeout:60s}") Duration defaultTimeout) {
    this.defaultTimeout = defaultTimeout;
  }

  /***
   * Runs a command, waiting up to the configured `openbabel.command.timeout` for it to complete.
   * @return the lines written to stdout by the command
   * @throws TimeoutException if the command doesn't complete in time, in which case it is killed
   */
  public List<String> executeCommand(ProcessBuilder processBuilder)
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    return executeCommand(processBuilder, defaultTimeout);
  }

  /***
   * Runs a command, waiting up to `timeout` for it to complete.
   * @return the lines written to stdout by the command
   * @throws TimeoutException if the command doesn't complete in time, in which case it is killed
   */
  public List<String> executeCommand(ProcessBuilder processBuilder, Duration timeout)
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    LOGGER.info("Executing command: {}", processBuilder.command());
    processBuilder.directory(null); // uses current working directory
    if (!processBuilder.redirectErrorStream()
        && processBuilder.redirectError() == ProcessBuilder.Redirect.PIPE) {
      // stderr isn't read, so discard it rather than let the process block once the pipe is full
      processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
    }
    Process process = processBuilder.start();
    try {
      List<String> output = new ArrayList<>();
      InputStreamConsumer streamConsumer =
          new InputStreamConsumer(process.getInputStream(), output::add);
      Future<?> future = executorService.submit(streamConsumer);
      if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
        LOGGER.warn("Command timed out after {}: {}", timeout, processBuilder.command());
        throw new TimeoutException(
            "Command timed out after " + timeout + ": " + processBuilder.command());
      }
      future.get(OUTPUT_READ_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      LOGGER.info("Found output: {}", StringUtils.abbreviate(String.join(", ", output), 500));
      return output;
    } finally {
      // kills the process (and anything it spawned) on timeout, interruption or any other failure
      destroy(process);
    }
  }

  private void destroy(Process process) {
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    if (!process.isAlive()) {
      return;
    }
    LOGGER.info("Killing process {}", process.pid());
    process.destroyForcibly();
    try {
      if (!process.waitFor(KILL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
        LOGGER.error("Process {} still running after being killed.", process.pid());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @PreDestroy
  public void shutdown() {
    executorService.shutdownNow();
  }

  /***
   * Perform an action on each line of an input stream
   */
  private record InputStreamConsumer(InputStream inputStream, Consumer<String> consumer)
      implements Runnable {

    @Override
    public void run() {
      new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
    }
  }
}
