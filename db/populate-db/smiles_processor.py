#!/usr/bin/env python3
"""
SMILES Data Processor
Fetches 100K SMILES strings and sends them to a local endpoint.

Requirements:
- requests
- pandas (for CSV handling)
- Optional: rdkit (for SMILES validation)

Data Source: ChEMBL dataset from Kaggle
"""

import requests
import pandas as pd
import time
import logging
from typing import List, Optional, Dict, Any
import sys
from pathlib import Path
import zipfile
import os
import subprocess

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('smiles_processor.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class SMILESProcessor:
    def __init__(self, endpoint_url: str = "http://localhost:8090/chemistry/saveBatch"):
        self.endpoint_url = endpoint_url
        self.processed_count = 0
        self.error_count = 0
        
    def download_smiles_data(self) -> List[str]:
        """
        Download SMILES data from Kaggle dataset.
        Checks if dataset exists locally, otherwise downloads it.
        """
        return self._download_kaggle_dataset()
    
    
    def _download_kaggle_dataset(self) -> List[str]:
        """
        Download from Kaggle dataset.
        Checks if dataset exists locally, otherwise downloads it automatically.
        """
        logger.info("Looking for local Kaggle dataset...")
        
        # Common paths where the dataset might be (now looking for txt files)
        possible_paths = [
            "chembl22.txt",
            "data/chembl22.txt",
            "datasets/chembl22.txt",
            "chembl_22.txt",
            "smiles_data.txt"
        ]
        
        # Check for existing dataset
        for path in possible_paths:
            if Path(path).exists():
                logger.info(f"Found dataset at: {path}")
                smiles_list = []
                
                # Parse tab-separated text file (SMILES\tCHEMBL_ID)
                try:
                    with open(path, 'r', encoding='utf-8') as f:
                        for line_num, line in enumerate(f, 1):
                            line = line.strip()
                            if line and not line.startswith('#'):  # Skip empty lines and comments
                                parts = line.split('\t')
                                if len(parts) >= 1:  # At least SMILES should be present
                                    smiles = parts[0].strip()
                                    if smiles:  # Ensure SMILES is not empty
                                        smiles_list.append(smiles)
                                        
                                        if len(smiles_list) >= 100000:  # Limit to 100k
                                            break
                    
                    logger.info(f"Loaded {len(smiles_list)} SMILES strings from {path}")
                    return smiles_list
                    
                except Exception as e:
                    logger.error(f"Error parsing {path}: {e}")
                    continue
        
        # If no local file found, try to download it
        logger.info("No local dataset found. Attempting to download...")
        return self._download_from_kaggle_url()
    
    def _download_from_kaggle_url(self) -> List[str]:
        """Download dataset using Kaggle API (fallback when Kaggle dataset not available locally)."""
        logger.info("Downloading ChEMBL dataset using Kaggle API...")
        
        dataset_name = "art3mis/chembl22"
        local_filename = "chembl22.txt"
        
        try:
            # Try to download using Kaggle API
            logger.info("Attempting to download using Kaggle CLI...")
            result = subprocess.run([
                "kaggle", "datasets", "download", "-d", dataset_name, "--unzip", "-p", "."
            ], capture_output=True, text=True, timeout=300)
            
            if result.returncode == 0:
                logger.info("Dataset downloaded successfully using Kaggle API")
                
                # Check if the expected file exists after download
                possible_files = [
                    "chembl22.txt",
                    "chembl_22.txt", 
                    "ChEMBL22.txt",
                    "ChEMBL_22.txt"
                ]
                
                downloaded_file = None
                for filename in possible_files:
                    if Path(filename).exists():
                        downloaded_file = filename
                        break
                
                if not downloaded_file:
                    # Check what files were actually downloaded
                    txt_files = list(Path(".").glob("*.txt"))
                    if txt_files:
                        downloaded_file = str(txt_files[0])
                        logger.info(f"Using downloaded file: {downloaded_file}")
                    else:
                        raise FileNotFoundError("No txt file found after download")
                
                # Rename the downloaded file to chembl22.txt for consistency
                if downloaded_file != local_filename:
                    logger.info(f"Renaming {downloaded_file} to {local_filename}")
                    Path(downloaded_file).rename(local_filename)
                    downloaded_file = local_filename
                
                # Parse the downloaded txt file (tab-separated format)
                smiles_list = []
                try:
                    with open(downloaded_file, 'r', encoding='utf-8') as f:
                        for line_num, line in enumerate(f, 1):
                            line = line.strip()
                            if line and not line.startswith('#'):  # Skip empty lines and comments
                                parts = line.split('\t')
                                if len(parts) >= 1:  # At least SMILES should be present
                                    smiles = parts[0].strip()
                                    if smiles:  # Ensure SMILES is not empty
                                        smiles_list.append(smiles)
                                        
                                        if len(smiles_list) >= 100000:  # Limit to 100k
                                            break
                    
                    logger.info(f"Loaded {len(smiles_list)} SMILES strings from downloaded dataset")
                    return smiles_list
                    
                except Exception as e:
                    logger.error(f"Error parsing downloaded file {downloaded_file}: {e}")
                    raise ValueError(f"Failed to parse downloaded dataset: {e}")
            else:
                raise Exception(f"Kaggle CLI failed: {result.stderr}")
                
        except FileNotFoundError:
            logger.error("Kaggle CLI not found. Please install kaggle package: pip install kaggle")
            logger.info("Then configure your API credentials following:")
            logger.info("https://github.com/Kaggle/kaggle-api#api-credentials")
        except subprocess.TimeoutExpired:
            logger.error("Download timed out after 5 minutes")
        except Exception as e:
            logger.error(f"Failed to download dataset: {e}")
            
        # If all else fails, provide manual instructions
        logger.info("Automatic download failed. Please manually download the dataset:")
        logger.info("1. Visit: https://www.kaggle.com/datasets/art3mis/chembl22")
        logger.info("2. Download the dataset file")
        logger.info("3. Place it as 'chembl22.txt' in the current directory")
        logger.info("4. Or install and configure Kaggle CLI: pip install kaggle")
        raise FileNotFoundError("Dataset not found and automatic download failed. Please download manually.")
    
    def validate_smiles(self, smiles: str) -> bool:
        """
        Validate SMILES string (optional, requires rdkit).
        """
        try:
            from rdkit import Chem
            mol = Chem.MolFromSmiles(smiles)
            return mol is not None
        except ImportError:
            # If rdkit not available, do basic validation
            return len(smiles) > 0 and smiles.strip() != ""
    
    def send_batch_to_endpoint(self, batch_data: List[Dict[str, Any]]) -> bool:
        """
        Send a batch of SMILES data to the endpoint.
        
        Args:
            batch_data: List of dictionaries containing chemical and chemicalId
            
        Returns:
            True if successful, False otherwise
        """
        try:
            response = requests.post(
                self.endpoint_url,
                json=batch_data,
                headers={'Content-Type': 'application/json'},
                timeout=30
            )
            response.raise_for_status()
            return True
            
        except requests.exceptions.RequestException as e:
            logger.error(f"Error sending batch of {len(batch_data)} SMILES: {e}")
            self.error_count += len(batch_data)
            return False
    
    def process_smiles_batch(self, smiles_list: List[str], batch_size: int = 1000) -> None:
        """
        Process SMILES strings in batches of 100 DTOs.
        
        Args:
            smiles_list: List of SMILES strings
            batch_size: Number of DTOs to send in each batch (fixed at 100)
        """
        total_count = len(smiles_list)
        logger.info(f"Starting to process {total_count} SMILES strings in batches of {batch_size}...")
        
        # Process in batches
        for batch_start in range(0, total_count, batch_size):
            batch_end = min(batch_start + batch_size, total_count)
            current_batch = []
            
            # Create batch of DTOs
            for i in range(batch_start, batch_end):
                smiles = smiles_list[i]
                if self.validate_smiles(smiles):
                    dto = {
                        "chemical": smiles,
                        "chemicalId": i + 1
                    }
                    current_batch.append(dto)
                else:
                    logger.warning(f"Invalid SMILES at position {i + 1}: {smiles}")
            
            # Send batch if not empty
            if current_batch:
                success = self.send_batch_to_endpoint(current_batch)
                
                if success:
                    self.processed_count += len(current_batch)
                    logger.info(f"Successfully sent batch of {len(current_batch)} SMILES (total processed: {self.processed_count})")
                else:
                    logger.error(f"Failed to send batch of {len(current_batch)} SMILES")
                
                # Small pause between batches to avoid overwhelming the server
                time.sleep(0.1)
            
            # Progress logging
            if batch_end % 1000 == 0 or batch_end == total_count:
                logger.info(f"Processed {batch_end}/{total_count} ({batch_end/total_count*100:.1f}%)")
        
        logger.info(f"Processing complete! Successfully sent: {self.processed_count}, Errors: {self.error_count}")

def main():
    """Main function to run the SMILES processor."""
    processor = SMILESProcessor()
    
    try:
        # Load SMILES data from Kaggle dataset (checks local first, downloads if needed)
        smiles_data = processor.download_smiles_data()
        
        # Process the SMILES data in batches of 100
        processor.process_smiles_batch(smiles_data)
        
    except KeyboardInterrupt:
        logger.info("Process interrupted by user.")
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
