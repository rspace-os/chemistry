package com.researchspace.chemistry.convert.convertor;

import com.researchspace.chemistry.ChemistryException;
import com.researchspace.chemistry.convert.ConvertDTO;
import com.researchspace.chemistry.util.CommandExecutor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Service;

@Service("OpenBabelConvertor")
public class OpenBabelConvertor implements Convertor {
  public static final String OPENBABEL_ERROR_OUTPUT_IDENTIFIER = "Open Babel 3.1.0 -- ";

  private final CommandExecutor commandExecutor;

  public OpenBabelConvertor(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  @Override
  public Optional<String> convert(ConvertDTO convertDTO) {
    try {
      File tempFile = File.createTempFile("temp-", ".tmp");
      Files.write(tempFile.toPath(), convertDTO.input().getBytes());
      ProcessBuilder builder = new ProcessBuilder();
      builder.command(
          "obabel",
          "-i" + convertDTO.inputFormat(),
          tempFile.getAbsolutePath(),
          "-o" + convertDTO.outputFormat());
      String output = String.join("\n", commandExecutor.executeCommand(builder));
      Files.delete(tempFile.toPath());
      if (output.isEmpty() || output.startsWith(OPENBABEL_ERROR_OUTPUT_IDENTIFIER)) {
        return Optional.empty();
      }
      return Optional.of(output);
    } catch (IOException | ExecutionException | InterruptedException | TimeoutException e) {
      throw new ChemistryException("Problem while converting.", e);
    }
  }
}
