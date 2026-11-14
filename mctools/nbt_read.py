#!/usr/bin/env python3
"""极简 NBT 读取器（只读，用于查看 level.dat 里的 GameRules/DayTime）。

用法: python nbt_read.py <level.dat> [key1 key2 ...]
不带 key 时打印顶层键名；带 key 时按路径打印值（支持 Data.GameRules.doDaylightCycle 这样点分）。
"""
import gzip
import struct
import sys


class Reader:
    def __init__(self, data):
        self.d = data
        self.i = 0

    def u1(self):
        v = self.d[self.i]
        self.i += 1
        return v

    def i1(self):
        v = struct.unpack_from(">b", self.d, self.i)[0]
        self.i += 1
        return v

    def i2(self):
        v = struct.unpack_from(">h", self.d, self.i)[0]
        self.i += 2
        return v

    def i4(self):
        v = struct.unpack_from(">i", self.d, self.i)[0]
        self.i += 4
        return v

    def i8(self):
        v = struct.unpack_from(">q", self.d, self.i)[0]
        self.i += 8
        return v

    def f4(self):
        v = struct.unpack_from(">f", self.d, self.i)[0]
        self.i += 4
        return v

    def f8(self):
        v = struct.unpack_from(">d", self.d, self.i)[0]
        self.i += 8
        return v

    def s(self):
        n = self.i2()
        v = self.d[self.i:self.i + n].decode("utf-8", "replace")
        self.i += n
        return v

    def payload(self, t):
        if t == 1:
            return self.i1()
        if t == 2:
            return self.i2()
        if t == 3:
            return self.i4()
        if t == 4:
            return self.i8()
        if t == 5:
            return self.f4()
        if t == 6:
            return self.f8()
        if t == 7:
            n = self.i4()
            v = self.d[self.i:self.i + n]
            self.i += n
            return f"<byte[{n}]>"
        if t == 8:
            return self.s()
        if t == 9:
            et = self.u1()
            n = self.i4()
            return [self.payload(et) for _ in range(n)]
        if t == 10:
            out = {}
            while True:
                tt = self.u1()
                if tt == 0:
                    return out
                name = self.s()
                out[name] = self.payload(tt)
        if t == 11:
            n = self.i4()
            v = struct.unpack_from(">" + "i" * n, self.d, self.i)
            self.i += 4 * n
            return list(v)
        if t == 12:
            n = self.i4()
            v = struct.unpack_from(">" + "q" * n, self.d, self.i)
            self.i += 8 * n
            return list(v)
        raise ValueError(f"未知 tag 类型 {t}")


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    raw = gzip.open(sys.argv[1], "rb").read()
    r = Reader(raw)
    t = r.u1()
    if t != 10:
        print("根不是 compound")
        return 1
    r.s()
    root = r.payload(10)

    if len(sys.argv) == 2:
        for k in root:
            print(k)
        return 0

    for path in sys.argv[2:]:
        cur = root
        ok = True
        for part in path.split("."):
            if isinstance(cur, dict) and part in cur:
                cur = cur[part]
            else:
                ok = False
                break
        print(f"{path} = {cur if ok else '<未找到>'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
