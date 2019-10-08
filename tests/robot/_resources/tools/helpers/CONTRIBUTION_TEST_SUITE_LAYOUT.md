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



CONTRIBUTION TEST CASES

DIR C.1_COMMIT_CONTRIBUTION
    C.1.a_commit_CONTRIBUTION__Single_valid_version.robot
    C.1.b_commit_CONTRIBUTION__with_errors_in_vc.robot
    C.1.c_commit_CONTRIBUTION__invalid_no_vc.robot
    C.1.d_commit_CONTRIBUTION__with_multiple_vc.robot
    C.1.e_commit_CONTRIBUTIONS__versioning_event_composition.robot
    C.1.f_commit_CONTRIBUTIONS__versioning_persistent_composition.robot
    C.1.g_commit_CONTRIBUTIONS__delete_composition.robot
    C.1.h_commit_CONTRIBUTIONS__versioning_with_errors.robot
    C.1.i_commit_CONTRIBUTIONS__versioning_persistent_composition_change_type_creation.robot
    C.1.j_commit_CONTRIBUTION__COMPOSITION_with_non_existent_OPT.robot

DIR C.2_GET_CONTRIBUTIONS
    C.2.a_get_CONTRIBUTIONS__EHR_with_contribution.robot
    C.2.b_get_CONTRIBUTIONS__EHR_with_no_contributions.robot
    C.2.c_get_CONTRIBUTIONS__EHR_does_not_exist.robot

DIR C.3_HAS_CONTRIBUTION
    C.3.a_has_CONTRIBUTION__EHR_with_contribution.robot
    C.3.b_has_CONTRIBUTION__EHR_is_empty.robot
    C.3.c_has_CONTRIBUTION__EHR_does_not_exist.robot
    C.3.d_has_CONTRIBUTION__EHR_with_non-existent_contribution.robot

DIR C.4_GET_CONTRIBUTION
    C.4.a_get_CONTRIBUTION__EHR_with_contribution.robot
    C.4.b_get_CONTRIBUTION__EHR_is_empty.robot
    C.4.c_get_CONTRIBUTION__EHR_does_not_exist.robot
    C.4.d_get_CONTRIBUTION__EHR_with_non-existent_contribution.robot
