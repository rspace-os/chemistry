package com.researchspace.chemistry.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@EnabledOnOs({OS.LINUX, OS.MAC})
class CommandExecutorTest {

  private final CommandExecutor commandExecutor = new CommandExecutor(Duration.ofSeconds(10));

  @TempDir Path tempDir;

  @AfterEach
  void tearDown() {
    commandExecutor.shutdown();
  }

  @Test
  void whenCommandCompletes_thenReturnOutputLines() throws Exception {
    List<String> output = commandExecutor.executeCommand(shell("echo first; echo second"));

    assertEquals(List.of("first", "second"), output);
  }

  @Test
  void whenCommandExceedsTimeout_thenThrowTimeoutAndKillProcess() throws Exception {
    Path pidFile = tempDir.resolve("pid");

    assertTimeoutPreemptively(
        Duration.ofSeconds(10),
        () ->
            assertThrows(
                TimeoutException.class,
                () ->
                    commandExecutor.executeCommand(
                        shell("echo $$ > " + pidFile + "; exec sleep 60"),
                        Duration.ofMillis(500))));

    long pid = Long.parseLong(Files.readString(pidFile).strip());
    assertFalse(
        ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false),
        "timed out process should have been killed");
  }

  @Test
  void whenCommandExceedsTimeout_thenChildProcessesAreAlsoKilled() throws Exception {
    Path childPidFile = tempDir.resolve("child-pid");

    assertThrows(
        TimeoutException.class,
        () ->
            commandExecutor.executeCommand(
                shell("sleep 60 & echo $! > " + childPidFile + "; wait"), Duration.ofMillis(500)));

    long childPid = Long.parseLong(Files.readString(childPidFile).strip());
    assertFalse(
        ProcessHandle.of(childPid).map(ProcessHandle::isAlive).orElse(false),
        "child of timed out process should have been killed");
  }

  @Test
  void whenOneCommandHangs_thenOtherCommandsStillComplete() throws Exception {
    Path startedFile = tempDir.resolve("started");
    Thread hungCommand =
        Thread.ofVirtual()
            .start(
                () -> {
                  try {
                    commandExecutor.executeCommand(
                        shell("touch " + startedFile + "; exec sleep 60"), Duration.ofSeconds(30));
                  } catch (Exception e) {
                    // expected: interrupted when the test finishes, which kills the process
                  }
                });

    try {
      // make sure the hung command is running before starting the next one
      assertTimeoutPreemptively(
          Duration.ofSeconds(5),
          () -> {
            while (!Files.exists(startedFile)) {
              Thread.sleep(50);
            }
          });
      List<String> output =
          assertTimeoutPreemptively(
              Duration.ofSeconds(5), () -> commandExecutor.executeCommand(shell("echo done")));
      assertEquals(List.of("done"), output);
      assertTrue(hungCommand.isAlive());
    } finally {
      hungCommand.interrupt();
      hungCommand.join(Duration.ofSeconds(10));
    }
  }

  @Test
  void whenCommandProducesLotsOfOutput_thenAllOutputIsRead() throws Exception {
    List<String> output = commandExecutor.executeCommand(shell("seq 1 100000"));

    assertEquals(100000, output.size());
    assertEquals("100000", output.getLast());
  }

  @Test
  void whenCommandWritesLotsToStderr_thenItDoesNotBlock() throws Exception {
    // well over the OS pipe buffer size, which would block the process if stderr were left unread
    List<String> output =
        commandExecutor.executeCommand(shell("seq 1 200000 >&2; echo done"), Duration.ofSeconds(5));

    assertEquals(List.of("done"), output);
  }

  private static ProcessBuilder shell(String script) {
    return new ProcessBuilder("sh", "-c", script);
  }
}
