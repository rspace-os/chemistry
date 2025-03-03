package com.researchspace.chemistry.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CommandExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutor.class);

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  public List<String> executeCommand(ProcessBuilder processBuilder)
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    LOGGER.info("Executing command: {}", processBuilder.command());
    processBuilder.directory(null); // uses current working directory
    Process process = processBuilder.start();
    List<String> output = new ArrayList<>();
    InputStreamConsumer streamConsumer =
        new InputStreamConsumer(process.getInputStream(), output::add);
    Future<?> future = executorService.submit(streamConsumer);
    process.waitFor();
    future.get(30, TimeUnit.SECONDS);
    LOGGER.info("Found output: {}", String.join(", ", output));
    return output;
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
