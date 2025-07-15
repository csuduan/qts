import importlib.util
import os.path
import sys
from qts.common import get_conf
def load_library_dynamic(type:str, version: str):
    if type == 'ctp':
        if version == 'v6.6.7':
            lib_path = 'ctp_v6_6_7'
        elif version == 'v6.7.2':
            lib_path = 'ctp_v6_7_2'
        else:
            #raise ValueError(f"Unsupported version: {version}")
            lib_path = 'ctp_v6_7_2' # default version
    else:
        raise ValueError(f"Unsupported type: {type}")

    # Add the library path to sys.path

    api_path = get_conf('api_path')
    path = os.path.join(api_path,lib_path)
    sys.path.insert(0, path)

    # Import the library
    spec = importlib.util.find_spec('openctp_ctp')
    #spec = importlib.util.spec_from_file_location('openctp_ctp',path)
    if spec is None:
        raise ImportError("Library 'openctp_ctp' not found")
    library = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(library)

    # Remove the library path from sys.path
    sys.path.pop(0)
    return library


def test():
    # Example usage
    type='ctp'
    version = 'v6.6.8'  # or '3.6.2'
    ctp = load_library_dynamic(type, version)
    mdapi = ctp.mdapi
    version1 = mdapi.CThostFtdcMdApi.GetApiVersion()
    print(version1)

