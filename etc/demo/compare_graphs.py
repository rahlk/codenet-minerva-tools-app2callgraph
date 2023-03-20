import json
import os
import sys
from pathlib import Path
from ipdb import set_trace
from rich.pretty import pprint as print

if __name__ == "__main__":
    fpath = sys.argv[1] if sys.argv[1] else os.getcwd()
    print(fpath)
    for _file in Path.glob(Path(fpath), "call_graph_*.json"):
        name = str(_file.stem).removeprefix(
            "call_graph_").removesuffix(".json")
        print(name)
        with open(_file, 'r') as json_file:
            cg = json.load(json_file)

        if name == 'rta':
            nodes_rta = [node['id'] for node in cg['nodes']]
            print("Num nodes in RTA = " + str(len(nodes_rta)))
        if name == 'zero':
            nodes_zero = [node['id'] for node in cg['nodes']]
            print("Num nodes in 0-CFA = " + str(len(nodes_zero)))
        if name == 'zero-one':
            nodes_zero_one = [node['id'] for node in cg['nodes']]
            print("Num nodes in 01-CFA= " + str(len(nodes_zero_one)))

    print("Missing nodes in RTA vs. 0-CFA")
    print(set(nodes_rta).difference(set(nodes_zero)))

    print("Missing nodes in RTA vs. 01-CFA")
    print(set(nodes_rta).difference(set(nodes_zero_one)))

    print("Missing nodes in 0-CFA vs. 01-CFA")
    print(set(nodes_zero).difference(set(nodes_zero_one)))
