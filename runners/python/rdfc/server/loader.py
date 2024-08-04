from typing import Callable
import importlib.util
from src.runtime import Processor

import os
import sys
import importlib
import zipfile


def extract_wheel(wheel_path) -> str:
    print(f"Extracting wheel: {wheel_path}")

    destination_directory = "/tmp/rdf-connect/python"
    if not os.path.exists(destination_directory):
        print(f"Creating directory: {destination_directory}")
        os.makedirs(destination_directory)

    wheel_filename = os.path.basename(wheel_path)
    destination = f"{destination_directory}/{wheel_filename}"

    with zipfile.ZipFile(wheel_path, 'r') as zip_ref:
        print(f"Extracting wheel: ${destination}")
        zip_ref.extractall(destination)

    print("Wheel extraction: done")
    return destination


def load_wheel(wheel_path: str, module_name: str, class_name: str) -> Callable[[dict[str, any]], Processor]:
    wheel_path = wheel_path.replace("file://", "")
    print(f"Loading wheel: {wheel_path}")
    destination = extract_wheel(wheel_path)

    # Append the extracted directory to the Python path.
    sys.path.insert(destination)

    # Dynamically import.
    module = importlib.import_module(module_name)

    # Retrieve the class.
    return getattr(module, class_name)
