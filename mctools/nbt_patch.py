#!/usr/bin/env python3
"""极简 NBT 读写器：用于安全地修改 level.dat（不引入第三方依赖）。

支持：读取整棵树 → 按规则改值 → 原样重新序列化（保持所有 tag 类型不变）。

用法:
  python nbt_patch.py <输入 level.dat> <输出文件> gamerule.doDaylightCycle=true gamerule.doWeatherCycle=true DayTime=1000

规则:
  gamerule.<名>=<值>   -> Data.GameRules.<名>（TAG_String）
  DayTime=<整数>       -> Data.DayTime（TAG_Long）
  Time=<整数>          -> Data.Time（TAG_Long）
"""
import gzip
import struct
import sys

TAG_NAMES = {
    0: "End", 1: "Byte", 2: "Short", 3: "Int", 4: "Long", 5: "Float", 6: "Double",
    7: "ByteArray", 8: "String", 9: "List", 10: "Compound", 11: "IntArray", 12: "LongArray",
}


class Node:
    __slots__ = ("tag", "val")

    def __init__(self, tag, val):
        self.tag = tag
        self.val = val


class Reader:
    def __init__(self, data):
        self.d = data
        self.i = 0

    def take(self, n):
        v = self.d[self.i:self.i + n]
        self.i += n
        return v

    def num(self, fmt, size):
        v = struct.unpack_from(fmt, self.d, self.i)[0]
        self.i += size
        return v

    def string(self):
        n = self.num(">H", 2)
        return self.take(n).decode("utf-8", "replace")

    def payload(self, tag):
        if tag == 1:
            return self.num(">b", 1)
        if tag == 2:
            return self.num(">h", 2)
        if tag == 3:
            return self.num(">i", 4)
        if tag == 4:
            return self.num(">q", 8)
        if tag == 5:
            return self.num(">f", 4)
        if tag == 6:
            return self.num(">d", 8)
        if tag == 7:
            n = self.num(">i", 4)
            return self.take(n)
        if tag == 8:
            return self.string()
        if tag == 9:
            et = self.num(">b", 1)
            n = self.num(">i", 4)
            return Node(et, [self.payload(et) for _ in range(n)])
        if tag == 10:
            out = {}
            while True:
                t = self.num(">b", 1)
                if t == 0:
                    return out
                name = self.string()
                out[name] = Node(t, self.payload(t))
        if tag == 11:
            n = self.num(">i", 4)
            vals = list(struct.unpack_from(">" + "i" * n, self.d, self.i)) if n else []
            self.i += 4 * n
            return vals
        if tag == 12:
            n = self.num(">i", 4)
            vals = list(struct.unpack_from(">" + "q" * n, self.d, self.i)) if n else []
            self.i += 8 * n
            return vals
        raise ValueError(f"未知 tag {tag}")


class Writer:
    def __init__(self):
        self.buf = bytearray()

    def num(self, fmt, v):
        self.buf += struct.pack(fmt, v)

    def string(self, s):
        b = s.encode("utf-8")
        self.num(">H", len(b))
        self.buf += b

    def payload(self, tag, val):
        if tag == 1:
            self.num(">b", val)
        elif tag == 2:
            self.num(">h", val)
        elif tag == 3:
            self.num(">i", val)
        elif tag == 4:
            self.num(">q", val)
        elif tag == 5:
            self.num(">f", val)
        elif tag == 6:
            self.num(">d", val)
        elif tag == 7:
            self.num(">i", len(val))
            self.buf += val
        elif tag == 8:
            self.string(val)
        elif tag == 9:
            self.num(">b", val.tag)
            self.num(">i", len(val.val))
            for item in val.val:
                self.payload(val.tag, item)
        elif tag == 10:
            for name, node in val.items():
                self.num(">b", node.tag)
                self.string(name)
                self.payload(node.tag, node.val)
            self.num(">b", 0)
        elif tag == 11:
            self.num(">i", len(val))
            for v in val:
                self.num(">i", v)
        elif tag == 12:
            self.num(">i", len(val))
            for v in val:
                self.num(">q", v)
        else:
            raise ValueError(f"未知 tag {tag}")


def main() -> int:
    if len(sys.argv) < 3:
        print(__doc__)
        return 2
    src, dst = sys.argv[1], sys.argv[2]
    ops = sys.argv[3:]

    raw = gzip.open(src, "rb").read()
    r = Reader(raw)
    root_tag = r.num(">b", 1)
    if root_tag != 10:
        print("根不是 Compound")
        return 1
    root_name = r.string()
    root = r.payload(10)

    data = root.get("Data", Node(10, {})).val
    gamerules = data.get("GameRules", Node(10, {})).val

    applied = []
    for op in ops:
        if "=" not in op:
            print(f"跳过非法参数: {op}")
            continue
        key, value = op.split("=", 1)
        if key.startswith("gamerule."):
            name = key[len("gamerule."):]
            gamerules[name] = Node(8, value)
            applied.append(f"GameRules.{name} = {value!r}")
        elif key in ("DayTime", "Time"):
            data[key] = Node(4, int(value))
            applied.append(f"Data.{key} = {value}")
        else:
            print(f"未知键: {key}")
            return 1

    data["GameRules"] = Node(10, gamerules)
    root["Data"] = Node(10, data)

    w = Writer()
    w.num(">b", 10)
    w.string(root_name)
    w.payload(10, root)
    gzip.open(dst, "wb").write(bytes(w.buf))

    for a in applied:
        print("  已设置 " + a)
    print(f"  输出: {dst} ({len(w.buf)} 字节未压缩)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
