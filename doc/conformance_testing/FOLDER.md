# openEHR Conformance Testing Specification (FOLDER)

| Author                                           | Version          | Comments                                                   |
|:------------------------------------------------:|:----------------:|:----------------------------------------------------------:|
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.1 (2019-06-18) | First version |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.2 (2019-09-02) | Added annex with REST API proposal |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 0.3 (2020-03-02) | Added test cases for update directory |
| Pablo Pazos Gutierrez <pablo.pazos@cabolabs.com> | 1.0 (2021-04-30) | Small corrections and fixes before publication |


# Index
<!-- 
  $ git clone https://github.com/Chris3606/TOC-Generator
  $ cd TOC-Generator
  $ python tocgen.py ../ehrbase/doc/conformance_testing
-->
<!--ts-->
<!--te-->


WIP



# Annex: proposal for FOLDER API

REF: https://wiki.vitagroup.ag/display/ETHERCIS/FOLDER+related+decisions+-+internal+implementation%2C+API+and+conformance

openEHR ticket: https://openehr.atlassian.net/browse/SPECPR-338?jql=status%20in%20(Analysis%2C%20Implementation%2C%20%22In%20Progress%22%2C%20%22In%20Review%22%2C%20Open%2C%20%22To%20Do%22)%20AND%20text%20~%20%22folder%22 

## Background

Current service model related for EHR.directory might lead to some complexities and issues for internal implementation and for the REST API. The goal is to discuss those issues and decide upon our internal implementation rules for FOLDER in general, and particularly for EHR.directory.

## A. Operations for the Service Model

Current operations are:

1. has_directory(ehr_id): Boolean
2. has_path(ehr_id, path): Boolean   // path from the root EHR.directory, also the idea of this path is that is defined by archetypes (this is another issue mentioned below)
3. create_directory(ehr_id, folder) //root directory
4. get_directory(ehr_id): FOLDER   // this might need to be VERSION<FOLDER>
5. get_directory_at_time(ehr_id, time): FOLDER   // this might also be VERSION<FOLDER>
6. update_directory(ehr_id, folder)   // folder is the full EHR.directory modified
7. delete_directory(ehr_id)
8. has_directory_version(ehr_id, version_uid): Boolean
9. get _directory_at_version(ehr_id, version_uid): FOLDER   // this might also be VERSION<FOLDER>
10. get_versioned_directory(ehr_id): VERSIONED_FOLDER


Issues:

1. To update, the client has to get the full EHR.directory structure, do changes on the client side (that means the management happens on the client), then the update needs to commit the whole structure with the modifications, this adds a lot of complexity on the client side and might not be the most natural way of managing an EHR.directory. (Luis: agree. FOLDERS in my view should be self standing structures and the system should allow for updating only one of them (e.g. change its items or details) as long as the edition of the FOLDER does not introduce inconsistencies in other FOLDERs.)
2. The has_path operation uses a path that should be defined by an archetype (mentioned by Thomas on the SEC Slack), my interpretation was those where instance paths considering the EHR.directory tree structure, which makes sense since it is impractical to have the whole EHR.directory structure defined by archetypes, and even some of those FOLDERs will be created in an ad-hoc way (IMO most will be created this way and using a generic archetype for definition, this is also the approach of Code24 which has been using folders for 6 years). Also paths are name-based, which makes them language dependent and creates the need for a constraint to have sibling FOLDERs with unique names. (Luis: the uid inherited from LOCATABLE (now optional 0..1) should be mandatory in our implementation for implementation reasons (1..1). At the moment is is the Primary Key in the Database, thus it is mandatory and unique).
3. Also related to paths, the current spec shows name-based paths to reference internal FOLDERs, but to reference to items in a FOLDER, the path uses numeric indexes, which seems inconsistent. One possibility is to use the item name on the path, the issue that creates is the items are really VERSIONED_OBJECT, which doesn't have a name, but the VERSIONED_OBJECT.latest_version() which is VERSION<T> has a name if T is LOCATABLE, so FOLDER.item[i].latest_version().data.name could be used in the path, but again, that creates another couple of issues: a. the name is not really form the item but from the contained data, and b. since the data could be updated, the name could change, changing the path. So the name-based path IMO is not really useful for any use case.
4. That last part makes me think of the name-based paths for FOLDERs, since FOLDER.name could also change, since FOLDERs could be created, renamed, deleted, etc. so the paths that were valid at one point could be invalid later. And one idea of these paths was to use them also for AQL, but IMO is almost impossible to get something very detailed from AQL using paths for FOLDERs, since I think most FOLDERs will be created ad-hoc and might not have a full structure defined by archetypes, only the basic structure, and maybe the new FOLDER.details structure, which could be archetyped but also could be used in an ad-hoc way.
5. Not issues from the operations but from the model: a. a FOLDER could have more than one parent, b. a FOLDER could have an ancestor as subfolder. These break the tree structure and openEHR needs to add some invariants to prevent this on the model.
6. We should clearly commit to implement FOLDERs directory as trees in the computational sense. The aim of this is to guarantee some performance issues (approx. O(lgn) when rearranged optimally) and avoid possible cycles that may derive from graph-like directories. This is in contradiction with some implementations that allow to virtually define graphs using the LINK class.
7. The operation "has_directory(ehr_id): Boolean" makes sense in EHRs, however for phenotyping in clinical research it may be actually the opposite. For example, a clinical study on back pain surgery may have a folder containing many EHRs rather than the other way around.


