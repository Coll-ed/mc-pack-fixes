import json, os, sys

W = sys.argv[1]
ns = sys.argv[2]
start = int(sys.argv[3])
end = int(sys.argv[4])
d = json.load(open(os.path.join(W, f'{ns}-en_us.json'), encoding='utf-8'))
items = list(d.items())[start:end]
print(f'== {ns} [{start}:{end}] / {len(d)} ==')
for k, v in items:
    print(f'{k}\t{v}')
