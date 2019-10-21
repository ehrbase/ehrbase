#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pytest
import jsonlib


class Test_JSON_Compare_Lib:
    """JSON-Compare-Library Tests."""

    def test_same_object(self):
        t1 = '{"one":"str","two": null,"three":1, "list":[],"dict":{}}'
        t2 = t1

        assert {} == jsonlib.compare_jsons(t1, t2)

    def test_value_changed(self):
        t1 = '{"one":"str","two": null,"three":1, "list":[],"dict":{}}'
        t2 = '{"one":"s","two": null,"three":1, "list":[],"dict":{}}'

        diff = jsonlib.compare_jsons(t1, t2)

        assert diff == {
            "values_changed": {"root['one']": {"new_value": "s", "old_value": "str"}}
        }
