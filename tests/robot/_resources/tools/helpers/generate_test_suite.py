# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Pablo Pazos (Hannover Medical School).
#
# This file is part of Project EHRbase
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.



import os
import yaml

"""
Author: Wladislaw Wagner
Created: 08.2019

This script creates the files and folders for a test suite from a given
YAML file as input.

Prerequisites:
- pip install -r requirements.txt


Usage:
    >>> cp TEST_SUITE_LAYOUT.yaml /path/to/target_folder
    >>> cp generate_test_suite.py /path/to/target_folder
    # e.g. project_root/tests/robot/CONTRIBUTION_TESTS

    >>> cd /path/to/target_folder

    >>> python generate_test_suite.py TEST_SUITE_LAYOUT.yaml
    # files and folders defined in .yaml are created in target_folder
    #     w/o overriding existing ones

NOTE:
Existing files and folders will NOT be overwritten. So it is save to use it in
a folder with existing test cases.
"""



def yaml_loader(file_path):
    """Loads a YAML file.
    """
    with open(file_path, 'r') as yaml_file:
        data = yaml.safe_load(yaml_file)
    return data


def main(file_path):
    # replace with needed .yaml file
    file_path = file_path

    # creates a python dict from YAML file
    data = yaml_loader(file_path)

    for folder in data['folders']:
        # create folders
        print('Creating TC folder:', folder)
        os.makedirs(folder, exist_ok=True)

        # create files inside folders
        list_of_files = data['folders'][folder]['files']
        for test_case in list_of_files:
            try:
                file_path = folder + '/' + test_case
                # `x` mode raises FileExistsError exception
                with open(file_path, 'x') as f:
                    f.write(data['placeholder'])
                print('Creating TC file:', test_case)
            except FileExistsError:
                print('TC already exists:', test_case)



if __name__ == '__main__':
    import sys
    main(sys.argv[1])
