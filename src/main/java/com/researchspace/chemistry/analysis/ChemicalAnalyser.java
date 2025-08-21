package com.researchspace.chemistry.analysis;

import com.researchspace.chemistry.extract.ExtractionResult;

public interface ChemicalAnalyser {

  /***
   * Extracts chemical information (e.g., mass, formula) from the chemical input string.
   * @param chemical the chemical in any of the supported chemical formats
   * @param groupByRole if true, groups molecules by their role (e.g. reactant, product, agent)
   *                    if false, returns all molecules in a flat list, each with a role of MOLECULE.
   * @return ExtractionResult containing the extracted chemical information
   */
  ExtractionResult analyse(String chemical, boolean groupByRole);
}
