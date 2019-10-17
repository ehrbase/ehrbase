*** Settings ***
Metadata    Version    0.1.0
Metadata    Author    *Pablo Pazos*
Metadata    Created    2019.09.09

Documentation    Data oriented tests to verify correct parsing of different AQL payloads

Resource         ${EXECDIR}${/}..${/}tests${/}robot${/}_resources${/}suite_settings.robot

Test Template    execute ad-hoc query

Force Tags       ADHOC-QUERY-OLD    obsolete

*** Test Cases ***
A.1.a_get_ehrs   A.1.a_get_ehrs.json
A.1.b_get_ehrs   A.1.b_get_ehrs.json
A.1.c_get_ehrs   A.1.c_get_ehrs.json
A.1.d_get_ehrs   A.1.d_get_ehrs.json
A.1.e_get_ehrs   A.1.e_get_ehrs.json
A.1.f_get_ehrs   A.1.f_get_ehrs.json
A.1.g_get_ehrs   A.1.g_get_ehrs.json
A.2.a_get_ehr_by_id   A.2.a_get_ehr_by_id.json
A.2.b_get_ehr_by_id   A.2.b_get_ehr_by_id.json
A.2.c_get_ehr_by_id   A.2.c_get_ehr_by_id.json
A.2.d_get_ehr_by_id   A.2.d_get_ehr_by_id.json
A.3.a_get_ehrs_by_contains_any_composition   A.3.a_get_ehrs_by_contains_any_composition.json
A.4.a_get_ehrs_by_contains_composition_with_archetype   A.4.a_get_ehrs_by_contains_composition_with_archetype.json
A.4.b_get_ehrs_by_contains_composition_with_archetype   A.4.b_get_ehrs_by_contains_composition_with_archetype.json
A.4.c_get_ehrs_by_contains_composition_with_archetype   A.4.c_get_ehrs_by_contains_composition_with_archetype.json

A.5.a_get_ehrs_by_contains_composition_contains_entry_of_type   A.5.a_get_ehrs_by_contains_composition_contains_entry_of_type.json
A.5.b_get_ehrs_by_contains_composition_contains_entry_of_type   A.5.b_get_ehrs_by_contains_composition_contains_entry_of_type.json
A.5.c_get_ehrs_by_contains_composition_contains_entry_of_type   A.5.c_get_ehrs_by_contains_composition_contains_entry_of_type.json
A.5.d_get_ehrs_by_contains_composition_contains_entry_of_type   A.5.d_get_ehrs_by_contains_composition_contains_entry_of_type.json

A.6.a_get_ehrs_by_contains_composition_contains_entry_with_archetype   A.6.a_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
A.6.b_get_ehrs_by_contains_composition_contains_entry_with_archetype   A.6.b_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
A.6.c_get_ehrs_by_contains_composition_contains_entry_with_archetype   A.6.c_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
A.6.d_get_ehrs_by_contains_composition_contains_entry_with_archetype   A.6.d_get_ehrs_by_contains_composition_contains_entry_with_archetype.json

B.1.a_get_full_compositions_from_all_ehrs   B.1.a_get_full_compositions_from_all_ehrs.json
B.2.a_get_full_compositions_from_ehr_by_id   B.2.a_get_full_compositions_from_ehr_by_id.json
B.3.a_get_full_compositions_with_archetype_from_all_ehrs   B.3.a_get_full_compositions_with_archetype_from_all_ehrs.json
B.4.a_get_full_compositions_contains_section_with_archetype_from_all_ehrs   B.4.a_get_full_compositions_contains_section_with_archetype_from_all_ehrs.json

B.5.a_get_full_compositions_by_contains_entry_of_type_from_all_ehrs   B.5.a_get_full_compositions_by_contains_entry_of_type_from_all_ehrs.json
B.5.b_get_full_compositions_by_contains_entry_of_type_from_all_ehrs   B.5.b_get_full_compositions_by_contains_entry_of_type_from_all_ehrs.json
B.5.c_get_full_compositions_by_contains_entry_of_type_from_all_ehrs   B.5.c_get_full_compositions_by_contains_entry_of_type_from_all_ehrs.json
B.5.d_get_full_compositions_by_contains_entry_of_type_from_all_ehrs   B.5.d_get_full_compositions_by_contains_entry_of_type_from_all_ehrs.json

B.6.a_get_full_compositions_by_contains_entry_with_archetype_from_all_ehrs   B.6.a_get_full_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
B.6.b_get_full_compositions_by_contains_entry_with_archetype_from_all_ehrs   B.6.b_get_full_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
B.6.c_get_full_compositions_by_contains_entry_with_archetype_from_all_ehrs   B.6.c_get_full_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
B.6.d_get_full_compositions_by_contains_entry_with_archetype_from_all_ehrs   B.6.d_get_full_compositions_by_contains_entry_with_archetype_from_all_ehrs.json

B.7.a_get_full_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs   B.7.a_get_full_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json
B.7.b_get_full_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs   B.7.b_get_full_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json
B.7.c_get_full_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs   B.7.c_get_full_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json

B.8.a_get_full_composition_by_uid   B.8.a_get_full_composition_by_uid.json
B.8.b_get_full_composition_by_uid   B.8.b_get_full_composition_by_uid.json
B.8.c_get_full_composition_by_uid   B.8.c_get_full_composition_by_uid.json
B.8.d_get_full_composition_by_uid   B.8.d_get_full_composition_by_uid.json

