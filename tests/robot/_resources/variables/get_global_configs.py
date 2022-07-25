import yaml

def get_variables():
    with open('robot/_resources/variables/additional_configs.yml', 'r') as file:
        yaml_content = yaml.safe_load(file)
        port = yaml_content['GLOBAL_PORT']
        json_obj = \
            {
                "GLOBAL_PORT":port,
                "BASEURL": "http://localhost:" + port + "/ehrbase/rest/openehr/v1",
                "ECISURL": "http://localhost:" + port + "/ehrbase/rest/ecis/v1",
                "ADMIN_BASEURL": "http://localhost:" + port + "/ehrbase/rest/admin",
                "HEARTBEAT_URL": "http://localhost:" + port + "/ehrbase/rest/status",
                "PLUGIN_URL": "http://localhost:" + port + "/ehrbase/plugin"
             }
    return json_obj
