package com.github.rspaceos.chemistry.search;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

  private final SearchService searchService;

  public SearchController(SearchService searchService) {
    this.searchService = searchService;
  }

  @PostMapping(value = "/chemistry/save")
  public @ResponseBody String convert(@Valid @RequestBody SaveDTO saveDTO) throws IOException {
    searchService.saveChemicalToFile(saveDTO.chemical(), saveDTO.chemicalId());
    return "Saved";
  }

  @PostMapping(value = "/chemistry/search")
  public @ResponseBody List<String> exportImage(@Valid @RequestBody SearchDTO searchDTO)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    return searchService.search(searchDTO.chemicalSearchTerm());
  }
}
