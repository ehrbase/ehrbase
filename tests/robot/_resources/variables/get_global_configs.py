import yaml

def get_global_variables():
    with open('robot/_resources/variables/additional_configs.yml', 'r') as file:
        yaml_content = yaml.safe_load(file)
        port = yaml_content['GLOBAL_PORT']
    return port