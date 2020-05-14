# -*- coding: utf-8 -*-
#
# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH).
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


import json

from robot.api import logger
from json import JSONDecodeError
from deepdiff import DeepDiff
from deepdiff import DeepSearch


def compare_jsons(
    json_1,
    json_2,
    exclude_paths=None,
    ignore_order=True,
    report_repetition=False,
    ignore_string_case=False,
    ignore_type_subclasses=False,
    verbose_level=2,
    **kwargs,
):
    """
    :json_1: valid JSON string \n
    :json_2: valid JSON string \n

    # DOCTEST EXAMPLES

        ## TEST_01
        >>> a = '{"1": "one", "2": 2, "3": null}'
        >>> b = '{"1": "one", "2": 2, "3": null}'
        >>> compare_jsons(a, b)
        {}

        ## TEST_02
        >>> a = '{"1": "one", "2": 2}'
        >>> b = '{"1": "one", "2": 22}'
        >>> compare_jsons(a, b, exclude_paths="root['2']")
        {}

        ## TEST_03
        >>> a = '{"1": "one"}'
        >>> b = '{"1": "ONE"}'
        >>> compare_jsons(a, b, ignore_string_case=True)
        {}
    """

    logger.debug("BEFORE TRY BLOCK")
    logger.debug("json_1 type: {}".format(type(json_1)))
    logger.debug("json_2 type: {}".format(type(json_2)))

    # if inputs are dictionaries take them as they are otherwise
    # try to convert to a python object (dict)
    if isinstance(json_1, dict):
        actual = json_1
    else:
        try:
            actual = json.loads(json_1)
        except (JSONDecodeError, TypeError) as error:
            raise JsonCompareError(f"Only VALID JSON strings accepted! ERROR: {error}")
    if isinstance(json_2, dict):
        expected = json_2
    else:
        try:
            expected = json.loads(json_2)
        except (JSONDecodeError, TypeError) as error:
            raise JsonCompareError(f"Only VALID JSON strings accepted! ERROR: {error}")

    logger.debug("AFTER TRY BLOCK")
    logger.debug(f"ACTUAL: {type(actual)}")
    logger.debug(f"EXPECTED: {type(expected)}")

    logger.debug(f"EXCLUDED PATHS: {exclude_paths} - (type: {type(exclude_paths)})")
    logger.debug(f"IGNORE ORDER: {ignore_order} - (type: {type(ignore_order)})")
    logger.debug(
        f"IGNORE_STRING_CASE: {ignore_string_case} - (type: {type(ignore_string_case)})"
    )
    logger.debug(f"IGNORE_TYPE_SUBCLASSES: {ignore_type_subclasses}")
    logger.debug(f"VERBOSE_LEVEL: {verbose_level}")
    logger.debug(f"KWARGS: {kwargs}")

    diff = DeepDiff(
        actual,
        expected,
        exclude_paths=exclude_paths,
        ignore_order=ignore_order,
        report_repetition=report_repetition,
        ignore_string_case=ignore_string_case,
        ignore_type_subclasses=ignore_type_subclasses,
        verbose_level=verbose_level,
        **kwargs,
    )

    # logger.debug(f"DIFF: {diff}")

    changes = [
        "type_changes",
        "values_changed",
        "repetition_change",
        "dictionary_item_added",
        "iterable_item_added",
        "dictionary_item_removed",
        "iterable_item_removed",
    ]

    change_counter = 0
    for change in changes:
        if change in diff:
            change_counter += 1
            logger.debug(f"{change_counter}. CHANGE ({change}): \n{diff[change]}\n\n")

    return diff.to_dict()