Proposals for operations:

1. has_directory(ehr_id): Boolean      // MAINTAIN
2. has_folder(ehr_id, folder_uid): Boolean   // NEW, uses uid not path
3. has_path(ehr_id, path): Boolean    // MAINTAIN - 1. spec needs to explicitly state "path" is an archetype path, not an instance path, 2. add an example with archetype paths to show how this operation will work, I think looks good on paper but it can be difficult to implement
4. create_directory(ehr_id, folder)     // MAINTAIN - discuss about the EHR and support self-standing FOLDERs without belonging to an EHR.
5. get_directory(ehr_id): FOLDER      // MAINTAIN
6. get_directory_at_time(ehr_id, time): FOLDER     // MAINTAIN
7. get_folder(ehr_id, folder_uid): FOLDER     // NEW, like cd + ls commands (this is optional since the information will be included in the result of get_directory). This will return the latest version of the directory provided that folder is not versioned.
8. create_folder(ehr_id, parent_folder_uid, new_folder)    // NEW, like mkdir command, if no parent_folder_uid is provided, the new_folder will be created under the EHR.directory
9. update_folder(ehr_id, updated_folder)    // NEW, allows to modify an individual FOLDER and what it contains, including name, details, folders and items. The updated_folder contains it's uid so there is no need for an extra parameter. If subfolders are deleted in the updated folder, they are deleted in the directory as well in EHRbase.
10. remove_folder(ehr_id, folder_uid)    // NEW, like rmdir -r (removes also subfolders and items)
11. add_item(ehr_id, folder_uid, versioned_object_uid)    // NEW, like the touch command, adds the item to the FOLDER.items via OBJECT_REF (TODO: verify OBJECT_REF needs namespace and type values but I think those could be set to default values set in the server config so we might not need to add extra parameters for those)
12. remove_item(ehr_id, folder_uid, versioned_object_uid)   // NEW, like the rm command, removes the versioned object reference from the FOLDER.items
13. delete_directory(ehr_id)    // MAINTAIN, but is contained in remove_folder when it is invoked with the EHR.directory.uid as folder_uid value
14. has_directory_version(ehr_id, version_uid): Boolean    // MAINTAIN
15. get _directory_at_version(ehr_id, version_uid): FOLDER   // MAINTAIN
16. get_versioned_directory(ehr_id): VERSIONED_FOLDER   // MAINTAIN


Notes:

1. Referencing FOLDERs by uid requires that the FOLDER.uid is set for all FOLDERs by the server. In the RM the uid is optional, so this could be an implementation constraint but still "spec valid".
2. The added operations seem to be a more natural way of managing with FOLDERs and their items like a user could do on a Linux Terminal and avoids the extra complexity of managing the whole EHR.directory on the client side for creating new FOLDERs and adding new references to items, also for deleting stuff, instead of having one big operation, we could map one action from a user to one operation on the Service Model. Still the create_folder() operation could receive a full FOLDER structure with subfolders and references to items, or just the basic data like name and details, and then it could be modified using the other operations, or the same create_folder() to add subfolders to it. That also adds more flexibility for client-side implementation.
3. About versioning, from the spec, the only versionable FOLDER is the EHR.directory, no internal FOLDERs could be versioned. Considering the new operations, each creation, update and removal of FOLDERs and items, would generate a new version of the containing EHR.directory, so this is an implementation consideration. Either way this should be done with the current operations in the SM spec, this is just to note that individual FOLDERs shouldn't be versioned (Code24 is versioning individual FOLDERs and they might propose a change request to make that valid in the spec, but won't be any time soon).
4. Using the parent_folder_id to create new FOLDERs prevent the generation of non-tree structures, since a. FOLDER.uid should always be assigned by the server and 2. only children to a given parent could be created.
5. TODO: we still need to discuss AQL requirements for FOLDERs and what will be needed to support those (from archetype modeling to internal implementation).


Reference FOLDER structure

<div align="center">

![](img/openEHR_Test_Folder_Data.png)
</div>