package com.researchspace.chemistry.search.repository;

import com.researchspace.chemistry.search.SearchType;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface ChemicalRepository {

  void saveChemical(String smiles, String chemicalId) throws IOException;

  void clearAll() throws IOException;

  void initialize() throws IOException;

  List<String> search(String smiles, SearchType searchType)
      throws IOException, ExecutionException, InterruptedException, TimeoutException;
}
