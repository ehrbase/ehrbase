= Testing CONTAINS in AQL queries

The purpose of this test set is to provide a wide range of queries over an absurdly comnplex Operational Template, nested.opt, which has five levels of archetype dependencies (archetype slots), which means we can use six levels of CONTAINS in these queries. The first level being EHR CONTAINS COMPOSITION, then the COMPOSITON has the rest five levels: COMPOSITION CONTAINS SECTION CONTAINS INSTRUCTION CONTAINS ITEM_TREE CONTAINS CLUSTER CONTAINS CLUSTER.

On these test sets we will:

 - [x] (nested_[1..6]*) use different levels of CONTAINS (1 to 6)
 - [x] (or3*, and3*, or2_and*, or2_and_p*, and*, or*) use different variations of parentheses in the CONTAINS expressions: a CONTAINS (b OR c); a CONTAINS (b AND c); etc.
 - [x] (noarchid*, nocompoarchid*) mention archetype IDs explicitly or not in the CONTAINS expression: a[archetype_id] CONTAINS b; a CONTAINS b[archetype_id]; etc.
 - [x] (nested_[1..6]*) mention middle types and archetype IDs or not: having a -> b -> c, then test: a CONTAINS c; a CONTAINS b CONTAINS c; a[archetype_id] CONTAINS c; a CONTAINS c[archetype_id]; etc.
