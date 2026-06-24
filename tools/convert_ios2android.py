#!/usr/bin/env python3
"""
Convert iOS Localizable.strings to Android strings.xml
Rewritten from Ruby version (tools/convert_ios2android.rb)
to produce valid Android string resources.

Fixes applied vs. original Ruby script:
  - Multiple %s substitutions get formatted="false"
  - Proper XML escaping of < > & "
  - Proper unescaping of iOS escape sequences (\\n, \\", \\')
  - Numbered placeholders %N$@ -> %N$s
  - Plain %@ -> %s (single substitution)
  - Single %s is left unnumbered (Android accepts it)
"""

import os
import re
import sys
from pathlib import Path


# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

CFG = {
    "files_mapping": {
        "en.lproj/Localizable.strings": "values/strings.xml",
        "zh-Hans.lproj/Localizable.strings": "values-zh/strings.xml",
        "ja.lproj/Localizable.strings": "values-ja/strings.xml",
    },
    "ios_base_dir": Path(__file__).resolve().parent.parent / "ios" / "oplayer" / "Resources",
    "android_base_dir": Path(__file__).resolve().parent.parent / "android" / "app" / "src" / "main" / "res",
}

# Regex for iOS Localizable.strings key=value pairs
# Handles:  "key" = "value";
# The value is captured between the last pair of quotes before the semicolon.
STRING_LINE_RE = re.compile(r'^\s*"([^"]+)"\s*=\s*"(.+?)";\s*$')

# Alternative pattern: match everything up to the last ";  at end of line
# This correctly handles escaped quotes like \" inside the value
STRING_LINE_RE2 = re.compile(r'^\s*"([^"]+)"\s*=\s*"(.+\n?)";\s*$')

# Regex to detect multiple substitutions (%@, %d, %f, or numbered %N$@ / %N$d etc.)
# We look for format specifiers like %@, %d, %f, %1$@, etc.
FORMAT_SPEC_RE = re.compile(r'%(\d+\$)?[@adfFcsxXeEgGn]')


# ---------------------------------------------------------------------------
# iOS escape sequence handling
# ---------------------------------------------------------------------------

def unescape_ios_string(value: str) -> str:
    """
    Convert iOS-style escape sequences in Localizable.strings values
    to their actual characters.

    iOS strings use:
      \\n  -> newline
      \\t  -> tab
      \\\\" -> literal quote
      \\\\\\ -> literal backslash
      \\'  -> literal single quote
      \\r  -> carriage return
      \\uXXXX -> unicode (Python handles \\uXXXX natively in most cases)
    """
    # We need to be careful with the order.
    # First handle backslash sequences that contain quotes or special chars
    # The value in the .strings file is already extracted from between quotes,
    # so \\" means a literal " inside the string.

    # Process escape sequences
    result = []
    i = 0
    while i < len(value):
        if value[i] == '\\' and i + 1 < len(value):
            next_char = value[i + 1]
            if next_char == 'n':
                result.append('\n')
                i += 2
            elif next_char == 't':
                result.append('\t')
                i += 2
            elif next_char == 'r':
                result.append('\r')
                i += 2
            elif next_char == '"':
                result.append('"')
                i += 2
            elif next_char == "'":
                result.append("'")
                i += 2
            elif next_char == '\\':
                result.append('\\')
                i += 2
            elif next_char == 'u' and i + 5 < len(value):
                # Unicode escape \uXXXX
                hex_str = value[i + 2:i + 6]
                try:
                    result.append(chr(int(hex_str, 16)))
                    i += 6
                except ValueError:
                    result.append(value[i])
                    i += 1
            else:
                # Unknown escape, keep as-is
                result.append(value[i])
                i += 1
        else:
            result.append(value[i])
            i += 1

    return ''.join(result)


# ---------------------------------------------------------------------------
# String resource formatting
# ---------------------------------------------------------------------------

def convert_format_string(value: str) -> tuple[str, bool]:
    """
    Convert iOS format specifiers to Android format specifiers.

    iOS uses: %@, %d, %f, %1$@, %2$d, etc.
    Android uses: %s, %d, %f, %1$s, %2$d, etc.

    Returns:
        (converted_string, needs_formatted_false)
        needs_formatted_false is True when there are multiple unnumbered
        substitutions, which Android requires formatted="false" for.
    """
    # First, convert iOS-style to Android-style
    # %N$@  ->  %N$s  (for any digit N)
    # %@    ->  %s
    # %N$d  ->  %N$d  (already compatible)
    # %N%f  ->  %N%f  (already compatible)

    # Step 1: Replace numbered %@ -> %N$s
    converted = re.sub(r'%(\d+)\$@', r'%\1$s', value)

    # Step 2: Replace unnumbered %@ -> %s
    # Must come after numbered replacement
    converted = converted.replace('%@', '%s')

    # Step 3: Now check how many substitution placeholders remain
    # Count %s, %d, %f, %x, etc. that are NOT numbered (%N$s etc.)
    # and count numbered ones separately.

    all_specs = FORMAT_SPEC_RE.findall(converted)
    numbered_specs = [s for s in all_specs if s]  # has a digit prefix like "1$"
    unnumbered_specs = [s for s in all_specs if not s]  # just empty string (e.g. %s, %d)

    # If there are multiple unnumbered substitutions, Android requires formatted="false"
    needs_formatted_false = len(unnumbered_specs) > 1

    return converted, needs_formatted_false


