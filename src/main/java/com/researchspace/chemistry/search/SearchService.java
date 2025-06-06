package com.researchspace.chemistry.search;

import com.researchspace.chemistry.convert.ConvertDTO;
import com.researchspace.chemistry.convert.ConvertService;
import com.researchspace.chemistry.convert.convertor.OpenBabelConvertor;
import com.researchspace.chemistry.search.repository.ChemicalRepository;
import com.researchspace.chemistry.search.repository.OpenBabelChemicalRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SearchService {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);

  private final ChemicalRepository chemicalRepository;
  private final ConvertService convertService;
  private final OpenBabelConvertor openBabelConvertor;

  @Autowired
  public SearchService(
      ChemicalRepository chemicalRepository,
      ConvertService convertService,
      OpenBabelConvertor openBabelConvertor) {
    this.chemicalRepository = chemicalRepository;
    this.convertService = convertService;
    this.openBabelConvertor = openBabelConvertor;
  }

  @PostConstruct
  public void initFiles() throws IOException {
    chemicalRepository.initialize();
  }

  public void clearFiles() throws IOException {
    LOGGER.info("clearing search indexes...");
    chemicalRepository.clearAll();
    LOGGER.info("... done");
  }

  public void saveChemicals(SaveDTO saveDTO) throws IOException {
    String smiles = getSmilesFromOpenBabel(saveDTO.chemical(), saveDTO.chemicalFormat());
    chemicalRepository.saveChemical(smiles, saveDTO.chemicalId());
  }

  public List<String> search(SearchDTO search)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    if (search.chemicalSearchTerm() != null && !search.chemicalSearchTerm().isEmpty()) {
      String smiles =
          getSmilesFromOpenBabel(search.chemicalSearchTerm(), search.searchTermFormat());
      return chemicalRepository.search(smiles, search.searchType());
    }
    return Collections.emptyList();
  }

  private String getSmilesFromOpenBabel(String originalChem, String originalFormat) {
    String initialSmiles = convertService.convert(new ConvertDTO(originalChem, "smiles"));
    return openBabelConvertor
        .convert(new ConvertDTO(initialSmiles, "smiles", "smiles"))
        .orElse(initialSmiles);
  }

  public void indexChemicals() throws IOException, ExecutionException, InterruptedException, TimeoutException {
    if(chemicalRepository instanceof OpenBabelChemicalRepository) {
      ((OpenBabelChemicalRepository) chemicalRepository).indexChemicals();
    } else {
      LOGGER.warn("Indexing is not supported for the current repository implementation.");
    }
  }


}
