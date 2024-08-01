from typing import Callable
import importlib.util
from src.runtime import Processor


def load_processor(path: str, class_name: str) -> Callable[[dict[str, any]], Processor]:
    spec = importlib.util.spec_from_file_location("module", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return getattr(module, class_name)
