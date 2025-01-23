package com.researchspace.chemistry.convert.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.researchspace.chemistry.convert.ConvertDTO;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CompositeConvertorTest {

  @Mock OpenBabelConvertor openBabelConvertor;
  @Mock IndigoConvertor indigoConvertor;

  CompositeConvertor compositeConvertor;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    compositeConvertor = new CompositeConvertor(openBabelConvertor, indigoConvertor);
  }

  @Test
  public void whenEmptyInputFormat_thenUseIndigoConvertor() {
    ConvertDTO input = new ConvertDTO("aChemical", "aFormat");
    String expected = "C";

    when(indigoConvertor.convert(any())).thenReturn(Optional.of(expected));

    String actual = compositeConvertor.convert(input).get();

    assertEquals(expected, actual);
    Mockito.verify(indigoConvertor, times(1)).convert(any());
    Mockito.verify(openBabelConvertor, times(0)).convert(any());
  }

  @Test
  public void whenInputFormatSupplied_tryOpenBabelFirst() {
    ConvertDTO input = new ConvertDTO("aChemical", "anInputFormat", "anOutputFormat");
    String expected = "C";

    when(openBabelConvertor.convert(any())).thenReturn(Optional.of(expected));

    String actual = compositeConvertor.convert(input).get();

    assertEquals(expected, actual);
    Mockito.verify(openBabelConvertor, times(1)).convert(any());
    Mockito.verify(indigoConvertor, never()).convert(any());
  }

  @Test
  public void whenNotConvertedByOpenBabel_thenConvertWithIndigo() {
    ConvertDTO input = new ConvertDTO("aChemical", "anInputFormat", "anOutputFormat");
    String expected = "C";

    when(openBabelConvertor.convert(any())).thenReturn(Optional.empty());
    when(indigoConvertor.convert(any())).thenReturn(Optional.of(expected));

    String actual = compositeConvertor.convert(input).get();

    assertEquals(expected, actual);
    Mockito.verify(openBabelConvertor, times(1)).convert(any());
    Mockito.verify(indigoConvertor, times(1)).convert(any());
  }

  @Test
  public void whenNotConvertedByEitherConvertor_thenReturnEmptyOptional() {
    ConvertDTO input = new ConvertDTO("aChemical", "anInputFormat", "anOutputFormat");

    when(openBabelConvertor.convert(any())).thenReturn(Optional.empty());
    when(indigoConvertor.convert(any())).thenReturn(Optional.empty());

    Optional<String> actual = compositeConvertor.convert(input);

    assertEquals(Optional.empty(), actual);
    Mockito.verify(openBabelConvertor, times(1)).convert(any());
    Mockito.verify(indigoConvertor, times(1)).convert(any());
  }
}
