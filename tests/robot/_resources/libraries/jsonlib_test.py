#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pytest
from jsonlib import compare_jsons
from jsonlib import payloads_match_exactly
from jsonlib import payload_is_superset_of_expected
from jsonlib import JsonCompareError


class Test_JSON_Compare_Lib:
    """robotframework-json-diff library tests."""

    # TEST FOR compare_jsons()
    def test_identical_objects(self):
        t1 = '{"one":"str","two": null,"three":1, "list":[],"dict":{}}'
        t2 = t1

        assert compare_jsons(t1, t2) == {}

    def test_value_changed(self):
        t1 = '{"one":"str","two": null,"three":1, "list":[],"dict":{}}'
        t2 = '{"one":"s","two": null,"three":1, "list":[],"dict":{}}'

        diff = compare_jsons(t1, t2)

        assert diff == {
            "values_changed": {"root['one']": {"new_value": "s", "old_value": "str"}}
        }

    def test_actual_is_invalid_json(self):
        t1 = "/foo/bar"
        t2 = '{"1": "one", "2": 2, "3": null}'

        with pytest.raises(JsonCompareError) as error:
            compare_jsons(t1, t2)

        assert "Only VALID JSON strings accepted!" in str(error.value)

    def test_expected_is_invalid_json(self):
        t1 = '{"1": "one", "2": 2, "3": null}'
        t2 = "/foo/bar"

        with pytest.raises(JsonCompareError) as error:
            compare_jsons(t1, t2)

        assert "Only VALID JSON strings accepted!" in str(error.value)

    def test_arguments_are_invalid_jsons(self):
        t1 = "/foo/bar"
        t2 = "/foo/bar"

        with pytest.raises(JsonCompareError) as error:
            compare_jsons(t1, t2)

        assert "Only VALID JSON strings accepted!" in str(error.value)

    @pytest.mark.parametrize(
        "t1, t2, e_result",
        [
            (
                None,
                "/foo",
                "ERROR: the JSON object must be str, bytes or bytearray, not NoneType",
            ),
            ("/foo", None, "Only VALID JSON strings accepted!"),
        ],
    )
    def test_one_argument_is_a_none_type(self, t1, t2, e_result):
        with pytest.raises(JsonCompareError) as error:
            compare_jsons(t1, t2)

        assert e_result in str(error.value)

    def test_arguments_are_a_none_type(self):
        t1 = None
        t2 = None

        with pytest.raises(JsonCompareError) as error:
            compare_jsons(t1, t2)

        assert (
            "ERROR: the JSON object must be str, bytes or bytearray, not NoneType"
            in str(error.value)
        )

    def test_type_change_verbose_level_0(self):
        t1 = '{"1": "one", "2": 2}'
        t2 = '{"1": 1, "2": 2}'

        diff = compare_jsons(t1, t2, verbose_level=0)

        assert diff == {
            "type_changes": {"root['1']": {"old_type": str, "new_type": int}}
        }

    def test_ignore_string_case_change(self):
        t1 = '{"1": "one"}'
        t2 = '{"1": "ONE"}'

        diff = compare_jsons(t1, t2, ignore_string_case=True)

        assert diff == {}

    def test_ignore_string_case_change_flase(self):
        t1 = '{"1": "one"}'
        t2 = '{"1": "ONE"}'

        diff = compare_jsons(t1, t2, ignore_string_case=False)

        assert diff == {
            "values_changed": {"root['1']": {"new_value": "ONE", "old_value": "one"}}
        }

    # TESTS FOR payloads_match_exactly()
    def test_match_identical_objects(self):
        t1 = '{"1": "one"}'
        t2 = '{"1": "one"}'

        assert payloads_match_exactly(t1, t2) == True

    def test_match_identical_objects_fail_01(self):
        t1 = '{"1": "one"}'
        t2 = '{"1": "ONE"}'

        with pytest.raises(JsonCompareError) as error:
            payloads_match_exactly(t1, t2)

        assert "Payloads do NOT match!" in str(error.value)

    def test_match_identical_objects_fail_02(self):
        t1 = '{"1": "one"}'
        t2 = '{"1": 1}'

        with pytest.raises(JsonCompareError) as error:
            payloads_match_exactly(t1, t2)

        assert "Payloads do NOT match!" in str(error.value)

    def test_match_identical_objects_fail_02(self):
        t1 = '{"1": "one"}'
        t2 = '{"1": "one", "2": "two"}'

        with pytest.raises(JsonCompareError) as error:
            payloads_match_exactly(t1, t2)

        assert "Payloads do NOT match!" in str(error.value)

    @pytest.mark.parametrize(
        "t1, t2, exp_result",
        [
            ("/foo", "/bar", "Only VALID JSON strings accepted!"),
            ("/foo", '{"1": "one"}', "Only VALID JSON strings accepted!"),
            ('{"1": "one"}', "/foo", "Only VALID JSON strings accepted!"),
            (
                None,
                None,
                "ERROR: the JSON object must be str, bytes or bytearray, not NoneType",
            ),
            (
                None,
                '{"1": "one"}',
                "ERROR: the JSON object must be str, bytes or bytearray, not NoneType",
            ),
            (
                '{"1": "one"}',
                None,
                "Only VALID JSON strings accepted! ERROR: the JSON object must be str, bytes or bytearray, not NoneType",
            ),
        ],
    )
    def test_match_invalid_arguments(self, t1, t2, exp_result):
        with pytest.raises(JsonCompareError) as error:
            payloads_match_exactly(t1, t2)

        assert exp_result in str(error.value)

    def test_match_too_few_args(self):
        with pytest.raises(TypeError) as error:
            payloads_match_exactly('{"1": "one"}')

        assert (
            "payloads_match_exactly() missing 1 required positional argument: 'json_2'"
            in str(error.value)
        )

    def test_match_too_many_args(self):
        with pytest.raises(TypeError) as error:
            payloads_match_exactly(
                '{"1": ["one", "two"]}', '{"1": ["two", "one"]}', False, "/foo"
            )

        assert (
            "payloads_match_exactly() takes from 2 to 3 positional arguments but 4 were given"
            in str(error.value)
        )

    def test_match_ignore_order_not_boolean(self):
        with pytest.raises(ValueError) as error:
            payloads_match_exactly(
                '{"1": ["one", "two"]}', '{"1": ["two", "one"]}', "/foo"
            )

        assert (
            "Argument `ignore_order` must eval to boolean. Valid values: ${TRUE} or ${FALSE}"
            in str(error.value)
        )

    def test_match_unknown_kwarg(self):
        with pytest.raises(ValueError) as error:
            payloads_match_exactly(
                '{"1": ["one", "two"]}', '{"1": ["two", "one"]}', foo="bar"
            )

        assert "parameter(s) are not valid" in str(error.value)

    # TESTS FOR payload_is_superset_of_t2()
    @pytest.mark.parametrize(
        "t1, t2, exp_result",
        [
            ("/foo", "/bar", "Only VALID JSON strings accepted!"),
            ("/foo", '{"1": "one"}', "Only VALID JSON strings accepted!"),
            ('{"1": "one"}', "/foo", "Only VALID JSON strings accepted!"),
            (None, None, "ERROR: the JSON object must be str"),
            (None, '{"1": "one"}', "ERROR: the JSON object must be str"),
            ('{"1": "one"}', None, "ERROR: the JSON object must be str"),
        ],
    )
    def test_superset_invalid_arguments(self, t1, t2, exp_result):
        with pytest.raises(JsonCompareError) as error:
            payload_is_superset_of_expected(t1, t2)

        assert exp_result in str(error.value)

    def test_superset_too_few_args(self):
        with pytest.raises(TypeError) as error:
            payload_is_superset_of_expected('{"1": "one"}')

        assert (
            "payload_is_superset_of_expected() missing 1 required positional argument: 'expected'"
            in str(error.value)
        )

    def test_superset_too_many_args(self):
        with pytest.raises(TypeError) as error:
            payload_is_superset_of_expected(
                '{"1": "one"}', '{"1": "one"}', '{"1": "one"}'
            )

        assert (
            "payload_is_superset_of_expected() takes 2 positional arguments but 3 were given"
            in str(error.value)
        )

    def test_superset_actual_meets_expectation(self):
        t1 = '{"a": 10, "b": 12, "c": [3, 2, 1]}'
        t2 = t1

        assert payload_is_superset_of_expected(t1, t2) == True

    def test_superset_critical_change(self):
        t1 = '{"a": 10, "b": 12, "c": [1]}'
        t2 = '{"a": 10, "b": 12, "c": [1, 2, 3]}'
        with pytest.raises(JsonCompareError) as error:
            payload_is_superset_of_expected(t1, t2)

        assert "Actual payload dosn't meet expectation!" in str(error.value)

    def test_superset_critical_change_ignore_order(self):
        t1 = '{"a": 10, "b": 12, "c": [3, 2, 1]}'
        t2 = '{"a": 10, "b": 12, "c": [1, 2, 3]}'

        assert payload_is_superset_of_expected(t1, t2) == True

    def test_superset_critical_change_ignore_order_false(self):
        t1 = '{"a": 10, "b": 12, "c": [3, 2, 1]}'
        t2 = '{"a": 10, "b": 12, "c": [1, 2, 3]}'

        with pytest.raises(JsonCompareError) as error:
            payload_is_superset_of_expected(t1, t2, ignore_order=False)

        assert "Actual payload dosn't meet expectation!" in str(error.value)

    def test_superset_none_critical_change(self):
        t1 = '{"a": 10, "b": 12, "c": [3, 2, 1]}'
        t2 = '{"a": 10, "b": 12, "c": [1]}'

        assert payload_is_superset_of_expected(t1, t2) == True
