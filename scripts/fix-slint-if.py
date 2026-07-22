#!/usr/bin/env python3
"""Fix Slint `if condition { Component {` to `if condition : Component {`.

Walks each .slint file, finds `if ... {` patterns (without a colon before the brace),
and if the next non-trivial token on the next line is a component name (PascalCase identifier),
rewrites as `if ... : Component {`.
"""
import re
import sys
from pathlib import Path

# Match: `if <condition> {` where condition has no `:` right before `{`
# Group 1 = indentation, Group 2 = condition
IF_RE = re.compile(r'^(\s+)if\s+(.+?)\s*\{\s*$', re.MULTILINE)
# PascalCase identifier (component name)
COMP_RE = re.compile(r'^\s+([A-Z][a-zA-Z0-9_]*)\s*\{')

def fix_file(path: Path) -> int:
    text = path.read_text()
    lines = text.split('\n')
    changes = 0
    out = []
    i = 0
    while i < len(lines):
        line = lines[i]
        m = IF_RE.match(line)
        if m and ':' not in line[line.find('if'):]:
            # Check if next line is `ComponentName {`
            if i + 1 < len(lines):
                cm = COMP_RE.match(lines[i + 1])
                if cm:
                    indent = m.group(1)
                    cond = m.group(2)
                    comp = cm.group(1)
                    # Rewrite this line and skip the next line's component declaration
                    out.append(f"{indent}if {cond} : {comp} {{")
                    # Remove the standalone component line (now redundant)
                    i += 1
                    changes += 1
                    i += 1
                    continue
        out.append(line)
        i += 1
    if changes:
        path.write_text('\n'.join(out))
    return changes

if __name__ == '__main__':
    base = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('.')
    total = 0
    for f in sorted(base.rglob('*.slint')):
        n = fix_file(f)
        if n:
            print(f"  {f}: {n} fix(es)")
            total += n
    print(f"Total: {total} fixes")
