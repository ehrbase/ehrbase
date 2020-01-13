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

# from deepdiff import DeepSearch, grep


def compare_jsons(
    json_1,
    json_2,
    exclude_paths=None,
    ignore_order=True,
    report_repetition=False,
    ignore_string_case=False,
    ignore_type_subclasses=False,
    verbose_level=2,
    **kwargs
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

    logger.debug("json_1 type: {}".format(type(json_1)))
    logger.debug("json_2 type: {}".format(type(json_2)))

    try:
        actual = json.loads(json_1)
        expected = json.loads(json_2)
    except (JSONDecodeError, TypeError) as error:
        raise JsonCompareError(
            "Only VALID JSON strings accepted! ERROR: {}".format(error)
        )
    
    logger.debug("actual type: {}".format(type(actual)))
    logger.debug("expected type: {}".format(type(expected)))
    logger.debug(
        "EXCLUDED PATHS: {}, type: {}".format(exclude_paths, type(exclude_paths))
    )
    logger.debug("IGNORE ORDER: {}, type: {}".format(ignore_order, type(ignore_order)))
    logger.debug(
        "IGNORE_STRING_CASE: {}, type: {}".format(
            ignore_string_case, type(ignore_string_case)
        )
    )
    logger.debug("IGNORE_TYPE_SUBCLASSES: {}".format(ignore_type_subclasses))
    logger.debug("VERBOSE_LEVEL: {}".format(verbose_level))
    logger.debug("KWARGS: {}".format(kwargs))

    diff = DeepDiff(
        actual,
        expected,
        exclude_paths=exclude_paths,
        ignore_order=ignore_order,
        report_repetition=report_repetition,
        ignore_string_case=ignore_string_case,
        ignore_type_subclasses=ignore_type_subclasses,
        verbose_level=verbose_level,
        **kwargs
    )

    logger.debug("DIFF: {}".format(diff))

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
            logger.debug(
                "{}. CHANGE ({}): {}".format(change_counter, change, diff[change])
            )

    return diff.to_dict()


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

    logger.debug("type(json_1): {}".format(type(json_1)))
    logger.debug("type(json_2): {}".format(type(json_2)))
    logger.debug("type(diff): {}".format(type(diff)))

    if diff != {}:
        logger.error("Payloads don't match!")
        raise JsonCompareError("Payloads do NOT match! Differences: {}".format(diff))
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

    logger.debug("type(payload): {}".format(type(payload)))
    logger.debug("type(expected): {}".format(type(expected)))

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
