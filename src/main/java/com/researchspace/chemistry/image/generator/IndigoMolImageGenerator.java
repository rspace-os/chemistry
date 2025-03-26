package com.researchspace.chemistry.image.generator;

import com.researchspace.chemistry.ChemistryException;
import com.researchspace.chemistry.convert.ConvertDTO;
import com.researchspace.chemistry.convert.ConvertService;
import com.researchspace.chemistry.image.ImageDTO;
import org.springframework.stereotype.Service;

/***
 * Image generator which uses Indigo to generate the image, but firstly converts the original format
 * into MOL. MOL has a high success rate for being converted to and generating images from. The
 * downside of MOL over some original formats (e.g. cdxml) is that some information is lost during conversion.
 */
@Service
public class IndigoMolImageGenerator extends BaseImageGenerator {
  private final IndigoImageUtils indigoImageUtils;
  private final ConvertService convertService;

  public IndigoMolImageGenerator(IndigoImageUtils indigoImageUtils, ConvertService convertService) {
    this.indigoImageUtils = indigoImageUtils;
    this.convertService = convertService;
  }

  @Override
  public byte[] generateImage(ImageDTO imageDTO) {
    ConvertDTO convertDTO = makeConversionDTO(imageDTO);
    try {
      String converted = convertService.convert(convertDTO);
      return indigoImageUtils.generateImage(
          new ImageDTO(
              converted, imageDTO.outputFormat(), imageDTO.width(), imageDTO.height(), "mol"));
    } catch (ChemistryException e) {
      return handleError(IndigoMolImageGenerator.class.getName(), e, imageDTO);
    }
  }

  private ConvertDTO makeConversionDTO(ImageDTO imageDTO) {
    return new ConvertDTO(imageDTO.input(), imageDTO.inputFormat(), "mol");
  }
}
