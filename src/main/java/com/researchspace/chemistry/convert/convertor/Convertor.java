package com.researchspace.chemistry.convert.convertor;

import com.researchspace.chemistry.convert.ConvertDTO;
import java.util.Optional;

public interface Convertor {

  Optional<String> convert(ConvertDTO convertDTO);
}
