import json
import os
import sys
from pathlib import Path
from ipdb import set_trace
from rich.pretty import pprint as print

if __name__ == "__main__":
    fpath = sys.argv[1] if sys.argv[1] else os.getcwd()
    cha_classes = Path(fpath).joinpath("classes_in_class_hierarchy.txt");
    with open(cha_classes, 'r') as cha_file:
        cha = cha_file.readlines()

    print("Total classes in cha = " + str(len(cha)))
    all_cg_classes = set()
    all_cha_classes = set()
    for cha_class in cha:
        all_cha_classes.add(cha_class.replace("/", ".").removeprefix("L").rstrip())

    for _file in Path.glob(Path(fpath), "call_graph_*.json"):
        name = str(_file.stem).removeprefix(
            "call_graph_").removesuffix(".json")
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

    all_cg_classes = set(nodes_rta+nodes_zero+nodes_zero_one)

    print("Missing nodes in CHA vs. Call graphs")
    print(set(all_cha_classes).difference(all_cg_classes))

    print("Missing nodes in RTA vs. 0-CFA")
    print(set(nodes_rta).difference(set(nodes_zero)))

    print("Missing nodes in RTA vs. 01-CFA")
    print(set(nodes_rta).difference(set(nodes_zero_one)))

    print("Missing nodes in 0-CFA vs. 01-CFA")
    print(set(nodes_zero).difference(set(nodes_zero_one)))
