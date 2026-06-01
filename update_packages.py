import os
import re

def update_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    new_content = content
    # Replace switchdektoptocompose with new package
    new_content = new_content.replace('package switchdektoptocompose', 'package com.kapcode.open.macropad.kmps.desktop')
    new_content = new_content.replace('import switchdektoptocompose', 'import com.kapcode.open.macropad.kmps.desktop')
    new_content = new_content.replace('switchdektoptocompose.', 'com.kapcode.open.macropad.kmps.desktop.')

    # Specific fix for the logic package in tests
    if 'jvmTest' in file_path:
        new_content = new_content.replace('package logic', 'package com.kapcode.open.macropad.kmps.desktop.logic')

    if new_content != content:
        with open(file_path, 'w') as f:
            f.write(new_content)
        return True
    return False

def main():
    # Search in all kotlin files in composeApp
    base_dirs = [
        'composeApp/src'
    ]

    updated_count = 0
    for base_dir in base_dirs:
        for root, dirs, files in os.walk(base_dir):
            for file in files:
                if file.endswith('.kt'):
                    if update_file(os.path.join(root, file)):
                        updated_count += 1

    print(f"Updated {updated_count} files.")

if __name__ == "__main__":
    main()
