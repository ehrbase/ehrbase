import json
from benedict import benedict

'''
This library is created for testing composition validation:
robot\COMPOSITION_TESTS\VALIDATION
'''


def make_expected_count_items_by_name(json_str: str, array_path: str, item_name: str, expected_count: str) -> str:
    '''
    This method makes follow steps:
    1) finds items into the array path by name.value and counts them
    2.a) if the count is less expected so it appends delta
    2.b) if the count is more expected so it removes delta

    :param json_str: json as string
    :param array_path: path to array with searchable item
    :param item_name: item name by that we will find this item
    :param expected_count: expected count of an item in the content
    :return: json as string
    '''

    data = json.loads(json_str)
    data = benedict(data)
    expected_count = int(expected_count)

    try:
        searchable_items = list(filter(lambda x: benedict(x)['name.value'] ==
                                       item_name, data[array_path]))
    except KeyError:
        raise Exception(
            f'path_to_item_array = \'{array_path}\' is maybe wrong!')

    fact_count = len(searchable_items)

    if fact_count == 0:
        raise Exception(f'Item with name "{item_name}" not found!')

    elif fact_count > expected_count:
        counter = fact_count - expected_count
        while counter > 0:
            for s in range(len(data[array_path])):
                if data[f'{array_path}[{s}].name.value'] == item_name:
                    del data[f'{array_path}[{s}]']
                    counter = counter - 1
                    break

    elif fact_count < expected_count:
        counter = expected_count - fact_count
        item = searchable_items[0]
        while counter > 0:
            data[array_path].append(item)
            counter = counter - 1

    return json.dumps(data, ensure_ascii=False, indent=2)


def modify_of_composition_high_level_items(json_str, language='exist', territory='exist', category='exist', composer='exist'):
    """This method modifies json for validation tests of composition class.
    Available_values for parameters:
        - exist
        - not_exist
        - invalid
    """

    data = json.loads(json_str)

    if language == 'not_exist':
        del data['language']
    elif language == 'invalid':
        data['language']['_type'] = 777
    elif language == 'exist':
        pass
    else:
        raise Exception('Invalid param value')

    if territory == 'not_exist':
        del data['territory']
    elif territory == 'invalid':
        data['territory']['_type'] = 777
    elif territory == 'exist':
        pass
    else:
        raise Exception('Invalid param value')

    if category == 'not_exist':
        del data['category']
    elif category == 'invalid':
        data['category']['_type'] = 777
    elif category == 'exist':
        pass
    else:
        raise Exception('Invalid param value')

    if composer == 'not_exist':
        del data['composer']
    elif composer == 'invalid':
        data['composer']['_type'] = 777
    elif composer == 'exist':
        pass
    else:
        raise Exception('Invalid param value')

    return json.dumps(data, ensure_ascii=False, indent=2)