def ignore_type_properties(obj, path):
    """
    This is a callback function. It is used inside of `compare_jsons_ignoring_properties()`.
    It takes the object and its path and returns a Boolean. If True is returned,
    the object is excluded from the results, otherwise it is included.
    `obj` refers to the value part of a key:value pair.
    `path` refers to the location of obj in the dict.
    """
    ignorable_types = [
        "ARCHETYPE_ID",
        "ARCHETYPED",
        "CODE_PHRASE",
        "DV_BOOLEAN",
        "DV_CODED_TEXT",
        "DV_COUNT",
        "DV_DATE",
        "DV_DATE_TIME",
        "DV_DATE_TIME",
        "DV_DURATION",
        "DV_EHR_URI",
        "DV_IDENTIFIER",
        "DV_MULTIMEDIA",
        "DV_ORDINAL",
        "DV_PARSABLE",
        "DV_PROPORTION",
        "DV_QUANTITY",
        "DV_SCALE",
        "DV_STATE",
        "DV_TEXT",
        "DV_TIME",
        "DV_URI",
        "REFERENCE_RANGE",
        "TEMPLATE_ID",
        "TERM_MAPPING",
        "TERMINOLOGY_ID",
    ]
    # BACKUP# return True if "_type" in path and obj in ignorable_types else False
    # Data Value (DV) is inside ELEMENT.value - those are NOT ignored
    if "_type" in path and "value" in path and obj in ignorable_types:
        return False
    # DV is NOT inside ELEMENT.value - those ARE ignored
    if "_type" in path and ("value" not in path) and obj in ignorable_types:
        logger.debug(f"path: {path}, object: {obj}")
        return True
    else:
        False


def compare_jsons_ignoring_properties(
    json_1, json_2, *properties, meta=True, path=True, **kwargs
):
    """
    Compares JSON and ignores meta and path properties by default.
    More properties can be added by their name and will be added
    to ignore_regex_path as r"\['name'\] where name is the property
    that is passed as argument.
    """
    ignore_properties = []

    for prop in properties:
        property = f"\['{prop}'\]"
        ignore_properties.append(r"{}".format(property))
    if meta:
        ignore_properties.append(r"root\['meta'\]")
    if path:
        ignore_properties.append(r"\['columns'\]\[\d+\]\['path'\]")

    return compare_jsons(
        json_1,
        json_2,
        exclude_regex_paths=ignore_properties,
        exclude_obj_callback=ignore_type_properties,
        **kwargs,
    )


def payloads_match_exactly(json_1, json_2, ignore_order=False, **kwargs):
    """
    :json_1: valid JSON string
    :json_2: valid JSON string

    # DOCTEST EXAMPLES

        ## TEST_01
        >>> a = '{"1": "one", "2": [1,2,3]}'
        >>> b = '{"1": "one", "2": [3,2,1]}'
        >>> payloads_match_exactly(a, b, ignore_order=True)
        True

        ## TEST_02
        >>> a = '{"1": "one", "2": [1,2,3]}'
        >>> b = '{"1": "one", "2": [3,2,1]}'
        >>> payloads_match_exactly(a, b)
        Traceback (most recent call last):
        jsonlib.JsonCompareError: Payloads do NOT match! Differences: {'values_changed': {"root['2'][0]": {'new_value': 3, 'old_value': 1}, "root['2'][2]": {'new_value': 1, 'old_value': 3}}}
    """
    if not type(ignore_order) == bool:
        raise ValueError(
            "Argument `ignore_order` must eval to boolean. Valid values: ${TRUE} or ${FALSE}"
        )

    diff = compare_jsons(json_1, json_2, ignore_order=ignore_order, **kwargs)

    logger.debug(f"type(json_1): {type(json_1)}")
    logger.debug(f"type(json_2): {type(json_2)}")
    logger.debug(f"type(diff): {type(diff)}")

    if diff != {}:
        logger.error("Payloads don't match!")
        raise JsonCompareError(f"Payloads do NOT match! Differences: {diff}")
    else:
        return True


