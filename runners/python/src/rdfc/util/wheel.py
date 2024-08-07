import sys
import typing
import importlib.util
import os
import rdfc
import importlib
import zipfile
import shutil


class Wheel:
    @staticmethod
    def extract(wheel_path) -> str:
        print(f"Extracting wheel: {wheel_path}", flush=True)

        destination_directory = os.path.dirname(wheel_path)
        wheel_filename: str = os.path.basename(wheel_path)
        destination = f"{destination_directory}/{wheel_filename.removesuffix(".whl")}"

        # Remove the directory if it exists.
        if os.path.exists(destination):
            print(f"Directory will be overwritten: {destination}", flush=True)
            shutil.rmtree(destination)
            print(f"Directory removed: {destination}", flush=True)

        with zipfile.ZipFile(wheel_path, "r") as zip_ref:
            print(f"Extracting wheel: ${destination}", flush=True)
            zip_ref.extractall(destination)

        print("Wheel extraction: done", flush=True)
        return destination

    @staticmethod
    def load(
        entrypoint: str, module_name: str, class_name: str
    ) -> typing.Callable[[rdfc.Arguments], rdfc.Processor]:
        entrypoint = entrypoint.removeprefix("file://").removesuffix("/")

        # Add module to path using `src`
        src_dir = f"{entrypoint}/src/"
        sys.path.append(src_dir)
        print(f"Including source directory: {src_dir}")

        # Add dependencies to path using `.venv`.
        lib_dir = f"{entrypoint}/.venv/lib/"
        python_versions = [
            _dir
            for _dir in os.listdir(lib_dir)
            if os.path.isdir(os.path.join(lib_dir, _dir))
        ]
        assert len(python_versions) == 1
        python_version = python_versions[0]
        packages_dir = f"{entrypoint}/.venv/lib/{python_version}/site-packages"
        print(f"Including packages directory: {packages_dir}")
        sys.path.append(packages_dir)

        # Dynamically import.
        print(f"Loading module: {module_name}", flush=True)
        module = importlib.import_module(module_name)
        print(f"Module loaded successfully: {module_name}")

        # Retrieve the class.
        print(f"Extracting processor: {class_name}")
        clazz = getattr(module, class_name)
        print(f"Processor extracted: {class_name}")
        return clazz