C.1.a_get_all_entries_from_ehr_with_uid_contains_compositions_from_all_ehrs   C.1.a_get_all_entries_from_ehr_with_uid_contains_compositions_from_all_ehrs.json
C.2.a_get_all_entries_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs   C.2.a_get_all_entries_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json

C.3.a_get_all_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs   C.3.a_get_all_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
C.3.b_get_all_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs   C.3.b_get_all_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
C.3.c_get_all_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs   C.3.c_get_all_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
C.3.d_get_all_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs   C.3.d_get_all_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json

C.4.a_get_all_entries_with_archetype_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs   C.4.a_get_all_entries_with_archetype_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
C.5.a_get_all_entries_with_archetype_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs_condition   C.5.a_get_all_entries_with_archetype_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs_condition.json

D.1.a_select_data_values_from_all_ehrs   D.1.a_select_data_values_from_all_ehrs.json
D.1.b_select_data_values_from_all_ehrs   D.1.b_select_data_values_from_all_ehrs.json
D.2.a_select_data_values_from_all_ehrs_contains_composition   D.2.a_select_data_values_from_all_ehrs_contains_composition.json
D.2.b_select_data_values_from_all_ehrs_contains_composition   D.2.b_select_data_values_from_all_ehrs_contains_composition.json
D.3.a_select_data_values_from_all_ehrs_contains_composition_with_archetype   D.3.a_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
D.3.b_select_data_values_from_all_ehrs_contains_composition_with_archetype   D.3.b_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
D.3.c_select_data_values_from_all_ehrs_contains_composition_with_archetype   D.3.c_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
D.3.d_select_data_values_from_all_ehrs_contains_composition_with_archetype   D.3.d_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
D.3.e_select_data_values_from_all_ehrs_contains_composition_with_archetype   D.3.e_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
D.3.f_select_data_values_from_all_ehrs_contains_composition_with_archetype   D.3.f_select_data_values_from_all_ehrs_contains_composition_with_archetype.json

D.3.g_select_data_values_from_all_ehrs_contains_composition_with_archetype   D.3.g_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
D.3.h_select_data_values_from_all_ehrs_contains_composition_with_archetype   D.3.h_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
D.3.i_select_data_values_from_all_ehrs_contains_composition_with_archetype   D.3.i_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
D.3.j_select_data_values_from_all_ehrs_contains_composition_with_archetype   D.3.j_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
D.3.k_select_data_values_from_all_ehrs_contains_composition_with_archetype   D.3.k_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
D.3.l_select_data_values_from_all_ehrs_contains_composition_with_archetype   D.3.l_select_data_values_from_all_ehrs_contains_composition_with_archetype.json

D.4.a_composition_data_values_from_ehr   D.4.a_composition_data_values_from_ehr.json
D.4.b_composition_data_values_from_ehr   D.4.b_composition_data_values_from_ehr.json
D.4.c_composition_data_values_from_ehr   D.4.c_composition_data_values_from_ehr.json
D.4.d_composition_data_values_from_ehr   D.4.d_composition_data_values_from_ehr.json
D.4.e_composition_data_values_from_ehr   D.4.e_composition_data_values_from_ehr.json
D.4.f_composition_data_values_from_ehr   D.4.f_composition_data_values_from_ehr.json
D.5.a_composition_data_by_archetype_from_ehr   D.5.a_composition_data_by_archetype_from_ehr.json
D.5.b_composition_data_by_archetype_from_ehr   D.5.b_composition_data_by_archetype_from_ehr.json
D.5.c_composition_data_by_archetype_from_ehr   D.5.c_composition_data_by_archetype_from_ehr.json
D.5.d_composition_data_by_archetype_from_ehr   D.5.d_composition_data_by_archetype_from_ehr.json

E.1.a_top_get_ehrs                  E.1.a_top_get_ehrs.json
E.1.b_top_get_compositions          E.1.b_top_get_compositions.json
E.1.c_top_get_entries               E.1.c_top_get_entries.json
E.2.a_orderby_get_ehrs              E.2.a_orderby_get_ehrs.json
E.2.b_orderby_get_compositions      E.2.b_orderby_get_compositions.json
E.2.c_orderby_get_entries           E.2.c_orderby_get_entries.json
E.3.a_timewindow_get_ehrs           E.3.a_timewindow_get_ehrs.json
E.3.b_timewindow_get_compositions   E.3.b_timewindow_get_compositions.json
E.3.c_timewindow_get_entries        E.3.c_timewindow_get_entries.json

*** Keywords ***
execute ad-hoc query
    [Arguments]     ${aql_payload}

                    Set Tags         not-ready
                    Set Log Level    TRACE

                    Set Test Variable   ${aql_file_path}   ${PROJECT_ROOT}${/}tests${/}robot${/}_resources${/}test_data_sets${/}query${/}${aql_payload}
    ${aql_file}=    Get File   ${aql_file_path}

    &{headers}=     Create Dictionary   Content-Type=application/json
                    ...                 Accept=application/json
                    ...                 Prefer=return=representation

                    Create Session      ${SUT}    ${${SUT}.URL}
                    ...                 auth=${${SUT}.CREDENTIALS}    debug=2    verify=True


    ${resp}=        Post Request        ${SUT}   /query/aql   data=${aql_file}   headers=${headers}
                    #Log To Console      ${resp.content}
                    Log                 ${resp.content}
                    Should Be Equal As Strings   ${resp.status_code}   200
