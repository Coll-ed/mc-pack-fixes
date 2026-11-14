import json, os, sys

W = sys.argv[1]
for ns in sys.argv[2:]:
    p = os.path.join(W, f'{ns}-en_us.json')
    d = json.load(open(p, encoding='utf-8'))
    print(f'== {ns} ({len(d)}) ==')
    for k, v in d.items():
        print(f'{k}\t{v}')
    print()
