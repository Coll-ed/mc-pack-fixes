import json, os, shutil, sys, zipfile

jar, workdir = sys.argv[1], sys.argv[2]
namespaces = sys.argv[3:]

entries = {}
for ns in namespaces:
    zh = os.path.join(workdir, f'{ns}-zh_cn.json')
    if not os.path.exists(zh):
        print(f'  跳过 {ns}: 无 zh_cn')
        continue
    entries[f'assets/{ns}/lang/zh_cn.json'] = open(zh, 'rb').read()

if not entries:
    print('  没有可注入的命名空间')
    raise SystemExit(1)

tmp = jar + '.tmp'
with zipfile.ZipFile(jar) as src, zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as dst:
    for item in src.infolist():
        dst.writestr(item, src.read(item.filename))
    for path, data in entries.items():
        dst.writestr(path, data)
shutil.move(tmp, jar)

with zipfile.ZipFile(jar) as z:
    for path in entries:
        d = json.loads(z.read(path).decode('utf-8'))
        print(f'  已注入 {path}  {len(d)} 键')
