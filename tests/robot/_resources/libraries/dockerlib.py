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


import docker
from robot.api import logger

client = docker.from_env()


def run_postgresql_container():
    """run a postgresql container in background with given envs"""
    env = ["POSTGRES_USER=postgres", "POSTGRES_PASSWORD=postgres"]
    container = client.containers.run(
        "ehrbase/ehrbase-postgres:13.3",
        name="ehrdb",
        environment=env,
        ports={"5432/tcp": 5432},
        detach=True,
    )
    # container = client.containers.get("ehrdb")
    # logs = container.logs()
    # return logs


def get_logs_from_ehrdb():
    container = client.containers.get("ehrdb")
    logger.debug("LOGS:  {}".format(container.logs()))
    # logs = container.logs()
    return container.logs()


def stop_ehrdb_container():
    container = client.containers.get("ehrdb")
    container.stop()
    # status = container.wait()
    status = container.wait(condition="not-running")
    logs = container.logs()
    return logs, status


def restart_ehrdb_container():
    container = client.containers.get("ehrdb")
    container.restart()


def remove_ehrdb_container():
    container = client.containers.get("ehrdb")
    container.stop()
    container.remove()
    try:
        container.wait(condition="removed")
    except docker.errors.NotFound:
        pass
    return
