import json, os, sys, zipfile

mods, workdir, jar = sys.argv[1], sys.argv[2], sys.argv[3]
namespaces = sys.argv[4:]

with zipfile.ZipFile(os.path.join(mods, jar)) as z:
    for ns in namespaces:
        path = f'assets/{ns}/lang/en_us.json'
        if path not in z.namelist():
            print(f'  跳过 {ns}: 无 en_us')
            continue
        data = json.loads(z.read(path).decode('utf-8'))
        out = os.path.join(workdir, f'{ns}-en_us.json')
        json.dump(data, open(out, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
        print(f'  {ns}: {len(data)} 键 → {os.path.basename(out)}')
