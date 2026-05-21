#!/usr/bin/env python3
"""Remove the default server block from nginx.conf"""
with open("/etc/nginx/nginx.conf", "r") as f:
    lines = f.readlines()

result = []
skip = False
brace_count = 0
for line in lines:
    stripped = line.strip()
    if not skip and stripped.startswith("server {") and line.startswith("    server"):
        skip = True
        brace_count = 1
        continue
    if skip:
        brace_count += line.count("{")
        brace_count -= line.count("}")
        if brace_count <= 0:
            skip = False
            continue
    else:
        result.append(line)

with open("/etc/nginx/nginx.conf", "w") as f:
    f.writelines(result)
print("OK - removed default server block")
