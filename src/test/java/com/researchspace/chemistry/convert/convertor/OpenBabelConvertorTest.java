package com.researchspace.chemistry.convert.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.researchspace.chemistry.ChemistryException;
import com.researchspace.chemistry.convert.ConvertDTO;
import com.researchspace.chemistry.util.CommandExecutor;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenBabelConvertorTest {

  @Mock CommandExecutor commandExecutor;

  @InjectMocks OpenBabelConvertor convertor;

  @Test
  public void whenSuccessfulConversion_thenReturnResults() throws Exception {
    String expected = "C";
    when(commandExecutor.executeCommand(any())).thenReturn(List.of(expected));

    Optional<String> actual = convertor.convert(new ConvertDTO("someInput", "someFormat"));

    assertEquals(expected, actual.get());
  }

  @Test
  public void whenMultiLineOutput_thenStringJoinedWithNewLine() throws Exception {
    when(commandExecutor.executeCommand(any())).thenReturn(List.of("C", "CC"));

    String expected = "C\nCC";
    Optional<String> actual = convertor.convert(new ConvertDTO("someInput", "someFormat"));

    assertEquals(expected, actual.get());
  }

  @Test
  public void whenNoOutputFromOpenBabel_thenReturnEmpty() throws Exception {
    when(commandExecutor.executeCommand(any())).thenReturn(Collections.emptyList());

    Optional<String> actual = convertor.convert(new ConvertDTO("someInput", "someFormat"));

    assertEquals(Optional.empty(), actual);
  }

  @Test
  public void whenExceptionThrownFromOpenBabel_thenWrapWithChemistryException() throws Exception {
    String ioExceptionMessage = "Problem accessing OpenBabel";
    when(commandExecutor.executeCommand(any())).thenThrow(new IOException(ioExceptionMessage));

    ChemistryException exception =
        assertThrows(
            ChemistryException.class,
            () -> {
              ConvertDTO convertDTO = new ConvertDTO("not-smiles", "cdxml");
              convertor.convert(convertDTO);
            });

    assertEquals("Problem while converting.", exception.getMessage());
    assertEquals(ioExceptionMessage, exception.getCause().getMessage());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Open Babel 3.1.0 --",
        "Open Babel 3.1.1 --",
        "Open Babel 3.1.15 --",
        "Open Babel 3.105.7 --"
      })
  public void whenOpenBabelCannotConvertAndReturnsVersion_thenErrorOutputIsRecongised(
      String openBabelOutput) throws Exception {
    when(commandExecutor.executeCommand(any())).thenReturn(List.of(openBabelOutput));

    Optional<String> actual = convertor.convert(new ConvertDTO("someInput", "someFormat"));
    assertEquals(Optional.empty(), actual);
  }
}
