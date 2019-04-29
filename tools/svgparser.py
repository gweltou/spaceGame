#! /usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import xml.etree.ElementTree as ET
import re

color_pattern = r".(\w+)\{fill:#([0-9A-Fa-f]{6});\}"
namespace = "{http://www.w3.org/2000/svg}"


def hex_to_int(hex_str):
    ints = []
    ints.append(str(int(hex_str[:2], 16)))
    ints.append(str(int(hex_str[2:4], 16)))
    ints.append(str(int(hex_str[4:6], 16)))
    
    return ints


def unpack(l):
    out = []
    for e in l:
        if e.tag.endswith('g'):
            out.extend(unpack(e))
        elif e.tag.endswith('polygon'):
            out.append(e)
    return out

if __name__ == "__main__":
    if len(sys.argv) == 1:
        print(f"usage: {sys.argv[0]} filename.svg")
        print(f"usage: {sys.argv[0]} filename.svg x_offset y_offset")
        sys.exit()
    output_lines = []
    tree = ET.parse(sys.argv[1])
    root = tree.getroot()
    col_text = root.findall(namespace+"style")[0].text
    colors = dict(re.findall(color_pattern, col_text))
    print(colors)
    
    balises = root.findall(namespace+"g")
    balises = unpack(balises)
    balises += root.findall(namespace+"polygon")
    
    for e in balises:
        print(e.attrib)
        cols = hex_to_int(colors[e.attrib['class']])
        points = e.attrib['points']
        points = points.replace(',', ' ').strip().split()
        points = [float(e) for e in points]
        print(points)
        if len(sys.argv) >= 4:
            for i in range(len(points)):
                if i%2==0:
                    points[i] -= float(sys.argv[2])
                else:
                    points[i] -= float(sys.argv[3])
        print(points)
        points = [str(p) for p in points]
        line = '\t'.join(points)
        line += '\t' + '\t'.join(cols).strip()
        line += '\n'
        output_lines.append(line)
    
    with open("output.tdat", "w") as f:
        f.writelines(output_lines)
