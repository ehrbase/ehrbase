# -*- coding: utf-8 -*-
#
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



import json
import jsondiff
from robot.api import logger
from deepdiff import DeepDiff
# from deepdiff import DeepSearch, grep
from pprint import pprint




def response_meets_expectation(response, expectation):
    logger.debug("Type of response: {}".format(type(response)))
    union = jsondiff.patch(response, expectation)
    difference = jsondiff.diff(response, union)
    if difference:
            raise JsonCompareError("Diff found: {}".format(difference))
    else:
        return True

def compare_json_payloads(response_json, expected_json):
    logger.debug("Compare JSON payloads")
    diff = DeepDiff(response_json, expected_json, ignore_order=True, verbose_level=2)

    changes = [
        'type_changes', 'values_changed', 'repetition_change',
        'dictionary_item_added', 'iterable_item_added', 'set_item_removed',
        'dictionary_item_removed', 'iterable_item_removed']

    for change in changes:
        if change in diff:
            logger.debug("What changed ({}): {}".format(change, diff[change]))

    return diff.to_dict()

def payloads_should_match_exactly(response_json, expected_json, ignore_order=True):
    """
    NOTE: DO NOT use this keyword standalone. It only works from  the following
          custom keyword:
          `${response payload} should exactly match ${expected json-file}`
    """
    diff = DeepDiff(response_json, expected_json, ignore_order, verbose_level=2)
    logger.debug(diff)
    logger.debug("IGNORE: {}".format(ignore_order))
    if diff != {}:
        raise JsonCompareError("Payloads do not match! Differences: {}".format(diff.to_dict()))
    else:
        return True

def subset_of_expected(response_json, expected_json, exclude_paths=None,
                       ignore_order=True, ignore_type_subclasses=False):
    """
    NOTE: DO NOT use this keyword standalone. It only works from  the following
          custom keywords:
          `is subset of` and `response is subset of`.

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
    # convert Robot's own dict type to normal Python dict
    # otherwise DeepDiff will report a type-change "robot.utils.dotdict.DotDict vs. dict"
    # response_json = dict(response_json)

    diff = DeepDiff(response_json, expected_json, ignore_order,
                    exclude_paths=exclude_paths,
                    ignore_type_subclasses=ignore_type_subclasses,
                    verbose_level=2)
                    #ignore_type_in_groups=[(robot.utils.dotdict.DotDict, dict)],

    logger.debug("Type of response: {}".format(type(response_json)))
    logger.debug("DIFFERENCE: {}".format(diff))
    logger.debug("IGNORE: {}".format(ignore_order))
    logger.debug("EXCLUDED PATHS: {}".format(exclude_paths))
    logger.debug("IGNORED SUBCLASSES: {}".format(ignore_type_subclasses))
    #logger.debug("IGNORED TYPE GROUPS: {}".format(ignore_type_in_groups))

    changes = [
        'type_changes', 'values_changed', 'repetition_change',
        'dictionary_item_added', 'iterable_item_added', 'set_item_removed',
        'dictionary_item_removed', 'iterable_item_removed']

    changes_to_ignore = [
        'set_item_removed', 'dictionary_item_removed', 'iterable_item_removed']

    critical_changes = [
        'type_changes', 'values_changed', 'repetition_change',
        'dictionary_item_added', 'iterable_item_added']

    fail = False
    if diff != {}:
        for change in changes:
            # check if change are relevant or can be ignored
            if change in critical_changes and change in diff:
                logger.warn("Critical change ({}): {}".format(change, diff[change]))
                fail = True
            elif change in changes_to_ignore and change in diff:
                logger.debug("Not relevant change ({}): {}".format(change, diff[change]))
    else:
        logger.debug("NO difference between payloads.")
        return True
    if fail:
        raise JsonCompareError("Payload is not subset of expected!")

class JsonCompareError(Exception):
    pass

    # BACKUP
    # try:
    #     do somthing()
    # except ValueError as e:
    #         raise JsonValidatorError('Error in schema: {}'.format(e))

    # if diff['values_changed']:
    # # logger.error("JSON ERROR!")
    # # logger.warn("JSON WARNING!")
    #


    # DeepDiff(expected, actual, ignore_order=True, verbose_level=2)
    # DeepDiff(json.dumps(expected,sort_keys=True), json.dumps(actual, sort_keys=True))

    # dict_ = dict(b=1, a=2, z=3, f=4, e=dict(F=1, C=2))
    # dump = json.dumps(dict_, sort_keys=True, indent=2)
