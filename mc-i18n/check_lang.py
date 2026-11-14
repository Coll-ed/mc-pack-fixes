import json, os, re, sys

W = sys.argv[1]
namespaces = sys.argv[2:]

for ns in namespaces:
    en_p = os.path.join(W, f'{ns}-en_us.json')
    zh_p = os.path.join(W, f'{ns}-zh_cn.json')
    if not os.path.exists(en_p):
        print(f'  {ns}: 缺 en_us')
        continue
    if not os.path.exists(zh_p):
        print(f'  {ns}: 尚未翻译（{len(json.load(open(en_p, encoding="utf-8")))} 键）')
        continue
    en = json.load(open(en_p, encoding='utf-8'))
    zh = json.load(open(zh_p, encoding='utf-8'))
    missing = [k for k in en if k not in zh]
    extra = [k for k in zh if k not in en]
    pat = re.compile(r'%(?:\d+\$)?[sd]|%%')
    bad = [k for k in en if k in zh and sorted(pat.findall(en[k])) != sorted(pat.findall(zh[k]))]
    status = 'OK' if not (missing or extra or bad) else 'CHECK'
    print(f'  {ns}: en {len(en)} / zh {len(zh)}  [{status}]')
    if missing:
        print('    缺:', missing[:8], f'（共 {len(missing)}）')
    if extra:
        print('    多:', extra[:8], f'（共 {len(extra)}）')
    if bad:
        print('    占位符差异:', bad[:8], f'（共 {len(bad)}）')