# ---------------------------------------------------------------------------
# XML escaping
# ---------------------------------------------------------------------------

def xml_escape(value: str) -> str:
    """
    Escape characters that are special in XML.
    We must escape: < > & " ' (in attribute values, ' is enough; in text, < > & suffice)

    Note: We escape quotes too because Android aapt can be strict.
    """
    # Order matters: & must be first
    value = value.replace('&', '&amp;')
    value = value.replace('<', '&lt;')
    value = value.replace('>', '&gt;')
    value = value.replace('"', '&quot;')
    return value


def xml_escape_text(value: str) -> str:
    """
    Escape text content of an XML element for Android strings.xml.
    Must escape: < > & \n \r \t \u0027 (apostrophe for aapt3)
    """
    value = value.replace('&', '&amp;')
    value = value.replace('<', '&lt;')
    value = value.replace('>', '&gt;')
    value = value.replace('\n', '\n')
    value = value.replace('\r', '\r')
    value = value.replace('\t', '\t')
    value = value.replace("'", '&apos;')
    return value


def escape_for_android_string(value: str) -> str:
    """
    Full escape for Android string text content.
    - \n -> \n (literal backslash-n, which Android aapt renders as newline)
    - \r -> \r
    - \t -> \t
    - \u0027 (apostrophe) -> \u0027 or &apos;
    - \u0022 (double quote) -> &quot;
    - & -> &amp;
    - < -> &lt;
    - > -> &gt;
    """
    # Order matters: \ first, then special chars
    value = value.replace('\\', '\\\\')  # \ -> \\
    value = value.replace('\n', '\\n')
    value = value.replace('\r', '\\r')
    value = value.replace('\t', '\\t')
    value = value.replace("'", "\\'")
    value = value.replace('"', '&quot;')
    value = value.replace('&', '&amp;')
    value = value.replace('<', '&lt;')
    value = value.replace('>', '&gt;')
    return value


# ---------------------------------------------------------------------------
# File loading
# ---------------------------------------------------------------------------

def load_strings_file(src_path: Path):
    """
    Parse an iOS Localizable.strings file.

    Returns a list of groups, each group being a dict with:
      - name: the comment block name (e.g., "首页行情")
      - values: list of dicts with keys and values

    Handles the comment-block structure:
      /* Group Name */
      "key1" = "value1";
      "key2" = "value2";

      /* Another Group */
      ...
    """
    content = src_path.read_text(encoding='utf-8')
    if not content:
        print(f"ERROR> empty file: {src_path.name}")
        return None

    groups = []
    current_group = None

    for line in content.splitlines():
        stripped = line.strip()

        # Check for comment block (group header)
        comment_match = re.match(r'/\*\s*(.+?)\s*\*/', stripped)
        if comment_match:
            # Save previous group
            if current_group is not None:
                groups.append(current_group)
            current_group = {
                'name': comment_match.group(1),
                'values': [],
            }
            continue

        # Check for key = value pair
        pair_match = STRING_LINE_RE.match(line)
        if pair_match:
            if current_group is None:
                current_group = {'name': 'Default', 'values': []}

            key = pair_match.group(1)
            raw_value = pair_match.group(2)

            # Unescape iOS-style escape sequences
            value = unescape_ios_string(raw_value)

            # Convert format specifiers
            formatted_value, needs_formatted_false = convert_format_string(value)

            current_group['values'].append({
                'name': key,
                'value': formatted_value,
                'needs_formatted_false': needs_formatted_false,
            })
            continue

    # Don't forget the last group
    if current_group is not None:
        groups.append(current_group)

    return groups


# ---------------------------------------------------------------------------
# File writing
# ---------------------------------------------------------------------------

def write_strings_xml(dst_path: Path, groups: list):
    """
    Write Android strings.xml file.
    """
    dst_path.parent.mkdir(parents=True, exist_ok=True)

    lines = []
    lines.append('<!-- The xml file is automatically generated, please do not modify it!!! by syalon. -->')
    lines.append('<resources>')

    for group in groups:
        lines.append(f'    <!-- {group["name"]} -->')
        for item in group['values']:
            name = xml_escape(item['name'])
            text = escape_for_android_string(item['value'])

            if item['needs_formatted_false']:
                lines.append(f'    <string name="{name}" formatted="false">{text}</string>')
            else:
                lines.append(f'    <string name="{name}">{text}</string>')

    lines.append('</resources>')
    lines.append('')  # trailing newline

    # Write with LF-only line endings (aapt3 on some platforms chokes on CRLF)
    content = '\n'.join(lines)
    dst_path.write_bytes(content.encode('utf-8'))


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def run():
    for ios_relative, android_relative in CFG['files_mapping'].items():
        src = CFG['ios_base_dir'] / ios_relative
        dst = CFG['android_base_dir'] / android_relative

        if not src.is_file():
            print(f"!!! ERROR: source file not found: {src}")
            continue

        print(f"Processing: {ios_relative} -> {android_relative}")

        try:
            groups = load_strings_file(src)
        except Exception as e:
            print(f"!!! ERROR >>> {e} in file: {ios_relative}")
            continue

        if groups is None:
            continue

        write_strings_xml(dst, groups)
        print(f"  -> written to {dst}")

    print("===== all done =====")


if __name__ == '__main__':
    run()