def payload_is_superset_of_expected(payload, expected, **kwargs):
    """
    Checks that given payload is contained in the expected result.
    In other words: payload has at least everything that is expected
    AND may have even more content beyond the expected.

    # DOCTEST EXAMPLES

        ## TEST_01
        >>> a = '{"1": "one", "2": [1,2,3], "3": 3}'
        >>> b = '{"1": "one", "2": [1,2,3]}'
        >>> payload_is_superset_of_expected(a, b)
        True

        ## TEST_02
        >>> a = '{"1": "one", "2": [1]}'
        >>> b = '{"1": "one", "2": [1,2,3]}'
        >>> payload_is_superset_of_expected(a, b)
        Traceback (most recent call last):
        jsonlib.JsonCompareError: Actual payload dosn't meet expectation!

    TODO: create a dictionary with proper names for relevant changes
    # changes that are relevant / test shoul FAIL
    # change                     meaning
    #   type_changes               expected vs. got
    #   values_changed             expected vs. got
    #      new_value                  expected value
    #      old_value                  recieved value
    #   repetition_change          expected vs. got
    #   dictionary_item_added      missing in response but was expected
    #   iterable_item_added        missing in response but was expected

    # changes that can be ignored / test should PASS
    # changed                    meaning
    #   dictionary_item_removed    is in response bu was NOT expected
    #   set_item_removed           is in response bu was NOT expected
    #   iterable_item_removed      is in response bu was NOT expected
    #   unprocessed                ? if occurs should be handled extra
    """

    logger.debug(f"type(payload): {type(payload)}")
    logger.debug(f"type(expected): {type(expected)}")

    diff = compare_jsons(payload, expected, **kwargs)

    changes = [
        "type_changes",
        "values_changed",
        "repetition_change",
        "dictionary_item_added",
        "iterable_item_added",
        "set_item_removed",
        "dictionary_item_removed",
        "iterable_item_removed",
    ]

    changes_to_ignore = [
        "set_item_removed",
        "dictionary_item_removed",
        "iterable_item_removed",
    ]

    critical_changes = [
        "type_changes",
        "values_changed",
        "repetition_change",
        "dictionary_item_added",
        "iterable_item_added",
    ]

    if diff != {}:
        for change in changes:
            # check if change are relevant or can be ignored
            if change in critical_changes and change in diff:
                logger.error("Critical changes detected!")
                raise JsonCompareError("Actual payload dosn't meet expectation!")

            elif change in changes_to_ignore and change in diff:
                logger.info("Changes detected, but not relevant.")
                return True
    else:
        logger.info("NO difference between payloads.")
        return True


def search_in(obj, item, **kwargs):
    if isinstance(obj, dict):
        obj = obj
    else:
        try:
            obj = json.loads(obj)
        except (JSONDecodeError, TypeError) as error:
            raise JsonCompareError(f"Only VALID JSON strings accepted! ERROR: {error}")
    logger.debug(f"OBJ: {obj}")
    logger.debug(f"ITEM: {item}")
    logger.debug(f"KWARGS: {kwargs}")

    ds = DeepSearch(obj, item, verbose_level=2, **kwargs)
    logger.debug(f"DS: {type(ds)}")
    print(ds)
    return ds


class JsonCompareError(Exception):
    pass


# oooooooooo.        .o.         .oooooo.   oooo    oooo ooooo     ooo ooooooooo.
# `888'   `Y8b      .888.       d8P'  `Y8b  `888   .8P'  `888'     `8' `888   `Y88.
#  888     888     .8"888.     888           888  d8'     888       8   888   .d88'
#  888oooo888'    .8' `888.    888           88888[       888       8   888ooo88P'
#  888    `88b   .88ooo8888.   888           888`88b.     888       8   888
#  888    .88P  .8'     `888.  `88b    ooo   888  `88b.   `88.    .8'   888
# o888bood8P'  o88o     o8888o  `Y8bood8P'  o888o  o888o    `YbodP'    o888o
#
# [ BACKUP ]


# convert Robot's own dict type to normal Python dict
#       otherwise DeepDiff will report a type-change "robot.utils.dotdict.DotDict vs. dict"
# response_json = dict(response_json)

# ignore_type_in_groups=[(robot.utils.dotdict.DotDict, dict)],
# logger.debug("IGNORED TYPE GROUPS: {}".format(ignore_type_in_groups))


# try:
#     do somthing()
# except ValueError as e:
#         raise JsonValidatorError('Error in schema: {}'.format(e))


# if diff['values_changed']:
# # logger.error("JSON ERROR!")
# # logger.warn("JSON WARNING!")


# DeepDiff(expected, actual, ignore_order=True, verbose_level=2)
# DeepDiff(json.dumps(expected,sort_keys=True), json.dumps(actual, sort_keys=True))


# dict_ = dict(b=1, a=2, z=3, f=4, e=dict(F=1, C=2))
# dump = json.dumps(dict_, sort_keys=True, indent=2)


# def compare_ignoring_path_and_type_properties(json_1, json_2, **kwargs):
#     ignore_properties = [
#         r"root\['meta'\]",
#         r"\['columns'\]\[\d+\]\['path'\]",
#         r"\['_type'\]",
#     ]
#     return compare_jsons(
#         json_1, json_2, exclude_regex_paths=ignore_properties, **kwargs
#     )
