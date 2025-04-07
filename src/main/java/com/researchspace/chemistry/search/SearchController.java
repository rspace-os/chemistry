package com.researchspace.chemistry.search;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.springframework.web.bind.annotation.DeleteMapping;
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

  @DeleteMapping(value = "/chemistry/clearSearchIndexes")
  public @ResponseBody String clearSearchIndexes() throws IOException {
    searchService.clearFiles();
    return "Cleared";
  }

  @PostMapping(value = "/chemistry/save")
  public @ResponseBody String save(@Valid @RequestBody SaveDTO saveDTO) throws IOException {
    searchService.saveChemicals(saveDTO);
    return "Saved";
  }

  @PostMapping(value = "/chemistry/search")
  public @ResponseBody List<String> search(@Valid @RequestBody SearchDTO searchDTO)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    return searchService.search(searchDTO);
  }

  @PostMapping(value = "/chemistry/index")
  public @ResponseBody String index()
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    searchService.indexChemicals();
    return "Indexed";
  }
}
