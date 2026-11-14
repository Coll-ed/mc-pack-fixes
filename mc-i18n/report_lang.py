import json, os, sys, zipfile

mods = sys.argv[1]
jars = sys.argv[2:]

total_en = 0
total_zh = 0
rows = []
gaps = []

for jar in jars:
    path = os.path.join(mods, jar)
    with zipfile.ZipFile(path) as z:
        names = set(z.namelist())
        for entry in sorted(n for n in names if n.endswith('/lang/en_us.json')):
            ns = entry.split('/')[1]
            en = json.loads(z.read(entry).decode('utf-8'))
            zh_entry = entry.replace('en_us.json', 'zh_cn.json')
            total_en += len(en)
            if zh_entry in names:
                zh = json.loads(z.read(zh_entry).decode('utf-8'))
                missing = len([k for k in en if k not in zh])
                total_zh += len(zh)
                rows.append((jar, ns, len(en), len(zh), missing))
            else:
                gaps.append((jar, ns, len(en)))

print('jar'.ljust(46), 'namespace'.ljust(18), 'en', 'zh', 'missing')
for jar, ns, en, zh, missing in rows:
    print(f'{jar[:44]:46s} {ns:18s} {en:<6} {zh:<6} {missing}')
print()
print(f'合计 en={total_en} zh={total_zh}')
if gaps:
    print('仍缺 zh_cn 的命名空间:')
    for jar, ns, en in gaps:
        print(f'  {jar} {ns} ({en})')
else:
    print('所有命名空间均已具备 zh_cn')
