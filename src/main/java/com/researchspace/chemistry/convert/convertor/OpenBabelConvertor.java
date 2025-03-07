package com.researchspace.chemistry.convert.convertor;

import com.researchspace.chemistry.ChemistryException;
import com.researchspace.chemistry.convert.ConvertDTO;
import com.researchspace.chemistry.util.CommandExecutor;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpenBabelConvertor implements Convertor {
  private static final Logger LOGGER = LoggerFactory.getLogger(OpenBabelConvertor.class);
  private static final String OPENBABEL_EMPTY_OUTPUT_3XX = "Open Babel 3.\\d+.\\d+ --";
  private final Pattern pattern = Pattern.compile(OPENBABEL_EMPTY_OUTPUT_3XX);

  private final CommandExecutor commandExecutor;

  public OpenBabelConvertor(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  @PostConstruct
  public void init() {
    try {
      LOGGER.info("Checking OpenBabel version compatibility.");
      ProcessBuilder builder = new ProcessBuilder();
      builder.command("obabel");
      String output = String.join("\n", commandExecutor.executeCommand(builder));
      Matcher matcher = pattern.matcher(output);
      if (!matcher.find()) {
        throw new ChemistryException("OpenBabel version isn't 3.X.X so may be incompatible.");
      }
    } catch (IOException | ExecutionException | InterruptedException | TimeoutException e) {
      throw new ChemistryException("Problem while initializing OpenBabel.", e);
    }
  }

  @Override
  public Optional<String> convert(ConvertDTO convertDTO) {
    try {
      File tempFile = File.createTempFile("temp-", ".tmp");
      byte[] chemBytes = getChemBytes(convertDTO.input());
      Files.write(tempFile.toPath(), chemBytes);
      ProcessBuilder builder = new ProcessBuilder();
      builder.command(
          "obabel",
          "-i" + convertDTO.inputFormat(),
          tempFile.getAbsolutePath(),
          "-o" + convertDTO.outputFormat());
      String output = String.join("\n", commandExecutor.executeCommand(builder));
      Files.delete(tempFile.toPath());
      if (output.isEmpty() || isFailedConversionOutput(output)) {
        return Optional.empty();
      }
      return Optional.of(output);
    } catch (IOException | ExecutionException | InterruptedException | TimeoutException e) {
      throw new ChemistryException("Problem while converting.", e);
    }
  }

  private boolean isFailedConversionOutput(String output) {
    Matcher matcher = pattern.matcher(output);
    return matcher.find();
  }

  private byte[] getChemBytes(String input) {
    if (input.length() % 4 != 0) {
      return input.getBytes();
    }
    if (Base64.isBase64(input)) {
      try {
        return java.util.Base64.getDecoder().decode(input.getBytes());
      } catch (IllegalArgumentException e) {
        // unencoded inputs such as smiles can be incorrectly identified as base64
        return input.getBytes();
      }
    }
    return input.getBytes();
  }
}
